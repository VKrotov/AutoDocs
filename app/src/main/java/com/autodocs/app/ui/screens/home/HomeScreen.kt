package com.autodocs.app.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import com.autodocs.app.AutoDocsApp
import com.autodocs.app.data.entity.Car
import com.autodocs.app.data.entity.FuelType
import com.autodocs.app.data.entity.TransmissionType
import com.autodocs.app.data.repository.PlanOverview
import com.autodocs.app.ui.components.CarPhoto
import com.autodocs.app.data.entity.displayName
import com.autodocs.app.data.notify.NotifySettings
import com.autodocs.app.data.plan.DocumentDue
import com.autodocs.app.data.plan.DueStatus
import com.autodocs.app.data.repository.UpcomingEntry
import com.autodocs.app.ui.screens.plan.RulePlanRow
import com.autodocs.app.ui.screens.plan.StatusList
import com.autodocs.app.ui.screens.plan.TaskPlanRow
import com.autodocs.app.ui.util.formatDaysLeft
import com.autodocs.app.ui.util.formatShortDate
import com.autodocs.app.ui.components.GlassPillButton
import com.autodocs.app.ui.components.GlassSurface
import com.autodocs.app.ui.components.PillText
import com.autodocs.app.ui.components.RoundGlassButton
import com.autodocs.app.ui.components.ScreenHeader
import com.autodocs.app.ui.components.autoDocsFieldColors
import com.autodocs.app.ui.theme.Accent
import com.autodocs.app.ui.theme.AutoDocsDimens
import com.autodocs.app.ui.theme.LinkColor
import com.autodocs.app.ui.theme.OnAccent
import com.autodocs.app.ui.theme.StatusOverdue
import com.autodocs.app.ui.theme.StatusSoon
import com.autodocs.app.ui.theme.TextPrimary
import com.autodocs.app.ui.theme.TextSecondary
import com.autodocs.app.ui.theme.VinTextStyle
import com.autodocs.app.ui.util.formatKm
import com.autodocs.app.ui.util.formatTodayHeader
import com.autodocs.app.ui.util.formatUpdatedAgo
import com.autodocs.app.ui.util.label
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    onAddCar: () -> Unit,
    onEditCar: (Long) -> Unit,
    onOpenPlan: () -> Unit,
    onOpenRule: (Long) -> Unit,
    onOpenPassport: (Long) -> Unit,
    onOpenMileage: () -> Unit = {},
    onOpenDocuments: () -> Unit = {},
    onOpenTask: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val app = LocalContext.current.applicationContext as AutoDocsApp
    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModelFactory(
            app.carRepository, app.planRepository, app.photoRepository, app.documentRepository,
            warnDays = { NotifySettings.load(app).docDaysBefore }
        )
    )
    val activeCar by viewModel.activeCar.collectAsState()
    val plan by viewModel.plan.collectAsState()
    val passportSides by viewModel.passportSides.collectAsState()
    val documents by viewModel.documents.collectAsState()

    val car = activeCar
    if (car == null) {
        EmptyState(onAddCar = onAddCar, modifier = modifier)
    } else {
        CarHome(
            car = car,
            onEdit = { onEditCar(car.id) },
            onArchive = { viewModel.archiveActiveCar(car.id) },
            onUpdateMileage = { viewModel.updateMileage(car.id, it) },
            passportSides = passportSides,
            onOpenPassport = { onOpenPassport(car.id) },
            onOpenMileage = onOpenMileage,
            documents = documents,
            onOpenDocuments = onOpenDocuments,
            upcoming = {
                UpcomingBlock(plan = plan, onOpenPlan = onOpenPlan, onOpenRule = onOpenRule, onOpenTask = onOpenTask)
            },
            modifier = modifier
        )
    }
}

@Composable
private fun EmptyState(onAddCar: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Немає жодного авто", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Text(
                "Додай своє перше авто, щоб почати вести історію ремонтів і ТО",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)
            )
            Button(
                onClick = onAddCar,
                colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = OnAccent)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Додати авто")
            }
        }
    }
}

