package me.nimingzhe.recordeverything.data

/**
 * 数据模型，对齐 MarkTimes 的 project / project_variable / project_option /
 * record / record_value 五表结构（见 docs/reverse-engineering.md）。
 *
 * Event   = 事件（原 project）：一类要记录的东西，例如“账本”“体重”。
 * Attribute = 属性（原 project_variable）：事件上的一个字段，例如“金额”“渠道”。
 * AttrOption = 可选值（原 project_option）：单选题的候选项。
 * Record  = 一条记录：只有时间与备注。
 * RecordValue = 一条记录上某个属性的取值，数值/文本/选项三选一。
 */
enum class EventKind { RECORD, HABIT, TIMER }

enum class AttrType { NUMBER, TEXT, CHOICE, SWITCH, DURATION }

data class AttrOption(
    val id: String = newId(),
    val name: String,
    val color: Int? = null
)

data class Attribute(
    val id: String = newId(),
    val name: String,
    val type: AttrType,
    val unit: String = "",
    val order: Int = 0,
    val required: Boolean = false,
    /** 语义角色：type / category / amount / channel，空串表示普通字段。 */
    val role: String = "",
    val options: List<AttrOption> = emptyList()
)

data class Event(
    val id: String = newId(),
    val name: String,
    val kind: EventKind = EventKind.RECORD,
    val icon: String = "📝",
    val color: Int = 0xFFD8B26A.toInt(),
    val attributes: List<Attribute> = emptyList(),
    val archived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class RecordValue(
    val attributeId: String,
    val number: Double? = null,
    val text: String? = null,
    val optionId: String? = null
)

data class Record(
    val id: String = newId(),
    val eventId: String,
    val time: Long = System.currentTimeMillis(),
    val remark: String = "",
    val values: List<RecordValue> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

fun newId(): String = java.util.UUID.randomUUID().toString().replace("-", "").take(16)
