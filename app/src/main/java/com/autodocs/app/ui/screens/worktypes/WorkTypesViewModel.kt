package com.autodocs.app.ui.screens.worktypes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.autodocs.app.data.entity.WorkItemCategory
import com.autodocs.app.data.entity.WorkType
import com.autodocs.app.data.repository.ServiceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WorkTypesViewModel(private val repository: ServiceRepository) : ViewModel() {
    val workTypes: StateFlow<List<WorkType>> = repository.observeWorkTypes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** [onResult] отримує false, якщо такий пункт уже є. */
    fun add(name: String, category: WorkItemCategory, onResult: (Boolean) -> Unit) {
        viewModelScope.launch { onResult(repository.addWorkType(name, category)) }
    }

    fun rename(workType: WorkType, newName: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch { onResult(repository.renameWorkType(workType, newName)) }
    }

    fun delete(workType: WorkType) {
        viewModelScope.launch { repository.deleteWorkType(workType) }
    }
}

class WorkTypesViewModelFactory(private val repository: ServiceRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = WorkTypesViewModel(repository) as T
}
