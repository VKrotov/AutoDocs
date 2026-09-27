package com.autodocs.app.ui.screens.documents

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.autodocs.app.data.PhotoStorage
import com.autodocs.app.data.entity.CarDocument
import com.autodocs.app.data.entity.DocumentType
import com.autodocs.app.data.entity.PhotoKind
import com.autodocs.app.data.entity.PhotoOwnerType
import com.autodocs.app.data.notify.NotifySettings
import com.autodocs.app.data.plan.DocumentDeadlines
import com.autodocs.app.data.plan.DocumentDue
import com.autodocs.app.data.repository.CarRepository
import com.autodocs.app.data.repository.DocumentRepository
import com.autodocs.app.data.repository.PhotoRef
import com.autodocs.app.data.repository.PhotoRepository
import com.autodocs.app.data.repository.PlanRepository
import com.autodocs.app.ui.util.parseMoney
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class DocumentFormState(
    val isLoading: Boolean = true,
    val docId: Long? = null,
    val carId: Long? = null,
    val carName: String = "",
    val type: DocumentType = DocumentType.OSAGO,
    val title: String = "",
    val number: String = "",
    val company: String = "",
    val validFrom: Long? = null,
    val validUntil: Long? = null,
    val priceText: String = "",
    val notes: String = "",
    val photos: List<PhotoRef> = emptyList(),
    /** Фото, які вже є в БД (їх можна відкрити в переглядачі). */
    val savedPhotos: List<String> = emptyList(),
    val isImportingPhotos: Boolean = false,
    val photoMessage: String? = null,
    /** Стан документа на сьогодні — для підказки «діє ще N днів». */
    val preview: DocumentDue? = null,
    val isSaving: Boolean = false,
    val isFinished: Boolean = false
) {
    val isNew: Boolean get() = docId == null
    val photoSlotsLeft: Int get() = PhotoRepository.MAX_PER_RECORD - photos.size
    val blocker: String?
        get() = when {
            type == DocumentType.OTHER && title.isBlank() -> "Вкажи назву документа"
            validUntil == null -> "Вкажи, до якої дати діє документ"
            validFrom != null && validFrom > validUntil -> "Дата «діє з» пізніша за «діє до»"
            else -> null
        }
}

private val PRICE_REGEX = Regex("^\\d{0,7}([.,]\\d{0,2})?$")

