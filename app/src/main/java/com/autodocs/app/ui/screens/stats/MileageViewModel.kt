package com.autodocs.app.ui.screens.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.autodocs.app.data.entity.Car
import com.autodocs.app.data.plan.MileageForecast
import com.autodocs.app.data.plan.MileagePoint
import com.autodocs.app.data.repository.CarRepository
import com.autodocs.app.data.repository.MileageRepository
import com.autodocs.app.data.stats.MileageHistory
import com.autodocs.app.data.stats.MileageHistoryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class MileageRange(val label: String, val days: Long?) {
    YEAR("Рік", 365),
    THREE_YEARS("3 роки", 3 * 365),
    ALL("Усе", null)
}

data class MileageState(
    val car: Car? = null,
    /** Усі точки (старі → нові), з позначкою підозрілих. */
    val points: List<MileageHistoryPoint> = emptyList(),
    val kmPerDay: Double? = null,
    val lastYearKm: Int? = null,
    val range: MileageRange = MileageRange.ALL,
    val isLoaded: Boolean = false
) {
    /** Точки для графіка: лише узгоджені й у межах вибраного періоду. */
    fun chartPoints(today: LocalDate = LocalDate.now()): List<MileageHistoryPoint> {
        val from = range.days?.let { today.minusDays(it) }
        return points.filter { !it.suspicious && (from == null || !it.date.isBefore(from)) }
    }

    val suspiciousCount: Int get() = points.count { it.suspicious }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MileageViewModel(
    carRepository: CarRepository,
    private val mileageRepository: MileageRepository,
    private val today: () -> LocalDate = { LocalDate.now() }
) : ViewModel() {
    private val range = MutableStateFlow(MileageRange.ALL)

    val state: StateFlow<MileageState> = carRepository.observeActiveCar()
        .flatMapLatest { car ->
            if (car == null) flowOf(MileageState(isLoaded = true))
            else combine(mileageRepository.observeHistory(car), range) { points, r ->
                val now = today()
                val ok = points.filterNot { it.suspicious }.map { MileagePoint(it.date, it.mileage) }
                MileageState(
                    car = car,
                    points = points,
                    kmPerDay = MileageForecast.kmPerDay(ok, now),
                    lastYearKm = MileageHistory.drivenBetween(points, now.minusDays(365), now),
                    range = r,
                    isLoaded = true
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MileageState())

    fun setRange(r: MileageRange) { range.value = r }

    fun deleteEntry(entryId: Long) {
        viewModelScope.launch { mileageRepository.deleteEntry(entryId) }
    }
}

class MileageViewModelFactory(
    private val carRepository: CarRepository,
    private val mileageRepository: MileageRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = MileageViewModel(carRepository, mileageRepository) as T
}
