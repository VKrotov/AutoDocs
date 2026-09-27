package com.autodocs.app.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.flow.first
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.autodocs.app.AutoDocsApp
import com.autodocs.app.data.entity.DocumentType
import com.autodocs.app.data.entity.PhotoOwnerType
import com.autodocs.app.ui.screens.documents.DocumentFormScreen
import com.autodocs.app.ui.screens.documents.DocumentsScreen
import com.autodocs.app.ui.screens.plan.TaskEditScreen
import com.autodocs.app.data.notify.MaintenanceNotifier
import com.autodocs.app.ui.screens.backup.BackupScreen
import com.autodocs.app.ui.screens.car.ArchiveScreen
import com.autodocs.app.ui.screens.car.CarFormScreen
import com.autodocs.app.ui.screens.home.HomeScreen
import com.autodocs.app.ui.screens.journal.JournalScreen
import com.autodocs.app.ui.screens.notify.NotificationSettingsScreen
import com.autodocs.app.ui.screens.photos.PhotoViewerScreen
import com.autodocs.app.ui.screens.photos.TechPassportScreen
import com.autodocs.app.ui.screens.plan.BaselineSetupScreen
import com.autodocs.app.ui.screens.plan.PlanScreen
import com.autodocs.app.ui.screens.plan.RuleEditScreen
import com.autodocs.app.ui.screens.record.RecordDetailScreen
import com.autodocs.app.ui.screens.record.RecordFormScreen
import com.autodocs.app.ui.screens.settings.SettingsScreen
import com.autodocs.app.ui.screens.stats.ExpenseStatsScreen
import com.autodocs.app.ui.screens.stats.MileageScreen
import com.autodocs.app.ui.screens.worktypes.WorkTypesScreen

