package com.autodocs.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.List
import androidx.compose.ui.graphics.vector.ImageVector
import com.autodocs.app.R

/** Чотири пункти плаваючої капсули меню (без "+", кнопка додавання окрема). */
enum class Destination(val route: String, val labelRes: Int, val icon: ImageVector) {
    HOME("home", R.string.nav_home, Icons.Filled.Home),
    JOURNAL("journal", R.string.nav_journal, Icons.Outlined.List),
    PLAN("plan", R.string.nav_plan, Icons.Outlined.DateRange),
    SETTINGS("settings", R.string.nav_settings, Icons.Filled.Settings)
}

/** Другорядні екрани поза нижнім меню (не показуються в капсулі). */
object Routes {
    const val CAR_ID_ARG = "carId"
    const val CAR_FORM_ADD = "car_form/add"
    const val CAR_FORM_EDIT_PATTERN = "car_form/edit/{$CAR_ID_ARG}"
    const val ARCHIVE = "archive"

    fun carFormEdit(carId: Long) = "car_form/edit/$carId"
}
