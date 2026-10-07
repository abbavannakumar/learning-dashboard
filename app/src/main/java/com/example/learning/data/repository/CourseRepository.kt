package com.example.learning.data.repository

import androidx.room.withTransaction
import com.example.learning.data.local.*
import com.example.learning.data.remote.ApiScenario
import com.example.learning.data.remote.CourseApi
import com.example.learning.domain.Course
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface CourseRepository {
    fun observeCourses(): Flow<List<Course>>
    fun observeCourse(id: Long): Flow<Course?>
    suspend fun refresh(scenario: ApiScenario = ApiScenario.SUCCESS): Boolean
    suspend fun completeLesson(id: Long)
}

class OfflineCourseRepository(private val api: CourseApi, private val database: LearningDatabase) : CourseRepository {
    private val dao = database.courseDao()
    private val refreshMutex = Mutex()
    override fun observeCourses() = dao.observeCourses().map { rows -> rows.map { it.toDomain() } }
    override fun observeCourse(id: Long) = dao.observeCourse(id).map { it?.toDomain() }

    // False means a successful empty response. Existing cached data is intentionally retained.
    override suspend fun refresh(scenario: ApiScenario): Boolean = refreshMutex.withLock {
        val courses = api.fetchCourses(scenario)
        database.withTransaction {
            dao.upsertCourses(courses.map { CourseEntity(it.id, it.title, it.instructor) })
            val lessons = courses.flatMap { it.lessons }
            dao.insertLessons(lessons.map { LessonEntity(it.id, it.courseId, it.title, it.position, it.completed) })
            lessons.forEach { dao.updateLessonMetadata(it.id, it.courseId, it.title, it.position) }
        }
        courses.isNotEmpty()
    }
    override suspend fun completeLesson(id: Long) {
        check(dao.completeLesson(id) == 1) { "Lesson no longer exists. Refresh and retry." }
    }
}
