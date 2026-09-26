package com.autodocs.app.ui.screens.plan

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.autodocs.app.AutoDocsApp
import com.autodocs.app.data.plan.DoneMark
import com.autodocs.app.ui.components.GlassSurface
import com.autodocs.app.ui.components.ScreenHeader
import com.autodocs.app.ui.components.autoDocsFieldColors
import com.autodocs.app.ui.theme.Accent
import com.autodocs.app.ui.theme.AutoDocsDimens
import com.autodocs.app.ui.theme.OnAccent
import com.autodocs.app.ui.theme.TextPrimary
import com.autodocs.app.ui.theme.TextSecondary
import com.autodocs.app.ui.util.formatKm
import com.autodocs.app.ui.util.formatLongDate
import com.autodocs.app.ui.util.formatRecordDate

/** Первинне налаштування регламенту: для кожного пункту — «коли робили востаннє». */
@Composable
fun BaselineSetupScreen(onDone: () -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val app = LocalContext.current.applicationContext as AutoDocsApp
    val viewModel: BaselineSetupViewModel = viewModel(
        factory = BaselineSetupViewModelFactory(app.carRepository, app.planRepository)
    )
    val state by viewModel.state.collectAsState()
    var pickingFor by rememberSaveable { mutableStateOf<Long?>(null) }

    LaunchedEffect(state.isSaved) { if (state.isSaved) onDone() }

    if (state.isLoading) {
        Box(modifier.fillMaxSize())
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = AutoDocsDimens.ScreenPadding),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ScreenHeader(title = "Коли робили востаннє", overline = "План ТО", onBack = onBack)
            Text(
                "Вкажи пробіг і/або дату, коли кожну роботу робили востаннє. Не пам'ятаєш — лиши порожнім: " +
                    "пункт рахуватиметься простроченим, доки не з'явиться запис у журналі.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
        items(state.rows, key = { it.ruleId }) { row ->
            GlassSurface(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = AutoDocsDimens.CardRadiusInner,
                contentPadding = 14.dp
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(row.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = TextPrimary)
                    Text(row.interval, style = MaterialTheme.typography.bodyMedium, fontSize = 13.sp, color = TextSecondary)
                    row.journalMark?.let { JournalNote(it) }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = row.mileageText,
                            onValueChange = { viewModel.onMileage(row.ruleId, it) },
                            label = { Text("Пробіг") },
                            suffix = { Text("км", color = TextSecondary) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = autoDocsFieldColors(),
                            modifier = Modifier.weight(1f)
                        )
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = row.dateUtcMillis?.let(::formatRecordDate) ?: "",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Дата") },
                                trailingIcon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = null) },
                                singleLine = true,
                                colors = autoDocsFieldColors(),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Box(Modifier.matchParentSize().clickable { pickingFor = row.ruleId })
                        }
                    }
                    if (row.isFilled) {
                        TextButton(onClick = { viewModel.clear(row.ruleId) }) {
                            Text("Не пам'ятаю — очистити", color = TextSecondary)
                        }
                    }
                }
            }
        }
        item {
            Button(
                onClick = viewModel::save,
                enabled = !state.isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = OnAccent),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(52.dp)
            ) { Text("Зберегти") }
        }
    }

    pickingFor?.let { ruleId ->
        val row = state.rows.firstOrNull { it.ruleId == ruleId }
        PastDatePickerDialog(
            initialUtcMillis = row?.dateUtcMillis,
            onPicked = { viewModel.onDate(ruleId, it) },
            onDismiss = { pickingFor = null }
        )
    }
}

@Composable
fun JournalNote(mark: DoneMark) {
    val parts = listOfNotNull(mark.date?.let(::formatLongDate), mark.mileage?.let { "${formatKm(it)} км" })
    Text(
        "У журналі: ${parts.joinToString(", ")} — враховується автоматично",
        style = MaterialTheme.typography.bodyMedium,
        fontSize = 13.sp,
        color = Accent
    )
}
