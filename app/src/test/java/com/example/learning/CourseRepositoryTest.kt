package com.example.learning

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.learning.data.local.LearningDatabase
import com.example.learning.data.remote.ApiScenario
import com.example.learning.data.remote.CourseApi
import com.example.learning.data.repository.OfflineCourseRepository
import com.example.learning.domain.Course
import com.example.learning.domain.Lesson
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = android.app.Application::class)
class CourseRepositoryTest {
    private val seed = Course(1, "Python", "John", List(4) { index ->
        Lesson(index + 1L, 1, "Lesson ${index + 1}", index, index == 0)
    })

    @Test fun completionSurvivesRefreshApiFailureAndDatabaseReopen() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "repository-${System.nanoTime()}.db"
        var failing = false
        val api = object : CourseApi {
            override suspend fun fetchCourses(scenario: ApiScenario): List<Course> {
                if (failing) throw IOException("Offline")
                return listOf(seed)
            }
        }
        var database = Room.databaseBuilder(context, LearningDatabase::class.java, name).build()
        try {
            var repository = OfflineCourseRepository(api, database)
            repository.refresh()
            assertEquals(25, repository.observeCourses().first().single().progress)
            repository.completeLesson(2)
            assertEquals(50, repository.observeCourse(1).first()!!.progress)
            // The original API still says this lesson is pending.
            repository.refresh()
            assertTrue(repository.observeCourse(1).first()!!.lessons[1].completed)
            assertEquals(50, repository.observeCourses().first().single().progress)
            failing = true
            try {
                repository.refresh()
                fail("Expected an offline error")
            } catch (_: IOException) { /* cached data must remain */ }
            database.close()
            database = Room.databaseBuilder(context, LearningDatabase::class.java, name).build()
            repository = OfflineCourseRepository(api, database)
            val restored = repository.observeCourses().first().single()
            assertEquals(50, restored.progress)
            assertTrue(restored.lessons[1].completed)
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }
}