/** Що з необов'язкових даних авто ще не заповнено — показуємо підказку «Доповнити». */
private fun Car.missingFields(): List<String> = buildList {
    if (engine.isBlank()) add("двигун")
    if (fuelType == FuelType.UNKNOWN) add("паливо")
    if (transmissionType == TransmissionType.UNKNOWN) add("КПП")
    if (licensePlate.isBlank()) add("номер")
    if (vin.isBlank()) add("VIN")
    if (mileage <= 0) add("пробіг")
    if (photoUri == null) add("фото")
}

private fun Car.specLine(): String = listOfNotNull(
    engine.takeIf { it.isNotBlank() },
    fuelType.takeIf { it != FuelType.UNKNOWN }?.label()?.lowercase(),
    transmissionType.takeIf { it != TransmissionType.UNKNOWN }?.label()?.lowercase()
).joinToString(" · ")

@Composable
private fun CarHome(
    car: Car,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
    onUpdateMileage: (Int) -> Unit,
    modifier: Modifier = Modifier,
    passportSides: Int = 0,
    onOpenPassport: () -> Unit = {},
    onOpenMileage: () -> Unit = {},
    documents: List<DocumentDue> = emptyList(),
    onOpenDocuments: () -> Unit = {},
    upcoming: @Composable () -> Unit = {}
) {
    var showArchiveConfirm by remember { mutableStateOf(false) }
    var showMileageDialog by remember { mutableStateOf(false) }
    var vinExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AutoDocsDimens.ScreenPadding)
    ) {
        ScreenHeader(
            title = car.name,
            overline = formatTodayHeader()
        ) {
            RoundGlassButton(Icons.Outlined.Edit, "Редагувати авто", onEdit)
            RoundGlassButton(Icons.Outlined.Inventory2, "Архівувати авто", { showArchiveConfirm = true })
        }

        GlassSurface(modifier = Modifier.fillMaxWidth(), contentPadding = 18.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Рядок 1: марка/модель + характеристики зліва; номер і пігулка VIN справа.
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("${car.make} ${car.model}", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        val spec = car.specLine()
                        if (spec.isNotEmpty()) {
                            Text(spec, style = MaterialTheme.typography.bodyMedium, fontSize = 13.sp, color = TextSecondary)
                        }
                    }
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (car.licensePlate.isNotBlank()) PlateChip(car.licensePlate)
                        if (car.vin.isNotBlank()) {
                            VinPill(expanded = vinExpanded, onClick = { vinExpanded = !vinExpanded })
                        }
                    }
                }

                AnimatedVisibility(visible = vinExpanded && car.vin.isNotBlank()) {
                    VinRow(car.vin)
                }

                if (car.photoUri != null) {
                    CarPhoto(
                        uri = car.photoUri,
                        modifier = Modifier.fillMaxWidth().height(170.dp)
                    )
                }

                // Пробіг
                Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        val hasMileage = car.mileage > 0
                        Text(
                            if (hasMileage) "Пробіг · ${formatUpdatedAgo(car.mileageUpdatedAt)}" else "Пробіг не вказано",
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 13.sp,
                            // Давно не оновлювали — підсвічуємо, бо від пробігу залежить прогноз ТО.
                            color = if (!hasMileage || isMileageStale(car.mileageUpdatedAt)) StatusSoon else TextSecondary
                        )
                        Text(
                            buildAnnotatedString {
                                if (hasMileage) {
                                    append(formatKm(car.mileage))
                                    withStyle(SpanStyle(fontSize = 17.sp, fontWeight = FontWeight.Medium, color = TextSecondary)) {
                                        append(" км")
                                    }
                                } else {
                                    append("—")
                                }
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    if (car.mileage > 0) {
                        GlassPillButton(onClick = onOpenMileage, modifier = Modifier.padding(end = 8.dp)) {
                            Icon(
                                Icons.AutoMirrored.Outlined.ShowChart,
                                contentDescription = "Історія пробігу",
                                tint = LinkColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    GlassPillButton(onClick = { showMileageDialog = true }) {
                        PillText(if (car.mileage > 0) "Оновити" else "Вказати")
                    }
                }

                // F26: техпаспорт — окремий рядок-посилання.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onOpenPassport)
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Badge, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Техпаспорт", style = MaterialTheme.typography.bodyMedium, color = TextPrimary, modifier = Modifier.weight(1f))
                    Text(
                        when (passportSides) { 0 -> "Додати фото"; 1 -> "1 з 2 сторін"; else -> "2 сторони" },
                        color = if (passportSides == 2) TextSecondary else LinkColor,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (passportSides == 2) FontWeight.Normal else FontWeight.SemiBold
                    )
                }

                // Етап 9: страховка й інші документи з терміном — найтерміновіший.
                DocumentsLine(documents = documents, onClick = onOpenDocuments)

                val missing = car.missingFields()
                if (missing.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onEdit)
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Не заповнено: ${missing.joinToString(", ")}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 13.sp,
                            color = TextSecondary,
                            modifier = Modifier.weight(1f)
                        )
                        Text("Доповнити", color = LinkColor, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        upcoming()

        Spacer(Modifier.height(24.dp))
    }

    if (showArchiveConfirm) {
        AlertDialog(
            onDismissRequest = { showArchiveConfirm = false },
            title = { Text("Архівувати «${car.name}»?") },
            text = { Text("Історію збережено. Потім зможеш додати нове активне авто.") },
            confirmButton = {
                TextButton(onClick = { showArchiveConfirm = false; onArchive() }) {
                    Text("Архівувати", color = StatusOverdue)
                }
            },
            dismissButton = {
                TextButton(onClick = { showArchiveConfirm = false }) { Text("Скасувати") }
            }
        )
    }

    if (showMileageDialog) {
        var text by remember { mutableStateOf(if (car.mileage > 0) car.mileage.toString() else "") }
        AlertDialog(
            onDismissRequest = { showMileageDialog = false },
            title = { Text(if (car.mileage > 0) "Оновити пробіг" else "Вказати пробіг") },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { if (it.length <= 7 && it.all { c -> c.isDigit() }) text = it },
                    label = { Text("Пробіг, км") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = autoDocsFieldColors()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    text.toIntOrNull()?.let(onUpdateMileage)
                    showMileageDialog = false
                }) { Text("Зберегти", color = Accent) }
            },
            dismissButton = {
                TextButton(onClick = { showMileageDialog = false }) { Text("Скасувати") }
            }
        )
    }
}

