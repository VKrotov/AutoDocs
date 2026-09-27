package com.autodocs.app.ui.screens.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.autodocs.app.data.entity.Car
import com.autodocs.app.data.entity.RecordWithItems
import com.autodocs.app.data.repository.CarRepository
import com.autodocs.app.data.repository.PhotoRepository
import com.autodocs.app.data.repository.ServiceRepository
import com.autodocs.app.data.stats.ExpenseStats
import com.autodocs.app.data.stats.JournalFilter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class JournalState(
    val car: Car? = null,
    /** Записи після пошуку/фільтра. */
    val records: List<RecordWithItems> = emptyList(),
    /** Скільки записів усього (без фільтра). */
    val totalCount: Int = 0,
    /** Роки з записами (нові → старі) — для фільтра. */
    val years: List<Int> = emptyList(),
    val filter: JournalFilter = JournalFilter(),
    /** recordId → кількість фото. */
    val photoCounts: Map<Long, Int> = emptyMap(),
    val isLoaded: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class JournalViewModel(
    carRepository: CarRepository,
    serviceRepository: ServiceRepository,
    photoRepository: PhotoRepository
) : ViewModel() {
    private val filter = MutableStateFlow(JournalFilter())

    val state: StateFlow<JournalState> = carRepository.observeActiveCar()
        .flatMapLatest { car ->
            if (car == null) {
                flowOf(JournalState(car = null, isLoaded = true))
            } else {
                combine(
                    serviceRepository.observeRecords(car.id),
                    photoRepository.observeRecordPhotoCounts(),
                    filter
                ) { records, counts, f ->
                    val years = ExpenseStats.years(records)
                    // Рік, якого вже немає (видалили записи), — скидаємо.
                    val effective = if (f.year != null && f.year !in years) f.copy(year = null) else f
                    JournalState(
                        car = car,
                        records = effective.apply(records),
                        totalCount = records.size,
                        years = years,
                        filter = effective,
                        photoCounts = counts,
                        isLoaded = true
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), JournalState())

    fun setQuery(query: String) = filter.update { it.copy(query = query) }

    fun setYear(year: Int?) = filter.update { it.copy(year = year) }

    fun clearFilter() { filter.value = JournalFilter() }
}

class JournalViewModelFactory(
    private val carRepository: CarRepository,
    private val serviceRepository: ServiceRepository,
    private val photoRepository: PhotoRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        JournalViewModel(carRepository, serviceRepository, photoRepository) as T
}
