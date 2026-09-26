package com.autodocs.app.ui.screens.record

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.autodocs.app.AutoDocsApp
import com.autodocs.app.data.entity.WorkItemCategory
import com.autodocs.app.data.entity.WorkType
import com.autodocs.app.ui.components.GlassPillButton
import com.autodocs.app.ui.components.GlassSurface
import com.autodocs.app.ui.components.PillText
import com.autodocs.app.ui.components.ScreenHeader
import com.autodocs.app.ui.components.SectionLabel
import com.autodocs.app.ui.components.SuggestTextField
import com.autodocs.app.ui.components.rememberPhotoSources
import com.autodocs.app.ui.components.autoDocsFieldColors
import com.autodocs.app.ui.theme.Accent
import com.autodocs.app.ui.theme.AutoDocsDimens
import com.autodocs.app.ui.theme.OnAccent
import com.autodocs.app.ui.theme.StatusSoon
import com.autodocs.app.ui.theme.TextPrimary
import com.autodocs.app.ui.theme.TextSecondary
import com.autodocs.app.ui.util.formatMoney
import com.autodocs.app.ui.util.formatRecordDate
import com.autodocs.app.ui.util.label
import com.autodocs.app.ui.util.todayUtcMillis

/** F04: створення/редагування запису журналу — дата, пробіг, СТО, позиції з цінами, нотатки. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordFormScreen(
    recordIdToEdit: Long?,
    onSaved: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val app = LocalContext.current.applicationContext as AutoDocsApp
    val viewModel: RecordFormViewModel = viewModel(
        key = "record_form_${recordIdToEdit ?: "new"}",
        factory = RecordFormViewModelFactory(app, app.carRepository, app.serviceRepository, app.photoRepository)
    )
    val state by viewModel.state.collectAsState()
    val workTypes by viewModel.workTypes.collectAsState()
    val stoNames by viewModel.stoNames.collectAsState()
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    val photoSources = rememberPhotoSources(
        onPicked = { uris, isScan -> viewModel.onPhotosPicked(uris, isScan) },
        onError = viewModel::onPhotoError
    )

    LaunchedEffect(recordIdToEdit) { viewModel.init(recordIdToEdit) }
    LaunchedEffect(state.isSaved, state.noActiveCar) {
        // Подія «збережено» спрацьовує рівно один раз, навіть якщо екран відтворено знову.
        if ((state.isSaved || state.noActiveCar) && viewModel.consumeFinishEvent()) onSaved()
    }

    if (state.isLoading) {
        Box(modifier.fillMaxSize())
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = AutoDocsDimens.ScreenPadding),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ScreenHeader(
                title = if (state.recordId == null) "Новий запис" else "Редагувати запис",
                overline = state.carName.ifBlank { null },
                onBack = onBack
            )
        }

        item { SectionLabel("Основне") }
        item {
            // Поле дати лише для читання: клік відкриває календар.
            Box {
                OutlinedTextField(
                    value = formatRecordDate(state.date),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Дата") },
                    trailingIcon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = null) },
                    colors = autoDocsFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Box(Modifier.matchParentSize().clickable { showDatePicker = true })
            }
        }
        item {
            OutlinedTextField(
                value = state.mileageText,
                onValueChange = viewModel::onMileageChange,
                label = { Text("Пробіг") },
                suffix = { Text("км", color = TextSecondary) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = autoDocsFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            SuggestTextField(
                value = state.stoName,
                onValueChange = viewModel::onStoChange,
                label = "СТО / хто робив (необов'язково)",
                suggestions = stoNames.filter {
                    it.contains(state.stoName.trim(), ignoreCase = true) && !it.equals(state.stoName.trim(), ignoreCase = true)
                },
                suggestionText = { it },
                onSuggestionPicked = viewModel::onStoChange
            )
        }

        item { SectionLabel("Роботи й запчастини", Modifier.padding(top = 8.dp)) }
        items(state.items, key = { it.key }) { draft ->
            ItemEditor(
                draft = draft,
                workTypes = workTypes,
                canRemove = state.items.size > 1,
                onCategoryChange = { viewModel.onItemCategoryChange(draft.key, it) },
                onNameChange = { viewModel.onItemNameChange(draft.key, it) },
                onPicked = { viewModel.onItemPicked(draft.key, it) },
                onPriceChange = { viewModel.onItemPriceChange(draft.key, it) },
                onRemove = { viewModel.removeItem(draft.key) }
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassPillButton(onClick = { viewModel.addItem(WorkItemCategory.ROBOTA) }) {
                    Icon(Icons.Filled.Add, contentDescription = null, tint = Accent, modifier = Modifier.size(18.dp))
                    PillText("Робота")
                }
                GlassPillButton(onClick = { viewModel.addItem(WorkItemCategory.ZAPCHASTYNA) }) {
                    Icon(Icons.Filled.Add, contentDescription = null, tint = Accent, modifier = Modifier.size(18.dp))
                    PillText("Запчастина")
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Text("Разом", style = MaterialTheme.typography.bodyLarge, color = TextSecondary, modifier = Modifier.weight(1f))
                Text(
                    "${formatMoney(state.total)} ₴",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        }

        item {
            RecordPhotosSection(
                photos = state.photos.map { it.uri },
                slotsLeft = state.photoSlotsLeft,
                importing = state.isImportingPhotos,
                message = state.photoMessage,
                sources = photoSources,
                onRemove = viewModel::removePhoto
            )
        }

        item { SectionLabel("Нотатки", Modifier.padding(top = 8.dp)) }
        item {
            OutlinedTextField(
                value = state.notes,
                onValueChange = viewModel::onNotesChange,
                label = { Text("Що ще варто пам'ятати (необов'язково)") },
                minLines = 2,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                colors = autoDocsFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            val blocker = state.blocker
            // Причину, чому не можна зберегти, показуємо НАД кнопкою — щоб її було видно.
            if (blocker != null) {
                Text(
                    blocker,
                    style = MaterialTheme.typography.bodyMedium,
                    color = StatusSoon,
                    modifier = Modifier.padding(top = 8.dp, start = 4.dp)
                )
            }
            Button(
                onClick = viewModel::save,
                enabled = blocker == null && !state.isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = OnAccent),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(52.dp)
            ) {
                Text(if (state.recordId == null) "Зберегти запис" else "Зберегти зміни")
            }
        }
    }

    if (showDatePicker) {
        val today = remember { todayUtcMillis() }
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.date,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis <= today
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let(viewModel::onDateChange)
                    showDatePicker = false
                }) { Text("Готово", color = Accent) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Скасувати") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ItemEditor(
    draft: ItemDraft,
    workTypes: List<WorkType>,
    canRemove: Boolean,
    onCategoryChange: (WorkItemCategory) -> Unit,
    onNameChange: (String) -> Unit,
    onPicked: (WorkType) -> Unit,
    onPriceChange: (String) -> Unit,
    onRemove: () -> Unit
) {
    GlassSurface(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = AutoDocsDimens.CardRadiusInner,
        contentPadding = 12.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WorkItemCategory.entries.forEach { category ->
                    FilterChip(
                        selected = draft.category == category,
                        onClick = { onCategoryChange(category) },
                        label = { Text(category.label()) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Accent.copy(alpha = 0.16f),
                            selectedLabelColor = TextPrimary,
                            labelColor = TextSecondary
                        )
                    )
                }
                Box(Modifier.weight(1f))
                if (canRemove) {
                    IconButton(onClick = onRemove) {
                        Icon(Icons.Outlined.Close, contentDescription = "Прибрати позицію", tint = TextSecondary)
                    }
                }
            }
            val query = draft.name.trim()
            SuggestTextField(
                value = draft.name,
                onValueChange = onNameChange,
                label = if (draft.category == WorkItemCategory.ROBOTA) "Що робили" else "Що купили / поставили",
                suggestions = workTypes.filter {
                    it.category == draft.category &&
                        it.name.contains(query, ignoreCase = true) &&
                        !it.name.equals(query, ignoreCase = true)
                },
                suggestionText = { it.name },
                onSuggestionPicked = onPicked
            )
            OutlinedTextField(
                value = draft.priceText,
                onValueChange = onPriceChange,
                label = { Text("Ціна") },
                suffix = { Text("₴", color = TextSecondary) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = autoDocsFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
