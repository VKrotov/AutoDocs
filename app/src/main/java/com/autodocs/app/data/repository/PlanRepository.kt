package com.autodocs.app.data.repository

import androidx.room.withTransaction
import com.autodocs.app.data.AppDatabase
import com.autodocs.app.data.entity.Car
import com.autodocs.app.data.entity.MaintenanceRule
import com.autodocs.app.data.entity.WorkItemCategory
import com.autodocs.app.data.entity.WorkType
import com.autodocs.app.data.plan.DoneMark
import com.autodocs.app.data.plan.DuePlan
import com.autodocs.app.data.plan.DueStatus
import com.autodocs.app.data.plan.MaintenanceCalculator
import com.autodocs.app.data.plan.MaintenanceTemplate
import com.autodocs.app.data.plan.MileageForecast
import com.autodocs.app.data.stats.MileageHistory
import com.autodocs.app.data.stats.MileageHistoryPoint
import com.autodocs.app.data.stats.MileageSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/** Одне правило регламенту разом із розрахунком (для екрана «План ТО»). */
data class RulePlan(
    val rule: MaintenanceRule,
    val name: String,
    val hint: String?,
    /** Остання відмітка саме з журналу (для підпису «у журналі: …»). */
    val journalMark: DoneMark?,
    val plan: DuePlan
)

data class PlanOverview(
    val car: Car,
    /** Орієнтовний пробіг на сьогодні (з урахуванням прогнозу). */
    val currentMileage: Int,
    val kmPerDay: Double?,
    val active: List<RulePlan>,
    val inactive: List<RulePlan>
) {
    val unknownCount: Int get() = active.count { it.plan.isUnknown }
    val isEmpty: Boolean get() = active.isEmpty() && inactive.isEmpty()

    /**
     * «Найближче ТО» на головній: лише пункти з відомим терміном (є відмітка, коли робили,
     * і з неї вдається порахувати залишок). Пункти «Вказати» / «?» сюди не потрапляють —
     * вони видно в «Плані ТО». Порядок: прострочені → скоро (жовті) → решта; усередині — хто раніше.
     */
    fun nearest(limit: Int = HOME_LIMIT): List<RulePlan> = active
        .filter { it.hasKnownDue }
        .sortedWith(
            compareBy<RulePlan> { it.plan.status.ordinal }
                .thenBy { it.plan.remainingDays ?: Long.MAX_VALUE }
                .thenBy { it.plan.remainingKm ?: Int.MAX_VALUE }
                .thenBy { it.name }
        )
        .take(limit)

    companion object {
        const val HOME_LIMIT = 5
    }
}

/** Термін відомий: є відмітка «коли робили» і з неї порахований залишок у км або днях. */
val RulePlan.hasKnownDue: Boolean
    get() = !plan.isUnknown && (plan.remainingKm != null || plan.remainingDays != null)

/** Базова (ручна) відмітка «коли робили востаннє» для одного правила. */
data class Baseline(val ruleId: Long, val mileage: Int?, val dateUtcMillis: Long?)

/**
 * F07/F08/F09: регламент ТО, розрахунок наступного ТО, прогноз пробігу.
 * «Остання відмітка» НЕ записується в правило при збереженні запису журналу — вона
 * рахується «на льоту» з журналу (тож видалений запис чесно повертає попередній стан);
 * у правилі зберігається лише ручна відмітка, яку користувач ввів сам.
 */
class PlanRepository(private val db: AppDatabase) {
    private val ruleDao = db.maintenanceRuleDao()
    private val workTypeDao = db.workTypeDao()
    private val recordDao = db.serviceRecordDao()
    private val mileageDao = db.mileageEntryDao()

