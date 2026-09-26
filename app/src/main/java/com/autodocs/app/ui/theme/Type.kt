package com.autodocs.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.autodocs.app.R

// Onest — змінний (variable) шрифт: різні нарізки отримуємо через FontVariation
// на одному й тому ж файлі, а не окремими файлами на кожну вагу.
val OnestFamily = FontFamily(
    Font(R.font.onest, weight = FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.onest, weight = FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.onest, weight = FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.onest, weight = FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700)))
)

// JetBrains Mono — для VIN та інших моноширинних значень.
val JetBrainsMonoFamily = FontFamily(
    Font(R.font.jetbrains_mono_regular, weight = FontWeight.Normal),
    Font(R.font.jetbrains_mono_medium, weight = FontWeight.Medium)
)

val AutoDocsTypography = Typography(
    headlineMedium = TextStyle( // заголовок "Мій Passat" (27px bold за дизайном)
        fontFamily = OnestFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 27.sp,
        lineHeight = 32.sp
    ),
    titleLarge = TextStyle( // великий пробіг (30px)
        fontFamily = OnestFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 30.sp,
        lineHeight = 36.sp
    ),
    titleMedium = TextStyle(
        fontFamily = OnestFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 24.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = OnestFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = OnestFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle( // підписи в нижньому меню
        fontFamily = OnestFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontFamily = OnestFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp
    )
)

/** Моноширинний стиль для VIN (пігулка → розгорнутий рядок). */
val VinTextStyle = TextStyle(
    fontFamily = JetBrainsMonoFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 14.sp,
    lineHeight = 20.sp
)
