package com.xhot.app

/**
 * Sort creator posts by original publish date (`t`), without mutating the input list.
 */
enum class TimeSort { NEWEST, OLDEST }

object PostTimeSort {
    private val DATE = Regex("""(\d{4})-(\d{2})-(\d{2})[ T](\d{2}):(\d{2})(?::(\d{2}))?""")

    /** Epoch millis, or null when the date cannot be parsed. */
    fun parseMillis(raw: String?): Long? {
        val s = raw?.trim().orEmpty()
        if (s.isEmpty()) return null
        val m = DATE.find(s) ?: return null
        val y = m.groupValues[1].toInt()
        val mo = m.groupValues[2].toInt()
        val d = m.groupValues[3].toInt()
        val h = m.groupValues[4].toInt()
        val mi = m.groupValues[5].toInt()
        val sec = m.groupValues[6].ifEmpty { "0" }.toInt()
        if (mo !in 1..12 || d !in 1..31 || h !in 0..23 || mi !in 0..59 || sec !in 0..59) return null
        val cal = java.util.Calendar.getInstance()
        cal.isLenient = false
        return try {
            cal.set(java.util.Calendar.MILLISECOND, 0)
            cal.set(y, mo - 1, d, h, mi, sec)
            cal.timeInMillis
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Returns a new list. [NEWEST] is latest publish date first;
     * [OLDEST] is earliest first. Unparseable dates go last in both modes.
     */
    fun <T> sorted(items: List<T>, mode: TimeSort, dateOf: (T) -> String?): List<T> {
        val keyed = items.mapIndexed { idx, item -> Triple(item, parseMillis(dateOf(item)), idx) }
        val cmp = compareBy<Triple<T, Long?, Int>> { if (it.second == null) 1 else 0 }
            .thenBy { if (mode == TimeSort.NEWEST) -(it.second ?: 0L) else (it.second ?: 0L) }
            .thenBy { it.third }
        return keyed.sortedWith(cmp).map { it.first }
    }
}
