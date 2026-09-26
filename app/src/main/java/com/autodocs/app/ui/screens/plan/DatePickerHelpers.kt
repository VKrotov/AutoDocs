package com.autodocs.app.ui.screens.plan

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
