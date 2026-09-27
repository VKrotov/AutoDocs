package com.autodocs.app.ui.screens.documents

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.autodocs.app.AutoDocsApp
import com.autodocs.app.data.entity.DocumentType
import com.autodocs.app.data.entity.label
import com.autodocs.app.ui.components.GlassPillButton
import com.autodocs.app.ui.components.GlassSurface
import com.autodocs.app.ui.components.PillText
import com.autodocs.app.ui.components.RoundGlassButton
import com.autodocs.app.ui.components.ScreenHeader
import com.autodocs.app.ui.components.SectionLabel
import com.autodocs.app.ui.screens.plan.StatusList
import com.autodocs.app.ui.screens.plan.StatusRow
import com.autodocs.app.ui.theme.Accent
import com.autodocs.app.ui.theme.AutoDocsDimens
import com.autodocs.app.ui.theme.LinkColor
import com.autodocs.app.ui.theme.TextPrimary
import com.autodocs.app.ui.theme.TextSecondary
import com.autodocs.app.ui.util.pluralUk

/** Етап 9: документи активного авто — техпаспорт, страховка, техогляд, інше. */
@Composable
fun DocumentsScreen(
    onBack: () -> Unit,
    onAdd: (DocumentType?) -> Unit,
    onOpen: (Long) -> Unit,
    onOpenPassport: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val app = LocalContext.current.applicationContext as AutoDocsApp
    val viewModel: DocumentsViewModel = viewModel(
        factory = DocumentsViewModelFactory(app, app.carRepository, app.documentRepository, app.photoRepository)
    )
    val state by viewModel.state.collectAsState()
    if (!state.isLoaded) {
        Box(modifier.fillMaxSize())
        return
    }
    DocumentsContent(
        state = state,
        onBack = onBack,
        onAdd = onAdd,
        onOpen = onOpen,
        onOpenPassport = { state.car?.let { onOpenPassport(it.id) } },
        modifier = modifier
    )
}

/** Stateless-вміст екрана (для снапшотів). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DocumentsContent(
    state: DocumentsUiState,
    onBack: () -> Unit,
    onAdd: (DocumentType?) -> Unit,
    onOpen: (Long) -> Unit,
    onOpenPassport: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showPrevious by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AutoDocsDimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ScreenHeader(title = "Документи", overline = state.car?.name, onBack = onBack) {
            if (state.car != null) RoundGlassButton(Icons.Filled.Add, "Додати документ", { onAdd(null) }, tint = Accent)
        }
        if (state.car == null) {
            Text("Немає активного авто", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
            return@Column
        }

        StatusList(listOf(Unit)) {
            StatusRow(
                title = "Техпаспорт",
                subtitle = "Свідоцтво про реєстрацію",
                status = null,
                icon = Icons.Outlined.Badge,
                trailing = when (state.passportSides) { 0 -> "Додати фото"; 1 -> "1 з 2 сторін"; else -> "2 сторони" },
                trailingColor = if (state.passportSides == 2) TextSecondary else LinkColor,
                trailingBold = state.passportSides != 2,
                onClick = onOpenPassport
            )
        }

        SectionLabel("Страховка й техогляд", Modifier.padding(top = 8.dp))
        if (state.current.isEmpty()) {
            GlassSurface(modifier = Modifier.fillMaxWidth(), cornerRadius = AutoDocsDimens.CardRadiusInner, contentPadding = 16.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Ще немає жодного документа", style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                    Text(
                        "Внеси поліс автоцивілки чи техогляд — нагадаю за ${state.warnDays} днів до кінця дії.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(DocumentType.OSAGO, DocumentType.INSPECTION, DocumentType.GREEN_CARD).forEach { type ->
                            GlassPillButton(onClick = { onAdd(type) }) {
                                Icon(type.icon(), contentDescription = null, tint = Accent, modifier = Modifier.size(18.dp))
                                PillText(type.label())
                            }
                        }
                    }
                }
            }
        } else {
            StatusList(state.current) { item -> DocumentRow(item, onClick = { onOpen(item.doc.id) }) }
        }

        if (state.previous.isNotEmpty()) {
            val n = state.previous.size.toLong()
            Text(
                if (showPrevious) "Сховати попередні" else "Попередні · $n",
                color = LinkColor,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { showPrevious = !showPrevious }
                    .padding(horizontal = 4.dp, vertical = 6.dp)
            )
            if (showPrevious) {
                StatusList(state.previous) { item -> DocumentRow(item, onClick = { onOpen(item.doc.id) }, dimmed = true) }
            }
        }

        if (state.current.isNotEmpty()) Text(
            "Нагадаю за ${state.warnDays} ${pluralUk(state.warnDays.toLong(), "день", "дні", "днів")} до кінця дії — змінити можна в налаштуваннях сповіщень. " +
                "Продовжив поліс — внеси новий: старий перейде в «Попередні» й більше не нагадуватиме.",
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 13.sp,
            color = TextSecondary,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Spacer(Modifier.height(24.dp))
    }
}
