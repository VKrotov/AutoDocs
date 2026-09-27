package com.autodocs.app.ui.screens.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.autodocs.app.data.entity.Car
import com.autodocs.app.data.repository.CarRepository
import com.autodocs.app.data.repository.MileageRepository
import com.autodocs.app.data.repository.ServiceRepository
import com.autodocs.app.data.stats.ExpenseStats
import com.autodocs.app.data.stats.ExpenseSummary
import com.autodocs.app.data.stats.StatsPeriod
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class ExpenseStatsState(
    val car: Car? = null,
    /** Роки з записами (нові → старі). */
    val years: List<Int> = emptyList(),
    val summary: ExpenseSummary? = null,
    val isLoaded: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class ExpenseStatsViewModel(
    carRepository: CarRepository,
    serviceRepository: ServiceRepository,
    mileageRepository: MileageRepository,
    private val today: () -> LocalDate = { LocalDate.now() }
) : ViewModel() {
    /** null — ще не вибрано, беремо період за замовчуванням. */
    private val chosen = MutableStateFlow<StatsPeriod?>(null)

    val state: StateFlow<ExpenseStatsState> = carRepository.observeActiveCar()
        .flatMapLatest { car ->
            if (car == null) flowOf(ExpenseStatsState(isLoaded = true))
            else combine(serviceRepository.observeRecords(car.id), mileageRepository.observeHistory(car), chosen) { records, mileage, period ->
                val years = ExpenseStats.years(records)
                val p = period?.takeIf { it.isAll || it.year in years } ?: ExpenseStats.defaultPeriod(records, today())
                ExpenseStatsState(car, years, ExpenseStats.summarize(records, p, mileage), isLoaded = true)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ExpenseStatsState())

    fun selectPeriod(period: StatsPeriod) { chosen.value = period }
}

class ExpenseStatsViewModelFactory(
    private val carRepository: CarRepository,
    private val serviceRepository: ServiceRepository,
    private val mileageRepository: MileageRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        ExpenseStatsViewModel(carRepository, serviceRepository, mileageRepository) as T
}
