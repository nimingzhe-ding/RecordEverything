package me.nimingzhe.recordeverything.data

enum class Sender { USER, APP }

enum class DraftKind { MONEY, BALANCE, GENERIC, UNKNOWN }

/**
 * 解析出来的「待确认记录」。用户点「只记这笔」之后才真正落库，
 * 所以他可以改金额、类别、渠道、日期、备注。
 */
data class Draft(
    val kind: DraftKind = DraftKind.MONEY,
    val eventName: String? = null,
    val amount: Double? = null,
    val category: String = "其他",
    val channel: String? = null,
    val time: Long = System.currentTimeMillis(),
    val remark: String = "",
    val unit: String = "元",
    val income: Boolean = false
)

/** 聊天里的一条消息；带 draft 的是那张待确认卡片。 */
data class ChatMessage(
    val id: String = newId(),
    val sender: Sender,
    val text: String = "",
    val draft: Draft? = null,
    val time: Long = System.currentTimeMillis()
)
