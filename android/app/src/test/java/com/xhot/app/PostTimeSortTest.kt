package com.xhot.app

import org.junit.Assert.*
import org.junit.Test

class PostTimeSortTest {
    private data class P(val id: String, val t: String, val k: String = "")

    private val sample = listOf(
        P("a", "2026-11-12 23:56", "2026-11-14 09:39:39"),
        P("b", "2026-11-11 19:06", "2026-11-12 19:17:05"),
        P("c", "2022-12-16 12:43", "2026-11-14 09:39:39"),
        P("d", "2026-11-12 23:56", "2026-01-01 00:00:00")
    )

    @Test
    fun newestPutsLatestPublishDateFirst() {
        val out = PostTimeSort.sorted(sample, TimeSort.NEWEST) { it.t }
        assertEquals(listOf("a", "d", "b", "c"), out.map { it.id })
    }

    @Test
    fun oldestPutsEarliestPublishDateFirst() {
        val out = PostTimeSort.sorted(sample, TimeSort.OLDEST) { it.t }
        assertEquals(listOf("c", "b", "a", "d"), out.map { it.id })
    }

    @Test
    fun sortsByPublishDateNotAddedAt() {
        val items = listOf(
            P("old_but_just_added", "2020-01-01 00:00", "2026-11-14 09:39:39"),
            P("new_but_added_earlier", "2026-11-12 23:56", "2026-01-01 00:00:00")
        )
        val byTime = PostTimeSort.sorted(items, TimeSort.NEWEST) { it.t }.map { it.id }
        val byAdded = items.sortedByDescending { it.k }.map { it.id }
        assertEquals(listOf("new_but_added_earlier", "old_but_just_added"), byTime)
        assertEquals(listOf("old_but_just_added", "new_but_added_earlier"), byAdded)
        assertNotEquals(byAdded, byTime)
    }

    @Test
    fun doesNotMutateOriginalList() {
        val orig = sample.toList()
        val snapshot = orig.map { it.copy() }
        val out = PostTimeSort.sorted(orig, TimeSort.OLDEST) { it.t }
        assertEquals(snapshot, orig)
        assertNotEquals(orig.map { it.id }, out.map { it.id })
        assertSame(orig[0], orig[0])
    }

    @Test
    fun parseCommonPublishFormats() {
        val a = PostTimeSort.parseMillis("2026-11-12 23:56")
        val b = PostTimeSort.parseMillis("2026-11-12 23:56:00")
        val c = PostTimeSort.parseMillis("2026-11-12T23:56:00")
        assertNotNull(a)
        assertEquals(a, b)
        assertEquals(a, c)
        assertTrue(PostTimeSort.parseMillis("2026-11-12 23:57")!! > a!!)
        assertTrue(PostTimeSort.parseMillis("2022-12-16 12:43")!! < a)
    }

    @Test
    fun parseRejectsEmptyAndInvalid() {
        assertNull(PostTimeSort.parseMillis(""))
        assertNull(PostTimeSort.parseMillis(null))
        assertNull(PostTimeSort.parseMillis("n/a"))
        assertNull(PostTimeSort.parseMillis("2026-13-40 99:99"))
    }

    @Test
    fun unparseableDatesGoLastInBothModes() {
        val items = listOf(P("ok", "2024-01-02 00:00"), P("bad", "nope"), P("old", "2020-01-01 00:00"))
        assertEquals(listOf("ok", "old", "bad"), PostTimeSort.sorted(items, TimeSort.NEWEST) { it.t }.map { it.id })
        assertEquals(listOf("old", "ok", "bad"), PostTimeSort.sorted(items, TimeSort.OLDEST) { it.t }.map { it.id })
    }

    @Test
    fun sameTimestampKeepsStableOrder() {
        val items = listOf(P("1", "2024-05-01 10:00"), P("2", "2024-05-01 10:00"), P("3", "2024-05-01 10:00"))
        assertEquals(listOf("1", "2", "3"), PostTimeSort.sorted(items, TimeSort.NEWEST) { it.t }.map { it.id })
        assertEquals(listOf("1", "2", "3"), PostTimeSort.sorted(items, TimeSort.OLDEST) { it.t }.map { it.id })
    }
}
