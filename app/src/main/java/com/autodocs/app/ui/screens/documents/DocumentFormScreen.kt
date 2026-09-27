package com.autodocs.app.ui.screens.documents

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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.autodocs.app.AutoDocsApp
import com.autodocs.app.data.entity.DocumentType
import com.autodocs.app.data.entity.hint
import com.autodocs.app.data.entity.label
import com.autodocs.app.data.plan.DocumentDue
import com.autodocs.app.data.plan.DueStatus
import com.autodocs.app.ui.components.RoundGlassButton
import com.autodocs.app.ui.components.ScreenHeader
import com.autodocs.app.ui.components.SectionLabel
import com.autodocs.app.ui.components.SuggestTextField
import com.autodocs.app.ui.components.autoDocsFieldColors
import com.autodocs.app.ui.components.rememberPhotoSources
import com.autodocs.app.ui.screens.plan.AnyDatePickerDialog
import com.autodocs.app.ui.screens.plan.DateField
import com.autodocs.app.ui.screens.record.RecordPhotosSection
import com.autodocs.app.ui.theme.Accent
import com.autodocs.app.ui.theme.AutoDocsDimens
import com.autodocs.app.ui.theme.LinkColor
import com.autodocs.app.ui.theme.OnAccent
import com.autodocs.app.ui.theme.StatusOverdue
import com.autodocs.app.ui.theme.StatusSoon
import com.autodocs.app.ui.theme.TextPrimary
import com.autodocs.app.ui.theme.TextSecondary
import com.autodocs.app.ui.util.formatDaysSigned
import com.autodocs.app.ui.util.formatLongDate
import com.autodocs.app.ui.util.pluralUk