@Composable
fun AutoDocsNavHost(openRequest: String? = null, onOpenRequestHandled: () -> Unit = {}) {
    val app = LocalContext.current.applicationContext as AutoDocsApp
    val activeCar by app.carRepository.observeActiveCar().collectAsState(initial = null)

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: Destination.HOME.route
    val currentDestination = Destination.entries.firstOrNull { it.route == currentRoute }
    // Капсула меню — лише на 4 головних вкладках; форми й деталі відкриваються "поверх" без неї.
    val showBottomMenu = currentDestination != null

    val back: () -> Unit = { navController.popBackStack() }

    /** Перехід на вкладку капсули зі збереженням стану вкладок. */
    val openTab: (Destination) -> Unit = { destination ->
        navController.navigate(destination.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }


    Scaffold(
        containerColor = Color.Transparent,
        // Системні відступи обробляємо самі: статус-бар — для всього контенту,
        // навігаційна панель — у капсулі меню або в самих другорядних екранах.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomMenu) {
                BottomMenu(
                    current = currentDestination ?: Destination.HOME,
                    onSelect = { destination ->
                        openTab(destination)
                    },
                    onAddClick = {
                        navController.navigate(if (activeCar != null) Routes.RECORD_FORM_ADD else Routes.CAR_FORM_ADD)
                    },
                    modifier = Modifier.navigationBarsPadding()
                )
            }
        }
    ) { innerPadding ->
        val tabModifier = Modifier.padding(innerPadding)
        // Для другорядних екранів: не заходимо під навбар і піднімаємось над клавіатурою.
        val secondaryModifier = Modifier.padding(innerPadding).navigationBarsPadding().imePadding()

        NavHost(
            navController = navController,
            startDestination = Destination.HOME.route,
            modifier = Modifier.fillMaxSize().statusBarsPadding()
        ) {
            composable(Destination.HOME.route) {
                HomeScreen(
                    onAddCar = { navController.navigate(Routes.CAR_FORM_ADD) },
                    onEditCar = { carId -> navController.navigate(Routes.carFormEdit(carId)) },
                    onOpenPlan = { openTab(Destination.PLAN) },
                    onOpenRule = { id -> navController.navigate(Routes.planRuleEdit(id)) },
                    onOpenPassport = { carId -> navController.navigate(Routes.techPassport(carId)) },
                    onOpenMileage = { navController.navigate(Routes.MILEAGE) },
                    onOpenDocuments = { navController.navigate(Routes.DOCUMENTS) },
                    onOpenTask = { id -> navController.navigate(Routes.taskEdit(id)) },
                    modifier = tabModifier
                )
            }
            composable(Destination.JOURNAL.route) {
                JournalScreen(
                    onOpenRecord = { id -> navController.navigate(Routes.recordDetail(id)) },
                    onAddRecord = { navController.navigate(Routes.RECORD_FORM_ADD) },
                    onAddCar = { navController.navigate(Routes.CAR_FORM_ADD) },
                    onOpenStats = { navController.navigate(Routes.EXPENSE_STATS) },
                    modifier = tabModifier
                )
            }
            composable(Destination.PLAN.route) {
                PlanScreen(
                    onOpenRule = { id -> navController.navigate(Routes.planRuleEdit(id)) },
                    onAddRule = { navController.navigate(Routes.PLAN_RULE_NEW) },
                    onOpenSetup = { navController.navigate(Routes.PLAN_SETUP) },
                    onAddCar = { navController.navigate(Routes.CAR_FORM_ADD) },
                    onOpenMileage = { navController.navigate(Routes.MILEAGE) },
                    onAddTask = { navController.navigate(Routes.TASK_NEW) },
                    onOpenTask = { id -> navController.navigate(Routes.taskEdit(id)) },
                    modifier = tabModifier
                )
            }
            composable(Destination.SETTINGS.route) {
                SettingsScreen(
                    onNavigateToArchive = { navController.navigate(Routes.ARCHIVE) },
                    onNavigateToWorkTypes = { navController.navigate(Routes.WORK_TYPES) },
                    onNavigateToBackup = { navController.navigate(Routes.BACKUP) },
                    onNavigateToNotifications = { navController.navigate(Routes.NOTIFY_SETTINGS) },
                    modifier = tabModifier
                )
            }

            composable(Routes.CAR_FORM_ADD) {
                CarFormScreen(carIdToEdit = null, onSaved = back, onBack = back, modifier = secondaryModifier)
            }
            composable(
                route = Routes.CAR_FORM_EDIT_PATTERN,
                arguments = listOf(navArgument(Routes.CAR_ID_ARG) { type = NavType.LongType })
            ) { entry ->
                CarFormScreen(
                    carIdToEdit = entry.arguments?.getLong(Routes.CAR_ID_ARG),
                    onSaved = back,
                    onBack = back,
                    modifier = secondaryModifier
                )
            }
            composable(Routes.ARCHIVE) {
                ArchiveScreen(onBack = back, modifier = secondaryModifier)
            }

            composable(
                route = Routes.RECORD_FORM_ADD_PATTERN,
                arguments = listOf(navArgument(Routes.TASK_ARG) { type = NavType.LongType; defaultValue = -1L })
            ) { entry ->
                RecordFormScreen(
                    recordIdToEdit = null,
                    fromTaskId = entry.arguments?.getLong(Routes.TASK_ARG)?.takeIf { it > 0 },
                    onSaved = {
                        // Після нового запису — у журнал, щоб одразу його бачити.
                        // Спершу ПРИБИРАЄМО форму зі стеку: інакше saveState "запам'ятовував" її
                        // під вкладкою «Головна», і тап на «Головна» знову відкривав форму,
                        // яка одразу перекидала назад у журнал (баг: не можна було повернутись на головну).
                        navController.popBackStack()
                        navController.navigate(Destination.JOURNAL.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onBack = back,
                    modifier = secondaryModifier
                )
            }
            composable(
                route = Routes.RECORD_FORM_EDIT_PATTERN,
                arguments = listOf(navArgument(Routes.RECORD_ID_ARG) { type = NavType.LongType })
            ) { entry ->
                RecordFormScreen(
                    recordIdToEdit = entry.arguments?.getLong(Routes.RECORD_ID_ARG),
                    onSaved = back,
                    onBack = back,
                    modifier = secondaryModifier
                )
            }
            composable(
                route = Routes.RECORD_DETAIL_PATTERN,
                arguments = listOf(navArgument(Routes.RECORD_ID_ARG) { type = NavType.LongType })
            ) { entry ->
                val recordId = entry.arguments?.getLong(Routes.RECORD_ID_ARG) ?: 0L
                RecordDetailScreen(
                    recordId = recordId,
                    onBack = back,
                    onEdit = { id -> navController.navigate(Routes.recordFormEdit(id)) },
                    onOpenPhoto = { index ->
                        navController.navigate(Routes.photoViewer(PhotoOwnerType.SERVICE_RECORD.name, recordId, index))
                    },
                    modifier = secondaryModifier
                )
            }
            composable(Routes.WORK_TYPES) {
                WorkTypesScreen(onBack = back, modifier = secondaryModifier)
            }
            composable(Routes.PLAN_RULE_NEW) {
                RuleEditScreen(ruleId = null, onDone = back, onBack = back, modifier = secondaryModifier)
            }
            composable(
                route = Routes.PLAN_RULE_EDIT_PATTERN,
                arguments = listOf(navArgument(Routes.RULE_ID_ARG) { type = NavType.LongType })
            ) { entry ->
                RuleEditScreen(
                    ruleId = entry.arguments?.getLong(Routes.RULE_ID_ARG),
                    onDone = back,
                    onBack = back,
                    modifier = secondaryModifier
                )
            }
            composable(Routes.PLAN_SETUP) {
                BaselineSetupScreen(onDone = back, onBack = back, modifier = secondaryModifier)
            }
            composable(
                route = Routes.PHOTO_VIEWER_PATTERN,
                arguments = listOf(
                    navArgument(Routes.OWNER_TYPE_ARG) { type = NavType.StringType },
                    navArgument(Routes.OWNER_ID_ARG) { type = NavType.LongType },
                    navArgument(Routes.START_ARG) { type = NavType.IntType }
                )
            ) { entry ->
                val args = entry.arguments
                PhotoViewerScreen(
                    ownerType = PhotoOwnerType.valueOf(args?.getString(Routes.OWNER_TYPE_ARG) ?: PhotoOwnerType.SERVICE_RECORD.name),
                    ownerId = args?.getLong(Routes.OWNER_ID_ARG) ?: 0L,
                    startIndex = args?.getInt(Routes.START_ARG) ?: 0,
                    onBack = back
                )
            }
            composable(
                route = Routes.TECH_PASSPORT_PATTERN,
                arguments = listOf(navArgument(Routes.CAR_ID_ARG) { type = NavType.LongType })
            ) { entry ->
                val carId = entry.arguments?.getLong(Routes.CAR_ID_ARG) ?: 0L
                TechPassportScreen(
                    carId = carId,
                    onBack = back,
                    onOpenPhoto = { index -> navController.navigate(Routes.photoViewer(PhotoOwnerType.CAR.name, carId, index)) },
                    modifier = secondaryModifier
                )
            }
            composable(Routes.NOTIFY_SETTINGS) {
                NotificationSettingsScreen(onBack = back, modifier = secondaryModifier)
            }
            composable(Routes.BACKUP) {
                BackupScreen(onBack = back, modifier = secondaryModifier)
            }
            composable(Routes.MILEAGE) {
                MileageScreen(
                    onBack = back,
                    onOpenRecord = { id -> navController.navigate(Routes.recordDetail(id)) },
                    modifier = secondaryModifier
                )
            }
            composable(Routes.EXPENSE_STATS) {
                ExpenseStatsScreen(onBack = back, modifier = secondaryModifier)
            }

            // ---- Етап 9 ----
            composable(Routes.DOCUMENTS) {
                DocumentsScreen(
                    onBack = back,
                    onAdd = { type -> navController.navigate(Routes.documentNew(type?.name)) },
                    onOpen = { id -> navController.navigate(Routes.documentEdit(id)) },
                    onOpenPassport = { carId -> navController.navigate(Routes.techPassport(carId)) },
                    modifier = secondaryModifier
                )
            }
            composable(
                route = Routes.DOCUMENT_NEW_PATTERN,
                arguments = listOf(
                    navArgument(Routes.DOC_TYPE_ARG) { type = NavType.StringType; defaultValue = "" },
                    navArgument(Routes.RENEW_ARG) { type = NavType.LongType; defaultValue = -1L }
                )
            ) { entry ->
                val typeName = entry.arguments?.getString(Routes.DOC_TYPE_ARG).orEmpty()
                DocumentFormScreen(
                    docId = null,
                    presetType = DocumentType.entries.firstOrNull { it.name == typeName },
                    renewFromId = entry.arguments?.getLong(Routes.RENEW_ARG)?.takeIf { it > 0 },
                    onDone = back,
                    onBack = back,
                    onRenew = {},
                    onOpenPhoto = { _, _ -> },
                    modifier = secondaryModifier
                )
            }
            composable(
                route = Routes.DOCUMENT_EDIT_PATTERN,
                arguments = listOf(navArgument(Routes.DOC_ID_ARG) { type = NavType.LongType })
            ) { entry ->
                DocumentFormScreen(
                    docId = entry.arguments?.getLong(Routes.DOC_ID_ARG),
                    presetType = null,
                    renewFromId = null,
                    onDone = back,
                    onBack = back,
                    onRenew = { id ->
                        // Замість старого документа — форма нового поліса (назад → до списку).
                        navController.popBackStack()
                        navController.navigate(Routes.documentNew(renewFrom = id))
                    },
                    onOpenPhoto = { id, index ->
                        navController.navigate(Routes.photoViewer(PhotoOwnerType.CAR_DOCUMENT.name, id, index))
                    },
                    modifier = secondaryModifier
                )
            }
            composable(Routes.TASK_NEW) {
                TaskEditScreen(
                    taskId = null,
                    onDone = back,
                    onBack = back,
                    onLogToJournal = {},
                    onOpenRecord = {},
                    modifier = secondaryModifier
                )
            }
            composable(
                route = Routes.TASK_EDIT_PATTERN,
                arguments = listOf(navArgument(Routes.TASK_ID_ARG) { type = NavType.LongType })
            ) { entry ->
                TaskEditScreen(
                    taskId = entry.arguments?.getLong(Routes.TASK_ID_ARG),
                    onDone = back,
                    onBack = back,
                    onLogToJournal = { id ->
                        navController.popBackStack()
                        navController.navigate(Routes.recordFormForTask(id))
                    },
                    onOpenRecord = { id -> navController.navigate(Routes.recordDetail(id)) },
                    modifier = secondaryModifier
                )
            }
        }
    }

    // Відкриття з натиснутого сповіщення (після того, як NavHost отримав граф).
    LaunchedEffect(openRequest) {
        if (openRequest == null) return@LaunchedEffect
        // Граф NavHost встановлюється трохи пізніше за перший кадр — чекаємо першого екрана в стеку,
        // інакше navigate() падає з «You must call setGraph() before calling getGraph()».
        navController.currentBackStackEntryFlow.first()
        when (openRequest) {
            MaintenanceNotifier.OPEN_PLAN -> openTab(Destination.PLAN)
            MaintenanceNotifier.OPEN_HOME -> openTab(Destination.HOME)
            MaintenanceNotifier.OPEN_DOCUMENTS -> {
                openTab(Destination.HOME)
                navController.navigate(Routes.DOCUMENTS)
            }
            else -> return@LaunchedEffect
        }
        onOpenRequestHandled()
    }
}
