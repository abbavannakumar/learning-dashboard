package com.example.learning.data.local

import androidx.room.*
import com.example.learning.domain.Course
import com.example.learning.domain.Lesson
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "courses")
data class CourseEntity(@PrimaryKey val id: Long, val title: String, val instructor: String)

@Entity(tableName = "lessons", foreignKeys = [ForeignKey(
    entity = CourseEntity::class, parentColumns = ["id"], childColumns = ["courseId"],
    onDelete = ForeignKey.CASCADE
)], indices = [Index("courseId")])
data class LessonEntity(
    @PrimaryKey val id: Long, val courseId: Long, val title: String,
    val position: Int, val completed: Boolean
)

data class CourseWithLessons(
    @Embedded val course: CourseEntity,
    @Relation(parentColumn = "id", entityColumn = "courseId") val lessons: List<LessonEntity>
) {
    fun toDomain() = Course(course.id, course.title, course.instructor,
        lessons.sortedBy { it.position }.map { Lesson(it.id, it.courseId, it.title, it.position, it.completed) })
}

@Dao
interface CourseDao {
    @Transaction @Query("SELECT * FROM courses ORDER BY id")
    fun observeCourses(): Flow<List<CourseWithLessons>>
    @Transaction @Query("SELECT * FROM courses WHERE id = :id")
    fun observeCourse(id: Long): Flow<CourseWithLessons?>
    @Upsert suspend fun upsertCourses(courses: List<CourseEntity>)
    // IGNORE deliberately preserves completion when the mock API returns its original seed again.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLessons(lessons: List<LessonEntity>)
    @Query("UPDATE lessons SET title = :title, position = :position WHERE id = :id AND courseId = :courseId")
    suspend fun updateLessonMetadata(id: Long, courseId: Long, title: String, position: Int)
    @Query("UPDATE lessons SET completed = 1 WHERE id = :id")
    suspend fun completeLesson(id: Long): Int
}

@Database(entities = [CourseEntity::class, LessonEntity::class], version = 1, exportSchema = true)
abstract class LearningDatabase : RoomDatabase() {
    abstract fun courseDao(): CourseDao
}
