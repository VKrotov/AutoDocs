package com.autodocs.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import com.autodocs.app.data.AppPrefs
import com.autodocs.app.data.notify.MaintenanceNotifier
import com.autodocs.app.data.notify.NotifySettings
import com.autodocs.app.ui.navigation.AutoDocsNavHost
import com.autodocs.app.ui.theme.AutoDocsTheme

class MainActivity : ComponentActivity() {

    /** Куди відкрити застосунок з натиснутого сповіщення ("plan" / "home"). */
    private val openRequest = mutableStateOf<String?>(null)

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* результат видно в Налаштуваннях */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Застосунок лише темний: іконки статус-бару завжди світлі, незалежно від теми телефона.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        if (savedInstanceState == null) openRequest.value = intent?.getStringExtra(MaintenanceNotifier.EXTRA_OPEN)
        askNotificationPermissionOnce()
        setContent {
            AutoDocsTheme {
                AutoDocsNavHost(
                    openRequest = openRequest.value,
                    onOpenRequestHandled = { openRequest.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(MaintenanceNotifier.EXTRA_OPEN)?.let { openRequest.value = it }
    }

    /** Один раз за інсталяцію питаємо дозвіл на сповіщення (далі — лише через Налаштування). */
    private fun askNotificationPermissionOnce() {
        val prefs = AppPrefs.get(this)
        if (prefs.getBoolean(AppPrefs.KEY_ASKED_NOTIFICATIONS, false)) return
        if (!NotifySettings.load(this).enabled) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return
        prefs.edit().putBoolean(AppPrefs.KEY_ASKED_NOTIFICATIONS, true).apply()
        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
