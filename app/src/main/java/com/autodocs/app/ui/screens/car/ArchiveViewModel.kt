package com.autodocs.app.ui.screens.car

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.autodocs.app.data.entity.Car
import com.autodocs.app.data.repository.CarRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class ArchiveViewModel(repository: CarRepository) : ViewModel() {
    val archivedCars: StateFlow<List<Car>> = repository.observeArchivedCars()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

class ArchiveViewModelFactory(private val repository: CarRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = ArchiveViewModel(repository) as T
}
