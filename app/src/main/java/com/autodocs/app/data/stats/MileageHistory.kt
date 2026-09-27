package com.autodocs.app.data.stats

import com.autodocs.app.data.plan.MileagePoint
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Звідки взялась відмітка пробігу. */
enum class MileageSource { ENTRY, RECORD, CAR }

/**
 * Одна точка історії пробігу для екрана «Пробіг».
 * [refId] — id відмітки (ENTRY) або запису журналу (RECORD); для CAR — id авто.
 */
data class MileageHistoryPoint(
    val date: LocalDate,
    val mileage: Int,
    val source: MileageSource,
    val refId: Long,
    /** Не узгоджується з іншими точками (пробіг «стрибає» назад або нереально різко вгору) — ймовірно, помилка вводу. */
    val suspicious: Boolean = false,
    /** Точний момент (epoch-ms) для впорядкування відміток одного дня; за замовчуванням — початок дня. */
    val at: Long = date.toEpochDay() * MILLIS_PER_DAY
)

private const val MILLIS_PER_DAY = 86_400_000L

/**
 * Чиста логіка історії пробігу (етап 8): зведення точок з усіх джерел, пошук підозрілих
 * відміток, підсумки за період. Без Android — покрито JUnit-тестами.
 */
object MileageHistory {

    /**
     * Зводить точки з відміток, журналу й картки авто: прибирає нульові й дублікати
     * (та сама дата + той самий пробіг — лишається «найважливіше» джерело: відмітка → запис → авто),
     * сортує за датою і позначає підозрілі.
     */
    fun build(
        entries: List<MileageHistoryPoint>,
        records: List<MileageHistoryPoint>,
        car: MileageHistoryPoint?
    ): List<MileageHistoryPoint> {
        val all = (entries + records + listOfNotNull(car)).filter { it.mileage > 0 }
        val unique = all
            .groupBy { it.date to it.mileage }
            .map { (_, same) -> same.minBy { it.source.ordinal } }
        return markSuspicious(unique)
    }

    /** Більше за це за добу не проїхати — такий стрибок вважаємо помилкою вводу (зайва цифра). */
    const val MAX_KM_PER_DAY = 1_500

    /**
     * Пробіг з часом лише зростає. Найдовша неспадна послідовність (у порядку дат) вважається
     * правдою, а точки поза нею — підозрілими: так одна помилка («3 580 000» замість «358 000»)
     * позначає лише себе, а не всі наступні точки.
     */
    fun markSuspicious(points: List<MileageHistoryPoint>): List<MileageHistoryPoint> {
        val sorted = points.sortedWith(compareBy<MileageHistoryPoint> { it.at }.thenBy { it.mileage })
        val n = sorted.size
        if (n == 0) return sorted
        val len = IntArray(n) { 1 }
        val prev = IntArray(n) { -1 }
        for (i in 0 until n) {
            for (j in 0 until i) {
                // Нестрогий порядок: однакова дата з різним пробігом — обидві можуть бути правдою.
                if (sorted[j].mileage <= sorted[i].mileage && len[j] + 1 > len[i]) {
                    len[i] = len[j] + 1
                    prev[i] = j
                }
            }
        }
        // При рівній довжині беремо ланцюжок, що закінчується пізніше (свіжі дані важливіші).
        var best = 0
        for (i in 1 until n) if (len[i] >= len[best]) best = i
        val keep = BooleanArray(n)
        var k = best
        while (k >= 0) { keep[k] = true; k = prev[k] }
        // Другий прохід: нереальний стрибок угору від попередньої прийнятої точки
        // (напр. остання відмітка «3 510 000» — назад пробіг не «стрибає», тож перший прохід її не бачить).
        var last = -1
        for (i in 0 until n) {
            if (!keep[i]) continue
            if (last >= 0) {
                val days = ChronoUnit.DAYS.between(sorted[last].date, sorted[i].date).coerceAtLeast(1)
                if (sorted[i].mileage - sorted[last].mileage > MAX_KM_PER_DAY * days) { keep[i] = false; continue }
            }
            last = i
        }
        return sorted.mapIndexed { i, p -> p.copy(suspicious = !keep[i]) }
    }

    /** Точки для прогнозу — без підозрілих. */
    fun consistent(points: List<MileagePoint>): List<MileagePoint> =
        consistentOf(points.map { MileageHistoryPoint(it.date, it.mileage, MileageSource.ENTRY, 0) })

    /** Те саме, але з точним часом відміток (відмітки одного дня впорядковуються правильно). */
    fun consistentOf(points: List<MileageHistoryPoint>): List<MileagePoint> =
        markSuspicious(points.filter { it.mileage > 0 }).filterNot { it.suspicious }.map { MileagePoint(it.date, it.mileage) }

    /**
     * Скільки проїхано між датами [from]..[to] (включно) за узгодженими точками:
     * різниця між найбільшим і найменшим пробігом у проміжку. Null — точок замало.
     */
    fun drivenBetween(points: List<MileageHistoryPoint>, from: LocalDate?, to: LocalDate?): Int? {
        val inRange = points.filter {
            !it.suspicious && (from == null || !it.date.isBefore(from)) && (to == null || !it.date.isAfter(to))
        }
        if (inRange.size < 2) return null
        val km = inRange.maxOf { it.mileage } - inRange.minOf { it.mileage }
        return km.takeIf { it > 0 }
    }

    /** Проміжок у днях між першою й останньою узгодженою точкою. */
    fun spanDays(points: List<MileageHistoryPoint>): Long {
        val ok = points.filterNot { it.suspicious }
        if (ok.size < 2) return 0
        return ChronoUnit.DAYS.between(ok.first().date, ok.last().date)
    }
}
