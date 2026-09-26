package com.autodocs.app.ui.screens.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.autodocs.app.data.entity.MaintenanceRule
import com.autodocs.app.data.entity.WorkItemCategory
import com.autodocs.app.data.entity.WorkType
import com.autodocs.app.data.plan.DoneMark
import com.autodocs.app.data.plan.DuePlan
import com.autodocs.app.data.plan.MaintenanceCalculator
import com.autodocs.app.data.repository.CarRepository
import com.autodocs.app.data.repository.PlanOverview
import com.autodocs.app.data.repository.PlanRepository
import com.autodocs.app.data.repository.ServiceRepository
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

data class RuleEditState(
    val isLoading: Boolean = true,
    val ruleId: Long? = null,
    val carId: Long? = null,
    /** Назва роботи. Для існуючого правила не редагується (перейменування — через довідник). */
    val workName: String = "",
    val hint: String? = null,
    val intervalKmText: String = "",
    val intervalMonthsText: String = "",
    val lastMileageText: String = "",
    val lastDateUtc: Long? = null,
    val isActive: Boolean = true,
    val journalMark: DoneMark? = null,
    val preview: DuePlan? = null,
    val error: String? = null,
    val isSaving: Boolean = false,
    val isFinished: Boolean = false
) {
    val isNew: Boolean get() = ruleId == null
    val blocker: String?
        get() = when {
            workName.isBlank() -> "Вкажи назву роботи"
            intervalKmText.toIntOrNull().let { it == null || it <= 0 } &&
                intervalMonthsText.toIntOrNull().let { it == null || it <= 0 } -> "Вкажи інтервал: км, місяці або обидва"
            else -> null
        }
}

class RuleEditViewModel(
    private val carRepository: CarRepository,
    private val planRepository: PlanRepository,
    serviceRepository: ServiceRepository
) : ViewModel() {

    private val _state = MutableStateFlow(RuleEditState())
    val state: StateFlow<RuleEditState> = _state.asStateFlow()

    val workTypes: StateFlow<List<WorkType>> = serviceRepository.observeWorkTypes()
        .map { list -> list.filter { it.category == WorkItemCategory.ROBOTA } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var overview: PlanOverview? = null
    private var initialized = false

    fun init(ruleId: Long?) {
        if (initialized) return
        initialized = true
        viewModelScope.launch {
            val car = carRepository.observeActiveCar().first()
            if (car == null) {
                _state.update { it.copy(isLoading = false, isFinished = true) }
                return@launch
            }
            overview = planRepository.observePlan(car).first()
            if (ruleId == null) {
                _state.update { it.copy(isLoading = false, carId = car.id) }
            } else {
                val rule = planRepository.getRule(ruleId)
                if (rule == null) {
                    _state.update { it.copy(isLoading = false, isFinished = true) }
                    return@launch
                }
                val rp = overview?.let { o -> (o.active + o.inactive).firstOrNull { it.rule.id == ruleId } }
                val name = planRepository.workTypeName(rule.workTypeId).orEmpty()
                _state.update {
                    it.copy(
                        isLoading = false,
                        ruleId = rule.id,
                        carId = rule.carId,
                        workName = name,
                        hint = rp?.hint,
                        intervalKmText = rule.intervalKm?.toString().orEmpty(),
                        intervalMonthsText = rule.intervalMonths?.toString().orEmpty(),
                        lastMileageText = rule.lastDoneMileage?.toString().orEmpty(),
                        lastDateUtc = rule.lastDoneDate,
                        isActive = rule.isActive,
                        journalMark = rp?.journalMark
                    )
                }
            }
            recompute()
        }
    }

    /** Живий перерахунок «наступне ТО» під поточні значення форми. */
    private fun recompute() {
        val s = _state.value
        val o = overview ?: return
        val manualMileage = s.lastMileageText.toIntOrNull()
        val manual = if (manualMileage != null || s.lastDateUtc != null) {
            DoneMark(s.lastDateUtc?.let(PlanRepository::utcToLocalDate), manualMileage, fromJournal = false)
        } else null
        val last = MaintenanceCalculator.latestMark(manual, listOfNotNull(s.journalMark))
        val preview = if (s.blocker == null) {
            MaintenanceCalculator.compute(
                s.intervalKmText.toIntOrNull()?.takeIf { it > 0 },
                s.intervalMonthsText.toIntOrNull()?.takeIf { it > 0 },
                last, o.currentMileage, o.kmPerDay, LocalDate.now()
            )
        } else null
        _state.update { it.copy(preview = preview) }
    }

    private fun edit(transform: (RuleEditState) -> RuleEditState) {
        _state.update { transform(it).copy(error = null) }
        recompute()
    }

    private fun digits(text: String, max: Int) = text.length <= max && text.all { it.isDigit() }

    fun onWorkName(value: String) = edit { it.copy(workName = value) }
    fun onIntervalKm(value: String) { if (digits(value, 6)) edit { it.copy(intervalKmText = value) } }
    fun onIntervalMonths(value: String) { if (digits(value, 3)) edit { it.copy(intervalMonthsText = value) } }
    fun onLastMileage(value: String) { if (digits(value, 7)) edit { it.copy(lastMileageText = value) } }
    fun onLastDate(utc: Long?) = edit { it.copy(lastDateUtc = utc) }
    fun onActive(value: Boolean) = edit { it.copy(isActive = value) }

    fun save() {
        val s = _state.value
        val carId = s.carId ?: return
        if (s.blocker != null || s.isSaving || s.isFinished) return
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            val workTypeId = if (s.isNew) {
                val id = planRepository.ensureWorkType(s.workName)
                val duplicate = overview?.let { o -> (o.active + o.inactive).any { it.rule.workTypeId == id } } == true
                if (duplicate) {
                    _state.update { it.copy(isSaving = false, error = "«${s.workName.trim()}» уже є в регламенті") }
                    return@launch
                }
                id
            } else {
                planRepository.getRule(s.ruleId!!)?.workTypeId ?: return@launch
            }
            planRepository.saveRule(
                MaintenanceRule(
                    id = s.ruleId ?: 0,
                    carId = carId,
                    workTypeId = workTypeId,
                    intervalKm = s.intervalKmText.toIntOrNull()?.takeIf { it > 0 },
                    intervalMonths = s.intervalMonthsText.toIntOrNull()?.takeIf { it > 0 },
                    lastDoneMileage = s.lastMileageText.toIntOrNull(),
                    lastDoneDate = s.lastDateUtc,
                    isActive = s.isActive
                )
            )
            _state.update { it.copy(isSaving = false, isFinished = true) }
        }
    }

    fun delete() {
        val id = _state.value.ruleId ?: return
        viewModelScope.launch {
            planRepository.deleteRule(id)
            _state.update { it.copy(isFinished = true) }
        }
    }
}

class RuleEditViewModelFactory(
    private val carRepository: CarRepository,
    private val planRepository: PlanRepository,
    private val serviceRepository: ServiceRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        RuleEditViewModel(carRepository, planRepository, serviceRepository) as T
}
