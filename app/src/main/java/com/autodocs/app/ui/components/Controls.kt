package com.autodocs.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autodocs.app.ui.theme.Accent
import com.autodocs.app.ui.theme.AutoDocsDimens
import com.autodocs.app.ui.theme.LinkColor
import com.autodocs.app.ui.theme.TextPrimary
import com.autodocs.app.ui.theme.TextSecondary

private val RoundButtonBg = Color(0x0DFFFFFF)      // rgba(255,255,255,.05)
private val RoundButtonBorder = Color(0x1AFFFFFF)  // rgba(255,255,255,.10)

/** Кругла скляна кнопка 44dp (шапка екрана: назад, редагувати, архів, видалити…). */
@Composable
fun RoundGlassButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = TextPrimary
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(RoundButtonBg)
            .border(1.dp, RoundButtonBorder, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(22.dp))
    }
}

/** Невелика скляна кнопка-пігулка з текстом кольору посилання («Оновити», «VIN ˅»). */
@Composable
fun GlassPillButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    height: Int = 36,
    content: @Composable RowScope.() -> Unit
) {
    val shape = RoundedCornerShape((height / 2).dp)
    Row(
        modifier = modifier
            .height(height.dp)
            .clip(shape)
            .background(if (highlighted) Accent.copy(alpha = 0.13f) else RoundButtonBg)
            .border(1.dp, if (highlighted) Accent.copy(alpha = 0.40f) else RoundButtonBorder, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        content = content
    )
}

@Composable
fun PillText(text: String, color: Color = LinkColor) {
    Text(text, color = color, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
}

/**
 * Шапка екрана за дизайном: (опційно) кнопка «назад», над заголовком — дрібний
 * надпис, заголовок 27sp, справа — круглі скляні кнопки дій.
 */
@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    overline: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Column(modifier = modifier.fillMaxWidth().padding(top = 12.dp, bottom = 14.dp)) {
        if (onBack != null) {
            RoundGlassButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Назад",
                onClick = onBack,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                if (overline != null) {
                    Text(overline, style = MaterialTheme.typography.labelMedium, fontSize = 13.sp, color = TextSecondary)
                }
                Text(title, style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), content = actions)
        }
    }
}

/** Підпис секції над скляною карткою. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = TextSecondary,
        modifier = modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

/** Кольори полів вводу, спільні для всіх форм. */
@Composable
fun autoDocsFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    disabledTextColor = TextPrimary,
    focusedBorderColor = Accent,
    unfocusedBorderColor = TextSecondary.copy(alpha = 0.4f),
    disabledBorderColor = TextSecondary.copy(alpha = 0.4f),
    focusedLabelColor = Accent,
    unfocusedLabelColor = TextSecondary,
    disabledLabelColor = TextSecondary,
    disabledTrailingIconColor = TextSecondary,
    cursorColor = Accent
)

val ScreenHorizontalPadding = AutoDocsDimens.ScreenPadding
