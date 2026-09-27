package com.autodocs.app.ui.screens.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.autodocs.app.data.entity.PlannedTask
import com.autodocs.app.data.entity.WorkItemCategory
import com.autodocs.app.data.entity.WorkType
import com.autodocs.app.data.plan.DuePlan
import com.autodocs.app.data.plan.OneOffPlanner
import com.autodocs.app.data.repository.CarRepository
import com.autodocs.app.data.repository.PlanOverview
import com.autodocs.app.data.repository.PlanRepository
import com.autodocs.app.data.repository.ServiceRepository
import com.autodocs.app.ui.util.todayUtcMillis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class TaskEditState(
    val isLoading: Boolean = true,
    val taskId: Long? = null,
    val carId: Long? = null,
    val carName: String = "",
    val title: String = "",
    val dueMileageText: String = "",
    val dueDate: Long? = null,
    val notes: String = "",
    val doneAt: Long? = null,
    val doneRecordId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    /** Орієнтовний пробіг на сьогодні — підказка для поля «до пробігу». */
    val currentMileage: Int = 0,
    val preview: DuePlan? = null,
    val isSaving: Boolean = false,
    val isFinished: Boolean = false,
    /** Подія: план збережено — відкрити новий запис журналу для нього (id плану). */
    val logToJournalTaskId: Long? = null
) {
    val isNew: Boolean get() = taskId == null
    val isDone: Boolean get() = doneAt != null
    val blocker: String? get() = if (title.isBlank()) "Вкажи, що треба зробити" else null
}

class TaskEditViewModel(
    private val carRepository: CarRepository,
    private val planRepository: PlanRepository,
    serviceRepository: ServiceRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TaskEditState())
    val state: StateFlow<TaskEditState> = _state.asStateFlow()

    /** Підказки назви — роботи з довідника. */
    val workTypes: StateFlow<List<WorkType>> = serviceRepository.observeWorkTypes()
        .map { list -> list.filter { it.category == WorkItemCategory.ROBOTA } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var overview: PlanOverview? = null
    private var initialized = false
    private var logConsumed = false

    fun init(taskId: Long?) {
        if (initialized) return
        initialized = true
        viewModelScope.launch {
            val car = carRepository.observeActiveCar().first()
            if (car == null) {
                _state.update { it.copy(isLoading = false, isFinished = true) }
                return@launch
            }
            overview = planRepository.observePlan(car).first()
            val current = overview?.currentMileage ?: car.mileage
            if (taskId == null) {
                _state.update { it.copy(isLoading = false, carId = car.id, carName = car.name, currentMileage = current) }
            } else {
                val task = planRepository.getTask(taskId)
                if (task == null) {
                    _state.update { it.copy(isLoading = false, isFinished = true) }
                    return@launch
                }
                _state.update {
                    it.copy(
                        isLoading = false,
                        taskId = task.id,
                        carId = task.carId,
                        carName = car.name,
                        title = task.title,
                        dueMileageText = task.dueMileage?.toString().orEmpty(),
                        dueDate = task.dueDate,
                        notes = task.notes.orEmpty(),
                        doneAt = task.doneAt,
                        doneRecordId = task.doneRecordId,
                        createdAt = task.createdAt,
                        currentMileage = current
                    )
                }
            }
            recompute()
        }
    }

    private fun draft(s: TaskEditState) = PlannedTask(
        id = s.taskId ?: 0,
        carId = s.carId ?: 0,
        title = s.title.trim(),
        dueDate = s.dueDate,
        dueMileage = s.dueMileageText.toIntOrNull()?.takeIf { it > 0 },
        notes = s.notes.trim().ifEmpty { null },
        doneAt = s.doneAt,
        doneRecordId = s.doneRecordId,
        createdAt = s.createdAt
    )

    private fun recompute() {
        val o = overview ?: return
        val s = _state.value
        val preview = OneOffPlanner.compute(draft(s), o.currentMileage, o.kmPerDay, LocalDate.now())
        _state.update { it.copy(preview = preview) }
    }

    private fun edit(transform: (TaskEditState) -> TaskEditState) {
        _state.update(transform)
        recompute()
    }

    fun onTitle(value: String) = edit { it.copy(title = value) }
    fun onDueMileage(value: String) {
        if (value.length <= 7 && value.all { it.isDigit() }) edit { it.copy(dueMileageText = value) }
    }
    fun onDueDate(utc: Long?) = edit { it.copy(dueDate = utc) }
    fun onNotes(value: String) = edit { it.copy(notes = value) }

    /** Швидке заповнення «до пробігу»: поточний пробіг + [km]. */
    fun addKmFromNow(km: Int) {
        val base = _state.value.currentMileage
        if (base > 0) edit { it.copy(dueMileageText = (base + km).toString()) }
    }

    private suspend fun persist(): Long? {
        val s = _state.value
        if (s.carId == null || s.blocker != null) return null
        return planRepository.saveTask(draft(s))
    }

    fun save() {
        val s = _state.value
        if (s.blocker != null || s.isSaving || s.isFinished) return
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            persist()
            _state.update { it.copy(isSaving = false, isFinished = true) }
        }
    }

    /** «Виконано» без запису в журнал — датою сьогодні. */
    fun markDone() {
        if (_state.value.isSaving || _state.value.isFinished) return
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            val id = persist()
            if (id != null) planRepository.markTaskDone(id, todayUtcMillis())
            _state.update { it.copy(isSaving = false, isFinished = true) }
        }
    }

    /** «Виконано → записати в журнал»: зберегти зміни плану й відкрити форму запису. */
    fun logToJournal() {
        if (_state.value.isSaving || _state.value.isFinished) return
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            val id = persist()
            _state.update { it.copy(isSaving = false, logToJournalTaskId = id) }
        }
    }

    /** true лише один раз — захист від повторної навігації. */
    fun consumeLogEvent(): Boolean {
        if (logConsumed) return false
        logConsumed = true
        return true
    }

    fun reopen() {
        val id = _state.value.taskId ?: return
        viewModelScope.launch {
            planRepository.reopenTask(id)
            _state.update { it.copy(doneAt = null, doneRecordId = null) }
            recompute()
        }
    }

    fun delete() {
        val id = _state.value.taskId ?: return
        viewModelScope.launch {
            planRepository.deleteTask(id)
            _state.update { it.copy(isFinished = true) }
        }
    }
}

class TaskEditViewModelFactory(
    private val carRepository: CarRepository,
    private val planRepository: PlanRepository,
    private val serviceRepository: ServiceRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        TaskEditViewModel(carRepository, planRepository, serviceRepository) as T
}
