package com.xhot.app

/**
 * Helpers for the 博主-tab creator dropdown: labels and a height cap so a long
 * list scrolls instead of running off the screen.
 */
object CreatorList {
    fun label(name: String?, handle: String?): String {
        val n = name?.trim().orEmpty()
        if (n.isNotEmpty()) return n
        return handle?.trim().orEmpty()
    }

    /**
     * Popup height in pixels. Zero when there are no rows; otherwise at most
     * [maxPx] so the menu stays on screen and can be scrolled.
     */
    fun cappedHeight(count: Int, itemPx: Int, maxPx: Int, extraPx: Int = 0): Int {
        if (count <= 0 || itemPx <= 0) return 0
        val want = count * itemPx + extraPx.coerceAtLeast(0)
        val cap = maxPx.coerceAtLeast(itemPx)
        return want.coerceAtMost(cap)
    }
}
