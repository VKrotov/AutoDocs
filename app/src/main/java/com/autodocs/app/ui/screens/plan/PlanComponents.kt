package com.autodocs.app.ui.screens.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.PriorityHigh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autodocs.app.data.entity.PlannedTask
import com.autodocs.app.data.plan.DueStatus
import com.autodocs.app.data.plan.TaskPlan
import com.autodocs.app.data.repository.PlanRepository
import com.autodocs.app.data.repository.RulePlan
import com.autodocs.app.ui.components.GlassSurface
import com.autodocs.app.ui.theme.AutoDocsDimens
import com.autodocs.app.ui.theme.StatusOverdue
import com.autodocs.app.ui.theme.StatusOverdueTile
import com.autodocs.app.ui.theme.StatusSoon
import com.autodocs.app.ui.theme.StatusSoonTile
import com.autodocs.app.ui.theme.TextPrimary
import com.autodocs.app.ui.theme.TextSecondary
import com.autodocs.app.ui.util.formatDaysSigned
import com.autodocs.app.ui.util.formatInterval
import com.autodocs.app.ui.util.formatKm
import com.autodocs.app.ui.util.formatShortDate
import com.autodocs.app.ui.util.formatKmSigned

private val NeutralTile = Color(0x0DFFFFFF)
private val NeutralTileBorder = Color(0x17FFFFFF)
private val NeutralIcon = Color(0xFFB4BECE)

/** Права колонка рядка: що і якого кольору показувати. */
private data class DueText(val text: String, val color: Color, val bold: Boolean)

private fun RulePlan.dueText(): DueText {
    val p = plan
    val color = when (p.status) {
        DueStatus.OVERDUE -> StatusOverdue
        DueStatus.SOON -> StatusSoon
        DueStatus.OK -> TextSecondary
    }
    val bold = p.status != DueStatus.OK
    return when {
        p.isUnknown -> DueText("Вказати", StatusOverdue, true)
        p.remainingKm != null -> DueText(formatKmSigned(p.remainingKm), color, bold)
        p.remainingDays != null -> DueText(formatDaysSigned(p.remainingDays), color, bold)
        else -> DueText("?", StatusOverdue, true)
    }
}

/** Підпис під назвою: для «гарячих» пунктів — скільки днів, інакше інтервал. */
private fun RulePlan.subtitle(): String {
    val p = plan
    return when {
        p.isUnknown -> "Невідомо, коли робили"
        p.status != DueStatus.OK && p.remainingKm != null && p.remainingDays != null ->
            if (p.remainingDays >= 0) "≈ ${formatDaysSigned(p.remainingDays)}" else formatInterval(rule.intervalKm, rule.intervalMonths)
        else -> formatInterval(rule.intervalKm, rule.intervalMonths)
    }
}

/** Рядок правила як у дизайні «Найближче ТО»: плитка-іконка статусу, назва, підпис, залишок справа. */
@Composable
fun RulePlanRow(item: RulePlan, onClick: () -> Unit, dimmed: Boolean = false) {
    val status = item.plan.status
    val icon = when {
        item.plan.isUnknown -> Icons.Outlined.HelpOutline
        status == DueStatus.OVERDUE -> Icons.Outlined.PriorityHigh
        status == DueStatus.SOON -> Icons.Outlined.Schedule
        else -> Icons.Outlined.Build
    }
    val due = if (dimmed) DueText("Вимкнено", TextSecondary, false) else item.dueText()
    StatusRow(
        title = item.name,
        subtitle = item.subtitle(),
        status = if (dimmed) null else status,
        icon = icon,
        trailing = due.text,
        trailingColor = due.color,
        trailingBold = due.bold,
        onClick = onClick,
        dimmed = dimmed
    )
}

/**
 * Загальний рядок зі статусом: плитка-іконка (червона/жовта/нейтральна), назва, підпис, текст справа.
 * [status] null — нейтральна плитка (вимкнене, без терміну, виконане).
 */
