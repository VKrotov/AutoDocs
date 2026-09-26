package com.autodocs.app.ui.screens.home

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.autodocs.app.AutoDocsApp
import com.autodocs.app.data.entity.Car
import com.autodocs.app.ui.components.GlassSurface
import com.autodocs.app.ui.theme.Accent
import com.autodocs.app.ui.theme.AutoDocsDimens
import com.autodocs.app.ui.theme.OnAccent
import com.autodocs.app.ui.theme.StatusOverdue
import com.autodocs.app.ui.theme.TextPrimary
import com.autodocs.app.ui.theme.TextSecondary
import com.autodocs.app.ui.theme.VinTextStyle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun com.autodocs.app.data.entity.FuelType.label(): String = when (this) {
    com.autodocs.app.data.entity.FuelType.PETROL -> "Бензин"
    com.autodocs.app.data.entity.FuelType.DIESEL -> "Дизель"
    com.autodocs.app.data.entity.FuelType.GAS -> "Газ"
    com.autodocs.app.data.entity.FuelType.HYBRID -> "Гібрид"
    com.autodocs.app.data.entity.FuelType.ELECTRIC -> "Електро"
}

private fun com.autodocs.app.data.entity.TransmissionType.label(): String = when (this) {
    com.autodocs.app.data.entity.TransmissionType.MANUAL -> "Механіка"
    com.autodocs.app.data.entity.TransmissionType.AUTOMATIC -> "Автомат"
    com.autodocs.app.data.entity.TransmissionType.ROBOT -> "Робот"
    com.autodocs.app.data.entity.TransmissionType.VARIATOR -> "Варіатор"
}

@Composable
fun HomeScreen(
    onAddCar: () -> Unit,
    onEditCar: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val app = LocalContext.current.applicationContext as AutoDocsApp
    val viewModel: HomeViewModel = viewModel(factory = HomeViewModelFactory(app.carRepository))
    val activeCar by viewModel.activeCar.collectAsState()

    val car = activeCar
    if (car == null) {
        EmptyState(onAddCar = onAddCar, modifier = modifier)
    } else {
        CarHome(
            car = car,
            onEdit = { onEditCar(car.id) },
            onArchive = { viewModel.archiveActiveCar(car.id) },
            onUpdateMileage = { viewModel.updateMileage(car.id, it) },
            modifier = modifier
        )
    }
}

@Composable
private fun EmptyState(onAddCar: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Немає жодного авто",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
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
                Spacer(Modifier.padding(4.dp))
                Text("Додати авто")
            }
        }
    }
}

@Composable
private fun CarHome(
    car: Car,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
    onUpdateMileage: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var showArchiveConfirm by remember { mutableStateOf(false) }
    var showMileageDialog by remember { mutableStateOf(false) }
    var vinExpanded by remember { mutableStateOf(false) }
    var justCopied by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    val dateFormat = remember { SimpleDateFormat("d MMMM", Locale("uk")) }

    Column(modifier = modifier.fillMaxSize().padding(horizontal = AutoDocsDimens.ScreenPadding)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = AutoDocsDimens.ScreenPadding, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    dateFormat.format(Date()),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
                Text(
                    car.name,
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = "Редагувати", tint = TextSecondary)
            }
            IconButton(onClick = { showArchiveConfirm = true }) {
                Icon(Icons.Filled.Archive, contentDescription = "Архівувати", tint = TextSecondary)
            }
        }

        GlassSurface(modifier = Modifier.fillMaxWidth()) {
            if (car.photoUri != null) {
                val context = LocalContext.current
                val bitmap = remember(car.photoUri) {
                    runCatching {
                        context.contentResolver.openInputStream(android.net.Uri.parse(car.photoUri))?.use {
                            BitmapFactory.decodeStream(it)
                        }
                    }.getOrNull()
                }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(14.dp))
                    )
                    Spacer(Modifier.padding(top = 6.dp))
                }
            }

            Text("${car.make} ${car.model}", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Text(
                "${car.engine} · ${car.fuelType.label()} · ${car.transmissionType.label()}",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(Modifier.padding(top = 10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                PlateChip(car.licensePlate)
            }

            Spacer(Modifier.padding(top = 8.dp))

            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { vinExpanded = !vinExpanded }
                    .padding(vertical = 4.dp)
            ) {
                Text("VIN", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                if (vinExpanded) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(car.vin, style = VinTextStyle, color = TextPrimary, modifier = Modifier.weight(1f))
                        TextButton(onClick = {
                            clipboard.setText(AnnotatedString(car.vin))
                            justCopied = true
                        }) {
                            Text(if (justCopied) "Скопійовано" else "Копіювати", color = Accent)
                        }
                    }
                } else {
                    Text(car.vin.take(4) + "···", style = VinTextStyle, color = TextSecondary)
                }
            }

            Spacer(Modifier.padding(top = 14.dp))

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Пробіг", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Text("${car.mileage} км", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                }
                TextButton(onClick = { showMileageDialog = true }) {
                    Text("Оновити", color = Accent)
                }
            }
        }
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
        var text by remember { mutableStateOf(car.mileage.toString()) }
        AlertDialog(
            onDismissRequest = { showMileageDialog = false },
            title = { Text("Оновити пробіг") },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { if (it.all { c -> c.isDigit() }) text = it },
                    label = { Text("Пробіг, км") },
                    singleLine = true
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
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(Accent.copy(alpha = 0.15f))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(plate, style = MaterialTheme.typography.labelMedium, color = Accent)
    }
}
