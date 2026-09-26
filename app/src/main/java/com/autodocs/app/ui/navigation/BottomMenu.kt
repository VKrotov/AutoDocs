package com.autodocs.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.autodocs.app.R
import com.autodocs.app.ui.theme.Accent
import com.autodocs.app.ui.theme.AutoDocsDimens
import com.autodocs.app.ui.theme.MenuBackground
import com.autodocs.app.ui.theme.OnAccent
import com.autodocs.app.ui.theme.TextPrimary
import com.autodocs.app.ui.theme.TextSecondary

/**
 * Плаваюча скляна капсула меню з підписами (Головна · Журнал · План ТО ·
 * Налаштування) + кругла акцентна кнопка "+" збоку — за фінальним дизайном.
 * Реальна дія кнопки "+" (створення запису) з'явиться на етапі 3.
 */
@Composable
fun BottomMenu(
    current: Destination,
    onSelect: (Destination) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(AutoDocsDimens.MenuCapsuleRadius))
                .background(MenuBackground)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Destination.entries.forEach { destination ->
                MenuItem(
                    destination = destination,
                    selected = destination == current,
                    onClick = { onSelect(destination) }
                )
            }
        }

        Box(
            modifier = Modifier
                .padding(start = 12.dp)
                .size(52.dp)
                .clip(CircleShape)
                .background(Accent)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onAddClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = stringResource(R.string.nav_add),
                tint = OnAccent
            )
        }
    }
}

@Composable
private fun RowScope.MenuItem(destination: Destination, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = destination.icon,
                contentDescription = null,
                tint = if (selected) Accent else TextSecondary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = stringResource(destination.labelRes),
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) TextPrimary else TextSecondary
            )
        }
    }
}
