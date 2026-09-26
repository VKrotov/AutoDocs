package com.autodocs.app.ui.screens.settings

import androidx.compose.foundation.clickable
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

private data class SettingsRow(val title: String, val hint: String? = null, val onClick: (() -> Unit)? = null)
private data class SettingsSection(val title: String, val rows: List<SettingsRow>)

/**
 * F13, базова версія (етап 1): структура розділів налаштувань є, але без
 * реальної логіки — вона з'являється разом із відповідними фічами
 * (авто/архів — етап 2, довідник робіт — етап 3, бекап — етап 4, сповіщення — етап 6).
 */
@Composable
fun SettingsScreen(
    onNavigateToArchive: () -> Unit,
    onNavigateToWorkTypes: () -> Unit,
    onNavigateToBackup: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sections = listOf(
        SettingsSection(
            title = stringResource(R.string.settings_section_car),
            rows = listOf(
                SettingsRow(
                    title = stringResource(R.string.settings_active_car),
                    onClick = onNavigateToArchive
                )
            )
        ),
        SettingsSection(
            title = stringResource(R.string.settings_section_journal),
            rows = listOf(
                SettingsRow(
                    title = stringResource(R.string.settings_work_types),
                    hint = stringResource(R.string.settings_work_types_hint),
                    onClick = onNavigateToWorkTypes
                )
            )
        ),
        SettingsSection(
            title = stringResource(R.string.settings_section_data),
            rows = listOf(
                SettingsRow(
                    title = stringResource(R.string.settings_backup),
                    hint = stringResource(R.string.settings_backup_hint),
                    onClick = onNavigateToBackup
                )
            )
        ),
        SettingsSection(
            title = stringResource(R.string.settings_section_notifications),
            rows = listOf(
                SettingsRow(
                    title = stringResource(R.string.settings_notifications),
                    hint = stringResource(R.string.settings_notifications_hint),
                    onClick = onNavigateToNotifications
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
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = AutoDocsDimens.ScreenPadding)
    ) {
        item {
            com.autodocs.app.ui.components.ScreenHeader(title = stringResource(R.string.settings_title))
        }
        items(sections) { section ->
            Column(modifier = Modifier.padding(bottom = AutoDocsDimens.SectionSpacing)) {
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
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .let { m -> row.onClick?.let { m.clickable(onClick = it) } ?: m }
                                .padding(vertical = 10.dp)
                        ) {
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
