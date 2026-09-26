package com.autodocs.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.autodocs.app.ui.theme.AutoDocsDimens
import com.autodocs.app.ui.theme.GlassBorder
import com.autodocs.app.ui.theme.GlassSurface

/**
 * Базова "скляна" картка дизайн-системи: напівпрозора заливка + тонка рамка.
 * Справжній blur-ефект (backdrop blur 24) Compose на цій версії Android
 * додамо окремо через Modifier.blur/RenderEffect, коли дійдемо до реальних
 * екранів (не критично для каркаса етапу 1).
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = AutoDocsDimens.CardRadiusOuter,
    contentPadding: Dp = 16.dp,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    Column(
        modifier = modifier
            .clip(shape)
            .background(GlassSurface)
            .border(1.dp, GlassBorder, shape)
            .padding(contentPadding)
    ) {
        content()
    }
}
