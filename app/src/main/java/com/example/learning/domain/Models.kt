package com.example.learning.domain

import kotlin.math.roundToInt

data class Lesson(val id: Long, val courseId: Long, val title: String, val position: Int, val completed: Boolean)
data class Course(val id: Long, val title: String, val instructor: String, val lessons: List<Lesson>) {
    val progress: Int get() = calculateProgress(lessons)
    val completedLessons: Int get() = lessons.count { it.completed }
}

fun calculateProgress(lessons: List<Lesson>): Int =
    if (lessons.isEmpty()) 0 else (lessons.count { it.completed } * 100.0 / lessons.size).roundToInt()