class DocumentFormViewModel(
    private val app: Application,
    private val carRepository: CarRepository,
    private val documentRepository: DocumentRepository,
    private val photoRepository: PhotoRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DocumentFormState())
    val state: StateFlow<DocumentFormState> = _state.asStateFlow()

    val companies: StateFlow<List<String>> = documentRepository.observeCompanies()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Фото, скопійовані в застосунок у цій формі, але ще не збережені в БД. */
    private val pendingFiles = mutableSetOf<String>()
    private var initialized = false

    companion object {
        /** Типовий строк дії: рік мінус день (поліс з 15.03 діє до 14.03 наступного року включно). */
        fun defaultUntil(fromUtc: Long): Long =
            PlanRepository.localDateToUtc(PlanRepository.utcToLocalDate(fromUtc).plusYears(1).minusDays(1))

        fun nextDay(utc: Long): Long = PlanRepository.localDateToUtc(PlanRepository.utcToLocalDate(utc).plusDays(1))
    }

    /** [docId] — редагування; [presetType] — вид нового документа; [renewFromId] — «Продовжити» наявний. */
    fun init(docId: Long?, presetType: DocumentType?, renewFromId: Long?) {
        if (initialized) return
        initialized = true
        viewModelScope.launch {
            if (docId != null) {
                val doc = documentRepository.get(docId)
                if (doc == null) {
                    _state.update { it.copy(isLoading = false, isFinished = true) }
                    return@launch
                }
                val car = carRepository.getCar(doc.carId)
                val photos = photoRepository.getFor(PhotoOwnerType.CAR_DOCUMENT, doc.id)
                _state.update {
                    it.copy(
                        isLoading = false,
                        docId = doc.id,
                        carId = doc.carId,
                        carName = car?.name.orEmpty(),
                        type = doc.type,
                        title = doc.title,
                        number = doc.number,
                        company = doc.company,
                        validFrom = doc.validFrom,
                        validUntil = doc.validUntil,
                        priceText = doc.price?.let(::formatPriceForEdit).orEmpty(),
                        notes = doc.notes.orEmpty(),
                        photos = photos.map { p -> PhotoRef(p.uri, p.kind) },
                        savedPhotos = photos.map { p -> p.uri }
                    )
                }
            } else {
                val car = carRepository.observeActiveCar().first()
                if (car == null) {
                    _state.update { it.copy(isLoading = false, isFinished = true) }
                    return@launch
                }
                val source = renewFromId?.let { documentRepository.get(it) }
                _state.update {
                    if (source != null) {
                        val from = nextDay(source.validUntil)
                        it.copy(
                            isLoading = false,
                            carId = car.id,
                            carName = car.name,
                            type = source.type,
                            title = source.title,
                            company = source.company,
                            validFrom = from,
                            validUntil = defaultUntil(from)
                        )
                    } else {
                        it.copy(isLoading = false, carId = car.id, carName = car.name, type = presetType ?: DocumentType.OSAGO)
                    }
                }
            }
            recompute()
        }
    }

    private fun formatPriceForEdit(price: Double): String =
        if (price % 1.0 == 0.0) price.toLong().toString() else "%.2f".format(java.util.Locale.US, price).trimEnd('0').replace('.', ',')

    private fun draft(s: DocumentFormState, validUntil: Long): CarDocument = CarDocument(
        id = s.docId ?: 0,
        carId = s.carId ?: 0,
        type = s.type,
        title = if (s.type == DocumentType.OTHER) s.title.trim() else "",
        number = s.number.trim(),
        company = s.company.trim(),
        validFrom = s.validFrom,
        validUntil = validUntil,
        price = parseMoney(s.priceText)?.takeIf { it > 0 },
        notes = s.notes.trim().ifEmpty { null }
    )

    private fun recompute() {
        val s = _state.value
        val until = s.validUntil
        val preview = if (until != null) {
            DocumentDeadlines.evaluate(listOf(draft(s, until)), LocalDate.now(), NotifySettings.load(app).docDaysBefore).first()
        } else null
        _state.update { it.copy(preview = preview) }
    }

    private fun edit(transform: (DocumentFormState) -> DocumentFormState) {
        _state.update(transform)
        recompute()
    }

    fun onType(value: DocumentType) = edit { it.copy(type = value) }
    fun onTitle(value: String) = edit { it.copy(title = value) }
    fun onNumber(value: String) = edit { it.copy(number = value) }
    fun onCompany(value: String) = edit { it.copy(company = value) }
    fun onNotes(value: String) = edit { it.copy(notes = value) }

    /** Дата «з»: якщо «до» ще не задано — підставляємо рік мінус день. */
    fun onValidFrom(utc: Long?) = edit {
        it.copy(validFrom = utc, validUntil = it.validUntil ?: utc?.let(::defaultUntil))
    }

    fun onValidUntil(utc: Long?) = edit { it.copy(validUntil = utc) }

    fun onPrice(text: String) {
        val cleaned = text.replace(" ", "")
        if (PRICE_REGEX.matches(cleaned)) edit { it.copy(priceText = cleaned) }
    }

    fun save() {
        val s = _state.value
        val until = s.validUntil ?: return
        if (s.carId == null || s.blocker != null || s.isSaving || s.isFinished) return
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            documentRepository.save(draft(s, until), _state.value.photos)
            pendingFiles.clear()
            _state.update { it.copy(isSaving = false, isFinished = true) }
        }
    }

    fun delete() {
        val id = _state.value.docId ?: return
        viewModelScope.launch {
            documentRepository.delete(id)
            _state.update { it.copy(isFinished = true) }
        }
    }

    // ---- Фото поліса ----

    fun onPhotosPicked(uris: List<Uri>, isScan: Boolean) {
        val free = _state.value.photoSlotsLeft
        if (free <= 0) return
        val take = uris.take(free)
        viewModelScope.launch {
            _state.update { it.copy(isImportingPhotos = true, photoMessage = null) }
            val kind = if (isScan) PhotoKind.SCAN else PhotoKind.RECORD_PHOTO
            val imported = take.mapNotNull { uri ->
                PhotoStorage.importImage(app, uri, prefix = "doc", maxSide = PhotoStorage.DOCUMENT_MAX_SIDE)
            }
            pendingFiles += imported
            _state.update {
                it.copy(
                    photos = it.photos + imported.map { u -> PhotoRef(u, kind) },
                    isImportingPhotos = false,
                    photoMessage = when {
                        uris.size > take.size -> "Додано ${imported.size}: не більше ${PhotoRepository.MAX_PER_RECORD} фото"
                        imported.size < take.size -> "Не всі фото вдалося відкрити"
                        else -> null
                    }
                )
            }
        }
    }

    fun onPhotoError(message: String) = _state.update { it.copy(photoMessage = message) }

    fun removePhoto(uri: String) {
        if (uri in pendingFiles) {
            pendingFiles -= uri
            photoRepository.discardFile(uri)
        }
        _state.update { it.copy(photos = it.photos.filterNot { p -> p.uri == uri }) }
    }

    override fun onCleared() {
        pendingFiles.forEach { photoRepository.discardFile(it) }
        pendingFiles.clear()
    }
}

class DocumentFormViewModelFactory(
    private val app: Application,
    private val carRepository: CarRepository,
    private val documentRepository: DocumentRepository,
    private val photoRepository: PhotoRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        DocumentFormViewModel(app, carRepository, documentRepository, photoRepository) as T
}
