package com.autodocs.app.ui.screens.record

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.autodocs.app.data.PhotoStorage
import com.autodocs.app.data.entity.PhotoKind
import com.autodocs.app.data.entity.PhotoOwnerType
import com.autodocs.app.data.entity.WorkItemCategory
import com.autodocs.app.data.entity.WorkType
import com.autodocs.app.data.repository.CarRepository
import com.autodocs.app.data.repository.ItemInput
import com.autodocs.app.data.repository.PhotoRef
import com.autodocs.app.data.repository.PhotoRepository
import com.autodocs.app.data.repository.ServiceRepository
import com.autodocs.app.ui.util.parseMoney
import com.autodocs.app.ui.util.todayUtcMillis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Чернетка однієї позиції у формі. [key] — стабільний ключ для списку. */
data class ItemDraft(
    val key: Long,
    val category: WorkItemCategory,
    val name: String = "",
    val workTypeId: Long? = null,
    val priceText: String = ""
)

data class RecordFormState(
    val recordId: Long? = null,
    val carId: Long? = null,
    val carName: String = "",
    val date: Long = todayUtcMillis(),
    val mileageText: String = "",
    val stoName: String = "",
    val notes: String = "",
    val items: List<ItemDraft> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val noActiveCar: Boolean = false,
    /** F05/F25: фото й скани запису (file:// у сховищі застосунку), до 5 шт. */
    val photos: List<PhotoRef> = emptyList(),
    val isImportingPhotos: Boolean = false,
    val photoMessage: String? = null
) {
    val photoSlotsLeft: Int get() = PhotoRepository.MAX_PER_RECORD - photos.size

    val total: Double get() = items.sumOf { parseMoney(it.priceText) ?: 0.0 }

    val filledItems: List<ItemDraft> get() = items.filter { it.name.isNotBlank() }

    /** Що заважає зберегти (null — усе гаразд). */
    val blocker: String?
        get() = when {
            mileageText.toIntOrNull() == null -> "Вкажи пробіг"
            filledItems.isEmpty() -> "Додай хоча б одну позицію з назвою"
            items.any { it.name.isBlank() && it.priceText.isNotBlank() } -> "У позиції з ціною немає назви"
            else -> null
        }
}

private val PRICE_REGEX = Regex("^\\d{0,7}([.,]\\d{0,2})?$")

