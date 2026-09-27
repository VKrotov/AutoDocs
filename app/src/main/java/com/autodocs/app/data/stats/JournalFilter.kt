package com.autodocs.app.data.stats

import com.autodocs.app.data.entity.RecordWithItems
import com.autodocs.app.data.entity.displayName

/** Пошук і фільтри журналу (етап 8). */
data class JournalFilter(val query: String = "", val year: Int? = null) {
    val isActive: Boolean get() = query.isNotBlank() || year != null

    /**
     * Слова запиту (без регістру). Запис підходить, якщо містить УСІ слова.
     * Кінцеву голосну відкидаємо («масло» знайде й «масла», «свічки» — «свічка»).
     */
    private val terms: List<String> = query.trim().lowercase().split(Regex("\\s+"))
        .filter { it.isNotEmpty() }
        .map(::stem)

    fun matches(r: RecordWithItems): Boolean {
        if (year != null && ExpenseStats.recordDate(r).year != year) return false
        if (terms.isEmpty()) return true
        val haystack = searchText(r)
        return terms.all { haystack.contains(it) }
    }

    /** Назви позицій запису, що містять хоча б одне слово запиту — щоб показати їх першими. */
    fun matchedItemNames(r: RecordWithItems): List<String> {
        if (terms.isEmpty()) return emptyList()
        return r.items.map { it.displayName() }.filter { name -> terms.any { name.lowercase().contains(it) } }
    }

    fun apply(records: List<RecordWithItems>): List<RecordWithItems> =
        if (!isActive) records else records.filter(::matches)

    companion object {
        private const val ENDINGS = "аоеєиіїуюяйь"

        fun stem(word: String): String {
            val cut = word.trimEnd { it in ENDINGS }
            return if (cut.length >= 3) cut else word
        }

        fun searchText(r: RecordWithItems): String = buildString {
            r.items.forEach { append(it.displayName()).append('\n') }
            r.record.stoName?.let { append(it).append('\n') }
            r.record.notes?.let { append(it).append('\n') }
        }.lowercase()
    }
}
