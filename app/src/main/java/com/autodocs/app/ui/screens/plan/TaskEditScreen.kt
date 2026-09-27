package com.autodocs.app.ui.screens.plan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.autodocs.app.AutoDocsApp
import com.autodocs.app.data.plan.DuePlan
import com.autodocs.app.data.plan.DueStatus
import com.autodocs.app.ui.components.GlassPillButton
import com.autodocs.app.ui.components.GlassSurface
import com.autodocs.app.ui.components.PillText
import com.autodocs.app.ui.components.RoundGlassButton
import com.autodocs.app.ui.components.ScreenHeader
import com.autodocs.app.ui.components.SectionLabel
import com.autodocs.app.ui.components.SuggestTextField
import com.autodocs.app.ui.components.autoDocsFieldColors
import com.autodocs.app.ui.theme.Accent
import com.autodocs.app.ui.theme.AutoDocsDimens
import com.autodocs.app.ui.theme.LinkColor
import com.autodocs.app.ui.theme.OnAccent
import com.autodocs.app.ui.theme.StatusOverdue
import com.autodocs.app.ui.theme.StatusSoon
import com.autodocs.app.ui.theme.TextPrimary
import com.autodocs.app.ui.theme.TextSecondary
import com.autodocs.app.ui.util.formatDaysSigned
import com.autodocs.app.ui.util.formatKm
import com.autodocs.app.ui.util.formatKmSigned
import com.autodocs.app.ui.util.formatRecordDate

