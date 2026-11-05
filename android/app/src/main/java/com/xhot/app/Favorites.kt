package com.xhot.app

import android.content.Context
import org.json.JSONObject

/** 本机收藏：按帖子 URL 存；刷新数据不会清掉。顺带存快照，方便收藏页展示。 */
class Favorites(ctx: Context) {
    private val sp = ctx.getSharedPreferences("xhot_favorites", Context.MODE_PRIVATE)

    fun urls(): Set<String> = sp.getStringSet("urls", emptySet())?.toSet() ?: emptySet()
    fun contains(url: String) = url in urls()

    fun toggle(url: String, meta: String, text: String, gone: Boolean, sortKey: String): Boolean {
        val s = urls().toMutableSet()
        val snaps = snaps()
        val on = if (url in s) {
            s.remove(url); snaps.remove(url); false
        } else {
            s.add(url)
            snaps.put(url, JSONObject().apply {
                put("m", meta); put("x", text); put("g", if (gone) 1 else 0); put("k", sortKey)
            })
            true
        }
        sp.edit().putStringSet("urls", s).putString("snaps", snaps.toString()).apply()
        return on
    }

    fun remember(url: String, meta: String, text: String, gone: Boolean, sortKey: String) {
        if (!contains(url)) return
        val snaps = snaps()
        snaps.put(url, JSONObject().apply {
            put("m", meta); put("x", text); put("g", if (gone) 1 else 0); put("k", sortKey)
        })
        sp.edit().putString("snaps", snaps.toString()).apply()
    }

    data class Item(val url: String, val meta: String, val text: String, val gone: Boolean, val sortKey: String)

    fun items(): List<Item> {
        val snaps = snaps()
        return urls().mapNotNull { u ->
            val o = snaps.optJSONObject(u) ?: return@mapNotNull null
            Item(u, o.optString("m"), o.optString("x"), o.optInt("g") == 1, o.optString("k"))
        }.sortedByDescending { it.sortKey }
    }

    private fun snaps(): JSONObject = try {
        JSONObject(sp.getString("snaps", "{}") ?: "{}")
    } catch (_: Exception) { JSONObject() }
}