@Composable
private fun PlateChip(plate: String) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(Color(0x12FFFFFF))
            .border(1.dp, Color(0x1AFFFFFF), shape)
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Text(
            plate,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.6.sp,
            color = TextPrimary
        )
    }
}

/** Пігулка «VIN ˅»: сам VIN не показується, доки її не натиснули. */
@Composable
private fun VinPill(expanded: Boolean, onClick: () -> Unit) {
    GlassPillButton(onClick = onClick, highlighted = expanded, height = 32) {
        Text(
            "VIN",
            color = LinkColor,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.6.sp
        )
        Icon(
            Icons.Rounded.KeyboardArrowDown,
            contentDescription = if (expanded) "Сховати VIN" else "Показати VIN",
            tint = LinkColor,
            modifier = Modifier.size(16.dp).rotate(if (expanded) 180f else 0f)
        )
    }
}

@Composable
private fun VinRow(vin: String) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(1600)
            copied = false
        }
    }
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Accent.copy(alpha = 0.08f))
            .border(1.dp, Accent.copy(alpha = 0.24f), shape)
            .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("VIN-код", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Text(vin, style = VinTextStyle, letterSpacing = 0.8.sp, color = TextPrimary)
        }
        Row(
            modifier = Modifier
                .height(36.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Accent)
                .clickable {
                    clipboard.setText(AnnotatedString(vin))
                    copied = true
                }
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Outlined.ContentCopy, contentDescription = null, tint = OnAccent, modifier = Modifier.size(16.dp))
            Text(
                if (copied) "Скопійовано" else "Копіювати",
                color = OnAccent,
                style = MaterialTheme.typography.labelMedium,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun isMileageStale(updatedAt: Long): Boolean =
    System.currentTimeMillis() - updatedAt > 14L * 24 * 60 * 60 * 1000

/** F11: «Найближче ТО» під карткою авто — як у фінальному дизайні (+ разові плани, етап 9). */
@Composable
private fun UpcomingBlock(
    plan: PlanOverview?,
    onOpenPlan: () -> Unit,
    onOpenRule: (Long) -> Unit,
    onOpenTask: (Long) -> Unit = {}
) {
    if (plan == null) return
    Spacer(Modifier.height(20.dp))
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 0.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            "Найближче ТО",
            style = MaterialTheme.typography.titleMedium,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
        Text(
            "Усе",
            color = LinkColor,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onOpenPlan).padding(4.dp)
        )
    }
    Spacer(Modifier.height(10.dp))
    // Прострочені → скоро → решта; пункти без відмітки («Вказати») — лише в «Плані ТО».
    val entries = plan.upcoming()
    when {
        entries.isNotEmpty() -> {
            StatusList(entries) { entry ->
                when (entry) {
                    is UpcomingEntry.Rule -> RulePlanRow(entry.item, onClick = { onOpenRule(entry.item.rule.id) })
                    is UpcomingEntry.Task -> TaskPlanRow(entry.item, onClick = { onOpenTask(entry.item.task.id) })
                }
            }
            if (plan.unknownCount > 0) {
                Text(
                    "Ще ${plan.unknownCount} без відмітки — у «Плані ТО»",
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 13.sp,
                    color = TextSecondary,
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onOpenPlan)
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                )
            }
        }
        plan.active.isEmpty() -> UpcomingHintCard(
            title = "Регламент ТО ще не налаштований",
            action = "Налаштувати в «Плані ТО» — займе хвилину",
            onClick = onOpenPlan
        )
        else -> UpcomingHintCard(
            title = "Поки нічого рахувати",
            action = "Вкажи в «Плані ТО», коли робили роботи (${plan.unknownCount})",
            onClick = onOpenPlan
        )
    }
}

