package com.autodocs.app.ui.screens.record

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.autodocs.app.AutoDocsApp
import com.autodocs.app.data.entity.ServiceRecordItem
import com.autodocs.app.data.entity.WorkItemCategory
import com.autodocs.app.data.entity.displayName
import com.autodocs.app.data.entity.total
import com.autodocs.app.ui.components.GlassSurface
import com.autodocs.app.ui.components.PhotoStrip
import com.autodocs.app.ui.components.RoundGlassButton
import com.autodocs.app.ui.components.ScreenHeader
import com.autodocs.app.ui.components.SectionLabel
import com.autodocs.app.ui.theme.AutoDocsDimens
import com.autodocs.app.ui.theme.StatusOverdue
import com.autodocs.app.ui.theme.TextPrimary
import com.autodocs.app.ui.theme.TextSecondary
import com.autodocs.app.ui.util.formatKm
import com.autodocs.app.ui.util.formatMoney
import com.autodocs.app.ui.util.formatRecordDate
import com.autodocs.app.ui.util.pluralLabel

/** Перегляд одного запису журналу: позиції з цінами, підсумки, СТО, нотатки. */
@Composable
fun RecordDetailScreen(
    recordId: Long,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onOpenPhoto: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val app = LocalContext.current.applicationContext as AutoDocsApp
    val viewModel: RecordDetailViewModel = viewModel(
        key = "record_detail_$recordId",
        factory = RecordDetailViewModelFactory(recordId, app.serviceRepository, app.photoRepository)
    )
    val state by viewModel.state.collectAsState()
    val photos by viewModel.photos.collectAsState()
    var confirmDelete by remember { mutableStateOf(false) }

    val data = state.record
    LaunchedEffect(state.isLoaded, data) {
        if (state.isLoaded && data == null) onBack()
    }
    if (data == null) {
        Box(modifier.fillMaxSize())
        return
    }
    val record = data.record

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AutoDocsDimens.ScreenPadding)
    ) {
        ScreenHeader(
            title = formatRecordDate(record.date),
            overline = "Запис журналу",
            onBack = onBack
        ) {
            RoundGlassButton(Icons.Outlined.Edit, "Редагувати запис", { onEdit(record.id) })
            RoundGlassButton(Icons.Outlined.Delete, "Видалити запис", { confirmDelete = true }, tint = StatusOverdue)
        }

        GlassSurface(modifier = Modifier.fillMaxWidth(), contentPadding = 18.dp) {
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Разом", style = MaterialTheme.typography.bodyMedium, fontSize = 13.sp, color = TextSecondary)
                    Text(
                        "${formatMoney(data.total())} ₴",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Пробіг", style = MaterialTheme.typography.bodyMedium, fontSize = 13.sp, color = TextSecondary)
                    Text("${formatKm(record.mileage)} км", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                }
            }
            record.stoName?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(12.dp))
                Text("СТО", style = MaterialTheme.typography.bodyMedium, fontSize = 13.sp, color = TextSecondary)
                Text(it, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
            }
        }

        WorkItemCategory.entries.forEach { category ->
            val items = data.items.filter { it.category == category }
            if (items.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                SectionLabel(category.pluralLabel())
                ItemsCard(items)
            }
        }

        if (photos.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            SectionLabel("Фото й документи · ${photos.size}")
            PhotoStrip(uris = photos.map { it.uri }, onOpen = onOpenPhoto)
        }

        record.notes?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(20.dp))
            SectionLabel("Нотатки")
            GlassSurface(modifier = Modifier.fillMaxWidth(), cornerRadius = AutoDocsDimens.CardRadiusInner) {
                Text(it, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
            }
        }

        Spacer(Modifier.height(24.dp))
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Видалити запис?") },
            text = { Text("Запис від ${formatRecordDate(record.date)}, усі його позиції та фото буде видалено без можливості відновлення.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.delete()
                }) { Text("Видалити", color = StatusOverdue) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Скасувати") }
            }
        )
    }
}

@Composable
private fun ItemsCard(items: List<ServiceRecordItem>) {
    GlassSurface(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = AutoDocsDimens.CardRadiusInner,
        contentPadding = 0.dp
    ) {
        items.forEachIndexed { index, item ->
            if (index > 0) HorizontalDivider(color = TextSecondary.copy(alpha = 0.12f), modifier = Modifier.padding(start = 14.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    item.displayName(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    if (item.price > 0) "${formatMoney(item.price)} ₴" else "—",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (item.price > 0) TextPrimary else TextSecondary
                )
            }
        }
        if (items.size > 1) {
            HorizontalDivider(color = TextSecondary.copy(alpha = 0.12f))
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text("Підсумок", style = MaterialTheme.typography.bodyMedium, color = TextSecondary, modifier = Modifier.weight(1f))
                Text(
                    "${formatMoney(items.sumOf { it.price })} ₴",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
    }
}