    companion object {
        fun utcToLocalDate(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
        fun localDateToUtc(date: LocalDate): Long = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        private fun timestampToLocalDate(millis: Long): LocalDate =
            Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()

        private val statusOrder = mapOf(DueStatus.OVERDUE to 0, DueStatus.SOON to 1, DueStatus.OK to 2)
    }

    fun observePlan(car: Car, today: () -> LocalDate = { LocalDate.now() }): Flow<PlanOverview> = combine(
        ruleDao.observeForCar(car.id),
        workTypeDao.observeAll(),
        recordDao.observeWithItemsForCar(car.id),
        mileageDao.observeForCar(car.id)
    ) { rules, workTypes, records, mileage ->
        val now = today()
        val names = workTypes.associate { it.id to it.name }

        // F09: точки пробігу з усіх джерел (з точним часом — щоб відмітки одного дня йшли по порядку).
        val points = buildList {
            mileage.forEach { add(MileageHistoryPoint(timestampToLocalDate(it.date), it.mileage, MileageSource.ENTRY, it.id, at = it.date)) }
            records.forEach { add(MileageHistoryPoint(utcToLocalDate(it.record.date), it.record.mileage, MileageSource.RECORD, it.record.id, at = it.record.date)) }
            if (car.mileage > 0) add(MileageHistoryPoint(timestampToLocalDate(car.mileageUpdatedAt), car.mileage, MileageSource.CAR, car.id, at = car.mileageUpdatedAt))
        }
        // Помилкові відмітки (пробіг «стрибає» назад чи нереально вгору) не псують прогноз.
        val rate = MileageForecast.kmPerDay(MileageHistory.consistentOf(points), now)
        val current = MileageForecast.estimateCurrent(car.mileage, timestampToLocalDate(car.mileageUpdatedAt), rate, now)

        val plans = rules.map { rule ->
            val name = names[rule.workTypeId] ?: "Без назви"
            val journal = records.flatMap { r ->
                val matches = r.items.any { item ->
                    item.workTypeId == rule.workTypeId ||
                        (item.workTypeId == null && item.customName?.trim().equals(name, ignoreCase = true))
                }
                if (matches) listOf(DoneMark(utcToLocalDate(r.record.date), r.record.mileage, fromJournal = true)) else emptyList()
            }
            val manual = if (rule.lastDoneMileage != null || rule.lastDoneDate != null) {
                DoneMark(rule.lastDoneDate?.let(::utcToLocalDate), rule.lastDoneMileage, fromJournal = false)
            } else null
            val journalMark = MaintenanceCalculator.latestMark(null, journal)
            val last = MaintenanceCalculator.latestMark(manual, journal)
            RulePlan(
                rule = rule,
                name = name,
                hint = MaintenanceTemplate.hintFor(name),
                journalMark = journalMark,
                plan = MaintenanceCalculator.compute(rule.intervalKm, rule.intervalMonths, last, current, rate, now)
            )
        }
        val sorted = plans.sortedWith(
            compareBy<RulePlan> { statusOrder[it.plan.status] }
                .thenBy { if (it.plan.isUnknown) 1 else 0 }
                .thenBy { it.plan.remainingDays ?: Long.MAX_VALUE }
                .thenBy { it.plan.remainingKm ?: Int.MAX_VALUE }
                .thenBy { it.name }
        )
        PlanOverview(
            car = car,
            currentMileage = current,
            kmPerDay = rate,
            active = sorted.filter { it.rule.isActive },
            inactive = sorted.filterNot { it.rule.isActive }
        )
    }

    /** Знайти пункт довідника («Робота») за назвою або створити його. */
    suspend fun ensureWorkType(name: String): Long {
        val trimmed = name.trim()
        workTypeDao.findByName(WorkItemCategory.ROBOTA, trimmed)?.let { return it.id }
        val id = workTypeDao.insert(WorkType(name = trimmed, category = WorkItemCategory.ROBOTA, isCustom = true))
        return if (id > 0) id else workTypeDao.findByName(WorkItemCategory.ROBOTA, trimmed)!!.id
    }

    /** Заповнити стандартний регламент (AZM). Правила, які вже є для цього авто, не дублюються. */
    suspend fun seedTemplate(carId: Long): Int = db.withTransaction {
        val existing = ruleDao.getForCar(carId).map { it.workTypeId }.toSet()
        var added = 0
        MaintenanceTemplate.azm.forEach { t ->
            val workTypeId = ensureWorkType(t.workName)
            if (workTypeId !in existing) {
                ruleDao.insert(
                    MaintenanceRule(
                        carId = carId,
                        workTypeId = workTypeId,
                        intervalKm = t.intervalKm,
                        intervalMonths = t.intervalMonths,
                        isActive = t.isActive
                    )
                )
                added++
            }
        }
        added
    }

    suspend fun getRule(id: Long): MaintenanceRule? = ruleDao.getById(id)

    suspend fun workTypeName(id: Long): String? = workTypeDao.getById(id)?.name

    /** Зберегти нове правило (id == 0) або оновити існуюче. Повертає id. */
    suspend fun saveRule(rule: MaintenanceRule): Long =
        if (rule.id == 0L) ruleDao.insert(rule) else { ruleDao.update(rule); rule.id }

    suspend fun deleteRule(id: Long) = ruleDao.deleteById(id)

    suspend fun saveBaselines(baselines: List<Baseline>) = db.withTransaction {
        baselines.forEach { b ->
            val rule = ruleDao.getById(b.ruleId) ?: return@forEach
            ruleDao.update(rule.copy(lastDoneMileage = b.mileage, lastDoneDate = b.dateUtcMillis))
        }
    }

    suspend fun countRulesForWorkType(workTypeId: Long): Int = ruleDao.countForWorkType(workTypeId)
}
