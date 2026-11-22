package com.xhot.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextUtils
import android.text.TextWatcher
import android.text.style.BackgroundColorSpan
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.os.SystemClock
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.ListPopupWindow
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.tabs.TabLayout
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

sealed class Row {
    data class Header(val text: String) : Row()
    data class Post(
        val meta: String,
        val text: String,
        val url: String,
        val gone: Boolean = false,
        val sortKey: String = ""
    ) : Row()
}

class MainActivity : AppCompatActivity() {
    private val urls = listOf(
        "https://api.github.com/repos/zh641153-source/x-hot-app/contents/data.json",
        "https://raw.githubusercontent.com/zh641153-source/x-hot-app/main/data.json",
        "https://zh641153-source.github.io/x-hot-app/data.json")
    private var data: JSONObject? = null
    private var tab = 0 // 0博主 1热贴 2收藏
    private var creatorIdx = 0
    private var searchQuery = ""
    private val sortMenu = CreatorSortMenuState()
    private var sortPopup: ListPopupWindow? = null
    private var lastPopupDismissAt = 0L
    private lateinit var status: TextView
    private lateinit var chipScroll: HorizontalScrollView
    private lateinit var chips: ChipGroup
    private lateinit var tabs: TabLayout
    private lateinit var swipe: SwipeRefreshLayout
    private lateinit var etSearch: EditText
    private lateinit var searchBar: LinearLayout
    private lateinit var rv: RecyclerView
    private lateinit var favs: Favorites
    private val adapter = Adapter()
    private val cache by lazy { File(filesDir, "data.json") }

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        favs = Favorites(this)
        val dp = resources.displayMetrics.density
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F5F5F5"))
        }

        val titleRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding((16 * dp).toInt(), (10 * dp).toInt(), (8 * dp).toInt(), (4 * dp).toInt())
        }
        titleRow.addView(TextView(this).apply {
            text = "X热帖"
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        })
        titleRow.addView(ImageButton(this).apply {
            setImageResource(R.drawable.ic_search)
            background = null
            contentDescription = "搜索"
            setPadding((10 * dp).toInt(), (8 * dp).toInt(), (10 * dp).toInt(), (8 * dp).toInt())
            setOnClickListener { if (searchBar.visibility == View.VISIBLE) closeSearch() else openSearch() }
        })
        root.addView(titleRow)

        status = TextView(this).apply {
            textSize = 12f
            setTextColor(Color.GRAY)
            setPadding((16 * dp).toInt(), 0, (16 * dp).toInt(), (4 * dp).toInt())
        }
        root.addView(status)

        searchBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            visibility = View.GONE
            setPadding((12 * dp).toInt(), (4 * dp).toInt(), (12 * dp).toInt(), (8 * dp).toInt())
            setBackgroundColor(Color.WHITE)
        }
        etSearch = EditText(this).apply {
            hint = "搜索已采集的推文关键字"
            textSize = 15f
            maxLines = 1
            isSingleLine = true
            imeOptions = EditorInfo.IME_ACTION_SEARCH
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            setBackgroundColor(Color.parseColor("#F1F3F4"))
            setPadding((12 * dp).toInt(), (10 * dp).toInt(), (12 * dp).toInt(), (10 * dp).toInt())
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
                override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    searchQuery = s?.toString()?.trim().orEmpty()
                    adapter.highlight = searchQuery
                    render()
                    if (searchQuery.isNotEmpty()) rv.scrollToPosition(0)
                }
            })
            setOnEditorActionListener { _, action, _ ->
                if (action == EditorInfo.IME_ACTION_SEARCH) { hideKeyboard(); true } else false
            }
        }
        searchBar.addView(etSearch)
        searchBar.addView(TextView(this).apply {
            text = "取消"
            textSize = 14f
            setTextColor(Color.parseColor("#1D9BF0"))
            setPadding((12 * dp).toInt(), (8 * dp).toInt(), (4 * dp).toInt(), (8 * dp).toInt())
            setOnClickListener { closeSearch() }
        })
        root.addView(searchBar)

        tabs = TabLayout(this)
        tabs.addTab(tabs.newTab().setText("博主"))
        tabs.addTab(tabs.newTab().setText("热贴"))
        tabs.addTab(tabs.newTab().setText("收藏"))
        root.addView(tabs)

        chips = ChipGroup(this).apply {
            isSingleSelection = true
            isSelectionRequired = true
            isSingleLine = true
            setPadding((8 * dp).toInt(), 0, (8 * dp).toInt(), 0)
        }
        chipScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(chips)
        }
        root.addView(chipScroll)

        rv = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
        }
        swipe = SwipeRefreshLayout(this).apply {
            addView(rv)
            setOnRefreshListener { fetch() }
        }
        root.addView(swipe, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)

        tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(t: TabLayout.Tab) {
                if (searchQuery.isNotEmpty()) return
                tab = t.position
                if (tab != 0) dismissSortPopup()
                render()
                rv.scrollToPosition(0)
            }
            override fun onTabUnselected(t: TabLayout.Tab) {}
            override fun onTabReselected(t: TabLayout.Tab) { rv.scrollToPosition(0) }
        })
        loadLocal(); render(); swipe.isRefreshing = true; fetch()
    }

    private fun openSearch() {
        dismissSortPopup()
        searchBar.visibility = View.VISIBLE
        etSearch.requestFocus()
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .showSoftInput(etSearch, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun closeSearch() {
        etSearch.setText("")
        searchQuery = ""
        adapter.highlight = ""
        searchBar.visibility = View.GONE
        hideKeyboard()
        render()
    }

    private fun hideKeyboard() {
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(etSearch.windowToken, 0)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (sortPopup?.isShowing == true) { dismissSortPopup(); return }
        if (searchBar.visibility == View.VISIBLE) closeSearch() else super.onBackPressed()
    }

    override fun onDestroy() {
        dismissSortPopup()
        super.onDestroy()
    }

    private fun loadLocal() {
        data = try {
            JSONObject(
                if (cache.exists()) cache.readText()
                else assets.open("data.json").bufferedReader().readText()
            )
        } catch (e: Exception) { null }
    }

    private fun fetch() = thread {
        var ok = false
        for (u in urls) {
            try {
                val c = URL(u + "?t=" + System.currentTimeMillis()).openConnection() as HttpURLConnection
                c.connectTimeout = 10000; c.readTimeout = 20000; c.useCaches = false
                c.setRequestProperty("Cache-Control", "no-cache"); c.setRequestProperty("Pragma", "no-cache")
                if (u.contains("api.github.com")) c.setRequestProperty("Accept", "application/vnd.github.raw")
                val txt = c.inputStream.bufferedReader().readText()
                JSONObject(txt); cache.writeText(txt); ok = true; break
            } catch (e: Exception) {}
        }
        runOnUiThread {
            swipe.isRefreshing = false
            if (ok) {
                val old = data?.optString("built"); loadLocal(); render()
                Toast.makeText(
                    this,
                    if (old == data?.optString("built")) "已是最新" else "已更新到 " + data?.optString("built"),
                    Toast.LENGTH_SHORT
                ).show()
            } else Toast.makeText(this, "网络不可用，显示离线缓存", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fmt(v: Long) = if (v >= 10000) String.format("%.1f万", v / 10000.0) else v.toString()

    private data class Hit(
        val meta: String, val text: String, val url: String, val gone: Boolean, val sortKey: String
    )

    private fun searchAll(d: JSONObject, q: String): List<Hit> {
        val key = q.trim()
        if (key.isEmpty()) return emptyList()
        val seen = LinkedHashSet<String>()
        val out = mutableListOf<Hit>()
        fun match(vararg fields: String) = fields.any { it.contains(key, ignoreCase = true) }

        val cs = d.optJSONArray("creators") ?: return emptyList()
        for (i in 0 until cs.length()) {
            val c = cs.getJSONObject(i)
            val name = c.optString("n"); val handle = c.optString("u")
            val ps = c.getJSONArray("posts")
            for (j in 0 until ps.length()) {
                val p = ps.getJSONObject(j)
                val text = p.optString("x")
                if (!match(text, name, handle, "@$handle")) continue
                val url = p.optString("u")
                if (!seen.add(url)) continue
                out.add(Hit(
                    "👁 ${fmt(p.optLong("v"))}   ❤ ${fmt(p.optLong("l"))}   ${p.optString("t")}   $name @$handle",
                    text.ifEmpty { "（纯图或视频）" }, url, p.optInt("g") == 1,
                    p.optString("k").ifEmpty { p.optString("t") }
                ))
            }
        }
        val ds = d.optJSONArray("days")
        if (ds != null) for (i in 0 until ds.length()) {
            val day = ds.getJSONObject(i)
            val ps = day.getJSONArray("posts")
            for (j in 0 until ps.length()) {
                val p = ps.getJSONObject(j)
                val text = p.optString("x"); val author = p.optString("a")
                if (!match(text, author)) continue
                val url = p.optString("u")
                if (!seen.add(url)) continue
                out.add(Hit(
                    "👁 ${fmt(p.optLong("v"))}   ${p.optString("t")}   $author · 热贴",
                    text.ifEmpty { "（纯图或视频）" }, url, p.optInt("g") == 1,
                    p.optString("k").ifEmpty { p.optString("t") }
                ))
            }
        }
        return out.sortedByDescending { it.sortKey }
    }

    private fun makeCreatorChip(index: Int, label: String): Chip {
        val dp = resources.displayMetrics.density
        return Chip(this).apply {
            text = label
            isCheckable = true
            id = View.generateViewId()
            isChecked = index == creatorIdx
            isCloseIconVisible = true
            setCloseIconResource(R.drawable.ic_sort_tri)
            closeIconContentDescription = "时间排序"
            closeIconSize = 16 * dp
            setOnClickListener {
                val switched = index != creatorIdx
                sortMenu.onNameClick(index)
                creatorIdx = sortMenu.selectedIndex
                dismissSortPopup()
                render()
                if (switched) rv.scrollToPosition(0)
            }
            setOnCloseIconClickListener { onCreatorTriangleClick(this, index) }
        }
    }

    private fun onCreatorTriangleClick(anchor: Chip, index: Int) {
        if (sortPopup?.isShowing == true && sortMenu.openIndex == index) {
            dismissSortPopup()
            return
        }
        if (sortPopup?.isShowing == true) dismissSortPopup()
        if (SystemClock.uptimeMillis() - lastPopupDismissAt < 160) return
        val switched = index != creatorIdx
        sortMenu.onTriangleClick(index)
        creatorIdx = sortMenu.selectedIndex
        if (sortMenu.openIndex == index) showSortPopup(anchor) else dismissSortPopup()
        render()
        if (switched) rv.scrollToPosition(0)
    }

    private fun syncCreatorChips() {
        for (i in 0 until chips.childCount) {
            val chip = chips.getChildAt(i) as? Chip ?: continue
            chip.isChecked = i == creatorIdx
            val open = sortMenu.openIndex == i
            chip.setCloseIconResource(if (open) R.drawable.ic_sort_tri_up else R.drawable.ic_sort_tri)
        }
    }

    private fun showSortPopup(anchor: View) {
        dismissSortPopup(keepState = true)
        val labels = arrayOf("最新", "最早")
        val popup = ListPopupWindow(this)
        popup.anchorView = anchor
        popup.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_single_choice, labels))
        popup.isModal = true
        popup.setContentWidth((160 * resources.displayMetrics.density).toInt())
        popup.setOnItemClickListener { _, _, pos, _ ->
            val mode = if (pos == 0) TimeSort.NEWEST else TimeSort.OLDEST
            val changed = mode != sortMenu.mode
            sortMenu.setMode(mode)
            dismissSortPopup()
            render()
            if (changed) rv.scrollToPosition(0)
        }
        popup.setOnDismissListener {
            lastPopupDismissAt = SystemClock.uptimeMillis()
            sortMenu.closeMenu()
            sortPopup = null
            syncCreatorChips()
        }
        sortPopup = popup
        popup.show()
        popup.listView?.choiceMode = ListView.CHOICE_MODE_SINGLE
        popup.listView?.setItemChecked(if (sortMenu.mode == TimeSort.NEWEST) 0 else 1, true)
    }

    private fun dismissSortPopup(keepState: Boolean = false) {
        val popup = sortPopup ?: return
        popup.setOnDismissListener(null)
        if (popup.isShowing) popup.dismiss()
        sortPopup = null
        if (!keepState) sortMenu.closeMenu()
        syncCreatorChips()
    }

    private fun rememberIfFav(p: Row.Post) {
        if (favs.contains(p.url)) favs.remember(p.url, p.meta, p.text, p.gone, p.sortKey)
    }

    private fun render() {
        val d = data
        if (d == null && tab != 2) {
            status.text = "暂无数据，下拉刷新"
            adapter.set(emptyList())
            return
        }
        status.text = if (d != null)
            "数据更新于 " + d.optString("built") + "（北京时间）· 下拉刷新"
        else "暂无数据，下拉刷新"
        val rows = mutableListOf<Row>()
        val searching = searchQuery.isNotEmpty()

        if (searching && d != null) {
            dismissSortPopup()
            chipScroll.visibility = View.GONE
            tabs.visibility = View.GONE
            val hits = searchAll(d, searchQuery)
            rows.add(Row.Header("搜索「$searchQuery」· 共 ${hits.size} 条（博主+热贴）"))
            if (hits.isEmpty()) rows.add(Row.Header("没有搜到包含该关键字的推文"))
            else for (h in hits) {
                val post = Row.Post(h.meta, h.text, h.url, h.gone, h.sortKey)
                rememberIfFav(post); rows.add(post)
            }
        } else {
            tabs.visibility = View.VISIBLE
            when (tab) {
                0 -> {
                    if (d == null) { adapter.set(emptyList()); return }
                    chipScroll.visibility = View.VISIBLE
                    val cs = d.getJSONArray("creators")
                    sortMenu.clampSelected(cs.length())
                    creatorIdx = sortMenu.selectedIndex
                    if (chips.childCount != cs.length()) {
                        chips.removeAllViews()
                        for (i in 0 until cs.length()) {
                            val c = cs.getJSONObject(i)
                            chips.addView(makeCreatorChip(i, c.optString("n").ifEmpty { c.optString("u") } + " " + c.getJSONArray("posts").length()))
                        }
                    }
                    syncCreatorChips()
                    if (cs.length() > 0) {
                        val c = cs.getJSONObject(creatorIdx)
                        rows.add(Row.Header("${c.optString("n")} @${c.optString("u")} · 粉丝 ${fmt(c.optLong("f"))} · ${c.getJSONArray("posts").length()} 条 ≥10万"))
                        val ps = c.getJSONArray("posts")
                        val raw = (0 until ps.length()).map { ps.getJSONObject(it) }
                        val list = PostTimeSort.sorted(raw, sortMenu.mode) { it.optString("t") }
                        for (p in list) {
                            val post = Row.Post(
                                "👁 ${fmt(p.optLong("v"))}   ❤ ${fmt(p.optLong("l"))}   ${p.optString("t")}",
                                p.optString("x").ifEmpty { "（纯图或视频）" },
                                p.optString("u"), p.optInt("g") == 1,
                                p.optString("k").ifEmpty { p.optString("t") }
                            )
                            rememberIfFav(post); rows.add(post)
                        }
                    }
                }
                1 -> {
                    if (d == null) { adapter.set(emptyList()); return }
                    dismissSortPopup()
                    chipScroll.visibility = View.GONE
                    val ds = d.getJSONArray("days")
                    val list = (0 until ds.length()).map { ds.getJSONObject(it) }.sortedByDescending { it.optString("d") }
                    for (day in list) {
                        val ps0 = day.getJSONArray("posts")
                        val ps = org.json.JSONArray(
                            (0 until ps0.length()).map { ps0.getJSONObject(it) }.sortedByDescending { it.optString("k") }
                        )
                        rows.add(Row.Header("${day.optString("d")} · ${ps.length()} 条"))
                        for (i in 0 until ps.length()) {
                            val p = ps.getJSONObject(i)
                            val post = Row.Post(
                                "👁 ${fmt(p.optLong("v"))}   ${p.optString("t")}   ${p.optString("a")}",
                                p.optString("x").ifEmpty { "（纯图或视频）" },
                                p.optString("u"), p.optInt("g") == 1,
                                p.optString("k").ifEmpty { p.optString("t") }
                            )
                            rememberIfFav(post); rows.add(post)
                        }
                    }
                }
                else -> {
                    dismissSortPopup()
                    chipScroll.visibility = View.GONE
                    val items = favs.items()
                    rows.add(Row.Header("收藏 · ${items.size} 条"))
                    if (items.isEmpty()) rows.add(Row.Header("还没有收藏，去博主/热贴里点「收藏」吧"))
                    else for (it in items) rows.add(Row.Post(it.meta, it.text.ifEmpty { "（纯图或视频）" }, it.url, it.gone, it.sortKey))
                }
            }
        }
        adapter.set(rows)
    }

    private val expanded = mutableSetOf<String>()
    private fun copy(t: String) {
        (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
            .setPrimaryClip(ClipData.newPlainText("文案", t))
        Toast.makeText(this, "已复制全文（${t.length} 字）", Toast.LENGTH_SHORT).show()
    }

    private fun open(url: String) {
        val u = Uri.parse(url)
        try { startActivity(Intent(Intent.ACTION_VIEW, u).setPackage("com.twitter.android")) }
        catch (e: Exception) { try { startActivity(Intent(Intent.ACTION_VIEW, u)) } catch (_: Exception) {} }
    }

    private fun highlight(text: String, q: String): CharSequence {
        val k = q.trim()
        if (k.isEmpty()) return text
        val sp = SpannableString(text)
        var i = text.indexOf(k, 0, ignoreCase = true)
        while (i >= 0) {
            sp.setSpan(BackgroundColorSpan(0xFFFFEB3B.toInt()), i, i + k.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            i = text.indexOf(k, i + k.length, ignoreCase = true)
        }
        return sp
    }

    private fun styleFav(btn: TextView, on: Boolean) {
        if (on) {
            btn.text = "★ 已收藏"
            btn.setTextColor(Color.WHITE)
            btn.setBackgroundColor(Color.parseColor("#F5A623"))
        } else {
            btn.text = "☆ 收藏"
            btn.setTextColor(Color.parseColor("#667788"))
            btn.setBackgroundColor(Color.parseColor("#EEF1F4"))
        }
    }

    inner class Adapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        private var rows: List<Row> = emptyList()
        var highlight: String = ""
        fun set(r: List<Row>) { rows = r; notifyDataSetChanged() }
        override fun getItemCount() = rows.size
        override fun getItemViewType(p: Int) = if (rows[p] is Row.Header) 0 else 1
        override fun onCreateViewHolder(parent: ViewGroup, t: Int): RecyclerView.ViewHolder {
            val dp = resources.displayMetrics.density
            val v: View = if (t == 0) TextView(parent.context).apply {
                textSize = 15f; setTypeface(null, Typeface.BOLD)
                setPadding((16 * dp).toInt(), (12 * dp).toInt(), (16 * dp).toInt(), (6 * dp).toInt())
            } else LinearLayout(parent.context).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(Color.WHITE)
                setPadding((14 * dp).toInt(), (10 * dp).toInt(), (14 * dp).toInt(), (10 * dp).toInt())
                // top: meta + 收藏
                addView(LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.TOP
                    addView(TextView(context).apply {
                        textSize = 12f; setTextColor(Color.parseColor("#E0452B"))
                        layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
                    })
                    addView(TextView(context).apply {
                        textSize = 12f
                        setPadding((10 * dp).toInt(), (4 * dp).toInt(), (10 * dp).toInt(), (4 * dp).toInt())
                    })
                })
                addView(TextView(context).apply {
                    textSize = 14f; setTextColor(Color.BLACK)
                    setPadding(0, (4 * dp).toInt(), 0, (4 * dp).toInt())
                })
                addView(TextView(context).apply { textSize = 12f; setTextColor(Color.GRAY) })
                addView(LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL; gravity = Gravity.END
                    addView(Button(context).apply {
                        text = "复制"; textSize = 13f; setTextColor(Color.WHITE)
                        setBackgroundColor(Color.parseColor("#1D9BF0"))
                        minHeight = 0; minimumHeight = 0
                        setPadding((14 * dp).toInt(), (4 * dp).toInt(), (14 * dp).toInt(), (4 * dp).toInt())
                    })
                    addView(TextView(context).apply {
                        text = "打开原帖 ›"; textSize = 13f; setTextColor(Color.parseColor("#1D9BF0"))
                        setPadding((16 * dp).toInt(), (8 * dp).toInt(), 0, (8 * dp).toInt())
                    })
                })
            }
            v.layoutParams = RecyclerView.LayoutParams(-1, -2).apply {
                if (t == 1) setMargins((8 * dp).toInt(), (3 * dp).toInt(), (8 * dp).toInt(), (3 * dp).toInt())
            }
            return object : RecyclerView.ViewHolder(v) {}
        }
        override fun onBindViewHolder(h: RecyclerView.ViewHolder, p: Int) {
            when (val r = rows[p]) {
                is Row.Header -> (h.itemView as TextView).text = r.text
                is Row.Post -> {
                    val l = h.itemView as LinearLayout
                    val top = l.getChildAt(0) as LinearLayout
                    val meta = top.getChildAt(0) as TextView
                    val favBtn = top.getChildAt(1) as TextView
                    meta.text = if (r.gone) r.meta + "   ⚠ 原帖已删/封号" else r.meta
                    styleFav(favBtn, favs.contains(r.url))
                    favBtn.setOnClickListener {
                        val on = favs.toggle(r.url, r.meta, r.text, r.gone, r.sortKey)
                        styleFav(favBtn, on)
                        Toast.makeText(this@MainActivity, if (on) "已收藏" else "已取消收藏", Toast.LENGTH_SHORT).show()
                        if (tab == 2 && searchQuery.isEmpty()) render()
                    }
                    val tv = l.getChildAt(1) as TextView
                    val hint = l.getChildAt(2) as TextView
                    val bar = l.getChildAt(3) as LinearLayout
                    tv.text = highlight(r.text, highlight)
                    val long = r.text.length > 180 || r.text.count { it == '\n' } > 7
                    fun apply() {
                        val ex = expanded.contains(r.url)
                        if (!long || ex) { tv.maxLines = Int.MAX_VALUE; tv.ellipsize = null }
                        else { tv.maxLines = 8; tv.ellipsize = TextUtils.TruncateAt.END }
                        hint.visibility = if (long) View.VISIBLE else View.GONE
                        hint.text = if (ex) "收起 ▲" else "展开全文 ▼"
                    }
                    apply()
                    val toggle = View.OnClickListener { if (!expanded.remove(r.url)) expanded.add(r.url); apply() }
                    tv.setOnClickListener(toggle); hint.setOnClickListener(toggle)
                    tv.setOnLongClickListener { copy(r.text); true }
                    bar.getChildAt(0).setOnClickListener { copy(r.text) }
                    bar.getChildAt(1).setOnClickListener { open(r.url) }
                }
            }
        }
    }
}
