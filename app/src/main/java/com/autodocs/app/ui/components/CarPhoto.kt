package com.autodocs.app.ui.components

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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

/** Фото авто із заокругленими кутами. Нічого не малює, поки фото вантажиться/відсутнє. */
@Composable
fun CarPhoto(uri: String?, modifier: Modifier = Modifier) {
    val bitmap = rememberUriBitmap(uri)
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(RoundedCornerShape(14.dp))
        )
    }
}
