package com.autodocs.app.ui.screens.documents

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.autodocs.app.data.entity.Car
import com.autodocs.app.data.entity.PhotoKind
import com.autodocs.app.data.entity.PhotoOwnerType
import com.autodocs.app.data.notify.NotifySettings
import com.autodocs.app.data.plan.DocumentDeadlines
import com.autodocs.app.data.plan.DocumentDue
import com.autodocs.app.data.repository.CarRepository
import com.autodocs.app.data.repository.DocumentRepository
import com.autodocs.app.data.repository.PhotoRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

data class DocumentsUiState(
    val isLoaded: Boolean = false,
    val car: Car? = null,
    /** Поточні документи (кожного виду — найсвіжіший): прострочені → скоро → решта. */
    val current: List<DocumentDue> = emptyList(),
    /** Замінені новішими — історія. */
    val previous: List<DocumentDue> = emptyList(),
    /** Скільки сторін техпаспорта сфотографовано (0..2). */
    val passportSides: Int = 0,
    val warnDays: Int = DocumentDeadlines.DEFAULT_WARN_DAYS
)

@OptIn(ExperimentalCoroutinesApi::class)
class DocumentsViewModel(
    private val app: Application,
    carRepository: CarRepository,
    documentRepository: DocumentRepository,
    photoRepository: PhotoRepository
) : ViewModel() {

    private fun warnDays() = NotifySettings.load(app).docDaysBefore

    val state: StateFlow<DocumentsUiState> = carRepository.observeActiveCar()
        .flatMapLatest { car ->
            if (car == null) flowOf(DocumentsUiState(isLoaded = true))
            else combine(
                documentRepository.observeDue(car.id, ::warnDays),
                photoRepository.observeFor(PhotoOwnerType.CAR, car.id)
            ) { docs, photos ->
                DocumentsUiState(
                    isLoaded = true,
                    car = car,
                    current = DocumentDeadlines.current(docs),
                    previous = DocumentDeadlines.previous(docs),
                    passportSides = photos.count { it.kind == PhotoKind.TECH_PASSPORT_FRONT || it.kind == PhotoKind.TECH_PASSPORT_BACK },
                    warnDays = warnDays()
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DocumentsUiState())
}

class DocumentsViewModelFactory(
    private val app: Application,
    private val carRepository: CarRepository,
    private val documentRepository: DocumentRepository,
    private val photoRepository: PhotoRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        DocumentsViewModel(app, carRepository, documentRepository, photoRepository) as T
}
