package com.autodocs.app.ui.screens.documents

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.PriorityHigh
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.autodocs.app.data.entity.DocumentType
import com.autodocs.app.data.entity.displayName
import com.autodocs.app.data.plan.DocumentDue
import com.autodocs.app.data.plan.DueStatus
import com.autodocs.app.ui.screens.plan.StatusRow
import com.autodocs.app.ui.theme.StatusOverdue
import com.autodocs.app.ui.theme.StatusSoon
import com.autodocs.app.ui.theme.TextSecondary
import com.autodocs.app.ui.util.formatDaysLeft
import com.autodocs.app.ui.util.formatShortDate

fun DocumentType.icon(): ImageVector = when (this) {
    DocumentType.OSAGO, DocumentType.KASKO -> Icons.Outlined.Shield
    DocumentType.GREEN_CARD -> Icons.Outlined.Public
    DocumentType.INSPECTION -> Icons.Outlined.Verified
    DocumentType.OTHER -> Icons.Outlined.Description
}

fun DueStatus.color(): Color = when (this) {
    DueStatus.OVERDUE -> StatusOverdue
    DueStatus.SOON -> StatusSoon
    DueStatus.OK -> TextSecondary
}

/** «до 14 бер. 2027 · ТАС» — термін і компанія. */
fun DocumentDue.subtitle(): String = listOfNotNull(
    "до ${formatShortDate(validUntil)}",
    doc.number.trim().takeIf { it.isNotEmpty() }?.let { "№ $it" },
    doc.company.trim().takeIf { it.isNotEmpty() }
).joinToString(" · ")

/** Праворуч: «прострочено» / «12 днів» / «≈ 8 міс». */
fun DocumentDue.trailing(): String = if (daysLeft < 0) "прострочено" else formatDaysLeft(daysLeft)

/** Рядок документа у списку: плитка статусу, назва, «до … · № … · компанія», скільки лишилось. */
@Composable
fun DocumentRow(item: DocumentDue, onClick: () -> Unit, dimmed: Boolean = false) {
    StatusRow(
        title = item.doc.displayName(),
        subtitle = item.subtitle(),
        status = if (dimmed) null else item.status,
        icon = if (!dimmed && item.status == DueStatus.OVERDUE) Icons.Outlined.PriorityHigh else item.doc.type.icon(),
        trailing = if (dimmed) "" else item.trailing(),
        trailingColor = item.status.color(),
        trailingBold = item.status != DueStatus.OK,
        onClick = onClick,
        dimmed = dimmed
    )
}
