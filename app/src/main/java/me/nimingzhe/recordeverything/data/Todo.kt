package me.nimingzhe.recordeverything.data

/**
 * 待办 / 日程。
 *
 * due 只到「天」（当天 0 点），time 是当天的第几分钟（可为空 = 全天）。
 * 无 due 的任务进收集箱，不排期。
 */
data class Todo(
    val id: String = newId(),
    val title: String,
    val note: String = "",
    val due: Long? = null,
    val time: Int? = null,
    val done: Boolean = false,
    val doneAt: Long? = null,
    val important: Boolean = false,
    val group: String = DEFAULT_GROUP,
    val createdAt: Long = System.currentTimeMillis()
) {
    /** 带时间的绝对时刻；无时间返回 null。 */
    fun at(): Long? = due?.let { d -> time?.let { d + it * 60_000L } }

    companion object {
        const val DEFAULT_GROUP = "日常"
        val GROUPS = listOf("日常", "工作", "学习", "生活")
    }
}

/** 按截止时间排序：有日期的在前（先早后晚），无日期的按创建时间。 */
fun List<Todo>.sorted(): List<Todo> = sortedWith(
    compareBy<Todo> { it.done }
        .thenBy { it.due ?: Long.MAX_VALUE }
        .thenBy { it.time ?: Int.MAX_VALUE }
        .thenBy { it.createdAt }
)
