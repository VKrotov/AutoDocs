package com.autodocs.app.ui.screens.backup

import android.app.Application
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.autodocs.app.AutoDocsApp
import com.autodocs.app.data.backup.BackupSummary
import com.autodocs.app.ui.components.GlassSurface
import com.autodocs.app.ui.components.ScreenHeader
import com.autodocs.app.ui.theme.Accent
import com.autodocs.app.ui.theme.AutoDocsDimens
import com.autodocs.app.ui.theme.LinkColor
import com.autodocs.app.ui.theme.OnAccent
import com.autodocs.app.ui.theme.StatusOverdue
import com.autodocs.app.ui.theme.StatusSoon
import com.autodocs.app.ui.theme.TextPrimary
import com.autodocs.app.ui.theme.TextSecondary
import com.autodocs.app.ui.util.formatDateTime
import com.autodocs.app.ui.util.isoToday
import com.autodocs.app.ui.util.pluralUk

/** F12: експорт усіх даних у .zip і відновлення з нього (для переїзду на новий телефон). */
@Composable
fun BackupScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val app = context.applicationContext as AutoDocsApp
    val viewModel: BackupViewModel = viewModel(
        factory = BackupViewModelFactory(context.applicationContext as Application, app.backupManager)
    )
    val state by viewModel.state.collectAsState()
    val busy = state.busyText != null

    val createLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri -> if (uri != null) viewModel.export(uri) }

    val openLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) viewModel.inspect(uri) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AutoDocsDimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ScreenHeader(title = "Резервна копія", onBack = onBack)

        Text(
            "Усі дані — авто, журнал, довідник і фото — в одному .zip-файлі. " +
                "Зберігай його в Google Drive, Telegram чи на комп'ютері: з нього все відновиться на новому телефоні.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        // --- Експорт ---
        GlassSurface(modifier = Modifier.fillMaxWidth(), contentPadding = 18.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CardTitle(Icons.Outlined.CloudUpload, "Зберегти копію")
                Text(
                    state.lastBackupAt?.let { "Остання копія: ${formatDateTime(it)}" } ?: "Копій ще не створювали",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Button(
                    onClick = { createLauncher.launch("AutoDocs_${isoToday()}.zip") },
                    enabled = !busy,
                    colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = OnAccent),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) { Text("Створити копію") }
            }
        }

        // --- Імпорт ---
        GlassSurface(modifier = Modifier.fillMaxWidth(), contentPadding = 18.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CardTitle(Icons.Outlined.Restore, "Відновити з копії")
                Text(
                    "Усі поточні дані на цьому телефоні буде замінено даними з копії. Перед заміною покажу, що в файлі.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = StatusSoon
                )
                OutlinedButton(
                    onClick = {
                        openLauncher.launch(
                            arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream")
                        )
                    },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) { Text("Вибрати файл копії", color = LinkColor) }
            }
        }

        state.busyText?.let { text ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(horizontal = 4.dp)) {
                Text(text, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                LinearProgressIndicator(color = Accent, modifier = Modifier.fillMaxWidth())
            }
        }

        state.message?.let { message ->
            MessageCard(message = message, onDismiss = viewModel::dismissMessage)
        }

        Spacer(Modifier.height(24.dp))
    }

    state.pendingRestore?.let { (_, summary) ->
        AlertDialog(
            onDismissRequest = viewModel::cancelRestore,
            title = { Text("Замінити дані копією?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SummaryLines(summary)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Поточні дані на цьому телефоні буде повністю замінено. Скасувати це не можна.",
                        color = StatusOverdue
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmRestore) { Text("Замінити", color = StatusOverdue) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelRestore) { Text("Скасувати") }
            }
        )
    }
}

@Composable
private fun CardTitle(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = Accent, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
    }
}

@Composable
private fun SummaryLines(summary: BackupSummary) {
    val cars = summary.cars.toLong()
    val records = summary.records.toLong()
    val photos = summary.photos.toLong()
    if (summary.exportedAt > 0) Text("Копія від ${formatDateTime(summary.exportedAt)}")
    Text(
        "$cars ${pluralUk(cars, "авто", "авто", "авто")}" +
            (summary.activeCarName?.let { " (активне: $it)" } ?: "")
    )
    Text("$records ${pluralUk(records, "запис", "записи", "записів")} у журналі")
    Text("$photos ${pluralUk(photos, "фото", "фото", "фото")}")
}

@Composable
private fun MessageCard(message: BackupMessage, onDismiss: () -> Unit) {
    val isError = message is BackupMessage.Error
    val tint: Color = if (isError) StatusOverdue else Accent
    val shape = RoundedCornerShape(AutoDocsDimens.CardRadiusInner)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(tint.copy(alpha = 0.08f))
            .border(1.dp, tint.copy(alpha = 0.3f), shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (isError) Icons.Outlined.ErrorOutline else Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                when (message) {
                    is BackupMessage.Exported -> "Копію збережено"
                    is BackupMessage.Restored -> "Дані відновлено"
                    is BackupMessage.Error -> "Не вийшло"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }
        when (message) {
            is BackupMessage.Exported -> SummaryText(message.summary)
            is BackupMessage.Restored -> SummaryText(message.summary)
            is BackupMessage.Error -> Text(message.text, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
        }
        TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
            Text("Гаразд", color = LinkColor)
        }
    }
}

@Composable
private fun SummaryText(summary: BackupSummary) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        val style = MaterialTheme.typography.bodyMedium
        val cars = summary.cars.toLong()
        val records = summary.records.toLong()
        val photos = summary.photos.toLong()
        Text("$cars авто · $records ${pluralUk(records, "запис", "записи", "записів")} · $photos фото", style = style, color = TextSecondary)
    }
}