@Composable
fun StatusRow(
    title: String,
    subtitle: String?,
    status: DueStatus?,
    icon: ImageVector,
    trailing: String,
    trailingColor: Color,
    trailingBold: Boolean,
    onClick: () -> Unit,
    dimmed: Boolean = false
) {
    val (tileBg, tileBorder, iconColor) = when (status) {
        DueStatus.OVERDUE -> Triple(StatusOverdueTile, StatusOverdue.copy(alpha = 0.30f), StatusOverdue)
        DueStatus.SOON -> Triple(StatusSoonTile, StatusSoon.copy(alpha = 0.28f), StatusSoon)
        else -> Triple(NeutralTile, NeutralTileBorder, NeutralIcon)
    }
    val tileShape = RoundedCornerShape(AutoDocsDimens.TileRadius)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .alpha(if (dimmed) 0.6f else 1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(tileShape)
                .background(tileBg)
                .border(1.dp, tileBorder, tileShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrEmpty()) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, fontSize = 13.sp, color = TextSecondary)
            }
        }
        if (trailing.isNotEmpty()) {
            Text(
                trailing,
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 13.sp,
                fontWeight = if (trailingBold) FontWeight.SemiBold else FontWeight.Normal,
                color = trailingColor
            )
        }
    }
}

/** Скляна картка-список із тонкими роздільниками (відступ під плитку, як у дизайні). */
@Composable
fun <T> StatusList(items: List<T>, row: @Composable (T) -> Unit) {
    GlassSurface(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = AutoDocsDimens.CardRadiusInner,
        contentPadding = 0.dp
    ) {
        items.forEachIndexed { index, item ->
            if (index > 0) {
                HorizontalDivider(color = Color(0x0FFFFFFF), modifier = Modifier.padding(start = 58.dp))
            }
            row(item)
        }
    }
}

// ---- Разові плани (етап 9) ----

/** Підпис разового плану: терміни «до …». */
fun TaskPlan.subtitle(): String {
    val parts = listOfNotNull(
        task.dueMileage?.let { "до ${formatKm(it)} км" },
        task.dueDate?.let { "до ${formatShortDate(PlanRepository.utcToLocalDate(it))}" }
    )
    return if (parts.isEmpty()) "Без терміну" else parts.joinToString(" або ")
}

@Composable
fun TaskPlanRow(item: TaskPlan, onClick: () -> Unit) {
    val p = item.plan
    val status = if (item.hasKnownDue || p.status != DueStatus.OK) p.status else null
    val color = when (p.status) {
        DueStatus.OVERDUE -> StatusOverdue
        DueStatus.SOON -> StatusSoon
        DueStatus.OK -> TextSecondary
    }
    val trailing = when {
        p.remainingKm != null -> formatKmSigned(p.remainingKm)
        p.remainingDays != null -> formatDaysSigned(p.remainingDays)
        else -> ""
    }
    StatusRow(
        title = item.task.title,
        subtitle = item.subtitle(),
        status = status,
        icon = if (p.status == DueStatus.OVERDUE) Icons.Outlined.PriorityHigh else Icons.Outlined.Flag,
        trailing = trailing,
        trailingColor = color,
        trailingBold = p.status != DueStatus.OK,
        onClick = onClick
    )
}

/** Виконаний разовий план — приглушений рядок з датою виконання. */
@Composable
fun DoneTaskRow(task: PlannedTask, onClick: () -> Unit) {
    StatusRow(
        title = task.title,
        subtitle = task.doneAt?.let { "Виконано ${formatShortDate(PlanRepository.utcToLocalDate(it))}" },
        status = null,
        icon = Icons.Outlined.TaskAlt,
        trailing = "",
        trailingColor = TextSecondary,
        trailingBold = false,
        onClick = onClick,
        dimmed = true
    )
}

/** Скляна картка зі списком правил. */
@Composable
fun RulePlanList(items: List<RulePlan>, onClick: (RulePlan) -> Unit, dimmed: Boolean = false) {
    StatusList(items) { item -> RulePlanRow(item = item, onClick = { onClick(item) }, dimmed = dimmed) }
}
