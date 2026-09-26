package com.autodocs.app.ui.screens.record

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.autodocs.app.data.entity.RecordWithItems
import com.autodocs.app.data.entity.Photo
import com.autodocs.app.data.entity.PhotoOwnerType
import com.autodocs.app.data.repository.PhotoRepository
import com.autodocs.app.data.repository.ServiceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** null — ще вантажиться; Loaded(null) — запис не знайдено (наприклад, щойно видалений). */
data class RecordDetailState(val isLoaded: Boolean = false, val record: RecordWithItems? = null)

class RecordDetailViewModel(
    private val recordId: Long,
    private val repository: ServiceRepository,
    photoRepository: PhotoRepository
) : ViewModel() {
    /** Фото й скани запису (F05/F25). */
    val photos: StateFlow<List<Photo>> = photoRepository.observeFor(PhotoOwnerType.SERVICE_RECORD, recordId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val state: StateFlow<RecordDetailState> = repository.observeRecord(recordId)
        .map { RecordDetailState(isLoaded = true, record = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RecordDetailState())

    /** Після видалення потік віддасть null — екран сам закриється (див. RecordDetailScreen). */
    fun delete() {
        viewModelScope.launch { repository.deleteRecord(recordId) }
    }
}

class RecordDetailViewModelFactory(
    private val recordId: Long,
    private val repository: ServiceRepository,
    private val photoRepository: PhotoRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = RecordDetailViewModel(recordId, repository, photoRepository) as T
}