/** Етап 9: додати / редагувати / продовжити документ (поліс, техогляд…). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DocumentFormScreen(
    docId: Long?,
    presetType: DocumentType?,
    renewFromId: Long?,
    onDone: () -> Unit,
    onBack: () -> Unit,
    onRenew: (Long) -> Unit,
    onOpenPhoto: (docId: Long, index: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val app = LocalContext.current.applicationContext as AutoDocsApp
    val viewModel: DocumentFormViewModel = viewModel(
        key = "document_form_${docId ?: "new"}_${renewFromId ?: ""}_${presetType ?: ""}",
        factory = DocumentFormViewModelFactory(app, app.carRepository, app.documentRepository, app.photoRepository)
    )
    val state by viewModel.state.collectAsState()
    val companies by viewModel.companies.collectAsState()
    var pickFrom by rememberSaveable { mutableStateOf(false) }
    var pickUntil by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val photoSources = rememberPhotoSources(
        onPicked = { uris, isScan -> viewModel.onPhotosPicked(uris, isScan) },
        onError = viewModel::onPhotoError
    )

    LaunchedEffect(docId) { viewModel.init(docId, presetType, renewFromId) }
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
            title = when {
                !state.isNew -> if (state.type == DocumentType.OTHER) state.title.ifBlank { "Документ" } else state.type.label()
                renewFromId != null -> "Новий поліс"
                else -> "Новий документ"
            },
            overline = state.carName.ifBlank { null },
            onBack = onBack
        ) {
            if (!state.isNew) {
                RoundGlassButton(Icons.Outlined.Delete, "Видалити документ", { confirmDelete = true }, tint = StatusOverdue)
            }
        }

        SectionLabel("Вид")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
            DocumentType.entries.forEach { type ->
                FilterChip(
                    selected = state.type == type,
                    onClick = { viewModel.onType(type) },
                    label = { Text(type.label()) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Accent.copy(alpha = 0.16f),
                        selectedLabelColor = TextPrimary,
                        labelColor = TextSecondary
                    )
                )
            }
        }
        state.type.hint()?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, fontSize = 13.sp, color = TextSecondary, modifier = Modifier.padding(horizontal = 4.dp))
        }
        if (state.type == DocumentType.OTHER) {
            OutlinedTextField(
                value = state.title,
                onValueChange = viewModel::onTitle,
                label = { Text("Назва документа") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                colors = autoDocsFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
        }

        SectionLabel("Термін дії", Modifier.padding(top = 4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DateField("Діє з", state.validFrom, onClick = { pickFrom = true }, modifier = Modifier.weight(1f))
            DateField("Діє до (включно)", state.validUntil, onClick = { pickUntil = true }, modifier = Modifier.weight(1f))
        }
        state.preview?.let { PreviewLine(it) }

        SectionLabel(if (state.type == DocumentType.INSPECTION) "Деталі" else "Поліс", Modifier.padding(top = 4.dp))
        OutlinedTextField(
            value = state.number,
            onValueChange = viewModel::onNumber,
            label = { Text(if (state.type == DocumentType.INSPECTION) "Номер протоколу" else "Номер поліса") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            colors = autoDocsFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )
        val query = state.company.trim()
        SuggestTextField(
            value = state.company,
            onValueChange = viewModel::onCompany,
            label = if (state.type == DocumentType.INSPECTION) "Де проходили" else "Страхова компанія",
            suggestions = companies.filter { it.contains(query, ignoreCase = true) && !it.equals(query, ignoreCase = true) }.take(6),
            suggestionText = { it },
            onSuggestionPicked = viewModel::onCompany
        )
        OutlinedTextField(
            value = state.priceText,
            onValueChange = viewModel::onPrice,
            label = { Text("Вартість") },
            suffix = { Text("₴", color = TextSecondary) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            colors = autoDocsFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = state.notes,
            onValueChange = viewModel::onNotes,
            label = { Text("Нотатки") },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            colors = autoDocsFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )

        RecordPhotosSection(
            photos = state.photos.map { it.uri },
            slotsLeft = state.photoSlotsLeft,
            importing = state.isImportingPhotos,
            message = state.photoMessage,
            sources = photoSources,
            onRemove = viewModel::removePhoto,
            title = "Фото документа",
            emptyHint = "Скан або фото поліса — щоб мати його під рукою",
            onOpen = { uri ->
                val index = state.savedPhotos.indexOf(uri)
                val id = state.docId
                if (index >= 0 && id != null) onOpenPhoto(id, index)
            }
        )

        state.blocker?.let {
            Text(it, color = StatusSoon, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 4.dp))
        }
        Button(
            onClick = viewModel::save,
            enabled = state.blocker == null && !state.isSaving,
            colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = OnAccent),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) { Text(if (state.isNew) "Додати документ" else "Зберегти") }
        val id = state.docId
        if (id != null) {
            OutlinedButton(onClick = { onRenew(id) }, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                Text(if (state.type == DocumentType.INSPECTION) "Внести наступний техогляд" else "Продовжити — внести новий поліс", color = LinkColor)
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    if (pickFrom) {
        AnyDatePickerDialog(initialUtcMillis = state.validFrom, onPicked = viewModel::onValidFrom, onDismiss = { pickFrom = false })
    }
    if (pickUntil) {
        AnyDatePickerDialog(
            initialUtcMillis = state.validUntil ?: state.validFrom?.let(DocumentFormViewModel::defaultUntil),
            onPicked = viewModel::onValidUntil,
            onDismiss = { pickUntil = false }
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Видалити документ?") },
            text = { Text("Разом із фото. Якщо поліс просто закінчився — краще внести новий: старий піде в «Попередні».") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; viewModel.delete() }) { Text("Видалити", color = StatusOverdue) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Скасувати") } }
        )
    }
}

/** «Діє ще 245 днів — до 14 березня 2027» кольором статусу. */
@Composable
private fun PreviewLine(due: DocumentDue) {
    val d = due.daysLeft
    val text = when {
        d < 0 -> "Термін дії закінчився ${formatDaysSigned(-d).removePrefix("−")} тому"
        d == 0L -> "Сьогодні останній день дії"
        else -> "Діє ще $d ${pluralUk(d, "день", "дні", "днів")} — до ${formatLongDate(due.validUntil)} включно"
    }
    Text(
        text,
        color = if (due.status == DueStatus.OK) Accent else due.status.color(),
        fontWeight = FontWeight.SemiBold,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}
