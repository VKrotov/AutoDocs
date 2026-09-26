package com.autodocs.app.ui.screens.record

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autodocs.app.data.repository.PhotoRepository
import com.autodocs.app.ui.components.GlassPillButton
import com.autodocs.app.ui.components.PhotoSources
import com.autodocs.app.ui.components.PhotoStrip
import com.autodocs.app.ui.components.PillText
import com.autodocs.app.ui.components.SectionLabel
import com.autodocs.app.ui.theme.Accent
import com.autodocs.app.ui.theme.StatusSoon
import com.autodocs.app.ui.theme.TextSecondary

/** F05/F25: фото й скани (чеки, акти) у формі запису — до 5 шт. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecordPhotosSection(
    photos: List<String>,
    slotsLeft: Int,
    importing: Boolean,
    message: String?,
    sources: PhotoSources,
    onRemove: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel("Фото й документи · ${photos.size}/${PhotoRepository.MAX_PER_RECORD}", Modifier.padding(top = 8.dp))
        if (photos.isNotEmpty()) {
            PhotoStrip(uris = photos, onOpen = {}, onRemove = { index -> onRemove(photos[index]) })
        } else {
            Text(
                "Чек, акт виконаних робіт, фото деталей — скануй документ або додай фото",
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 13.sp,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
        if (importing) {
            CircularProgressIndicator(color = Accent, modifier = Modifier.size(24.dp))
        } else if (slotsLeft > 0) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SourceButton(Icons.Outlined.DocumentScanner, "Сканувати") { sources.scan(slotsLeft) }
                SourceButton(Icons.Outlined.PhotoCamera, "Камера") { sources.camera() }
                SourceButton(Icons.Outlined.PhotoLibrary, "Галерея") { sources.gallery(slotsLeft) }
            }
        }
        message?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, fontSize = 13.sp, color = StatusSoon, modifier = Modifier.padding(horizontal = 4.dp))
        }
    }
}

@Composable
private fun SourceButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    GlassPillButton(onClick = onClick) {
        Icon(icon, contentDescription = null, tint = Accent, modifier = Modifier.size(18.dp))
        PillText(label)
    }
}
