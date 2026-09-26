package com.autodocs.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.autodocs.app.ui.theme.TextPrimary

/**
 * Текстове поле з підказками (довідник робіт, раніше введені СТО).
 * Підказки — чипи ПІД полем, а не випадаюче меню: спливаюче вікно меню
 * на телефоні могло перекривати кнопку «Зберегти» й «з'їдати» натискання.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> SuggestTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    suggestions: List<T>,
    suggestionText: (T) -> String,
    onSuggestionPicked: (T) -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    var picked by remember { mutableStateOf(false) }
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                picked = false
            },
            label = { Text(label) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            colors = autoDocsFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focused = it.isFocused }
        )
        val shown = if (focused && !picked) suggestions.take(6) else emptyList()
        if (shown.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
            ) {
                shown.forEach { suggestion ->
                    SuggestionChip(
                        onClick = {
                            onSuggestionPicked(suggestion)
                            picked = true
                        },
                        label = {
                            Text(suggestionText(suggestion), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(labelColor = TextPrimary)
                    )
                }
            }
        }
    }
}
