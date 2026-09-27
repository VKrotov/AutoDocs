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

/** Другорядні екрани поза нижнім меню (на них капсула меню ховається). */
object Routes {
    const val CAR_ID_ARG = "carId"
    const val RECORD_ID_ARG = "recordId"

    const val CAR_FORM_ADD = "car_form/add"
    const val CAR_FORM_EDIT_PATTERN = "car_form/edit/{$CAR_ID_ARG}"
    const val ARCHIVE = "archive"

    const val RECORD_FORM_ADD = "record_form/add"
    const val RECORD_FORM_EDIT_PATTERN = "record_form/edit/{$RECORD_ID_ARG}"
    const val RECORD_DETAIL_PATTERN = "record/{$RECORD_ID_ARG}"
    const val WORK_TYPES = "work_types"
    const val BACKUP = "backup"

    const val RULE_ID_ARG = "ruleId"
    const val PLAN_RULE_NEW = "plan_rule/new"
    const val PLAN_RULE_EDIT_PATTERN = "plan_rule/edit/{$RULE_ID_ARG}"
    const val PLAN_SETUP = "plan_setup"
    const val NOTIFY_SETTINGS = "notify_settings"

    const val MILEAGE = "mileage"
    const val EXPENSE_STATS = "expense_stats"

    const val OWNER_TYPE_ARG = "ownerType"
    const val OWNER_ID_ARG = "ownerId"
    const val START_ARG = "start"
    const val PHOTO_VIEWER_PATTERN = "photos/{$OWNER_TYPE_ARG}/{$OWNER_ID_ARG}/{$START_ARG}"
    const val TECH_PASSPORT_PATTERN = "passport/{$CAR_ID_ARG}"

    fun photoViewer(ownerType: String, ownerId: Long, start: Int) = "photos/$ownerType/$ownerId/$start"
    fun techPassport(carId: Long) = "passport/$carId"

    fun planRuleEdit(ruleId: Long) = "plan_rule/edit/$ruleId"

    fun carFormEdit(carId: Long) = "car_form/edit/$carId"
    fun recordFormEdit(recordId: Long) = "record_form/edit/$recordId"
    fun recordDetail(recordId: Long) = "record/$recordId"
}
