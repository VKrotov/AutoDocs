package com.autodocs.app.ui.screens.plan

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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PlanUiState(
    val isLoaded: Boolean = false,
    val car: Car? = null,
    val overview: PlanOverview? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class PlanViewModel(
    carRepository: CarRepository,
    private val planRepository: PlanRepository
) : ViewModel() {

    val state: StateFlow<PlanUiState> = carRepository.observeActiveCar()
        .flatMapLatest { car ->
            if (car == null) flowOf(PlanUiState(isLoaded = true))
            else planRepository.observePlan(car).map { PlanUiState(isLoaded = true, car = car, overview = it) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlanUiState())

    /** Заповнити стандартний регламент; [onDone] — кількість доданих правил. */
    fun seedTemplate(onDone: (Int) -> Unit) {
        val carId = state.value.car?.id ?: return
        viewModelScope.launch { onDone(planRepository.seedTemplate(carId)) }
    }
}

class PlanViewModelFactory(
    private val carRepository: CarRepository,
    private val planRepository: PlanRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = PlanViewModel(carRepository, planRepository) as T
}
