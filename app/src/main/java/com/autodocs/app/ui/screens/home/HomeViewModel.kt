package com.autodocs.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.autodocs.app.data.entity.Car
import com.autodocs.app.data.repository.CarRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(private val repository: CarRepository) : ViewModel() {
    val activeCar: StateFlow<Car?> = repository.observeActiveCar()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun updateMileage(carId: Long, mileage: Int) {
        viewModelScope.launch { repository.updateMileage(carId, mileage) }
    }

    fun archiveActiveCar(carId: Long) {
        viewModelScope.launch { repository.archiveCar(carId) }
    }
}

class HomeViewModelFactory(private val repository: CarRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = HomeViewModel(repository) as T
}
