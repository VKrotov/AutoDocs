package com.autodocs.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.autodocs.app.data.entity.Car
import com.autodocs.app.data.repository.CarRepository
import com.autodocs.app.data.repository.PlanOverview
import com.autodocs.app.data.repository.PlanRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val repository: CarRepository,
    planRepository: PlanRepository
) : ViewModel() {
    val activeCar: StateFlow<Car?> = repository.observeActiveCar()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** F11: план ТО активного авто — для блоку «Найближче ТО». */
    val plan: StateFlow<PlanOverview?> = repository.observeActiveCar()
        .flatMapLatest { car -> if (car == null) flowOf(null) else planRepository.observePlan(car) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun updateMileage(carId: Long, mileage: Int) {
        viewModelScope.launch { repository.updateMileage(carId, mileage) }
    }

    fun archiveActiveCar(carId: Long) {
        viewModelScope.launch { repository.archiveCar(carId) }
    }
}

class HomeViewModelFactory(
    private val repository: CarRepository,
    private val planRepository: PlanRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = HomeViewModel(repository, planRepository) as T
}
