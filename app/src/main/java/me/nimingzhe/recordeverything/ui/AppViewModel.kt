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
import me.nimingzhe.recordeverything.data.ChatMessage
import me.nimingzhe.recordeverything.data.Draft
import me.nimingzhe.recordeverything.data.DraftKind
import me.nimingzhe.recordeverything.data.Event
import me.nimingzhe.recordeverything.data.Ledger
import me.nimingzhe.recordeverything.data.Record
import me.nimingzhe.recordeverything.data.RecordValue
import me.nimingzhe.recordeverything.data.Seed
import me.nimingzhe.recordeverything.data.Sender
import me.nimingzhe.recordeverything.data.Snapshot
import me.nimingzhe.recordeverything.data.Store
import me.nimingzhe.recordeverything.nl.ChatParser
import me.nimingzhe.recordeverything.util.Dates
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class Tab { CHAT, CALENDAR, STATS, SETTINGS }

data class UiState(
    val ready: Boolean = false,
    val tab: Tab = Tab.CHAT,
    val events: List<Event> = emptyList(),
    val records: List<Record> = emptyList(),
    val messages: List<ChatMessage> = emptyList(),
    val pending: ChatMessage? = null,
    val year: Int = Dates.year(System.currentTimeMillis()),
    val month0: Int = Dates.month0(System.currentTimeMillis()),
    val selectedDay: Long? = null
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
                events = loaded.events,
                records = loaded.records.sortedByDescending { it.time },
                messages = loaded.messages,
                pending = loaded.messages.lastOrNull { it.draft != null },
                year = Dates.year(now),
                month0 = Dates.month0(now)
            )
        }
    }

    // ================= 聊天 =================

    fun send(text: String) {
        val body = text.trim()
        if (body.isEmpty()) return
        val userMsg = ChatMessage(sender = Sender.USER, text = body)
        val result = ChatParser.parse(body, System.currentTimeMillis())
        val base = _state.value.messages + userMsg
        if (result.draft != null) {
            val card = ChatMessage(sender = Sender.APP, text = result.reply, draft = result.draft)
            _state.value = _state.value.copy(messages = base + card, pending = card)
        } else {
            _state.value = _state.value.copy(
                messages = base + ChatMessage(sender = Sender.APP, text = result.reply),
                pending = null
            )
        }
        persist()
    }

    fun updateDraft(draft: Draft) {
        val pending = _state.value.pending ?: return
        val updated = pending.copy(draft = draft)
        _state.value = _state.value.copy(
            messages = _state.value.messages.map { if (it.id == updated.id) updated else it },
            pending = updated
        )
        persist()
    }

    fun cancelDraft() {
        val pending = _state.value.pending ?: return
        _state.value = _state.value.copy(
            messages = _state.value.messages.filterNot { it.id == pending.id } +
                ChatMessage(sender = Sender.APP, text = "好，这笔先不记。"),
            pending = null
        )
        persist()
    }

    fun confirmDraft() {
        val pending = _state.value.pending ?: return
        val draft = pending.draft ?: return
        val record = commit(draft)
        _state.value = _state.value.copy(
            records = (_state.value.records + record).sortedByDescending { it.time },
            messages = _state.value.messages.filterNot { it.id == pending.id } +
                ChatMessage(sender = Sender.APP, text = confirmationText(draft)),
            pending = null
        )
        persist()
    }

    private fun confirmationText(draft: Draft): String = when (draft.kind) {
        DraftKind.MONEY -> buildString {
            append("记好了：")
            append(draft.category)
            append(' ')
            append(if (draft.income) "+" else "-")
            append("¥")
            append(Ledger.money(draft.amount ?: 0.0))
            append(' ')
            append(Dates.chip(draft.time))
        }
        DraftKind.BALANCE -> "记好了，${draft.channel ?: "现金"}余额 ¥${Ledger.money(draft.amount ?: 0.0)}"
        DraftKind.GENERIC -> "记好了：${draft.eventName ?: "记录"} ${draft.amount?.let { Ledger.plain(it) } ?: ""}${draft.unit}"
        DraftKind.UNKNOWN -> "记好了"
    }

    /** 把 Draft 落成 Record；必要时补建事件。 */
    private fun commit(draft: Draft): Record {
        val eventName = draft.eventName ?: "账本"
        val event = ensureEvent(eventName)
        if (event.name == "账本") {
            val typeAttr = Ledger.attr(event, Ledger.ROLE_TYPE)
            val amountAttr = Ledger.attr(event, Ledger.ROLE_AMOUNT)
            val catAttr = Ledger.attr(event, Ledger.ROLE_CATEGORY)
            val chAttr = Ledger.attr(event, Ledger.ROLE_CHANNEL)
            val values = ArrayList<RecordValue>()
            typeAttr?.options?.firstOrNull { it.name == if (draft.income) "收入" else "支出" }?.let {
                values.add(RecordValue(typeAttr.id, optionId = it.id))
            }
            amountAttr?.let { a -> draft.amount?.let { values.add(RecordValue(a.id, number = it)) } }
            catAttr?.options?.firstOrNull { it.name == draft.category }?.let {
                values.add(RecordValue(catAttr.id, optionId = it.id))
            }
            chAttr?.let { a ->
                draft.channel?.let { ch ->
                    a.options.firstOrNull { it.name == ch }?.let {
                        values.add(RecordValue(a.id, optionId = it.id))
                    }
                }
            }
            return Record(eventId = event.id, time = draft.time, remark = draft.remark, values = values)
        }
        val amountAttr = event.attributes.firstOrNull()
            ?: return Record(eventId = event.id, time = draft.time, remark = draft.remark)
        return Record(
            eventId = event.id,
            time = draft.time,
            remark = draft.remark,
            values = listOf(RecordValue(amountAttr.id, number = draft.amount))
        )
    }

    private fun ensureEvent(name: String): Event {
        val existing = _state.value.events.firstOrNull { it.name == name }
        if (existing != null) return existing
        val fresh = Seed.events().firstOrNull { it.name == name }
            ?: Event(name = name, icon = "📝", attributes = emptyList())
        _state.value = _state.value.copy(events = _state.value.events + fresh)
        return fresh
    }

    // ================= 手工记账（日历页） =================

    fun addMoney(
        amount: Double,
        category: String,
        channel: String?,
        time: Long,
        remark: String,
        income: Boolean
    ) {
        val draft = Draft(
            kind = DraftKind.MONEY,
            eventName = "账本",
            amount = amount,
            category = category,
            channel = channel,
            time = time,
            remark = remark,
            income = income
        )
        val record = commit(draft)
        _state.value = _state.value.copy(
            records = (_state.value.records + record).sortedByDescending { it.time }
        )
        persist()
    }

    fun deleteRecord(record: Record) {
        _state.value = _state.value.copy(records = _state.value.records.filterNot { it.id == record.id })
        persist()
    }

    // ================= 导航 =================

    fun setTab(tab: Tab) {
        _state.value = _state.value.copy(tab = tab)
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
            selectedDay = Dates.startOfDay(now)
        )
    }

    // ================= 查询 =================

    fun recordsOfDay(millis: Long): List<Record> {
        val start = Dates.startOfDay(millis)
        val end = Dates.addDays(start, 1)
        return _state.value.records.filter { it.time >= start && it.time < end }.sortedByDescending { it.time }
    }

    /** 某月每天：(支出为负合计, 收入合计)。按「日」索引。 */
    fun monthTotals(): Map<Int, Pair<Double, Double>> {
        val s = _state.value
        val events = s.events.associateBy { it.id }
        val out = HashMap<Int, Pair<Double, Double>>()
        s.records.forEach { r ->
            if (Dates.year(r.time) != s.year || Dates.month0(r.time) != s.month0) return@forEach
            val ev = events[r.eventId] ?: return@forEach
            if (!Ledger.isMoney(ev)) return@forEach
            val amount = Ledger.amount(r, ev) ?: return@forEach
            val day = Dates.dayOfMonth(r.time)
            val prev = out[day] ?: (0.0 to 0.0)
            out[day] = if (Ledger.isIncome(r, ev)) prev.first to (prev.second + amount)
            else (prev.first - amount) to prev.second
        }
        return out
    }

    fun monthSummary(): Triple<Double, Double, Int> {
        val totals = monthTotals()
        return Triple(
            totals.values.sumOf { it.first },
            totals.values.sumOf { it.second },
            totals.values.count { it.first != 0.0 || it.second != 0.0 }
        )
    }

    fun categoryTotals(): List<Pair<String, Double>> {
        val s = _state.value
        val events = s.events.associateBy { it.id }
        val map = LinkedHashMap<String, Double>()
        s.records.forEach { r ->
            if (Dates.year(r.time) != s.year || Dates.month0(r.time) != s.month0) return@forEach
            val ev = events[r.eventId] ?: return@forEach
            if (!Ledger.isMoney(ev) || Ledger.isIncome(r, ev)) return@forEach
            val amount = Ledger.amount(r, ev) ?: return@forEach
            val key = Ledger.categoryName(r, ev).ifBlank { "其他" }
            map[key] = (map[key] ?: 0.0) + amount
        }
        return map.entries.sortedByDescending { it.value }.map { it.key to it.value }
    }

    fun channelTotals(): List<Pair<String, Double>> {
        val s = _state.value
        val events = s.events.associateBy { it.id }
        val map = LinkedHashMap<String, Double>()
        s.records.forEach { r ->
            if (Dates.year(r.time) != s.year || Dates.month0(r.time) != s.month0) return@forEach
            val ev = events[r.eventId] ?: return@forEach
            if (!Ledger.isMoney(ev) || Ledger.isIncome(r, ev)) return@forEach
            val amount = Ledger.amount(r, ev) ?: return@forEach
            val key = Ledger.channelName(r, ev).ifBlank { "未标注" }
            map[key] = (map[key] ?: 0.0) + amount
        }
        return map.entries.sortedByDescending { it.value }.map { it.key to it.value }
    }

    fun streakDays(): Int {
        val days = _state.value.records.map { Dates.startOfDay(it.time) }.distinct().sortedDescending()
        if (days.isEmpty()) return 0
        var cursor = Dates.startOfDay(System.currentTimeMillis())
        if (days.first() != cursor) {
            cursor = Dates.addDays(cursor, -1)
            if (days.first() != cursor) return 0
        }
        var count = 0
        for (d in days) {
            if (d == cursor) {
                count++
                cursor = Dates.addDays(cursor, -1)
            } else if (d < cursor) {
                break
            }
        }
        return count
    }

    fun eventOf(record: Record): Event? = _state.value.events.firstOrNull { it.id == record.eventId }

    // ================= 导出 =================

    fun exportJson(): String {
        val s = _state.value
        val root = JSONObject()
        root.put("app", "RecordEverything")
        root.put("exportedAt", System.currentTimeMillis())
        val arr = JSONArray()
        s.records.sortedBy { it.time }.forEach { r ->
            val ev = eventOf(r)
            arr.put(
                JSONObject().apply {
                    put("time", r.time)
                    put("event", ev?.name ?: "")
                    put("type", Ledger.typeName(r, ev))
                    put("category", Ledger.categoryName(r, ev))
                    put("channel", Ledger.channelName(r, ev))
                    put("amount", (Ledger.amount(r, ev) ?: 0.0).let { if (it == 0.0) JSONObject.NULL else it })
                    put("unit", Ledger.attr(ev, Ledger.ROLE_AMOUNT)?.unit ?: "")
                    put("remark", r.remark)
                }
            )
        }
        root.put("records", arr)
        return root.toString(2)
    }

    fun exportCsv(): String {
        val s = _state.value
        val sb = StringBuilder("时间,事件,类型,类别,渠道,金额,备注\n")
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
        s.records.sortedBy { it.time }.forEach { r ->
            val ev = eventOf(r)
            sb.append(fmt.format(Date(r.time))).append(',')
                .append(csv(ev?.name.orEmpty())).append(',')
                .append(csv(Ledger.typeName(r, ev))).append(',')
                .append(csv(Ledger.categoryName(r, ev))).append(',')
                .append(csv(Ledger.channelName(r, ev))).append(',')
                .append(Ledger.amount(r, ev)?.let { Ledger.plain(it) } ?: "").append(',')
                .append(csv(r.remark)).append('\n')
        }
        return sb.toString()
    }

    private fun csv(v: String): String =
        if (v.contains(',') || v.contains('"') || v.contains('\n')) "\"" + v.replace("\"", "\"\"") + "\"" else v

    fun clearAll() {
        store.clearRecords()
        val now = System.currentTimeMillis()
        _state.value = UiState(
            ready = true,
            events = Seed.events(),
            records = emptyList(),
            messages = emptyList(),
            year = Dates.year(now),
            month0 = Dates.month0(now)
        )
    }

    // ================= 持久化 =================

    private fun persist() {
        val s = _state.value
        val snap = Snapshot(events = s.events, records = s.records, messages = s.messages)
        viewModelScope.launch(Dispatchers.IO) { store.save(snap) }
    }
}
