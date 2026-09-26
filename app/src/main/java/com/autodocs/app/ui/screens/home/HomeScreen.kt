package com.autodocs.app.ui.screens.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.autodocs.app.R
import com.autodocs.app.ui.theme.TextSecondary

/**
 * Заглушка головного екрана. Скляна картка авто, "Найближче ТО" тощо —
 * етапи 2/5/6, коли з'являться дані з БД.
 */
@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(R.string.placeholder_coming_soon),
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
    }
}
