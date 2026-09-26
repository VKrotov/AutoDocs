package com.autodocs.app.ui.screens.car

import android.app.Application
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.autodocs.app.AutoDocsApp
import com.autodocs.app.data.entity.FuelType
import com.autodocs.app.data.entity.TransmissionType
import com.autodocs.app.ui.components.CarPhoto
import com.autodocs.app.ui.components.ScreenHeader
import com.autodocs.app.ui.components.SectionLabel
import com.autodocs.app.ui.components.autoDocsFieldColors
import com.autodocs.app.ui.theme.Accent
import com.autodocs.app.ui.theme.AutoDocsDimens
import com.autodocs.app.ui.theme.GlassBorder
import com.autodocs.app.ui.theme.GlassSurface
import com.autodocs.app.ui.theme.LinkColor
import com.autodocs.app.ui.theme.OnAccent
import com.autodocs.app.ui.theme.TextSecondary
import com.autodocs.app.ui.util.label

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarFormScreen(
    carIdToEdit: Long?,
    onSaved: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as AutoDocsApp
    val viewModel: CarFormViewModel = viewModel(
        factory = CarFormViewModelFactory(context.applicationContext as Application, app.carRepository)
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
        if (uri != null) viewModel.onPhotoPicked(uri)
    }

    var fuelExpanded by rememberSaveable { mutableStateOf(false) }
    var transmissionExpanded by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = AutoDocsDimens.ScreenPadding),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ScreenHeader(
                title = if (carIdToEdit == null) "Нове авто" else "Редагувати авто",
                onBack = onBack
            )
        }

        item {
            PhotoPickerBox(
                photoUri = state.photoUri,
                isImporting = state.isPhotoImporting,
                onPick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                onRemove = viewModel::onPhotoRemoved
            )
        }

        item { SectionLabel("Обов'язково", Modifier.padding(top = 8.dp)) }
        item { LabeledField("Назва (напр. «Мій Passat»)", state.name, viewModel::onNameChange) }
        item { LabeledField("Марка", state.make, viewModel::onMakeChange) }
        item { LabeledField("Модель", state.model, viewModel::onModelChange) }

        item { SectionLabel("Можна дописати пізніше", Modifier.padding(top = 8.dp)) }
        item { LabeledField("Двигун (напр. «2.0 AZM»)", state.engine, viewModel::onEngineChange) }

        item {
            ExposedDropdownMenuBox(expanded = fuelExpanded, onExpandedChange = { fuelExpanded = it }) {
                OutlinedTextField(
                    value = state.fuelType.label(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Паливо") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fuelExpanded) },
                    colors = autoDocsFieldColors(),
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
                    colors = autoDocsFieldColors(),
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

        item {
            LabeledField(
                "Держ. номер",
                state.licensePlate,
                viewModel::onLicensePlateChange,
                capitalization = KeyboardCapitalization.Characters
            )
        }
        item {
            LabeledField(
                "VIN",
                state.vin,
                viewModel::onVinChange,
                capitalization = KeyboardCapitalization.Characters
            )
        }
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
                enabled = state.isValid && !state.isLoading && !state.isPhotoImporting,
                colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = OnAccent),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(52.dp)
            ) {
                Text(if (carIdToEdit == null) "Додати авто" else "Зберегти")
            }
            if (!state.isValid) {
                Text(
                    "Щоб зберегти, достатньо заповнити назву, марку й модель",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun PhotoPickerBox(
    photoUri: String?,
    isImporting: Boolean,
    onPick: () -> Unit,
    onRemove: () -> Unit
) {
    val shape = RoundedCornerShape(AutoDocsDimens.CardRadiusInner)
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .clip(shape)
                .background(GlassSurface)
                .border(1.dp, GlassBorder, shape)
                .clickable(onClick = onPick),
            contentAlignment = Alignment.Center
        ) {
            when {
                isImporting -> CircularProgressIndicator(color = Accent, modifier = Modifier.size(28.dp))
                photoUri != null -> CarPhoto(uri = photoUri, modifier = Modifier.fillMaxSize())
                else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.AddAPhoto, contentDescription = null, tint = TextSecondary)
                    Text(
                        "Додати фото авто",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }
        if (photoUri != null && !isImporting) {
            Row {
                TextButton(onClick = onPick) { Text("Змінити фото", color = LinkColor) }
                TextButton(onClick = onRemove) { Text("Прибрати фото", color = TextSecondary) }
            }
        }
    }
}

@Composable
private fun LabeledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Sentences
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, capitalization = capitalization),
        colors = autoDocsFieldColors(),
        modifier = Modifier.fillMaxWidth()
    )
}
