package com.autodocs.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.autodocs.app.ui.theme.GlassBorder
import com.autodocs.app.ui.theme.GlassSurface
import com.autodocs.app.ui.theme.TextSecondary

/** Мініатюра фото/скану (квадрат з заокругленням). */
@Composable
fun PhotoThumb(uri: String, size: Dp = 76.dp, onClick: (() -> Unit)? = null, onRemove: (() -> Unit)? = null) {
    val shape = RoundedCornerShape(12.dp)
    val bitmap = rememberUriBitmap(uri, maxSide = 360)
    Box(modifier = Modifier.size(size)) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(shape)
                .background(GlassSurface)
                .border(1.dp, GlassBorder, shape)
                .let { if (onClick != null) it.clickable(onClick = onClick) else it },
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {
                Image(bitmap.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(size))
            } else {
                Icon(Icons.Outlined.Description, contentDescription = null, tint = TextSecondary)
            }
        }
        if (onRemove != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color(0xCC000000))
                    .clickable(onClick = onRemove),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Прибрати фото", tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }
    }
}

/** Горизонтальна стрічка мініатюр. */
@Composable
fun PhotoStrip(uris: List<String>, onOpen: (Int) -> Unit, onRemove: ((Int) -> Unit)? = null) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        itemsIndexed(uris, key = { _, u -> u }) { index, uri ->
            PhotoThumb(
                uri = uri,
                onClick = { onOpen(index) },
                onRemove = onRemove?.let { remove -> { remove(index) } }
            )
        }
    }
}
