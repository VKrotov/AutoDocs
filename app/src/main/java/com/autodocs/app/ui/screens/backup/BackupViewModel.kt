package com.autodocs.app.ui.screens.backup

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.autodocs.app.data.AppPrefs
import com.autodocs.app.data.backup.BackupException
import com.autodocs.app.data.backup.BackupManager
import com.autodocs.app.data.backup.BackupSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface BackupMessage {
    data class Exported(val summary: BackupSummary) : BackupMessage
    data class Restored(val summary: BackupSummary) : BackupMessage
    data class Error(val text: String) : BackupMessage
}

data class BackupState(
    val busyText: String? = null,
    val lastBackupAt: Long? = null,
    /** Файл вибрано й перевірено — чекаємо підтвердження заміни даних. */
    val pendingRestore: Pair<Uri, BackupSummary>? = null,
    val message: BackupMessage? = null
)

class BackupViewModel(
    private val app: Application,
    private val manager: BackupManager
) : ViewModel() {

    private val _state = MutableStateFlow(BackupState(lastBackupAt = readLastBackup()))
    val state: StateFlow<BackupState> = _state.asStateFlow()

    private fun readLastBackup(): Long? =
        AppPrefs.get(app).getLong(AppPrefs.KEY_LAST_BACKUP_AT, 0L).takeIf { it > 0 }

    private fun errorText(e: Throwable): String =
        (e as? BackupException)?.message ?: "Щось пішло не так: ${e.message ?: e.javaClass.simpleName}"

    fun export(uri: Uri) {
        if (_state.value.busyText != null) return
        viewModelScope.launch {
            _state.update { it.copy(busyText = "Створюю копію…", message = null) }
            val message = runCatching { manager.export(uri) }
                .fold({ BackupMessage.Exported(it) }, { BackupMessage.Error(errorText(it)) })
            _state.update { it.copy(busyText = null, message = message, lastBackupAt = readLastBackup()) }
        }
    }

    /** Крок 1 імпорту: перевірити файл і показати, що в ньому. */
    fun inspect(uri: Uri) {
        if (_state.value.busyText != null) return
        viewModelScope.launch {
            _state.update { it.copy(busyText = "Перевіряю файл…", message = null) }
            runCatching { manager.readSummary(uri) }
                .onSuccess { summary -> _state.update { it.copy(busyText = null, pendingRestore = uri to summary) } }
                .onFailure { e -> _state.update { it.copy(busyText = null, message = BackupMessage.Error(errorText(e))) } }
        }
    }

    fun cancelRestore() = _state.update { it.copy(pendingRestore = null) }

    /** Крок 2 імпорту: повна заміна даних. */
    fun confirmRestore() {
        val (uri, _) = _state.value.pendingRestore ?: return
        viewModelScope.launch {
            _state.update { it.copy(pendingRestore = null, busyText = "Відновлюю дані…", message = null) }
            val message = runCatching { manager.restore(uri) }
                .fold({ BackupMessage.Restored(it) }, { BackupMessage.Error(errorText(it)) })
            _state.update { it.copy(busyText = null, message = message) }
        }
    }

    fun dismissMessage() = _state.update { it.copy(message = null) }
}

class BackupViewModelFactory(
    private val app: Application,
    private val manager: BackupManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = BackupViewModel(app, manager) as T
}
