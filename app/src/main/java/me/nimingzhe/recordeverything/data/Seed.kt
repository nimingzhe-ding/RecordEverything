package me.nimingzhe.recordeverything.data

/** 首次启动时的内置事件（对应 MarkTimes 的模板库，但只保留最常用的几个）。 */
object Seed {

    private val ledgerTypes = listOf("支出", "收入")

    val ledgerCategories = listOf(
        "餐饮", "交通", "购物", "居住", "娱乐", "医疗", "学习", "人情", "其他",
        "工资", "红包", "报销", "退款"
    )

    val ledgerChannels = listOf("微信", "支付宝", "现金", "银行卡", "信用卡")

    fun events(): List<Event> = listOf(
        Event(
            name = "账本",
            kind = EventKind.RECORD,
            icon = "💰",
            attributes = listOf(
                Attribute(
                    name = "类型",
                    type = AttrType.CHOICE,
                    role = Ledger.ROLE_TYPE,
                    order = 0,
                    required = true,
                    options = ledgerTypes.map { AttrOption(name = it) }
                ),
                Attribute(
                    name = "金额",
                    type = AttrType.NUMBER,
                    role = Ledger.ROLE_AMOUNT,
                    unit = "元",
                    order = 1,
                    required = true
                ),
                Attribute(
                    name = "类别",
                    type = AttrType.CHOICE,
                    role = Ledger.ROLE_CATEGORY,
                    order = 2,
                    options = ledgerCategories.map { AttrOption(name = it) }
                ),
                Attribute(
                    name = "渠道",
                    type = AttrType.CHOICE,
                    role = Ledger.ROLE_CHANNEL,
                    order = 3,
                    options = ledgerChannels.map { AttrOption(name = it) }
                )
            )
        ),
        Event(
            name = "体重",
            icon = "⚖️",
            attributes = listOf(
                Attribute(name = "体重", type = AttrType.NUMBER, unit = "kg", order = 0, required = true)
            )
        ),
        Event(
            name = "喝水",
            icon = "💧",
            attributes = listOf(
                Attribute(name = "杯数", type = AttrType.NUMBER, unit = "杯", order = 0)
            )
        ),
        Event(
            name = "睡眠",
            icon = "😴",
            attributes = listOf(
                Attribute(name = "时长", type = AttrType.NUMBER, unit = "小时", order = 0)
            )
        ),
        Event(
            name = "习惯打卡",
            kind = EventKind.HABIT,
            icon = "✅",
            attributes = listOf(
                Attribute(
                    name = "完成",
                    type = AttrType.SWITCH,
                    order = 0,
                    options = listOf(AttrOption(name = "是"), AttrOption(name = "否"))
                )
            )
        )
    )
}
