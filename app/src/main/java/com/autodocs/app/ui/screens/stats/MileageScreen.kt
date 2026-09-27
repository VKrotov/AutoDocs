package com.autodocs.app.ui.screens.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.autodocs.app.AutoDocsApp
import com.autodocs.app.data.stats.MileageHistoryPoint
import com.autodocs.app.data.stats.MileageSource
import com.autodocs.app.ui.components.GlassSurface
import com.autodocs.app.ui.components.ScreenHeader
import com.autodocs.app.ui.components.SectionLabel
import com.autodocs.app.ui.components.TimeLineChart
import com.autodocs.app.ui.theme.AutoDocsDimens
import com.autodocs.app.ui.theme.StatusOverdue
import com.autodocs.app.ui.theme.TextPrimary
import com.autodocs.app.ui.theme.TextSecondary
import com.autodocs.app.ui.util.formatKm
import com.autodocs.app.ui.util.formatLongDate
import com.autodocs.app.ui.util.formatUpdatedAgo
import kotlin.math.roundToInt

/** Етап 8: графік історії пробігу + список відміток з видаленням помилкових. */
@Composable
fun MileageScreen(
    onBack: () -> Unit,
    onOpenRecord: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val app = LocalContext.current.applicationContext as AutoDocsApp
    val viewModel: MileageViewModel = viewModel(factory = MileageViewModelFactory(app.carRepository, app.mileageRepository))
    val state by viewModel.state.collectAsState()
    MileageContent(state, onBack, onOpenRecord, viewModel::setRange, viewModel::deleteEntry, modifier)
}

