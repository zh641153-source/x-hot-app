package com.xhot.app

import org.junit.Assert.*
import org.junit.Test

class CreatorSortMenuStateTest {
    @Test
    fun defaultIsNewestAndClosed() {
        val s = CreatorSortMenuState()
        assertEquals(TimeSort.NEWEST, s.mode)
        assertFalse(s.isMenuOpen)
        assertEquals(0, s.selectedIndex)
        assertNull(s.openIndex)
    }

    @Test
    fun triangleTogglesMenuForSameCreator() {
        val s = CreatorSortMenuState()
        s.onTriangleClick(2)
        assertEquals(2, s.selectedIndex)
        assertEquals(2, s.openIndex)
        s.onTriangleClick(2)
        assertEquals(2, s.selectedIndex)
        assertNull(s.openIndex)
        assertEquals(TimeSort.NEWEST, s.mode)
    }

    @Test
    fun nameClickSwitchesCreatorAndClosesMenuWithoutOpening() {
        val s = CreatorSortMenuState()
        s.onTriangleClick(0)
        s.setMode(TimeSort.OLDEST)
        s.onNameClick(3)
        assertEquals(3, s.selectedIndex)
        assertNull(s.openIndex)
        assertEquals(TimeSort.OLDEST, s.mode)
        s.onNameClick(1)
        assertEquals(1, s.selectedIndex)
        assertNull(s.openIndex)
    }

    @Test
    fun triangleOnAnotherCreatorClosesPreviousAndOpensOneMenu() {
        val s = CreatorSortMenuState()
        s.onTriangleClick(1)
        s.onTriangleClick(4)
        assertEquals(4, s.selectedIndex)
        assertEquals(4, s.openIndex)
        assertNull(setOf(s.openIndex).firstOrNull { it == 1 })
    }

    @Test
    fun closingKeepsChosenSort() {
        val s = CreatorSortMenuState()
        s.onTriangleClick(0)
        s.setMode(TimeSort.OLDEST)
        s.closeMenu()
        assertEquals(TimeSort.OLDEST, s.mode)
        assertNull(s.openIndex)
        s.onTriangleClick(0)
        assertEquals(TimeSort.OLDEST, s.mode)
        assertEquals(0, s.openIndex)
    }

    @Test
    fun clampSelectedClosesInvalidMenu() {
        val s = CreatorSortMenuState()
        s.onTriangleClick(8)
        s.clampSelected(5)
        assertEquals(0, s.selectedIndex)
        assertNull(s.openIndex)
    }
}
