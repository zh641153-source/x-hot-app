package com.xhot.app

/**
 * Sort-menu state for the 博主 chip row.
 * The triangle toggles the dropdown; the name only switches the creator and closes any open menu.
 * At most one menu is open. Closing it keeps the current [mode].
 */
class CreatorSortMenuState {
    var mode: TimeSort = TimeSort.NEWEST
        private set
    var selectedIndex: Int = 0
        private set
    var openIndex: Int? = null
        private set

    val isMenuOpen: Boolean get() = openIndex != null

    fun onNameClick(index: Int) {
        selectedIndex = index
        openIndex = null
    }

    fun onTriangleClick(index: Int) {
        selectedIndex = index
        openIndex = if (openIndex == index) null else index
    }

    fun setMode(newMode: TimeSort) {
        mode = newMode
    }

    fun closeMenu() {
        openIndex = null
    }

    fun clampSelected(size: Int) {
        if (size <= 0) {
            selectedIndex = 0
            openIndex = null
            return
        }
        if (selectedIndex >= size) {
            selectedIndex = 0
            openIndex = null
        } else if (openIndex != null && openIndex!! >= size) {
            openIndex = null
        }
    }
}
