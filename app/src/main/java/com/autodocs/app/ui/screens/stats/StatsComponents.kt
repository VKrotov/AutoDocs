package com.autodocs.app.ui.screens.stats

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autodocs.app.ui.components.GlassPillButton
import com.autodocs.app.ui.components.GlassSurface
import com.autodocs.app.ui.components.PillText
import com.autodocs.app.ui.theme.AutoDocsDimens
import com.autodocs.app.ui.theme.TextPrimary
import com.autodocs.app.ui.theme.TextSecondary
import java.time.LocalDate
import java.time.temporal.ChronoUnit

internal val MonthShort = listOf("січ", "лют", "бер", "кві", "тра", "чер", "лип", "сер", "вер", "жов", "лис", "гру")
internal val MonthFull = listOf(
    "Січень", "Лютий", "Березень", "Квітень", "Травень", "Червень",
    "Липень", "Серпень", "Вересень", "Жовтень", "Листопад", "Грудень"
)

/** Ряд пігулок-перемикачів (період, рік…). Прокручується, якщо не влазить. */
@Composable
fun <T> ChoiceChips(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { option ->
            GlassPillButton(onClick = { onSelect(option) }, highlighted = option == selected, height = 34) {
                PillText(label(option), color = if (option == selected) TextPrimary else TextSecondary)
            }
        }
    }
}

/** Плитка з одним числом: підпис зверху, значення, (опційно) пояснення. */
@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier, hint: String? = null) {
    GlassSurface(modifier = modifier, cornerRadius = AutoDocsDimens.CardRadiusInner, contentPadding = 14.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, fontSize = 13.sp, color = TextSecondary, maxLines = 1)
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (hint != null) {
                Text(hint, style = MaterialTheme.typography.bodyMedium, fontSize = 12.sp, color = TextSecondary, maxLines = 1)
            }
        }
    }
}

/** Рядок-«підказка» над графіком: що вибрано тапом (або запрошення торкнутись). */
@Composable
fun ChartReadout(title: String?, value: String?, placeholder: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (title != null && value != null) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, modifier = Modifier.weight(1f), maxLines = 1)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        } else {
            Text(placeholder, style = MaterialTheme.typography.bodyMedium, fontSize = 13.sp, color = TextSecondary)
        }
    }
}

/**
 * Поділки осі часу: перші числа місяців з кроком 1/2/3/6/12/24 міс, щоб вийшло ≤ 5 підписів.
 * Січень підписуємо роком, решту — коротким місяцем; при кроці ≥ 12 міс — лише роки.
 */
internal fun timeTicks(from: LocalDate, to: LocalDate, maxTicks: Int = 5): List<Pair<Long, String>> {
    val months = ChronoUnit.MONTHS.between(from.withDayOfMonth(1), to.withDayOfMonth(1)).coerceAtLeast(1)
    val step = listOf(1L, 2L, 3L, 6L, 12L, 24L, 60L).firstOrNull { months / it + 1 <= maxTicks } ?: 120L
    var d = from.withDayOfMonth(1).plusMonths(1)
    if (step >= 12) d = LocalDate.of(from.year + 1, 1, 1)
    else while ((d.monthValue - 1) % step != 0L) d = d.plusMonths(1)
    val out = mutableListOf<Pair<Long, String>>()
    while (!d.isAfter(to)) {
        val label = if (d.monthValue == 1) d.year.toString() else MonthShort[d.monthValue - 1]
        out += d.toEpochDay() to label
        d = d.plusMonths(step)
    }
    return out
}
