package com.example.learning.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.learning.domain.Course
import com.example.learning.domain.Lesson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.IOException

enum class ApiScenario { SUCCESS, FAILURE, EMPTY }
interface CourseApi { suspend fun fetchCourses(scenario: ApiScenario): List<Course> }

class MockCourseApi(private val context: Context) : CourseApi {
    override suspend fun fetchCourses(scenario: ApiScenario): List<Course> {
        delay(700)
        val connectivity = context.getSystemService(ConnectivityManager::class.java)
        val capabilities = connectivity.getNetworkCapabilities(connectivity.activeNetwork)
        if (capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) != true) {
            throw IOException("You're offline. Connect to refresh your courses.")
        }
        if (scenario == ApiScenario.FAILURE) throw IOException("The demo API is unavailable. Please retry.")
        if (scenario == ApiScenario.EMPTY) return emptyList()
        return withContext(Dispatchers.IO) {
            val json = context.assets.open("courses.json").bufferedReader().use { it.readText() }
            val array = JSONArray(json)
            List(array.length()) { index ->
                val item = array.getJSONObject(index)
                val id = item.getLong("id")
                val titles = item.getJSONArray("lessonTitles")
                val completed = item.getInt("completedLessons")
                Course(id, item.getString("title"), item.getString("instructor"),
                    List(titles.length()) { position ->
                        Lesson(id * 1000 + position + 1, id, titles.getString(position), position, position < completed)
                    })
            }
        }
    }
}
