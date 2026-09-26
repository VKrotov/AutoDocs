package com.autodocs.app.data.plan

/**
 * Стартовий регламент ТО (F07) — під VW Passat B5+ 2.0 AZM, механіка, ~350 000 км.
 * [workName] — назва пункту довідника робіт (категорія «Робота»): за нею правило
 * зв'язується з записами журналу. Якщо такого пункту в довіднику немає — його буде створено.
 * [hint] — коротка підказка, що саме міняти (показується в картці правила).
 */
data class TemplateRule(
    val workName: String,
    val intervalKm: Int?,
    val intervalMonths: Int?,
    val isActive: Boolean = true,
    val hint: String? = null
)

object MaintenanceTemplate {
    val azm: List<TemplateRule> = listOf(
        TemplateRule("Заміна масла двигуна", 10_000, 12, hint = "Масло + фільтр · 4,0 л · VW 500/501/502 · 5W-40 або 10W-40"),
        TemplateRule("Контроль рівня масла", 1_000, 1, hint = "Перевірити щупом, за потреби долити"),
        TemplateRule("Заміна ременя ГРМ з роликами", 60_000, 48, hint = "Разом з помпою. Завод: 180 000 км, огляд з 90 000"),
        TemplateRule("Заміна поліклинового ременя", 60_000, 48),
        TemplateRule("Заміна свічок запалювання", 30_000, 24, hint = "Зазор 0,9 мм"),
        TemplateRule("Заміна повітряного фільтра", 30_000, 24),
        TemplateRule("Заміна паливного фільтра", 30_000, 24),
        TemplateRule("Заміна салонного фільтра", 15_000, 12),
        TemplateRule("Заміна гальмівної рідини", null, 24, hint = "DOT4"),
        TemplateRule("Заміна антифризу", 60_000, 48, hint = "G12 / G12+"),
        TemplateRule("Заміна масла МКПП", 90_000, 72),
        TemplateRule("Заміна масла АКПП (ATF)", 60_000, 48, isActive = false, hint = "Лише для автомата"),
        TemplateRule("Діагностика ходової", 15_000, 12, hint = "Огляд ходової та гальм"),
        TemplateRule("Огляд вентиляції картера і дроселя", 30_000, 24)
    )

    /** Підказка за назвою роботи (для правил, створених із шаблону). */
    fun hintFor(workName: String): String? =
        azm.firstOrNull { it.workName.equals(workName, ignoreCase = true) }?.hint
}
