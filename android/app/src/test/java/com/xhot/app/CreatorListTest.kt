package com.xhot.app

import org.junit.Assert.*
import org.junit.Test

class CreatorListTest {
    @Test
    fun labelPrefersDisplayName() {
        assertEquals("小宇妈妈", CreatorList.label("小宇妈妈", "xiaoyumama9968"))
        assertEquals("xiaoyumama9968", CreatorList.label("", "xiaoyumama9968"))
        assertEquals("handle", CreatorList.label("  ", "handle"))
        assertEquals("", CreatorList.label("", ""))
        assertEquals("", CreatorList.label(null, null))
    }

    @Test
    fun heightFitsSmallListExactly() {
        assertEquals(48 * 3, CreatorList.cappedHeight(3, 48, 1000))
        assertEquals(48 * 3 + 8, CreatorList.cappedHeight(3, 48, 1000, 8))
    }

    @Test
    fun heightCapsLongListSoItCanScroll() {
        val max = 500
        val h = CreatorList.cappedHeight(55, 48, max)
        assertEquals(max, h)
        assertTrue(h < 55 * 48)
        assertTrue(h >= 48)
    }

    @Test
    fun heightZeroWhenEmptyOrInvalid() {
        assertEquals(0, CreatorList.cappedHeight(0, 48, 500))
        assertEquals(0, CreatorList.cappedHeight(10, 0, 500))
        assertEquals(0, CreatorList.cappedHeight(-1, 48, 500))
    }
}
