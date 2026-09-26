package com.autodocs.app.ui.screens.worktypes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.autodocs.app.AutoDocsApp
import com.autodocs.app.data.entity.WorkItemCategory
import com.autodocs.app.data.entity.WorkType
import com.autodocs.app.ui.components.RoundGlassButton
import com.autodocs.app.ui.components.ScreenHeader
import com.autodocs.app.ui.components.autoDocsFieldColors
import com.autodocs.app.ui.theme.Accent
import com.autodocs.app.ui.theme.AutoDocsDimens
import com.autodocs.app.ui.theme.GlassBorder
import com.autodocs.app.ui.theme.GlassSurface
import com.autodocs.app.ui.theme.StatusOverdue
import com.autodocs.app.ui.theme.TextPrimary
import com.autodocs.app.ui.theme.TextSecondary
import com.autodocs.app.ui.util.pluralLabel

/** F06: довідник робіт і запчастин — підказки у формі запису. */
@Composable
fun WorkTypesScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val app = LocalContext.current.applicationContext as AutoDocsApp
    val viewModel: WorkTypesViewModel = viewModel(factory = WorkTypesViewModelFactory(app.serviceRepository))
    val all by viewModel.workTypes.collectAsState()

    var category by rememberSaveable { mutableStateOf(WorkItemCategory.ROBOTA) }
    var showAdd by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<WorkType?>(null) }
    var deleting by remember { mutableStateOf<WorkType?>(null) }

    val list = all.filter { it.category == category }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = AutoDocsDimens.ScreenPadding),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            ScreenHeader(title = "Довідник", overline = "Роботи й запчастини", onBack = onBack) {
                RoundGlassButton(Icons.Filled.Add, "Додати пункт", { showAdd = true }, tint = Accent)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 12.dp)) {
                WorkItemCategory.entries.forEach { c ->
                    FilterChip(
                        selected = category == c,
                        onClick = { category = c },
                        label = { Text("${c.pluralLabel()} · ${all.count { it.category == c }}") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Accent.copy(alpha = 0.16f),
                            selectedLabelColor = TextPrimary,
                            labelColor = TextSecondary
                        )
                    )
                }
            }
            Text(
                "Нові назви з форми запису додаються сюди автоматично. Натисни на пункт, щоб перейменувати.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
            )
        }
        itemsIndexed(list, key = { _, it -> it.id }) { index, workType ->
            // Суцільна скляна "картка" зі списку рядків: закруглюємо лише перший і останній.
            val top = if (index == 0) AutoDocsDimens.CardRadiusInner else 0.dp
            val bottom = if (index == list.lastIndex) AutoDocsDimens.CardRadiusInner else 0.dp
            val shape = RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(GlassSurface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { renaming = workType }
                        .padding(start = 14.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        workType.name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f).padding(vertical = 10.dp)
                    )
                    IconButton(onClick = { deleting = workType }) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Видалити", tint = TextSecondary)
                    }
                }
                if (index != list.lastIndex) {
                    HorizontalDivider(color = GlassBorder, modifier = Modifier.padding(start = 14.dp))
                }
            }
        }
    }

    if (showAdd) {
        NameDialog(
            title = "Новий пункт: ${if (category == WorkItemCategory.ROBOTA) "робота" else "запчастина"}",
            initial = "",
            confirmText = "Додати",
            onConfirm = { name, done -> viewModel.add(name, category, done) },
            onDismiss = { showAdd = false }
        )
    }
    renaming?.let { wt ->
        NameDialog(
            title = "Перейменувати",
            initial = wt.name,
            confirmText = "Зберегти",
            onConfirm = { name, done -> viewModel.rename(wt, name, done) },
            onDismiss = { renaming = null }
        )
    }
    deleting?.let { wt ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Видалити «${wt.name}»?") },
            text = { Text("Зі старих записів журналу ця назва не зникне — вона там збережена окремо.") },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(wt); deleting = null }) { Text("Видалити", color = StatusOverdue) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Скасувати") } }
        )
    }
}

@Composable
private fun NameDialog(
    title: String,
    initial: String,
    confirmText: String,
    onConfirm: (String, (Boolean) -> Unit) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initial) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it; error = null },
                label = { Text("Назва") },
                singleLine = true,
                isError = error != null,
                supportingText = error?.let { msg -> { Text(msg) } },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                colors = autoDocsFieldColors()
            )
        },
        confirmButton = {
            TextButton(
                enabled = text.isNotBlank(),
                onClick = {
                    onConfirm(text) { ok -> if (ok) onDismiss() else error = "Такий пункт уже є" }
                }
            ) { Text(confirmText, color = Accent) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Скасувати") } }
    )
}
