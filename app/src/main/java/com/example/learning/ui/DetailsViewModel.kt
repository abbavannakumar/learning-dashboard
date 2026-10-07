package com.example.learning.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.learning.data.repository.CourseRepository
import com.example.learning.domain.Course
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DetailsState(val course: Course? = null, val loading: Boolean = true, val saving: Set<Long> = emptySet(), val error: String? = null)

class DetailsViewModel(private val repository: CourseRepository, courseId: Long) : ViewModel() {
    private val mutable = MutableStateFlow(DetailsState())
    val state = mutable.asStateFlow()
    init {
        viewModelScope.launch {
            try { repository.observeCourse(courseId).collect { course -> mutable.update { it.copy(course = course, loading = false) } } }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { mutable.update { it.copy(loading = false, error = "Unable to read this course. Go back and retry.") } }
        }
    }
    fun completeLesson(id: Long) {
        if (id in mutable.value.saving) return
        mutable.update { it.copy(saving = it.saving + id, error = null) }
        viewModelScope.launch {
            try { repository.completeLesson(id) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { mutable.update { it.copy(error = e.message ?: "Could not save completion. Please retry.") } }
            finally { mutable.update { it.copy(saving = it.saving - id) } }
        }
    }
}
