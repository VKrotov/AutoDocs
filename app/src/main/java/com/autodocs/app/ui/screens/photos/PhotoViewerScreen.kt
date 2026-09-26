package com.autodocs.app.ui.screens.photos

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.autodocs.app.AutoDocsApp
import com.autodocs.app.data.PhotoStorage
import com.autodocs.app.data.entity.Photo
import com.autodocs.app.data.entity.PhotoKind
import com.autodocs.app.data.entity.PhotoOwnerType
import com.autodocs.app.ui.components.RoundGlassButton
import com.autodocs.app.ui.components.rememberUriBitmap
import com.autodocs.app.ui.theme.Accent
import com.autodocs.app.ui.theme.TextPrimary
import java.io.File

private fun PhotoKind.caption(): String = when (this) {
    PhotoKind.TECH_PASSPORT_FRONT -> "Техпаспорт · лицьова сторона"
    PhotoKind.TECH_PASSPORT_BACK -> "Техпаспорт · зворотна сторона"
    PhotoKind.SCAN -> "Скан документа"
    PhotoKind.RECORD_PHOTO -> "Фото"
    PhotoKind.CAR_PHOTO -> "Фото авто"
}

/** Повноекранний перегляд фото власника: гортання, зум щипком, подвійний тап, «Поділитися». */
@Composable
fun PhotoViewerScreen(
    ownerType: PhotoOwnerType,
    ownerId: Long,
    startIndex: Int,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as AutoDocsApp
    val photos by remember(ownerType, ownerId) { app.photoRepository.observeFor(ownerType, ownerId) }
        .collectAsState(initial = null)

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        val list = photos
        when {
            list == null -> CircularProgressIndicator(color = Accent, modifier = Modifier.align(Alignment.Center))
            list.isEmpty() -> {
                LaunchedEffect(Unit) { onBack() }
            }
            else -> {
                val pager = rememberPagerState(initialPage = startIndex.coerceIn(0, list.lastIndex)) { list.size }
                var zoomed by remember { mutableStateOf(false) }
                HorizontalPager(state = pager, userScrollEnabled = !zoomed, modifier = Modifier.fillMaxSize()) { page ->
                    ZoomableImage(uri = list[page].uri, onZoomChanged = { zoomed = it })
                }
                val current = list.getOrNull(pager.currentPage)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RoundGlassButton(Icons.AutoMirrored.Filled.ArrowBack, "Назад", onBack)
                    Text(
                        (current?.kind?.caption() ?: "") + if (list.size > 1) "  ·  ${pager.currentPage + 1}/${list.size}" else "",
                        color = TextPrimary,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                    )
                    if (current != null) {
                        RoundGlassButton(Icons.Outlined.Share, "Поділитися", { share(context, current) })
                    }
                }
            }
        }
    }
}

private fun share(context: android.content.Context, photo: Photo) {
    val path = Uri.parse(photo.uri).path ?: return
    val file = File(path)
    if (!file.isFile) return
    val uri = PhotoStorage.contentUriFor(context, file)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "image/jpeg"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(send, "Поділитися").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

@Composable
private fun ZoomableImage(uri: String, onZoomChanged: (Boolean) -> Unit) {
    val bitmap = rememberUriBitmap(uri, maxSide = 2400)
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = {
                    if (scale > 1f) { scale = 1f; offset = Offset.Zero } else scale = 2.5f
                    onZoomChanged(scale > 1f)
                })
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 5f)
                    offset = if (scale == 1f) Offset.Zero else offset + pan
                    onZoomChanged(scale > 1f)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (bitmap == null) {
            CircularProgressIndicator(color = Accent)
        } else {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    }
            )
        }
    }
}
