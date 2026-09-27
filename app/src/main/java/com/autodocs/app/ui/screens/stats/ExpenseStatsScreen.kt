package com.autodocs.app.ui.screens.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.autodocs.app.AutoDocsApp
import com.autodocs.app.data.stats.ExpenseSummary
import com.autodocs.app.data.stats.StatsPeriod
import com.autodocs.app.data.stats.TopEntry
import com.autodocs.app.ui.components.GlassSurface
import com.autodocs.app.ui.components.ScreenHeader
import com.autodocs.app.ui.components.SectionLabel
import com.autodocs.app.ui.components.SimpleBarChart
import com.autodocs.app.ui.theme.AutoDocsDimens
import com.autodocs.app.ui.theme.TextPrimary
import com.autodocs.app.ui.theme.TextSecondary
import com.autodocs.app.ui.util.formatKm
import com.autodocs.app.ui.util.formatMoney
import com.autodocs.app.ui.util.label
import com.autodocs.app.ui.util.pluralUk
import kotlin.math.roundToInt

/** Етап 8: статистика витрат — за рік або за весь час. */
@Composable
fun ExpenseStatsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val app = LocalContext.current.applicationContext as AutoDocsApp
    val viewModel: ExpenseStatsViewModel = viewModel(
        factory = ExpenseStatsViewModelFactory(app.carRepository, app.serviceRepository, app.mileageRepository)
    )
    val state by viewModel.state.collectAsState()
    ExpenseStatsContent(state, onBack, viewModel::selectPeriod, modifier)
}

@Composable
internal fun ExpenseStatsContent(
    state: ExpenseStatsState,
    onBack: () -> Unit,
    onSelectPeriod: (StatsPeriod) -> Unit,
    modifier: Modifier = Modifier
) {
    val summary = state.summary

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AutoDocsDimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ScreenHeader(title = "Витрати", overline = state.car?.name, onBack = onBack)
        if (!state.isLoaded || summary == null) return@Column
        if (state.years.isEmpty()) {
            Text(
                "Статистика з'явиться, коли в журналі будуть записи з цінами.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            return@Column
        }
        val periods = state.years.map { StatsPeriod(it) } + StatsPeriod.ALL
        ChoiceChips(periods, summary.period, { it.year?.toString() ?: "Усі роки" }, onSelectPeriod)

        TotalCard(summary)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("Роботи", "${formatMoney(summary.works)} ₴", Modifier.weight(1f), hint = share(summary.works, summary.total))
            StatTile("Запчастини", "${formatMoney(summary.parts)} ₴", Modifier.weight(1f), hint = share(summary.parts, summary.total))
        }
        StatTile(
            "Вартість 1 км",
            summary.perKm?.let { "${formatMoney(Math.round(it * 100) / 100.0)} ₴" } ?: "—",
            Modifier.fillMaxWidth(),
            hint = summary.drivenKm?.takeIf { summary.perKm != null }?.let { "проїхано ≈ ${formatKm(it)} км" }
                ?: "мало відміток пробігу за період"
        )
        BarsCard(summary)
        if (summary.topItems.isNotEmpty()) {
            TopList("Найбільші витрати", summary.topItems) { e ->
                listOfNotNull(e.category?.label(), "${e.count} ${pluralUk(e.count.toLong(), "раз", "рази", "разів")}").joinToString(" · ")
            }
        }
        if (summary.topSto.isNotEmpty()) {
            TopList("СТО", summary.topSto) { e -> "${e.count} ${pluralUk(e.count.toLong(), "візит", "візити", "візитів")}" }
        }
        Spacer(Modifier.height(16.dp))
    }
}

private fun share(part: Double, total: Double): String =
    if (total > 0) "${(part / total * 100).roundToInt()} %" else "—"

@Composable
private fun TotalCard(s: ExpenseSummary) {
    GlassSurface(modifier = Modifier.fillMaxWidth(), contentPadding = 18.dp) {
        Text(
            if (s.period.year != null) "Витрачено за ${s.period.year}" else "Витрачено за весь час",
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 13.sp,
            color = TextSecondary
        )
        Text(
            buildAnnotatedString {
                append(formatMoney(s.total))
                withStyle(SpanStyle(fontSize = 17.sp, fontWeight = FontWeight.Medium, color = TextSecondary)) { append(" ₴") }
            },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        val n = s.recordCount.toLong()
        Text(
            if (n > 0) "$n ${pluralUk(n, "запис", "записи", "записів")} · у середньому ${formatMoney(s.averagePerRecord.roundToInt().toDouble())} ₴"
            else "Записів за цей період немає",
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 13.sp,
            color = TextSecondary
        )
    }
}

@Composable
private fun BarsCard(s: ExpenseSummary) {
    var selected by rememberSaveable(s.period.year) { mutableStateOf<Int?>(null) }
    val byMonth = s.period.year != null
    GlassSurface(modifier = Modifier.fillMaxWidth(), contentPadding = 16.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (byMonth) "По місяцях" else "По роках", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            val bar = selected?.let { s.bars.getOrNull(it) }
            ChartReadout(
                title = bar?.let {
                    val name = if (byMonth) "${MonthFull[it.key - 1]} ${s.period.year}" else it.key.toString()
                    val n = it.records.toLong()
                    "$name · $n ${pluralUk(n, "запис", "записи", "записів")}"
                },
                value = bar?.let { "${formatMoney(it.amount)} ₴" },
                placeholder = "Торкнись стовпчика, щоб побачити суму"
            )
            SimpleBarChart(
                values = s.bars.map { it.amount },
                labels = s.bars.map { if (byMonth) MonthShort[it.key - 1] else it.key.toString() },
                selectedIndex = selected,
                onSelect = { selected = if (selected == it) null else it },
                description = "Витрати ${if (byMonth) "по місяцях" else "по роках"}, разом ${formatMoney(s.total)} гривень"
            )
        }
    }
}

@Composable
private fun TopList(title: String, entries: List<TopEntry>, subtitle: (TopEntry) -> String) {
    Column {
        SectionLabel(title)
        GlassSurface(modifier = Modifier.fillMaxWidth(), cornerRadius = AutoDocsDimens.CardRadiusInner, contentPadding = 0.dp) {
            entries.forEachIndexed { i, e ->
                if (i > 0) HorizontalDivider(color = Color(0x0FFFFFFF), modifier = Modifier.padding(start = 14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            e.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(subtitle(e), style = MaterialTheme.typography.bodyMedium, fontSize = 13.sp, color = TextSecondary)
                    }
                    Text(
                        "${formatMoney(e.amount)} ₴",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}
