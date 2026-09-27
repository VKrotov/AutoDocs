package com.autodocs.app.ui.screens.journal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import com.autodocs.app.ui.components.RoundGlassButton
import com.autodocs.app.ui.components.autoDocsFieldColors
import com.autodocs.app.ui.screens.stats.ChoiceChips
import com.autodocs.app.ui.theme.LinkColor
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.autodocs.app.AutoDocsApp
import com.autodocs.app.data.entity.RecordWithItems
import com.autodocs.app.data.entity.displayName
import com.autodocs.app.data.entity.total
import com.autodocs.app.ui.components.GlassSurface
import com.autodocs.app.ui.components.ScreenHeader
import com.autodocs.app.ui.theme.Accent
import com.autodocs.app.ui.theme.AutoDocsDimens
import com.autodocs.app.ui.theme.OnAccent
import com.autodocs.app.ui.theme.TextPrimary
import com.autodocs.app.ui.theme.TextSecondary
import com.autodocs.app.ui.util.formatKm
import com.autodocs.app.ui.util.formatMoney
import com.autodocs.app.ui.util.formatMonthYear
import com.autodocs.app.ui.util.formatRecordDate
import com.autodocs.app.ui.util.pluralUk
import com.autodocs.app.ui.util.utcMillisToLocalDate

/** F03: журнал ремонтів і ТО активного авто, згрупований за місяцями. */
@Composable
fun JournalScreen(
    onOpenRecord: (Long) -> Unit,
    onAddRecord: () -> Unit,
    onAddCar: () -> Unit,
    onOpenStats: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val app = LocalContext.current.applicationContext as AutoDocsApp
    val viewModel: JournalViewModel = viewModel(
        factory = JournalViewModelFactory(app.carRepository, app.serviceRepository, app.photoRepository)
    )
    val state by viewModel.state.collectAsState()
    var searchOpen by rememberSaveable { mutableStateOf(false) }

    val car = state.car
    when {
        !state.isLoaded -> Box(modifier.fillMaxSize())
        car == null -> EmptyMessage(
            title = "Спершу додай авто",
            text = "Журнал ведеться для активного авто",
            buttonText = "Додати авто",
            onClick = onAddCar,
            modifier = modifier
        )
        state.totalCount == 0 -> Column(modifier.fillMaxSize().padding(horizontal = AutoDocsDimens.ScreenPadding)) {
            ScreenHeader(title = "Журнал", overline = car.name)
            EmptyMessage(
                title = "Записів ще немає",
                text = "Додай перший ремонт чи ТО — роботи, запчастини, ціни, СТО і пробіг",
                buttonText = "Додати запис",
                onClick = onAddRecord
            )
        }
        else -> {
            val filter = state.filter
            val groups = state.records.groupBy { utcMillisToLocalDate(it.record.date).withDayOfMonth(1) }
            val totalSpent = state.records.sumOf { it.total() }
            val count = state.records.size.toLong()
            LazyColumn(
                modifier = modifier.fillMaxSize().padding(horizontal = AutoDocsDimens.ScreenPadding),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item(key = "header") {
                    ScreenHeader(title = "Журнал", overline = car.name) {
                        RoundGlassButton(
                            Icons.Outlined.Search,
                            "Пошук",
                            onClick = {
                                if (searchOpen) viewModel.setQuery("")
                                searchOpen = !searchOpen
                            },
                            tint = if (searchOpen || filter.query.isNotBlank()) Accent else TextPrimary
                        )
                        RoundGlassButton(Icons.Outlined.BarChart, "Статистика витрат", onOpenStats)
                    }
                }
                if (searchOpen || filter.query.isNotBlank()) {
                    item(key = "search") {
                        SearchField(
                            query = filter.query,
                            onQueryChange = viewModel::setQuery,
                            onClose = { viewModel.setQuery(""); searchOpen = false }
                        )
                    }
                }
                if (state.years.size >= 2) {
                    item(key = "years") {
                        ChoiceChips(
                            options = listOf<Int?>(null) + state.years,
                            selected = filter.year,
                            label = { it?.toString() ?: "Усі роки" },
                            onSelect = viewModel::setYear
                        )
                    }
                }
                item(key = "summary") {
                    Text(
                        (if (filter.isActive) "Знайдено: " else "") +
                            "$count ${pluralUk(count, "запис", "записи", "записів")} · витрачено ${formatMoney(totalSpent)} ₴",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )
                }
                if (state.records.isEmpty()) {
                    item(key = "nothing") {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Нічого не знайдено", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Text(
                                "Скинути пошук і фільтр",
                                color = LinkColor,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier
                                    .padding(top = 8.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { viewModel.clearFilter(); searchOpen = false }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
                groups.forEach { (month, records) ->
                    item(key = "m_$month") {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 12.dp, bottom = 2.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                formatMonthYear(month),
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "${formatMoney(records.sumOf { it.total() })} ₴",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                    items(records, key = { it.record.id }) { record ->
                        RecordCard(
                            record = record,
                            photoCount = state.photoCounts[record.record.id] ?: 0,
                            highlighted = filter.matchedItemNames(record),
                            onClick = { onOpenRecord(record.record.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit, onClose: () -> Unit) {
    val focus = remember { FocusRequester() }
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth().focusRequester(focus),
        placeholder = { Text("Робота, запчастина, СТО, нотатка") },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = TextSecondary) },
        trailingIcon = {
            IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = "Закрити пошук", tint = TextSecondary) }
        },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        colors = autoDocsFieldColors(),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
    )
    LaunchedEffect(Unit) { if (query.isEmpty()) runCatching { focus.requestFocus() } }
}

@Composable
private fun RecordCard(record: RecordWithItems, photoCount: Int = 0, highlighted: List<String> = emptyList(), onClick: () -> Unit) {
    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AutoDocsDimens.CardRadiusInner))
            .clickable(onClick = onClick),
        cornerRadius = AutoDocsDimens.CardRadiusInner,
        contentPadding = 14.dp
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    formatRecordDate(record.record.date),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                val meta = listOfNotNull(
                    "${formatKm(record.record.mileage)} км",
                    record.record.stoName?.takeIf { it.isNotBlank() },
                    if (photoCount > 0) "$photoCount фото" else null
                ).joinToString(" · ")
                Text(meta, style = MaterialTheme.typography.bodyMedium, fontSize = 13.sp, color = TextSecondary)
            }
            Text(
                "${formatMoney(record.total())} ₴",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }
        if (record.items.isNotEmpty()) {
            // Знайдені пошуком позиції — першими, щоб було видно, чому запис підійшов.
            val all = record.items.map { it.displayName() }
            val names = highlighted + (all - highlighted.toSet())
            val shown = names.take(3).joinToString(", ")
            val rest = names.size - 3
            Text(
                if (rest > 0) "$shown +$rest" else shown,
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 13.sp,
                color = TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun EmptyMessage(
    title: String,
    text: String,
    buttonText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Text(
                text,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)
            )
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = OnAccent)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(buttonText)
            }
        }
    }
}
