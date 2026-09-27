package com.autodocs.app.ui.screens.plan

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.autodocs.app.ui.components.autoDocsFieldColors
import com.autodocs.app.ui.util.formatRecordDate
import com.autodocs.app.ui.theme.Accent
import com.autodocs.app.ui.util.todayUtcMillis

/** Діалог вибору дати в минулому (дата — millis опівночі UTC, як у записах журналу). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PastDatePickerDialog(initialUtcMillis: Long?, onPicked: (Long) -> Unit, onDismiss: () -> Unit) {
    val today = remember { todayUtcMillis() }
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initialUtcMillis ?: today,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis <= today
        }
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let(onPicked)
                onDismiss()
            }) { Text("Готово", color = Accent) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Скасувати") } }
    ) {
        DatePicker(state = state)
    }
}

/** Діалог вибору будь-якої дати (терміни «до», дати дії документів). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnyDatePickerDialog(initialUtcMillis: Long?, onPicked: (Long) -> Unit, onDismiss: () -> Unit) {
    val today = remember { todayUtcMillis() }
    val state = rememberDatePickerState(initialSelectedDateMillis = initialUtcMillis ?: today)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let(onPicked)
                onDismiss()
            }) { Text("Готово", color = Accent) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Скасувати") } }
    ) {
        DatePicker(state = state)
    }
}

/** Поле дати лише для читання: тап відкриває календар. */
@Composable
fun DateField(label: String, valueUtc: Long?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        OutlinedTextField(
            value = valueUtc?.let(::formatRecordDate) ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = null) },
            singleLine = true,
            colors = autoDocsFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )
        Box(Modifier.matchParentSize().clickable(onClickLabel = label, onClick = onClick))
    }
}
