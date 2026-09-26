package com.autodocs.app.ui.components

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.autodocs.app.data.PhotoStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Асинхронно завантажує зменшену копію зображення (не блокує UI-потік). */
@Composable
fun rememberUriBitmap(uri: String?, maxSide: Int = 1280): Bitmap? {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(initialValue = null, uri) {
        value = if (uri == null) null else withContext(Dispatchers.IO) {
            PhotoStorage.decodeScaled(context, Uri.parse(uri), maxSide)
        }
    }
    return bitmap
}

/**
 * М'яке згасання країв: центр зображення лишається як є, а до країв воно плавно
 * стає прозорим і "розчиняється" в темному склі картки — без гострих рамок.
 * [horizontal]/[vertical] — яка частка ширини/висоти з кожного боку згасає.
 */
fun Modifier.fadedEdges(horizontal: Float = 0.12f, vertical: Float = 0.16f): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        drawRect(
            brush = Brush.horizontalGradient(
                0f to Color.Transparent,
                horizontal to Color.Black,
                1f - horizontal to Color.Black,
                1f to Color.Transparent
            ),
            blendMode = BlendMode.DstIn
        )
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Transparent,
                vertical to Color.Black,
                1f - vertical to Color.Black,
                1f to Color.Transparent
            ),
            blendMode = BlendMode.DstIn
        )
    }

/** Фото авто з м'яко згаслими краями. Нічого не малює, поки фото вантажиться/відсутнє. */
@Composable
fun CarPhoto(uri: String?, modifier: Modifier = Modifier) {
    val bitmap = rememberUriBitmap(uri)
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.fadedEdges()
        )
    }
}
