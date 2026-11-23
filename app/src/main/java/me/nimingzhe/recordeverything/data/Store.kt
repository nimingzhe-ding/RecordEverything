package me.nimingzhe.recordeverything.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class Snapshot(
    val events: List<Event> = emptyList(),
    val records: List<Record> = emptyList(),
    val messages: List<ChatMessage> = emptyList()
)

/**
 * 单文件 JSON 持久层。原 App 用 Room + 22 个版本迁移，这里刻意简化：
 * 整个库就是一个 JSON，写盘用「临时文件 + rename」，导出即分享这份结构。
 */
class Store(private val context: Context) {

    private val file: File
        get() = File(context.filesDir, "recordeverything.json")

    fun load(): Snapshot {
        val f = file
        if (!f.exists()) return Snapshot(events = Seed.events())
        return try {
            val root = JSONObject(f.readText())
            Snapshot(
                events = root.optJSONArray("events").toEvents(),
                records = root.optJSONArray("records").toRecords(),
                messages = root.optJSONArray("messages").toMessages()
            ).let { if (it.events.isEmpty()) it.copy(events = Seed.events()) else it }
        } catch (t: Throwable) {
            Snapshot(events = Seed.events())
        }
    }

    fun save(snapshot: Snapshot) {
        val root = JSONObject()
        root.put("schema", 1)
        root.put("savedAt", System.currentTimeMillis())
        root.put("events", JSONArray().also { arr -> snapshot.events.forEach { arr.put(it.toJson()) } })
        root.put("records", JSONArray().also { arr -> snapshot.records.forEach { arr.put(it.toJson()) } })
        root.put("messages", JSONArray().also { arr -> snapshot.messages.takeLast(300).forEach { arr.put(it.toJson()) } })
        val tmp = File(context.filesDir, "recordeverything.json.tmp")
        tmp.writeText(root.toString())
        val target = file
        if (target.exists()) target.delete()
        tmp.renameTo(target)
    }

    fun clearRecords() {
        save(Snapshot(events = Seed.events(), records = emptyList(), messages = emptyList()))
    }
}

// ---------- 序列化 ----------

internal fun Event.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("name", name)
    put("kind", kind.name)
    put("icon", icon)
    put("color", color)
    put("archived", archived)
    put("createdAt", createdAt)
    put("attributes", JSONArray().also { arr ->
        attributes.forEach { a ->
            arr.put(JSONObject().apply {
                put("id", a.id)
                put("name", a.name)
                put("type", a.type.name)
                put("unit", a.unit)
                put("order", a.order)
                put("required", a.required)
                put("role", a.role)
                put("options", JSONArray().also { oa ->
                    a.options.forEach { o ->
                        oa.put(JSONObject().apply {
                            put("id", o.id)
                            put("name", o.name)
                            put("color", o.color ?: 0)
                        })
                    }
                })
            })
        }
    })
}

internal fun JSONObject.toEvent(): Event = Event(
    id = optString("id", newId()),
    name = optString("name", "未命名"),
    kind = runCatching { EventKind.valueOf(optString("kind", "RECORD")) }.getOrDefault(EventKind.RECORD),
    icon = optString("icon", "📝"),
    color = optInt("color", 0xFFD8B26A.toInt()),
    archived = optBoolean("archived", false),
    createdAt = optLong("createdAt", System.currentTimeMillis()),
    attributes = optJSONArray("attributes").toAttributes()
)

internal fun JSONArray?.toEvents(): List<Event> =
    if (this == null) emptyList() else (0 until length()).map { optJSONObject(it).toEvent() }

internal fun JSONArray?.toAttributes(): List<Attribute> {
    if (this == null) return emptyList()
    return (0 until length()).map { i ->
        val o = optJSONObject(i)
        Attribute(
            id = o.optString("id", newId()),
            name = o.optString("name", ""),
            type = runCatching { AttrType.valueOf(o.optString("type", "NUMBER")) }.getOrDefault(AttrType.NUMBER),
            unit = o.optString("unit", ""),
            order = o.optInt("order", 0),
            required = o.optBoolean("required", false),
            role = o.optString("role", ""),
            options = (0 until o.optJSONArray("options").let { it?.length() ?: 0 }).map { k ->
                val oc = o.optJSONArray("options").optJSONObject(k)
                AttrOption(
                    id = oc.optString("id", newId()),
                    name = oc.optString("name", ""),
                    color = oc.optInt("color", 0).takeIf { it != 0 }
                )
            }
        )
    }
}

internal fun Record.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("eventId", eventId)
    put("time", time)
    put("remark", remark)
    put("createdAt", createdAt)
    put("values", JSONArray().also { arr ->
        values.forEach { v ->
            arr.put(JSONObject().apply {
                put("attributeId", v.attributeId)
                put("number", v.number ?: JSONObject.NULL)
                put("text", v.text ?: JSONObject.NULL)
                put("optionId", v.optionId ?: JSONObject.NULL)
            })
        }
    })
}

internal fun JSONObject.toRecord(): Record = Record(
    id = optString("id", newId()),
    eventId = optString("eventId", ""),
    time = optLong("time", System.currentTimeMillis()),
    remark = optString("remark", ""),
    createdAt = optLong("createdAt", System.currentTimeMillis()),
    values = (0 until optJSONArray("values").let { it?.length() ?: 0 }).map { i ->
        val o = optJSONArray("values").optJSONObject(i)
        RecordValue(
            attributeId = o.optString("attributeId", ""),
            number = if (o.isNull("number")) null else o.optDouble("number"),
            text = if (o.isNull("text")) null else o.optString("text"),
            optionId = if (o.isNull("optionId")) null else o.optString("optionId")
        )
    }
)

internal fun JSONArray?.toRecords(): List<Record> =
    if (this == null) emptyList() else (0 until length()).map { optJSONObject(it).toRecord() }

internal fun ChatMessage.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("sender", sender.name)
    put("text", text)
    put("time", time)
    draft?.let { d ->
        put("draft", JSONObject().apply {
            put("kind", d.kind.name)
            put("amount", d.amount ?: JSONObject.NULL)
            put("category", d.category)
            put("channel", d.channel ?: JSONObject.NULL)
            put("time", d.time)
            put("remark", d.remark)
            put("eventName", d.eventName ?: JSONObject.NULL)
            put("unit", d.unit)
            put("income", d.income)
        })
    }
}

internal fun JSONObject.toMessage(): ChatMessage = ChatMessage(
    id = optString("id", newId()),
    sender = runCatching { Sender.valueOf(optString("sender", "APP")) }.getOrDefault(Sender.APP),
    text = optString("text", ""),
    time = optLong("time", System.currentTimeMillis()),
    draft = optJSONObject("draft")?.let { d ->
        Draft(
            kind = runCatching { DraftKind.valueOf(d.optString("kind", "UNKNOWN")) }.getOrDefault(DraftKind.UNKNOWN),
            amount = if (d.isNull("amount")) null else d.optDouble("amount"),
            category = d.optString("category", "其他"),
            channel = if (d.isNull("channel")) null else d.optString("channel"),
            time = d.optLong("time", System.currentTimeMillis()),
            remark = d.optString("remark", ""),
            eventName = if (d.isNull("eventName")) null else d.optString("eventName"),
            unit = d.optString("unit", ""),
            income = d.optBoolean("income", false)
        )
    }
)

internal fun JSONArray?.toMessages(): List<ChatMessage> =
    if (this == null) emptyList() else (0 until length()).map { optJSONObject(it).toMessage() }
