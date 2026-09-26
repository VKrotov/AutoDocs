package com.autodocs.app.ui.screens.car

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.autodocs.app.AutoDocsApp
import com.autodocs.app.ui.components.GlassSurface
import com.autodocs.app.ui.theme.AutoDocsDimens
import com.autodocs.app.ui.theme.TextPrimary
import com.autodocs.app.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** F01: список архівних авто (перегляд, без відновлення на цьому етапі). */
@Composable
fun ArchiveScreen(modifier: Modifier = Modifier) {
    val app = LocalContext.current.applicationContext as AutoDocsApp
    val viewModel: ArchiveViewModel = viewModel(factory = ArchiveViewModelFactory(app.carRepository))
    val archivedCars by viewModel.archivedCars.collectAsState()
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }

    Column(modifier = modifier.fillMaxSize().padding(horizontal = AutoDocsDimens.ScreenPadding)) {
        Text(
            text = "Архів авто",
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
            modifier = Modifier.padding(vertical = AutoDocsDimens.ScreenPadding)
        )

        if (archivedCars.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Архів порожній", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = AutoDocsDimens.ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(archivedCars, key = { it.id }) { car ->
                    GlassSurface(modifier = Modifier.fillMaxWidth()) {
                        Text(car.name, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text(
                            "${car.make} ${car.model} · ${car.licensePlate}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Text(
                            "В архіві з ${dateFormat.format(Date(car.createdAt))}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}
