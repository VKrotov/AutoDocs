package com.autodocs.app.ui.screens.car

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.autodocs.app.AutoDocsApp
import com.autodocs.app.data.entity.FuelType
import com.autodocs.app.data.entity.TransmissionType
import com.autodocs.app.ui.theme.Accent
import com.autodocs.app.ui.theme.GlassSurface
import com.autodocs.app.ui.theme.OnAccent
import com.autodocs.app.ui.theme.TextPrimary
import com.autodocs.app.ui.theme.TextSecondary

private fun FuelType.label(): String = when (this) {
    FuelType.PETROL -> "Бензин"
    FuelType.DIESEL -> "Дизель"
    FuelType.GAS -> "Газ"
    FuelType.HYBRID -> "Гібрид"
    FuelType.ELECTRIC -> "Електро"
}

private fun TransmissionType.label(): String = when (this) {
    TransmissionType.MANUAL -> "Механіка"
    TransmissionType.AUTOMATIC -> "Автомат"
    TransmissionType.ROBOT -> "Робот"
    TransmissionType.VARIATOR -> "Варіатор"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarFormScreen(
    carIdToEdit: Long?,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as AutoDocsApp
    val viewModel: CarFormViewModel = viewModel(
        factory = CarFormViewModelFactory(app.carRepository)
    )
    val state by viewModel.state.collectAsState()

    LaunchedEffect(carIdToEdit) {
        if (carIdToEdit != null) viewModel.loadForEdit(carIdToEdit)
    }
    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onSaved()
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                // деякі джерела (наприклад, Google Photos) не підтримують persistable permission — це ок
            }
            viewModel.onPhotoPicked(uri.toString())
        }
    }

    var fuelExpanded by rememberSaveable { mutableStateOf(false) }
    var transmissionExpanded by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 20.dp),
        contentPadding = PaddingValues(vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = if (carIdToEdit == null) "Нове авто" else "Редагувати авто",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary
            )
        }

        item {
            PhotoPicker(
                photoUri = state.photoUri,
                onPick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
            )
        }

        item { LabeledField("Назва (напр. «Мій Passat»)", state.name, viewModel::onNameChange) }
        item { LabeledField("Марка", state.make, viewModel::onMakeChange) }
        item { LabeledField("Модель", state.model, viewModel::onModelChange) }
        item { LabeledField("Двигун (напр. «2.0 AZM»)", state.engine, viewModel::onEngineChange) }

        item {
            ExposedDropdownMenuBox(expanded = fuelExpanded, onExpandedChange = { fuelExpanded = it }) {
                OutlinedTextField(
                    value = state.fuelType.label(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Паливо") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fuelExpanded) },
                    colors = fieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                )
                ExposedDropdownMenu(expanded = fuelExpanded, onDismissRequest = { fuelExpanded = false }) {
                    FuelType.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.label()) },
                            onClick = { viewModel.onFuelTypeChange(option); fuelExpanded = false }
                        )
                    }
                }
            }
        }

        item {
            ExposedDropdownMenuBox(expanded = transmissionExpanded, onExpandedChange = { transmissionExpanded = it }) {
                OutlinedTextField(
                    value = state.transmissionType.label(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Коробка передач") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = transmissionExpanded) },
                    colors = fieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                )
                ExposedDropdownMenu(expanded = transmissionExpanded, onDismissRequest = { transmissionExpanded = false }) {
                    TransmissionType.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.label()) },
                            onClick = { viewModel.onTransmissionTypeChange(option); transmissionExpanded = false }
                        )
                    }
                }
            }
        }

        item { LabeledField("Держ. номер", state.licensePlate, viewModel::onLicensePlateChange) }
        item { LabeledField("VIN", state.vin, viewModel::onVinChange) }
        item {
            LabeledField(
                label = "Поточний пробіг, км",
                value = state.mileageText,
                onValueChange = viewModel::onMileageChange,
                keyboardType = KeyboardType.Number
            )
        }

        item {
            Button(
                onClick = viewModel::save,
                enabled = state.isValid && !state.isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = OnAccent),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text(if (carIdToEdit == null) "Додати авто" else "Зберегти")
            }
        }
    }
}

@Composable
private fun PhotoPicker(photoUri: String?, onPick: () -> Unit) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(GlassSurface)
            .clickable(onClick = onPick),
        contentAlignment = Alignment.Center
    ) {
        if (photoUri != null) {
            val bitmap = remember(photoUri) {
                runCatching {
                    context.contentResolver.openInputStream(android.net.Uri.parse(photoUri))?.use {
                        BitmapFactory.decodeStream(it)
                    }
                }.getOrNull()
            }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = TextSecondary)
                Text("Додати фото авто", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LabeledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = fieldColors(),
        modifier = Modifier.fillMaxWidth()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedBorderColor = Accent,
    unfocusedBorderColor = TextSecondary.copy(alpha = 0.4f),
    focusedLabelColor = Accent,
    unfocusedLabelColor = TextSecondary,
    cursorColor = Accent
)
