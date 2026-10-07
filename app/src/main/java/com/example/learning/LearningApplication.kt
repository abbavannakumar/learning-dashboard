package com.example.learning

import android.app.Application
import androidx.room.Room
import com.example.learning.data.local.LearningDatabase
import com.example.learning.data.remote.MockCourseApi
import com.example.learning.data.repository.AuthRepository
import com.example.learning.data.repository.OfflineCourseRepository

class LearningApplication : Application() {
    val database by lazy { Room.databaseBuilder(this, LearningDatabase::class.java, "learning.db").build() }
    val courses by lazy { OfflineCourseRepository(MockCourseApi(this), database) }
    val auth by lazy { AuthRepository() }
}
