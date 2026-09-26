package com.autodocs.app.ui.screens.notify

import android.Manifest
import android.app.Application
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.autodocs.app.data.notify.MaintenanceNotifier
import com.autodocs.app.ui.components.GlassSurface
import com.autodocs.app.ui.components.ScreenHeader
import com.autodocs.app.ui.components.SectionLabel
import com.autodocs.app.ui.theme.Accent
import com.autodocs.app.ui.theme.AutoDocsDimens
import com.autodocs.app.ui.theme.LinkColor
import com.autodocs.app.ui.theme.OnAccent
import com.autodocs.app.ui.theme.StatusOverdue
import com.autodocs.app.ui.theme.TextPrimary
import com.autodocs.app.ui.theme.TextSecondary
import com.autodocs.app.ui.util.formatKm

/** F10: налаштування нагадувань про ТО. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NotificationSettingsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val viewModel: NotificationSettingsViewModel = viewModel(
        factory = NotificationSettingsViewModelFactory(context.applicationContext as Application)
    )
    val state by viewModel.state.collectAsState()
    val s = state.settings

    var canPost by remember { mutableStateOf(MaintenanceNotifier.canPost(context)) }
    LifecycleResumeEffect(Unit) {
        canPost = MaintenanceNotifier.canPost(context)
        onPauseOrDispose { }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        canPost = MaintenanceNotifier.canPost(context)
        if (!canPost) {
            // Систему вже раз питали й відмовили — ведемо в налаштування застосунку.
            context.startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AutoDocsDimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ScreenHeader(title = "Сповіщення", overline = "Налаштування", onBack = onBack)

        if (!canPost) {
            val shape = RoundedCornerShape(AutoDocsDimens.CardRadiusInner)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(StatusOverdue.copy(alpha = 0.08f))
                    .border(1.dp, StatusOverdue.copy(alpha = 0.3f), shape)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Сповіщення для AutoDocs вимкнені", style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                Text(
                    "Без дозволу нагадування про ТО не з'являтимуться.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                    colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = OnAccent)
                ) { Text("Дозволити сповіщення") }
            }
        }

        GlassSurface(modifier = Modifier.fillMaxWidth(), cornerRadius = AutoDocsDimens.CardRadiusInner, contentPadding = 14.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Нагадувати про ТО", style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                    Text(
                        "Перевірка раз на день о ${s.hour}:00",
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
                Switch(
                    checked = s.enabled,
                    onCheckedChange = { on -> viewModel.update { it.copy(enabled = on) } },
                    colors = SwitchDefaults.colors(checkedTrackColor = Accent, checkedThumbColor = OnAccent)
                )
            }
        }

        Column(modifier = Modifier.alpha(if (s.enabled) 1f else 0.5f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionLabel("Попереджати за", Modifier.padding(top = 4.dp))
            ChipRow(listOf(7, 14, 30), s.daysBefore, { "$it днів" }) { v -> viewModel.update { it.copy(daysBefore = v) } }
            ChipRow(listOf(500, 1_000, 2_000), s.kmBefore, { "${formatKm(it)} км" }) { v -> viewModel.update { it.copy(kmBefore = v) } }

            SectionLabel("Нагадати внести пробіг, якщо не оновлювався", Modifier.padding(top = 4.dp))
            ChipRow(listOf(0, 7, 14, 30), s.mileageDays, { if (it == 0) "Не нагадувати" else "$it днів" }) { v ->
                viewModel.update { it.copy(mileageDays = v) }
            }

            SectionLabel("Час щоденної перевірки", Modifier.padding(top = 4.dp))
            ChipRow(listOf(8, 10, 12, 18, 20), s.hour, { "$it:00" }) { v -> viewModel.update { it.copy(hour = v) } }
        }

        Text(
            "Про той самий пункт нагадаю не частіше, ніж раз на тиждень, доки його не зробиш і не внесеш у журнал. " +
                "Пункти без відмітки «коли робили» не нагадуються — їх видно в «Плані ТО».",
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 13.sp,
            color = TextSecondary,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
        )

        OutlinedButton(
            onClick = viewModel::checkNow,
            enabled = !state.checking && canPost,
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) { Text("Перевірити зараз", color = LinkColor) }
        state.message?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, modifier = Modifier.padding(horizontal = 4.dp))
        }
        Spacer(Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> ChipRow(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(label(option)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Accent.copy(alpha = 0.16f),
                    selectedLabelColor = TextPrimary,
                    labelColor = TextSecondary
                )
            )
        }
    }
}
