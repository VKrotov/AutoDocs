package com.autodocs.app.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.autodocs.app.BuildConfig
import com.autodocs.app.R
import com.autodocs.app.ui.components.GlassSurface
import com.autodocs.app.ui.theme.AutoDocsDimens
import com.autodocs.app.ui.theme.TextPrimary
import com.autodocs.app.ui.theme.TextSecondary

private data class SettingsRow(val title: String, val hint: String? = null)
private data class SettingsSection(val title: String, val rows: List<SettingsRow>)

/**
 * F13, базова версія (етап 1): структура розділів налаштувань є, але без
 * реальної логіки — вона з'являється разом із відповідними фічами
 * (авто/архів — етап 2, бекап — етап 4, сповіщення — етап 6).
 */
@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val sections = listOf(
        SettingsSection(
            title = stringResource(R.string.settings_section_car),
            rows = listOf(SettingsRow(stringResource(R.string.settings_active_car)))
        ),
        SettingsSection(
            title = stringResource(R.string.settings_section_data),
            rows = listOf(
                SettingsRow(
                    stringResource(R.string.settings_backup),
                    stringResource(R.string.settings_backup_hint)
                )
            )
        ),
        SettingsSection(
            title = stringResource(R.string.settings_section_notifications),
            rows = listOf(
                SettingsRow(
                    stringResource(R.string.settings_notifications),
                    stringResource(R.string.settings_notifications_hint)
                )
            )
        ),
        SettingsSection(
            title = stringResource(R.string.settings_section_about),
            rows = listOf(SettingsRow(stringResource(R.string.settings_version) + ": ${BuildConfig.VERSION_NAME}"))
        )
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = AutoDocsDimens.ScreenPadding),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = AutoDocsDimens.ScreenPadding)
    ) {
        item {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary
            )
        }
        items(sections) { section ->
            Column(modifier = Modifier.padding(top = AutoDocsDimens.SectionSpacing)) {
                Text(
                    text = section.title,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                GlassSurface(modifier = Modifier.fillMaxWidth()) {
                    section.rows.forEachIndexed { index, row ->
                        if (index > 0) {
                            androidx.compose.material3.HorizontalDivider(
                                color = TextSecondary.copy(alpha = 0.15f)
                            )
                        }
                        Column(modifier = Modifier.padding(vertical = 10.dp)) {
                            Text(row.title, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                            row.hint?.let {
                                Text(it, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            }
                        }
                    }
                }
            }
        }
    }
}
