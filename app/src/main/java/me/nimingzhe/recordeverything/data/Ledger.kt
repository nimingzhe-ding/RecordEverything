package me.nimingzhe.recordeverything.data

import java.text.DecimalFormat

/** 从“事件 + 记录”里读出语义值的工具函数。 */
object Ledger {

    const val ROLE_TYPE = "type"
    const val ROLE_CATEGORY = "category"
    const val ROLE_AMOUNT = "amount"
    const val ROLE_CHANNEL = "channel"

    fun attr(event: Event?, role: String): Attribute? =
        event?.attributes?.firstOrNull { it.role == role }

    fun isMoney(event: Event?): Boolean =
        attr(event, ROLE_TYPE) != null && attr(event, ROLE_AMOUNT) != null

    fun value(record: Record, attr: Attribute?): RecordValue? =
        attr?.let { a -> record.values.firstOrNull { it.attributeId == a.id } }

    fun optionName(record: Record, attr: Attribute?): String? {
        val v = value(record, attr) ?: return null
        return attr?.options?.firstOrNull { it.id == v.optionId }?.name ?: v.text
    }

    fun amount(record: Record, event: Event?): Double? =
        value(record, attr(event, ROLE_AMOUNT))?.number

    fun typeName(record: Record, event: Event?): String =
        optionName(record, attr(event, ROLE_TYPE)).orEmpty()

    fun categoryName(record: Record, event: Event?): String =
        optionName(record, attr(event, ROLE_CATEGORY)).orEmpty()

    fun channelName(record: Record, event: Event?): String =
        optionName(record, attr(event, ROLE_CHANNEL)).orEmpty()

    fun isIncome(record: Record, event: Event?): Boolean =
        typeName(record, event).contains("收入")

    /** 支出为负、收入为正。 */
    fun signed(record: Record, event: Event?): Double {
        val a = amount(record, event) ?: return 0.0
        return if (isIncome(record, event)) a else -a
    }

    /** 账单类事件取金额，其它事件取第一个数值字段。 */
    fun primaryNumber(record: Record, event: Event?): Double? =
        amount(record, event)
            ?: event?.attributes?.mapNotNull { a -> value(record, a)?.number }?.firstOrNull()

    fun primaryText(record: Record, event: Event?): String {
        val fromText = event?.attributes
            ?.mapNotNull { a -> value(record, a)?.text?.takeIf { it.isNotBlank() } }
            ?.firstOrNull()
        if (!fromText.isNullOrBlank()) return fromText
        return event?.attributes
            ?.mapNotNull { a -> optionName(record, a)?.takeIf { it.isNotBlank() } }
            ?.firstOrNull()
            .orEmpty()
    }

    fun money(value: Double): String {
        val f = DecimalFormat("#,##0.00")
        return f.format(value)
    }

    fun plain(value: Double): String {
        val f = DecimalFormat("#,##0.##")
        return f.format(value)
    }
}
