package com.autodocs.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.autodocs.app.data.entity.Car
import com.autodocs.app.data.repository.CarRepository
import com.autodocs.app.data.repository.PlanOverview
import com.autodocs.app.data.entity.PhotoKind
import com.autodocs.app.data.entity.PhotoOwnerType
import com.autodocs.app.data.repository.PhotoRepository
import com.autodocs.app.data.repository.DocumentRepository
import com.autodocs.app.data.plan.DocumentDeadlines
import com.autodocs.app.data.plan.DocumentDue
import com.autodocs.app.data.repository.PlanRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val repository: CarRepository,
    planRepository: PlanRepository,
    photoRepository: PhotoRepository,
    documentRepository: DocumentRepository,
    private val warnDays: () -> Int
) : ViewModel() {
    val activeCar: StateFlow<Car?> = repository.observeActiveCar()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** F11: план ТО активного авто — для блоку «Найближче ТО». */
    val plan: StateFlow<PlanOverview?> = repository.observeActiveCar()
        .flatMapLatest { car -> if (car == null) flowOf(null) else planRepository.observePlan(car) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** F26: скільки сторін техпаспорта сфотографовано (0..2). */
    val passportSides: StateFlow<Int> = repository.observeActiveCar()
        .flatMapLatest { car ->
            if (car == null) flowOf(0)
            else photoRepository.observeFor(PhotoOwnerType.CAR, car.id).map { list ->
                list.count { it.kind == PhotoKind.TECH_PASSPORT_FRONT || it.kind == PhotoKind.TECH_PASSPORT_BACK }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    /** Етап 9: поточні документи (найтерміновіший — першим) для рядка «Страховка й документи». */
    val documents: StateFlow<List<DocumentDue>> = repository.observeActiveCar()
        .flatMapLatest { car ->
            if (car == null) flowOf(emptyList())
            else documentRepository.observeDue(car.id, warnDays).map { DocumentDeadlines.current(it) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateMileage(carId: Long, mileage: Int) {
        viewModelScope.launch { repository.updateMileage(carId, mileage) }
    }

    fun archiveActiveCar(carId: Long) {
        viewModelScope.launch { repository.archiveCar(carId) }
    }
}

class HomeViewModelFactory(
    private val repository: CarRepository,
    private val planRepository: PlanRepository,
    private val photoRepository: PhotoRepository,
    private val documentRepository: DocumentRepository,
    private val warnDays: () -> Int
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        HomeViewModel(repository, planRepository, photoRepository, documentRepository, warnDays) as T
}
