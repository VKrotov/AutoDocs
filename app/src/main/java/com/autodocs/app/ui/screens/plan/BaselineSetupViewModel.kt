package com.autodocs.app.ui.screens.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.autodocs.app.data.plan.DoneMark
import com.autodocs.app.data.repository.Baseline
import com.autodocs.app.data.repository.CarRepository
import com.autodocs.app.data.repository.PlanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Рядок первинного налаштування: «коли цю роботу робили востаннє». */
data class BaselineRow(
    val ruleId: Long,
    val name: String,
    val interval: String,
    val journalMark: DoneMark?,
    val mileageText: String,
    val dateUtcMillis: Long?
) {
    val isFilled: Boolean get() = mileageText.isNotBlank() || dateUtcMillis != null
}

data class BaselineState(
    val isLoading: Boolean = true,
    val rows: List<BaselineRow> = emptyList(),
    val isSaving: Boolean = false,
    val isSaved: Boolean = false
)

class BaselineSetupViewModel(
    private val carRepository: CarRepository,
    private val planRepository: PlanRepository
) : ViewModel() {
    private val _state = MutableStateFlow(BaselineState())
    val state: StateFlow<BaselineState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val car = carRepository.observeActiveCar().first()
            if (car == null) {
                _state.update { it.copy(isLoading = false) }
                return@launch
            }
            val overview = planRepository.observePlan(car).first()
            val rows = overview.active.map { rp ->
                BaselineRow(
                    ruleId = rp.rule.id,
                    name = rp.name,
                    interval = com.autodocs.app.ui.util.formatInterval(rp.rule.intervalKm, rp.rule.intervalMonths),
                    journalMark = rp.journalMark,
                    mileageText = rp.rule.lastDoneMileage?.toString().orEmpty(),
                    dateUtcMillis = rp.rule.lastDoneDate
                )
            }
            // Спершу ті, де нічого не відомо, — саме їх треба заповнити.
            val sorted = rows.sortedBy { if (it.isFilled || it.journalMark != null) 1 else 0 }
            _state.update { it.copy(isLoading = false, rows = sorted) }
        }
    }

    private fun updateRow(ruleId: Long, transform: (BaselineRow) -> BaselineRow) =
        _state.update { s -> s.copy(rows = s.rows.map { if (it.ruleId == ruleId) transform(it) else it }) }

    fun onMileage(ruleId: Long, text: String) {
        if (text.length <= 7 && text.all { it.isDigit() }) updateRow(ruleId) { it.copy(mileageText = text) }
    }

    fun onDate(ruleId: Long, utcMillis: Long?) = updateRow(ruleId) { it.copy(dateUtcMillis = utcMillis) }

    fun clear(ruleId: Long) = updateRow(ruleId) { it.copy(mileageText = "", dateUtcMillis = null) }

    fun save() {
        val s = _state.value
        if (s.isSaving || s.isSaved) return
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            planRepository.saveBaselines(
                s.rows.map { Baseline(it.ruleId, it.mileageText.toIntOrNull(), it.dateUtcMillis) }
            )
            _state.update { it.copy(isSaving = false, isSaved = true) }
        }
    }
}

class BaselineSetupViewModelFactory(
    private val carRepository: CarRepository,
    private val planRepository: PlanRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = BaselineSetupViewModel(carRepository, planRepository) as T
}