/** Етап 9: разовий план «зробити до дати / пробігу» — додати, змінити, закрити. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskEditScreen(
    taskId: Long?,
    onDone: () -> Unit,
    onBack: () -> Unit,
    onLogToJournal: (taskId: Long) -> Unit,
    onOpenRecord: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val app = LocalContext.current.applicationContext as AutoDocsApp
    val viewModel: TaskEditViewModel = viewModel(
        key = "task_edit_${taskId ?: "new"}",
        factory = TaskEditViewModelFactory(app.carRepository, app.planRepository, app.serviceRepository)
    )
    val state by viewModel.state.collectAsState()
    val workTypes by viewModel.workTypes.collectAsState()
    var pickDate by rememberSaveable { mutableStateOf(false) }
    var askDone by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(taskId) { viewModel.init(taskId) }
    LaunchedEffect(state.isFinished) { if (state.isFinished) onDone() }
    LaunchedEffect(state.logToJournalTaskId) {
        val id = state.logToJournalTaskId
        if (id != null && viewModel.consumeLogEvent()) onLogToJournal(id)
    }

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
            title = if (state.isNew) "Новий разовий план" else "Разовий план",
            overline = "План ТО · ${state.carName}",
            onBack = onBack
        ) {
            if (!state.isNew) {
                RoundGlassButton(Icons.Outlined.Delete, "Видалити план", { confirmDelete = true }, tint = StatusOverdue)
            }
        }

        val doneAt = state.doneAt
        if (doneAt != null) {
            GlassSurface(modifier = Modifier.fillMaxWidth(), cornerRadius = AutoDocsDimens.CardRadiusInner, contentPadding = 14.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Виконано ${formatRecordDate(doneAt)}", style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                    val recordId = state.doneRecordId
                    if (recordId != null) {
                        TextButton(onClick = { onOpenRecord(recordId) }) { Text("Відкрити запис у журналі", color = LinkColor) }
                    }
                    TextButton(onClick = viewModel::reopen) { Text("Повернути в план", color = LinkColor) }
                }
            }
        }

        val query = state.title.trim()
        SuggestTextField(
            value = state.title,
            onValueChange = viewModel::onTitle,
            label = "Що зробити (напр. «Замінити шарові опори»)",
            suggestions = if (query.length < 2) emptyList() else workTypes
                .filter { it.name.contains(query, ignoreCase = true) && !it.name.equals(query, ignoreCase = true) }
                .take(8),
            suggestionText = { it.name },
            onSuggestionPicked = { viewModel.onTitle(it.name) }
        )

        SectionLabel("Зробити до — що настане раніше", Modifier.padding(top = 4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField(state.dueMileageText, viewModel::onDueMileage, "Пробіг", "км", Modifier.weight(1f))
            DateField("Дата", state.dueDate, onClick = { pickDate = true }, modifier = Modifier.weight(1f))
        }
        if (state.currentMileage > 0) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1_000, 5_000, 10_000).forEach { km ->
                    GlassPillButton(onClick = { viewModel.addKmFromNow(km) }, height = 32) { PillText("+${formatKm(km)} км") }
                }
            }
            Text(
                "Зараз ≈ ${formatKm(state.currentMileage)} км",
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 13.sp,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
        if (state.dueDate != null || state.dueMileageText.isNotBlank()) {
            TextButton(onClick = { viewModel.onDueMileage(""); viewModel.onDueDate(null) }) {
                Text("Без терміну", color = TextSecondary)
            }
        }

        if (!state.isDone) state.preview?.let { TaskPreviewCard(it) }

        OutlinedTextField(
            value = state.notes,
            onValueChange = viewModel::onNotes,
            label = { Text("Нотатки") },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            colors = autoDocsFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )

        state.blocker?.let {
            Text(it, color = StatusSoon, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 4.dp))
        }
        Button(
            onClick = viewModel::save,
            enabled = state.blocker == null && !state.isSaving,
            colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = OnAccent),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) { Text(if (state.isNew) "Додати в план" else "Зберегти") }
        if (!state.isNew && !state.isDone) {
            OutlinedButton(
                onClick = { askDone = true },
                enabled = state.blocker == null && !state.isSaving,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) { Text("Виконано", color = LinkColor) }
        }
        Spacer(Modifier.height(24.dp))
    }

    if (pickDate) {
        AnyDatePickerDialog(initialUtcMillis = state.dueDate, onPicked = viewModel::onDueDate, onDismiss = { pickDate = false })
    }
    if (askDone) {
        AlertDialog(
            onDismissRequest = { askDone = false },
            title = { Text("Виконано «${state.title.trim()}»") },
            text = { Text("Записати роботу в журнал? Форма запису відкриється вже з цією позицією — лишиться вказати ціну й СТО.") },
            confirmButton = {
                TextButton(onClick = { askDone = false; viewModel.logToJournal() }) { Text("Записати в журнал", color = Accent) }
            },
            dismissButton = {
                TextButton(onClick = { askDone = false; viewModel.markDone() }) { Text("Просто відмітити") }
            }
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Видалити план?") },
            text = { Text("Записи журналу не зміняться.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; viewModel.delete() }) { Text("Видалити", color = StatusOverdue) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Скасувати") } }
        )
    }
}

@Composable
private fun TaskPreviewCard(plan: DuePlan) {
    val color = when (plan.status) {
        DueStatus.OVERDUE -> StatusOverdue
        DueStatus.SOON -> StatusSoon
        DueStatus.OK -> Accent
    }
    GlassSurface(modifier = Modifier.fillMaxWidth(), cornerRadius = AutoDocsDimens.CardRadiusInner, contentPadding = 14.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Термін", style = MaterialTheme.typography.bodyMedium, fontSize = 13.sp, color = TextSecondary)
            if (plan.dueMileage == null && plan.dueDate == null) {
                Text("Без терміну — просто нагадування в «Плані ТО»", style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                return@Column
            }
            val left = listOfNotNull(
                plan.remainingKm?.let { "лишилось ${formatKmSigned(it)}" },
                plan.remainingDays?.let { "≈ ${formatDaysSigned(it)}" }
            )
            Text(
                left.joinToString(" · ").ifEmpty { "Вкажи пробіг авто, щоб рахувати залишок" },
                color = if (left.isEmpty()) TextSecondary else color,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