@Composable
internal fun MileageContent(
    state: MileageState,
    onBack: () -> Unit,
    onOpenRecord: (Long) -> Unit,
    onRange: (MileageRange) -> Unit,
    onDeleteEntry: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var toDelete by remember { mutableStateOf<MileageHistoryPoint?>(null) }

    val car = state.car
    if (!state.isLoaded || car == null) {
        Column(modifier.fillMaxSize().padding(horizontal = AutoDocsDimens.ScreenPadding)) {
            ScreenHeader(title = "Пробіг", onBack = onBack)
        }
        return
    }
    val history = state.points.asReversed() // нові зверху

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = AutoDocsDimens.ScreenPadding),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { ScreenHeader(title = "Пробіг", overline = car.name, onBack = onBack) }
        item { SummaryCard(car.mileage, car.mileageUpdatedAt, state.kmPerDay, state.lastYearKm) }
        item { ChartCard(state, onRange = onRange) }
        if (state.suspiciousCount > 0) {
            item { SuspiciousBanner(state.suspiciousCount) }
        }
        if (history.isNotEmpty()) {
            item {
                SectionLabel("Історія · ${history.size}", modifier = Modifier.padding(top = 4.dp))
                GlassSurface(modifier = Modifier.fillMaxWidth(), cornerRadius = AutoDocsDimens.CardRadiusInner, contentPadding = 0.dp) {
                    history.forEachIndexed { index, p ->
                        if (index > 0) HorizontalDivider(color = Color(0x0FFFFFFF), modifier = Modifier.padding(start = 14.dp))
                        HistoryRow(
                            point = p,
                            onDelete = { toDelete = p },
                            onOpenRecord = { onOpenRecord(p.refId) }
                        )
                    }
                }
            }
            item {
                Text(
                    "Відмітки з'являються, коли оновлюєш пробіг на головній або додаєш запис у журнал. " +
                        "Помилкову відмітку можна видалити — прогноз і план ТО перерахуються.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 13.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }

    toDelete?.let { p ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Видалити відмітку?") },
            text = { Text("${formatKm(p.mileage)} км · ${formatLongDate(p.date)}. Якщо це поточний пробіг авто, він повернеться до попередньої відмітки.") },
            confirmButton = {
                TextButton(onClick = { onDeleteEntry(p.refId); toDelete = null }) { Text("Видалити", color = StatusOverdue) }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Скасувати") } }
        )
    }
}

@Composable
private fun SummaryCard(mileage: Int, updatedAt: Long, kmPerDay: Double?, lastYearKm: Int?) {
    GlassSurface(modifier = Modifier.fillMaxWidth(), contentPadding = 18.dp) {
        Text(
            if (mileage > 0) "Пробіг · ${formatUpdatedAgo(updatedAt)}" else "Пробіг не вказано",
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 13.sp,
            color = TextSecondary
        )
        Text(
            buildAnnotatedString {
                if (mileage > 0) {
                    append(formatKm(mileage))
                    withStyle(SpanStyle(fontSize = 17.sp, fontWeight = FontWeight.Medium, color = TextSecondary)) { append(" км") }
                } else append("—")
            },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MiniStat("км/день", kmPerDay?.let { it.roundToInt().toString() } ?: "—", Modifier.weight(1f))
            MiniStat("км/міс", kmPerDay?.let { formatKm((it * 30).roundToInt()) } ?: "—", Modifier.weight(1f))
            MiniStat("за 12 міс", lastYearKm?.let { formatKm(it) } ?: "—", Modifier.weight(1f))
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = TextPrimary, maxLines = 1)
        Text(label, style = MaterialTheme.typography.bodyMedium, fontSize = 12.sp, color = TextSecondary, maxLines = 1)
    }
}

@Composable
private fun ChartCard(state: MileageState, onRange: (MileageRange) -> Unit) {
    val points = state.chartPoints()
    var selected by rememberSaveable(state.range, points.size) { mutableStateOf<Int?>(null) }
    GlassSurface(modifier = Modifier.fillMaxWidth(), contentPadding = 16.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ChoiceChips(MileageRange.entries, state.range, { it.label }, onRange)
            if (points.size < 2) {
                Text(
                    "Для графіка потрібно хоча б дві відмітки пробігу в цьому періоді.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                val sel = selected?.let { points.getOrNull(it) }
                ChartReadout(
                    title = sel?.let { formatLongDate(it.date) },
                    value = sel?.let { "${formatKm(it.mileage)} км" },
                    placeholder = "Торкнись графіка, щоб побачити значення"
                )
                val xs = points.map { it.date.toEpochDay() }
                TimeLineChart(
                    xs = xs,
                    ys = points.map { it.mileage },
                    xTicks = timeTicks(points.first().date, points.last().date),
                    selectedIndex = selected,
                    onSelect = { selected = it },
                    description = "Графік пробігу: від ${formatKm(points.first().mileage)} до ${formatKm(points.last().mileage)} км"
                )
            }
        }
    }
}

@Composable
private fun SuspiciousBanner(count: Int) {
    val shape = RoundedCornerShape(AutoDocsDimens.CardRadiusInner)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(StatusOverdue.copy(alpha = 0.08f))
            .border(1.dp, StatusOverdue.copy(alpha = 0.3f), shape)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = StatusOverdue)
        Spacer(Modifier.width(12.dp))
        Text(
            if (count == 1) "1 відмітка не узгоджується з іншими — схоже на помилку вводу. Вона не враховується в прогнозі."
            else "$count відмітки не узгоджуються з іншими — схоже на помилки вводу. Вони не враховуються в прогнозі.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary
        )
    }
}

@Composable
private fun HistoryRow(point: MileageHistoryPoint, onDelete: () -> Unit, onOpenRecord: () -> Unit) {
    val clickable = point.source == MileageSource.RECORD
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (clickable) it.clickable(onClick = onOpenRecord) else it }
            .padding(start = 14.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                "${formatKm(point.mileage)} км",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = if (point.suspicious) StatusOverdue else TextPrimary
            )
            val source = when (point.source) {
                MileageSource.ENTRY -> "Відмітка"
                MileageSource.RECORD -> "Запис журналу"
                MileageSource.CAR -> "Картка авто"
            }
            Text(
                listOfNotNull(formatLongDate(point.date), source, if (point.suspicious) "не узгоджується" else null).joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 13.sp,
                color = TextSecondary
            )
        }
        when (point.source) {
            MileageSource.ENTRY -> IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.DeleteOutline, contentDescription = "Видалити відмітку ${formatKm(point.mileage)} км", tint = TextSecondary)
            }
            MileageSource.RECORD -> Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = TextSecondary)
            }
            MileageSource.CAR -> Spacer(Modifier.size(48.dp))
        }
    }
}
