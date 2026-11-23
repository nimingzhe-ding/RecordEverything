package me.nimingzhe.recordeverything.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.nimingzhe.recordeverything.data.Snapshot
import me.nimingzhe.recordeverything.data.Store
import me.nimingzhe.recordeverything.data.Todo
import me.nimingzhe.recordeverything.data.sorted
import me.nimingzhe.recordeverything.nl.TodoParser
import me.nimingzhe.recordeverything.util.Dates
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class Tab { TODAY, CALENDAR, ALL, SETTINGS }

/** 「全部」页的筛选。 */
enum class Filter { OPEN, TODAY, WEEK, DONE, ALL }

data class UiState(
    val ready: Boolean = false,
    val tab: Tab = Tab.TODAY,
    val todos: List<Todo> = emptyList(),
    val draft: Todo? = null,
    val draftHint: String = "",
    val filter: Filter = Filter.OPEN,
    val year: Int = Dates.year(System.currentTimeMillis()),
    val month0: Int = Dates.month0(System.currentTimeMillis()),
    val selectedDay: Long? = null,
    val group: String? = null
)

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val store = Store(app)
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val loaded = withContext(Dispatchers.IO) { store.load() }
            val now = System.currentTimeMillis()
            _state.value = _state.value.copy(
                ready = true,
                todos = loaded.todos,
                year = Dates.year(now),
                month0 = Dates.month0(now),
                selectedDay = Dates.startOfDay(now)
            )
        }
    }

    // ================= 新增 =================

    /** 解析一句话，生成待确认的草稿。 */
    fun compose(text: String) {
        val body = text.trim()
        if (body.isEmpty()) return
        val parsed = TodoParser.parse(body, System.currentTimeMillis())
        _state.value = _state.value.copy(
            draft = parsed.draft,
            draftHint = parsed.hint
        )
    }

    fun updateDraft(todo: Todo) {
        _state.value = _state.value.copy(draft = todo)
    }

    fun cancelDraft() {
        _state.value = _state.value.copy(draft = null, draftHint = "")
    }

    fun confirmDraft() {
        val draft = _state.value.draft ?: return
        if (draft.title.isBlank()) return
        _state.value = _state.value.copy(
            todos = _state.value.todos + draft,
            draft = null,
            draftHint = ""
        )
        persist()
    }

    /** 直接新建一条，不经过解析（手动输入）。 */
    fun add(title: String, due: Long?, time: Int?, group: String = Todo.DEFAULT_GROUP) {
        val t = title.trim()
        if (t.isEmpty()) return
        _state.value = _state.value.copy(
            todos = _state.value.todos + Todo(title = t, due = due, time = time, group = group)
        )
        persist()
    }

    // ================= 修改 =================

    fun toggle(id: String) {
        _state.value = _state.value.copy(
            todos = _state.value.todos.map { t ->
                if (t.id != id) t
                else t.copy(
                    done = !t.done,
                    doneAt = if (t.done) null else System.currentTimeMillis()
                )
            }
        )
        persist()
    }

    fun update(todo: Todo) {
        _state.value = _state.value.copy(
            todos = _state.value.todos.map { if (it.id == todo.id) todo else it }
        )
        persist()
    }

    fun delete(id: String) {
        _state.value = _state.value.copy(todos = _state.value.todos.filterNot { it.id == id })
        persist()
    }

    /** 推迟到明天（保留原时刻）。 */
    fun snoozeToTomorrow(id: String) {
        val t = _state.value.todos.firstOrNull { it.id == id } ?: return
        val base = t.due ?: Dates.startOfDay(System.currentTimeMillis())
        update(t.copy(due = Dates.addDays(base, 1)))
    }

    /** 设为某天（null = 收回收集箱）；保留原时刻，仅当设为有日期时才有意义。 */
    fun setDue(id: String, day: Long?) {
        val t = _state.value.todos.firstOrNull { it.id == id } ?: return
        update(t.copy(due = day?.let { Dates.startOfDay(it) }, time = if (day == null) null else t.time))
    }

    /** 拖到某天（日历里用）。 */
    fun moveTo(todo: Todo, day: Long) {
        update(todo.copy(due = Dates.startOfDay(day)))
    }

    // ================= 导航与筛选 =================

    fun setTab(tab: Tab) {
        _state.value = _state.value.copy(tab = tab)
    }

    fun setFilter(filter: Filter) {
        _state.value = _state.value.copy(filter = filter)
    }

    fun setGroup(group: String?) {
        _state.value = _state.value.copy(group = group)
    }

    fun shiftMonth(delta: Int) {
        val s = _state.value
        val next = if (delta > 0) Dates.nextMonth(s.year, s.month0) else Dates.prevMonth(s.year, s.month0)
        _state.value = s.copy(year = next.first, month0 = next.second, selectedDay = null)
    }

    fun selectDay(millis: Long?) {
        _state.value = _state.value.copy(selectedDay = millis)
    }

    fun jumpToToday() {
        val now = System.currentTimeMillis()
        _state.value = _state.value.copy(
            year = Dates.year(now),
            month0 = Dates.month0(now),
            selectedDay = Dates.startOfDay(now),
            tab = Tab.TODAY
        )
    }

    // ================= 查询 =================

    fun todosOfDay(day: Long): List<Todo> {
        val start = Dates.startOfDay(day)
        return _state.value.todos.filter { it.due == start }.sorted()
    }

    /** 今天：过期未完成 + 今天 + 已完成（沉底）。 */
    fun todayList(): List<Todo> {
        val today = Dates.startOfDay(System.currentTimeMillis())
        val tomorrow = Dates.addDays(today, 1)
        return _state.value.todos
            .filter { t -> t.due == null || (t.due != null && t.due < tomorrow) }
            .sorted()
    }

    fun overdue(): List<Todo> {
        val today = Dates.startOfDay(System.currentTimeMillis())
        return _state.value.todos.filter { !it.done && it.due != null && it.due < today }.sorted()
    }

    /** 未来 7 天按天分组。 */
    fun nextSevenDays(): List<Pair<Long, List<Todo>>> {
        val today = Dates.startOfDay(System.currentTimeMillis())
        return (0..6).map { i ->
            val day = Dates.addDays(today, i)
            day to _state.value.todos.filter { it.due == day }.sorted()
        }
    }

    fun allFiltered(): List<Todo> {
        val s = _state.value
        val today = Dates.startOfDay(System.currentTimeMillis())
        var list = s.todos
        s.group?.let { g -> list = list.filter { it.group == g } }
        list = when (s.filter) {
            Filter.OPEN -> list.filter { !it.done }
            Filter.TODAY -> list.filter { it.due == today }
            Filter.WEEK -> list.filter {
                val d = it.due
                d != null && d >= today && d < Dates.addDays(today, 7)
            }
            Filter.DONE -> list.filter { it.done }
            Filter.ALL -> list
        }
        return list.sorted()
    }

    /** 每个月每天的任务数，供日历格子用：(未完成, 已完成)。 */
    fun monthCounts(): Map<Int, Pair<Int, Int>> {
        val s = _state.value
        val out = HashMap<Int, Pair<Int, Int>>()
        s.todos.forEach { t ->
            val d = t.due ?: return@forEach
            if (Dates.year(d) != s.year || Dates.month0(d) != s.month0) return@forEach
            val day = Dates.dayOfMonth(d)
            val prev = out[day] ?: (0 to 0)
            out[day] = if (t.done) prev.first to (prev.second + 1) else (prev.first + 1) to prev.second
        }
        return out
    }

    fun counts(): Triple<Int, Int, Int> {
        val today = Dates.startOfDay(System.currentTimeMillis())
        val open = _state.value.todos.count { !it.done }
        val dueToday = _state.value.todos.count { !it.done && it.due == today }
        val over = _state.value.todos.count { !it.done && it.due != null && it.due < today }
        return Triple(open, dueToday, over)
    }

    fun streak(): Int {
        val doneDays = _state.value.todos
            .filter { it.done && it.doneAt != null }
            .map { Dates.startOfDay(it.doneAt!!) }
            .distinct()
            .sortedDescending()
        if (doneDays.isEmpty()) return 0
        var cursor = Dates.startOfDay(System.currentTimeMillis())
        if (doneDays.first() != cursor) {
            cursor = Dates.addDays(cursor, -1)
            if (doneDays.first() != cursor) return 0
        }
        var n = 0
        for (d in doneDays) {
            when {
                d == cursor -> {
                    n++
                    cursor = Dates.addDays(cursor, -1)
                }
                d < cursor -> break
            }
        }
        return n
    }

    // ================= 导出 =================

    fun exportMarkdown(): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
        val sb = StringBuilder("# 记录 · 待办清单\n\n")
        sb.append("导出时间：").append(fmt.format(Date())).append("\n\n")
        val open = _state.value.todos.filter { !it.done }.sorted()
        val done = _state.value.todos.filter { it.done }.sortedByDescending { it.doneAt ?: 0 }
        sb.append("## 未完成（").append(open.size).append("）\n\n")
        if (open.isEmpty()) sb.append("_没有未完成的待办_\n\n")
        open.forEach { t -> sb.append("- [ ] ").append(line(t)).append('\n') }
        sb.append("\n## 已完成（").append(done.size).append("）\n\n")
        if (done.isEmpty()) sb.append("_还没有完成任何一件_\n\n")
        done.forEach { t -> sb.append("- [x] ").append(line(t)).append('\n') }
        return sb.toString()
    }

    private fun line(t: Todo): String = buildString {
        append(t.title)
        t.due?.let { d ->
            append(" — ")
            append(Dates.formatDateShort(d))
            t.time?.let { append(' ').append(TodoParser.fmtMinute(it)) }
        }
        if (t.group.isNotBlank()) append(" · ").append(t.group)
        if (t.note.isNotBlank()) append("（").append(t.note).append("）")
    }

    fun exportJson(): String {
        val root = JSONObject()
        root.put("app", "RecordEverything")
        root.put("exportedAt", System.currentTimeMillis())
        val arr = JSONArray()
        _state.value.todos.sorted().forEach { t ->
            arr.put(
                JSONObject().apply {
                    put("title", t.title)
                    put("note", t.note)
                    put("due", t.due ?: JSONObject.NULL)
                    put("time", t.time?.let { TodoParser.fmtMinute(it) } ?: JSONObject.NULL)
                    put("done", t.done)
                    put("doneAt", t.doneAt ?: JSONObject.NULL)
                    put("important", t.important)
                    put("group", t.group)
                }
            )
        }
        root.put("todos", arr)
        return root.toString(2)
    }

    fun exportCsv(): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
        val sb = StringBuilder("标题,日期,时间,分组,状态,备注\n")
        _state.value.todos.sorted().forEach { t ->
            sb.append(csv(t.title)).append(',')
                .append(t.due?.let { fmt.format(Date(it)) } ?: "").append(',')
                .append(t.time?.let { TodoParser.fmtMinute(it) } ?: "").append(',')
                .append(csv(t.group)).append(',')
                .append(if (t.done) "已完成" else "未完成").append(',')
                .append(csv(t.note)).append('\n')
        }
        return sb.toString()
    }

    private fun csv(v: String): String =
        if (v.contains(',') || v.contains('"') || v.contains('\n')) "\"" + v.replace("\"", "\"\"") + "\"" else v

    fun clearAll() {
        store.clearAll()
        val now = System.currentTimeMillis()
        _state.value = UiState(
            ready = true,
            year = Dates.year(now),
            month0 = Dates.month0(now),
            selectedDay = Dates.startOfDay(now)
        )
    }

    // ================= 持久化 =================

    private fun persist() {
        val snap = Snapshot(todos = _state.value.todos)
        viewModelScope.launch(Dispatchers.IO) { store.save(snap) }
    }
}