class RecordFormViewModel(
    private val app: Application,
    private val carRepository: CarRepository,
    private val serviceRepository: ServiceRepository,
    private val photoRepository: PhotoRepository
) : ViewModel() {

    /** Фото, скопійовані в застосунок у цій формі, але ще не збережені в БД. */
    private val pendingFiles = mutableSetOf<String>()

    private val _state = MutableStateFlow(RecordFormState())
    val state: StateFlow<RecordFormState> = _state.asStateFlow()

    val workTypes: StateFlow<List<WorkType>> = serviceRepository.observeWorkTypes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stoNames: StateFlow<List<String>> = serviceRepository.observeStoNames()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var finishConsumed = false

    /** true лише при першому виклику після збереження — захист від повторної навігації. */
    fun consumeFinishEvent(): Boolean {
        if (finishConsumed) return false
        finishConsumed = true
        return true
    }

    private var nextKey = 1L
    private var initialized = false

    fun init(recordId: Long?) {
        if (initialized) return
        initialized = true
        viewModelScope.launch {
            if (recordId == null) {
                val car = carRepository.observeActiveCar().first()
                if (car == null) {
                    _state.update { it.copy(isLoading = false, noActiveCar = true) }
                    return@launch
                }
                _state.update {
                    it.copy(
                        carId = car.id,
                        carName = car.name,
                        mileageText = if (car.mileage > 0) car.mileage.toString() else "",
                        items = listOf(newDraft(WorkItemCategory.ROBOTA)),
                        isLoading = false
                    )
                }
            } else {
                val data = serviceRepository.getRecord(recordId)
                if (data == null) {
                    _state.update { it.copy(isLoading = false, isSaved = true) }
                    return@launch
                }
                val car = carRepository.getCar(data.record.carId)
                _state.update {
                    it.copy(
                        recordId = data.record.id,
                        carId = data.record.carId,
                        carName = car?.name.orEmpty(),
                        date = data.record.date,
                        mileageText = data.record.mileage.toString(),
                        stoName = data.record.stoName.orEmpty(),
                        notes = data.record.notes.orEmpty(),
                        items = data.items.map { item ->
                            ItemDraft(
                                key = nextKey++,
                                category = item.category,
                                name = item.customName.orEmpty(),
                                workTypeId = item.workTypeId,
                                priceText = if (item.price > 0) formatPriceForEdit(item.price) else ""
                            )
                        }.ifEmpty { listOf(newDraft(WorkItemCategory.ROBOTA)) },
                        photos = photoRepository.getFor(PhotoOwnerType.SERVICE_RECORD, data.record.id)
                            .map { p -> PhotoRef(p.uri, p.kind) },
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun formatPriceForEdit(price: Double): String =
        if (price % 1.0 == 0.0) price.toLong().toString() else "%.2f".format(java.util.Locale.US, price).trimEnd('0').replace('.', ',')

    private fun newDraft(category: WorkItemCategory) = ItemDraft(key = nextKey++, category = category)

    fun onDateChange(utcMillis: Long) = _state.update { it.copy(date = utcMillis) }

    fun onMileageChange(value: String) {
        if (value.length <= 7 && value.all { it.isDigit() }) _state.update { it.copy(mileageText = value) }
    }

    fun onStoChange(value: String) = _state.update { it.copy(stoName = value) }

    fun onNotesChange(value: String) = _state.update { it.copy(notes = value) }

    fun addItem(category: WorkItemCategory) = _state.update { it.copy(items = it.items + newDraft(category)) }

    fun removeItem(key: Long) = _state.update { s -> s.copy(items = s.items.filterNot { it.key == key }) }

    private fun updateItem(key: Long, transform: (ItemDraft) -> ItemDraft) =
        _state.update { s -> s.copy(items = s.items.map { if (it.key == key) transform(it) else it }) }

    fun onItemCategoryChange(key: Long, category: WorkItemCategory) =
        updateItem(key) { if (it.category == category) it else it.copy(category = category, workTypeId = null) }

    fun onItemNameChange(key: Long, name: String) = updateItem(key) { it.copy(name = name, workTypeId = null) }

    fun onItemPicked(key: Long, workType: WorkType) =
        updateItem(key) { it.copy(name = workType.name, workTypeId = workType.id, category = workType.category) }

    fun onItemPriceChange(key: Long, text: String) {
        val cleaned = text.replace(" ", "")
        if (PRICE_REGEX.matches(cleaned)) updateItem(key) { it.copy(priceText = cleaned) }
    }

    fun save() {
        val s = _state.value
        val carId = s.carId ?: return
        // isSaved: запис уже збережено — повторне натискання не має створювати дубль.
        if (s.blocker != null || s.isSaving || s.isSaved) return
        val mileage = s.mileageText.toIntOrNull() ?: return
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            val id = serviceRepository.saveRecord(
                recordId = s.recordId,
                carId = carId,
                date = s.date,
                mileage = mileage,
                stoName = s.stoName.trim().ifEmpty { null },
                notes = s.notes.trim().ifEmpty { null },
                items = s.filledItems.map {
                    ItemInput(
                        category = it.category,
                        name = it.name.trim(),
                        price = parseMoney(it.priceText) ?: 0.0,
                        workTypeId = it.workTypeId
                    )
                }
            )
            photoRepository.replaceFor(PhotoOwnerType.SERVICE_RECORD, id, _state.value.photos)
            pendingFiles.clear()
            _state.update { it.copy(isSaving = false, isSaved = true) }
        }
    }

    // ---- Фото ----

    /** Скопіювати вибрані/зняті фото в застосунок (не більше вільних місць). */
    fun onPhotosPicked(uris: List<Uri>, isScan: Boolean) {
        val free = _state.value.photoSlotsLeft
        if (free <= 0) return
        val take = uris.take(free)
        viewModelScope.launch {
            _state.update { it.copy(isImportingPhotos = true, photoMessage = null) }
            val kind = if (isScan) PhotoKind.SCAN else PhotoKind.RECORD_PHOTO
            val imported = take.mapNotNull { uri ->
                PhotoStorage.importImage(
                    app, uri,
                    prefix = if (isScan) "scan" else "rec",
                    maxSide = if (isScan) PhotoStorage.DOCUMENT_MAX_SIDE else 1600
                )
            }
            pendingFiles += imported
            val skipped = uris.size - take.size
            _state.update {
                it.copy(
                    photos = it.photos + imported.map { u -> PhotoRef(u, kind) },
                    isImportingPhotos = false,
                    photoMessage = when {
                        skipped > 0 -> "Додано ${imported.size}: не більше ${PhotoRepository.MAX_PER_RECORD} фото на запис"
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
        // Уже збережені фото видаляються з диска лише при збереженні запису.
        _state.update { it.copy(photos = it.photos.filterNot { p -> p.uri == uri }) }
    }

    override fun onCleared() {
        // Форму закрили без збереження — прибираємо скопійовані, але не збережені файли.
        pendingFiles.forEach { photoRepository.discardFile(it) }
        pendingFiles.clear()
    }
}

class RecordFormViewModelFactory(
    private val app: Application,
    private val carRepository: CarRepository,
    private val serviceRepository: ServiceRepository,
    private val photoRepository: PhotoRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        RecordFormViewModel(app, carRepository, serviceRepository, photoRepository) as T
}
