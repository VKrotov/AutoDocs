package com.autodocs.app.ui.screens.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.autodocs.app.AutoDocsApp
import com.autodocs.app.data.repository.PlanOverview
import com.autodocs.app.ui.components.GlassSurface
import com.autodocs.app.ui.components.RoundGlassButton
import com.autodocs.app.ui.components.ScreenHeader
import com.autodocs.app.ui.components.SectionLabel
import com.autodocs.app.ui.theme.Accent
import com.autodocs.app.ui.theme.AutoDocsDimens
import com.autodocs.app.ui.theme.LinkColor
import com.autodocs.app.ui.theme.OnAccent
import com.autodocs.app.ui.theme.StatusSoon
import com.autodocs.app.ui.theme.TextPrimary
import com.autodocs.app.ui.theme.TextSecondary
import com.autodocs.app.ui.util.formatKm
import com.autodocs.app.ui.util.pluralUk
import kotlin.math.roundToInt

/** F07/F08/F09: «План ТО» — регламент активного авто з розрахунком наступного ТО. */
@Composable
fun PlanScreen(
    onOpenRule: (Long) -> Unit,
    onAddRule: () -> Unit,
    onOpenSetup: () -> Unit,
    onAddCar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val app = LocalContext.current.applicationContext as AutoDocsApp
    val viewModel: PlanViewModel = viewModel(factory = PlanViewModelFactory(app.carRepository, app.planRepository))
    val state by viewModel.state.collectAsState()
    var showInactive by rememberSaveable { mutableStateOf(false) }

    val car = state.car
    val overview = state.overview
    when {
        !state.isLoaded -> Box(modifier.fillMaxSize())
        car == null -> CenterMessage(
            title = "Спершу додай авто",
            text = "План ТО рахується для активного авто",
            button = "Додати авто",
            onClick = onAddCar,
            modifier = modifier
        )
        overview == null -> Box(modifier.fillMaxSize())
        else -> Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AutoDocsDimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ScreenHeader(title = "План ТО", overline = car.name) {
                RoundGlassButton(Icons.Filled.Add, "Додати пункт регламенту", onAddRule, tint = Accent)
            }

            MileageCard(overview)

            if (overview.isEmpty) {
                EmptyRules(onSeed = { viewModel.seedTemplate { if (it > 0) onOpenSetup() } }, onAddRule = onAddRule)
            } else {
                if (overview.unknownCount > 0) {
                    UnknownBanner(count = overview.unknownCount, onClick = onOpenSetup)
                }
                if (overview.active.isNotEmpty()) {
                    RulePlanList(items = overview.active, onClick = { onOpenRule(it.rule.id) })
                }
                if (overview.inactive.isNotEmpty()) {
                    Text(
                        if (showInactive) "Сховати вимкнені" else "Вимкнені пункти · ${overview.inactive.size}",
                        color = LinkColor,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { showInactive = !showInactive }
                            .padding(horizontal = 4.dp, vertical = 6.dp)
                    )
                    if (showInactive) {
                        RulePlanList(items = overview.inactive, onClick = { onOpenRule(it.rule.id) }, dimmed = true)
                    }
                }
                Text(
                    "Термін — що настане раніше: пробіг чи час. Відмітки підтягуються з журналу автоматично, " +
                        "коли в записі є відповідна робота.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 13.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun MileageCard(overview: PlanOverview) {
    GlassSurface(modifier = Modifier.fillMaxWidth(), contentPadding = 18.dp) {
        val rate = overview.kmPerDay
        val estimated = rate != null && overview.currentMileage != overview.car.mileage
        Text(
            if (estimated) "Пробіг зараз (прогноз)" else "Пробіг",
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 13.sp,
            color = TextSecondary
        )
        Text(
            buildAnnotatedString {
                if (overview.currentMileage > 0) {
                    append((if (estimated) "≈ " else "") + formatKm(overview.currentMileage))
                    withStyle(SpanStyle(fontSize = 17.sp, fontWeight = FontWeight.Medium, color = TextSecondary)) { append(" км") }
                } else {
                    append("—")
                }
            },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(Modifier.height(4.dp))
        Text(
            if (rate != null) {
                val perDay = rate.roundToInt()
                val perMonth = (rate * 30).roundToInt()
                "У середньому $perDay км/день · ≈ ${formatKm(perMonth)} км/міс"
            } else {
                "Прогноз пробігу з'явиться, коли буде історія: оновлюй пробіг на головній або додавай записи з пробігом."
            },
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 13.sp,
            color = TextSecondary
        )
    }
}

@Composable
private fun UnknownBanner(count: Int, onClick: () -> Unit) {
    val shape = RoundedCornerShape(AutoDocsDimens.CardRadiusInner)
    val n = count.toLong()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(StatusSoon.copy(alpha = 0.08f))
            .border(1.dp, StatusSoon.copy(alpha = 0.3f), shape)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.EditCalendar, contentDescription = null, tint = StatusSoon)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "$count ${pluralUk(n, "пункт", "пункти", "пунктів")} без відмітки",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
            Text(
                if (count == 1) "Вкажи, коли робили востаннє — інакше він рахується простроченим"
                else "Вкажи, коли робили востаннє — інакше вони рахуються простроченими",
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 13.sp,
                color = TextSecondary
            )
        }
        Spacer(Modifier.width(8.dp))
        Text("Вказати", color = LinkColor, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun EmptyRules(onSeed: () -> Unit, onAddRule: () -> Unit) {
    GlassSurface(modifier = Modifier.fillMaxWidth(), contentPadding = 18.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Регламент ще не налаштований", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Text(
                "Заповню стандартний регламент для Passat B5+ 2.0 AZM (масло, ГРМ, фільтри, свічки, рідини…). " +
                    "Потім запитаю, коли кожну роботу робили востаннє. Інтервали можна змінити.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Button(
                onClick = onSeed,
                colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = OnAccent),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) { Text("Заповнити стандартний регламент") }
            OutlinedButton(onClick = onAddRule, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                Text("Додати пункт вручну", color = LinkColor)
            }
        }
    }
}

@Composable
private fun CenterMessage(title: String, text: String, button: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Text(text, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, modifier = Modifier.padding(top = 8.dp, bottom = 20.dp))
            Button(onClick = onClick, colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = OnAccent)) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(button)
            }
        }
    }
}