/** Рядок «Страховка й документи» у картці авто: найтерміновіший документ або «Додати». */
@Composable
private fun DocumentsLine(documents: List<DocumentDue>, onClick: () -> Unit) {
    val top = documents.firstOrNull()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClickLabel = "Відкрити документи", onClick = onClick)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.Shield, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            if (top == null) "Страховка й документи" else top.doc.displayName(),
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
        val (text, color, bold) = when {
            top == null -> Triple("Додати", LinkColor, true)
            top.status == DueStatus.OVERDUE -> Triple("прострочено", StatusOverdue, true)
            top.status == DueStatus.SOON -> Triple(if (top.daysLeft == 0L) "останній день" else formatDaysLeft(top.daysLeft), StatusSoon, true)
            else -> Triple("до ${formatShortDate(top.validUntil)}", TextSecondary, false)
        }
        Text(
            text,
            color = color,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun UpcomingHintCard(title: String, action: String, onClick: () -> Unit) {
    GlassSurface(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(AutoDocsDimens.CardRadiusInner)).clickable(onClick = onClick),
        cornerRadius = AutoDocsDimens.CardRadiusInner,
        contentPadding = 14.dp
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
        Text(action, style = MaterialTheme.typography.bodyMedium, fontSize = 13.sp, color = LinkColor)
    }
}
