package com.example.learning.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.learning.data.remote.ApiScenario
import com.example.learning.data.repository.CourseRepository
import com.example.learning.domain.Course
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardState(
    val courses: List<Course> = emptyList(), val loading: Boolean = true,
    val refreshing: Boolean = false, val error: String? = null,
    val emptyResponse: Boolean = false, val scenario: ApiScenario = ApiScenario.SUCCESS
)

class DashboardViewModel(private val repository: CourseRepository) : ViewModel() {
    private val mutable = MutableStateFlow(DashboardState())
    val state = mutable.asStateFlow()
    init {
        viewModelScope.launch {
            try { repository.observeCourses().collect { courses -> mutable.update { it.copy(courses = courses) } } }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { mutable.update { it.copy(loading = false, error = "Unable to read saved courses. Restart the app and retry.") } }
        }
        refresh()
    }
    fun setScenario(scenario: ApiScenario) { mutable.update { it.copy(scenario = scenario) } }
    fun refresh() {
        if (mutable.value.refreshing) return
        mutable.update { it.copy(refreshing = true, error = null, emptyResponse = false) }
        val scenario = mutable.value.scenario
        viewModelScope.launch {
            try {
                val hasCourses = repository.refresh(scenario)
                mutable.update { it.copy(loading = false, refreshing = false, emptyResponse = !hasCourses) }
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) {
                mutable.update { it.copy(loading = false, refreshing = false, error = e.message ?: "Unable to refresh courses. Please retry.") }
            }
        }
    }
}
