package com.autodocs.app.ui.screens.photos

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autodocs.app.AutoDocsApp
import com.autodocs.app.data.PhotoStorage
import com.autodocs.app.data.entity.PhotoKind
import com.autodocs.app.data.entity.PhotoOwnerType
import com.autodocs.app.ui.components.GlassPillButton
import com.autodocs.app.ui.components.PillText
import com.autodocs.app.ui.components.ScreenHeader
import com.autodocs.app.ui.components.SectionLabel
import com.autodocs.app.ui.components.rememberPhotoSources
import com.autodocs.app.ui.components.rememberUriBitmap
import com.autodocs.app.ui.theme.Accent
import com.autodocs.app.ui.theme.AutoDocsDimens
import com.autodocs.app.ui.theme.GlassBorder
import com.autodocs.app.ui.theme.GlassSurface
import com.autodocs.app.ui.theme.StatusOverdue
import com.autodocs.app.ui.theme.StatusSoon
import com.autodocs.app.ui.theme.TextSecondary
import kotlinx.coroutines.launch

/** F26: фото техпаспорта (ID-картка, 2 сторони) активного авто. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TechPassportScreen(carId: Long, onBack: () -> Unit, onOpenPhoto: (Int) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val app = context.applicationContext as AutoDocsApp
    val scope = rememberCoroutineScope()
    val photos by remember(carId) { app.photoRepository.observeFor(PhotoOwnerType.CAR, carId) }.collectAsState(initial = emptyList())
    var target by rememberSaveable { mutableStateOf<PhotoKind?>(null) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    val sources = rememberPhotoSources(
        onPicked = { uris, _ ->
            val kind = target
            val first = uris.firstOrNull()
            if (kind != null && first != null) {
                scope.launch {
                    busy = true
                    val stored = PhotoStorage.importImage(app, first, "passport", PhotoStorage.DOCUMENT_MAX_SIDE)
                    if (stored != null) app.photoRepository.setSingle(PhotoOwnerType.CAR, carId, kind, stored)
                    else message = "Не вдалося відкрити фото"
                    busy = false
                }
            }
        },
        onError = { message = it }
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AutoDocsDimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ScreenHeader(title = "Техпаспорт", overline = "Документи авто", onBack = onBack)
        Text(
            "Свідоцтво про реєстрацію (ID-картка) — обидві сторони. Сканер сам знайде межі картки й вирівняє її.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        listOf(PhotoKind.TECH_PASSPORT_FRONT to "Лицьова сторона", PhotoKind.TECH_PASSPORT_BACK to "Зворотна сторона").forEach { (kind, title) ->
            val photo = photos.firstOrNull { it.kind == kind }
            SectionLabel(title, Modifier.padding(top = 8.dp))
            CardSlot(
                uri = photo?.uri,
                onOpen = { photo?.let { p -> onOpenPhoto(photos.indexOf(p)) } },
                onAdd = { target = kind; sources.scan(1) }
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassPillButton(onClick = { target = kind; sources.scan(1) }) {
                    Icon(Icons.Outlined.DocumentScanner, contentDescription = null, tint = Accent, modifier = Modifier.size(18.dp))
                    PillText(if (photo == null) "Сканувати" else "Переснути")
                }
                GlassPillButton(onClick = { target = kind; sources.gallery(1) }) {
                    Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, tint = Accent, modifier = Modifier.size(18.dp))
                    PillText("Галерея")
                }
                if (photo != null) {
                    TextButton(onClick = {
                        scope.launch { app.photoRepository.setSingle(PhotoOwnerType.CAR, carId, kind, null) }
                    }) { Text("Видалити", color = StatusOverdue) }
                }
            }
        }
        if (busy) CircularProgressIndicator(color = Accent, modifier = Modifier.size(24.dp))
        message?.let { Text(it, color = StatusSoon, fontSize = 13.sp, style = MaterialTheme.typography.bodyMedium) }
        androidx.compose.foundation.layout.Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun CardSlot(uri: String?, onOpen: () -> Unit, onAdd: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(85.6f / 54f) // пропорції ID-картки
            .clip(shape)
            .background(GlassSurface)
            .border(1.dp, GlassBorder, shape)
            .clickable(onClick = if (uri != null) onOpen else onAdd),
        contentAlignment = Alignment.Center
    ) {
        if (uri != null) {
            val bitmap = rememberUriBitmap(uri, maxSide = 1200)
            if (bitmap != null) {
                Image(bitmap.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                CircularProgressIndicator(color = Accent, modifier = Modifier.size(24.dp))
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Outlined.AddAPhoto, contentDescription = null, tint = TextSecondary)
                Text("Додати", color = TextSecondary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp))
            }
        }
    }
}

