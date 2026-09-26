package com.autodocs.app.ui.screens.notify

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.autodocs.app.data.notify.MaintenanceNotifier
import com.autodocs.app.data.notify.NotifySettings
import com.autodocs.app.data.notify.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotifyUiState(
    val settings: NotifySettings,
    val checking: Boolean = false,
    val message: String? = null
)

class NotificationSettingsViewModel(private val app: Application) : ViewModel() {
    private val _state = MutableStateFlow(NotifyUiState(NotifySettings.load(app)))
    val state: StateFlow<NotifyUiState> = _state.asStateFlow()

    fun update(transform: (NotifySettings) -> NotifySettings) {
        val old = _state.value.settings
        val new = transform(old)
        if (new == old) return
        NotifySettings.save(app, new)
        _state.update { it.copy(settings = new, message = null) }
        if (new.enabled != old.enabled || new.hour != old.hour) {
            runCatching { ReminderScheduler.schedule(app, reschedule = true) }
        }
    }

    fun checkNow() {
        if (_state.value.checking) return
        viewModelScope.launch {
            _state.update { it.copy(checking = true, message = null) }
            val result = runCatching { MaintenanceNotifier.check(app, force = true) }.getOrNull()
            val msg = when (result) {
                MaintenanceNotifier.Result.SENT -> "Готово — подивись у шторку сповіщень"
                MaintenanceNotifier.Result.NO_PERMISSION -> "Немає дозволу на сповіщення"
                MaintenanceNotifier.Result.NO_CAR -> "Немає активного авто"
                MaintenanceNotifier.Result.NOTHING, MaintenanceNotifier.Result.DISABLED -> "Нічого надсилати"
                null -> "Не вдалося перевірити"
            }
            _state.update { it.copy(checking = false, message = msg) }
        }
    }
}

class NotificationSettingsViewModelFactory(private val app: Application) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = NotificationSettingsViewModel(app) as T
}
