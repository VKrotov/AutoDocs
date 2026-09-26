package com.autodocs.app.ui.screens.plan

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.autodocs.app.AutoDocsApp
import com.autodocs.app.data.plan.DuePlan
import com.autodocs.app.data.plan.DueStatus
import com.autodocs.app.ui.components.GlassSurface
import com.autodocs.app.ui.components.RoundGlassButton
import com.autodocs.app.ui.components.ScreenHeader
import com.autodocs.app.ui.components.SectionLabel
import com.autodocs.app.ui.components.SuggestTextField
import com.autodocs.app.ui.components.autoDocsFieldColors
import com.autodocs.app.ui.theme.Accent
import com.autodocs.app.ui.theme.AutoDocsDimens
import com.autodocs.app.ui.theme.OnAccent
import com.autodocs.app.ui.theme.StatusOverdue
import com.autodocs.app.ui.theme.StatusSoon
import com.autodocs.app.ui.theme.TextPrimary
import com.autodocs.app.ui.theme.TextSecondary
import com.autodocs.app.ui.util.formatDaysSigned
import com.autodocs.app.ui.util.formatKm
import com.autodocs.app.ui.util.formatKmSigned
import com.autodocs.app.ui.util.formatLongDate
import com.autodocs.app.ui.util.formatRecordDate

/** F07: додати/редагувати пункт регламенту ТО. */
@Composable
fun RuleEditScreen(ruleId: Long?, onDone: () -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val app = LocalContext.current.applicationContext as AutoDocsApp
    val viewModel: RuleEditViewModel = viewModel(
        key = "rule_edit_${ruleId ?: "new"}",
        factory = RuleEditViewModelFactory(app.carRepository, app.planRepository, app.serviceRepository)
    )
    val state by viewModel.state.collectAsState()
    val workTypes by viewModel.workTypes.collectAsState()
    var pickDate by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(ruleId) { viewModel.init(ruleId) }
    LaunchedEffect(state.isFinished) { if (state.isFinished) onDone() }

    if (state.isLoading) {
        Box(modifier.fillMaxSize())
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AutoDocsDimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ScreenHeader(
            title = if (state.isNew) "Новий пункт" else state.workName,
            overline = "Регламент ТО",
            onBack = onBack
        ) {
            if (!state.isNew) {
                RoundGlassButton(Icons.Outlined.Delete, "Видалити пункт", { confirmDelete = true }, tint = StatusOverdue)
            }
        }

        if (state.isNew) {
            val query = state.workName.trim()
            SuggestTextField(
                value = state.workName,
                onValueChange = viewModel::onWorkName,
                label = "Робота (напр. «Заміна масла двигуна»)",
                suggestions = workTypes.filter {
                    it.name.contains(query, ignoreCase = true) && !it.name.equals(query, ignoreCase = true)
                },
                suggestionText = { it.name },
                onSuggestionPicked = { viewModel.onWorkName(it.name) }
            )
        }
        state.hint?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, modifier = Modifier.padding(horizontal = 4.dp))
        }

        SectionLabel("Інтервал — що настане раніше", Modifier.padding(top = 4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField(state.intervalKmText, viewModel::onIntervalKm, "Кожні", "км", Modifier.weight(1f))
            NumberField(state.intervalMonthsText, viewModel::onIntervalMonths, "Або раз на", "міс", Modifier.weight(1f))
        }

        SectionLabel("Коли робили востаннє", Modifier.padding(top = 4.dp))
        state.journalMark?.let { JournalNote(it) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField(state.lastMileageText, viewModel::onLastMileage, "Пробіг", "км", Modifier.weight(1f))
            Box(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = state.lastDateUtc?.let(::formatRecordDate) ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Дата") },
                    trailingIcon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = null) },
                    singleLine = true,
                    colors = autoDocsFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Box(Modifier.matchParentSize().clickable { pickDate = true })
            }
        }
        if (state.lastDateUtc != null || state.lastMileageText.isNotBlank()) {
            TextButton(onClick = { viewModel.onLastMileage(""); viewModel.onLastDate(null) }) {
                Text("Очистити ручну відмітку", color = TextSecondary)
            }
        }

        GlassSurface(modifier = Modifier.fillMaxWidth(), cornerRadius = AutoDocsDimens.CardRadiusInner, contentPadding = 14.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Враховувати в плані", style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                    Text(
                        "Вимкнений пункт не рахується і не нагадує",
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
                Switch(
                    checked = state.isActive,
                    onCheckedChange = viewModel::onActive,
                    colors = SwitchDefaults.colors(checkedTrackColor = Accent, checkedThumbColor = OnAccent)
                )
            }
        }

        state.preview?.let { PreviewCard(it) }

        val problem = state.error ?: state.blocker
        if (problem != null) {
            Text(problem, color = StatusSoon, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 4.dp))
        }
        Button(
            onClick = viewModel::save,
            enabled = state.blocker == null && !state.isSaving,
            colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = OnAccent),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) { Text(if (state.isNew) "Додати в регламент" else "Зберегти") }
        Spacer(Modifier.height(24.dp))
    }

    if (pickDate) {
        PastDatePickerDialog(
            initialUtcMillis = state.lastDateUtc,
            onPicked = viewModel::onLastDate,
            onDismiss = { pickDate = false }
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Видалити «${state.workName}» з регламенту?") },
            text = { Text("Записи журналу не зміняться. Пункт довідника теж лишиться.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; viewModel.delete() }) { Text("Видалити", color = StatusOverdue) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Скасувати") } }
        )
    }
}

@Composable
private fun NumberField(value: String, onChange: (String) -> Unit, label: String, suffix: String, modifier: Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        suffix = { Text(suffix, color = TextSecondary) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        colors = autoDocsFieldColors(),
        modifier = modifier
    )
}

@Composable
private fun PreviewCard(plan: DuePlan) {
    val color = when (plan.status) {
        DueStatus.OVERDUE -> StatusOverdue
        DueStatus.SOON -> StatusSoon
        DueStatus.OK -> Accent
    }
    GlassSurface(modifier = Modifier.fillMaxWidth(), cornerRadius = AutoDocsDimens.CardRadiusInner, contentPadding = 14.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Наступне ТО", style = MaterialTheme.typography.bodyMedium, fontSize = 13.sp, color = TextSecondary)
            if (plan.isUnknown) {
                Text("Невідомо, коли робили — рахується простроченим", color = StatusOverdue, style = MaterialTheme.typography.bodyLarge)
                return@Column
            }
            val parts = listOfNotNull(
                plan.dueMileage?.let { "на ${formatKm(it)} км" },
                plan.dueDate?.let { "до ${formatLongDate(it)}" }
            )
            Text(
                parts.joinToString(" або ").ifEmpty { "Недостатньо даних для розрахунку" },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
            val left = listOfNotNull(
                plan.remainingKm?.let { "лишилось ${formatKmSigned(it)}" },
                plan.remainingDays?.let { "≈ ${formatDaysSigned(it)}" }
            )
            if (left.isNotEmpty()) {
                Text(left.joinToString(" · "), color = color, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
