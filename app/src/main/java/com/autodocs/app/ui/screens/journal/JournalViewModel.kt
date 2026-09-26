package com.autodocs.app.ui.screens.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.autodocs.app.data.entity.Car
import com.autodocs.app.data.entity.RecordWithItems
import com.autodocs.app.data.repository.CarRepository
import com.autodocs.app.data.repository.PhotoRepository
import com.autodocs.app.data.repository.ServiceRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class JournalState(
    val car: Car? = null,
    val records: List<RecordWithItems> = emptyList(),
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
    val state: StateFlow<JournalState> = carRepository.observeActiveCar()
        .flatMapLatest { car ->
            if (car == null) {
                flowOf(JournalState(car = null, isLoaded = true))
            } else {
                combine(serviceRepository.observeRecords(car.id), photoRepository.observeRecordPhotoCounts()) { records, counts ->
                    JournalState(car, records, counts, isLoaded = true)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), JournalState())
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
