package me.nimingzhe.recordeverything.nl

import me.nimingzhe.recordeverything.data.Draft
import me.nimingzhe.recordeverything.data.DraftKind
import me.nimingzhe.recordeverything.util.Dates
import java.util.Calendar

data class ParseResult(
    val draft: Draft?,
    val reply: String
)

/**
 * 中文自然语言 → 记录。纯规则实现（无网络、无模型），
 * 覆盖「昨天午饭 25 微信」「工资 8000」「微信结余 530.46」这类日常说法。
 */
object ChatParser {

    private val numberRe = Regex("""(\d+(?:\.\d{1,2})?)\s*(万|w|W|k|K)?""")
    private val unitWords = listOf("元", "块", "块钱", "¥", "￥")

    val channels = listOf("微信", "支付宝", "现金", "银行卡", "信用卡", "花呗", "云闪付")

    val categories = listOf(
        "餐饮", "交通", "购物", "居住", "娱乐", "医疗", "学习", "人情", "其他",
        "工资", "红包", "报销", "退款"
    )

    /** 关键词 → 类别；先匹配长词，避免「午饭」被「饭」抢走。 */
    private val categoryWords: List<Pair<String, String>> = listOf(
        "早饭" to "餐饮", "早餐" to "餐饮", "午饭" to "餐饮", "午餐" to "餐饮",
        "晚饭" to "餐饮", "晚餐" to "餐饮", "夜宵" to "餐饮", "外卖" to "餐饮",
        "奶茶" to "餐饮", "咖啡" to "餐饮", "火锅" to "餐饮", "聚餐" to "餐饮",
        "吃饭" to "餐饮", "零食" to "餐饮", "水果" to "餐饮", "饭" to "餐饮",
        "打车" to "交通", "滴滴" to "交通", "出租车" to "交通", "地铁" to "交通",
        "公交" to "交通", "高铁" to "交通", "火车" to "交通", "机票" to "交通",
        "加油" to "交通", "停车" to "交通",
        "房租" to "居住", "水费" to "居住", "电费" to "居住", "燃气" to "居住",
        "物业" to "居住", "话费" to "居住", "宽带" to "居住", "水电" to "居住",
        "衣服" to "购物", "超市" to "购物", "淘宝" to "购物", "京东" to "购物",
        "日用品" to "购物", "化妆品" to "购物",
        "电影" to "娱乐", "游戏" to "娱乐", "演出" to "娱乐", "旅游" to "娱乐",
        "门票" to "娱乐", "健身" to "娱乐", "会员" to "娱乐",
        "药" to "医疗", "医院" to "医疗", "挂号" to "医疗", "体检" to "医疗",
        "书" to "学习", "课程" to "学习", "学费" to "学习",
        "红包" to "红包", "礼物" to "人情", "请客" to "人情", "份子" to "人情",
        "工资" to "工资", "薪水" to "工资", "奖金" to "工资", "报销" to "报销",
        "退款" to "退款", "转账" to "其他"
    )

    private val incomeWords = listOf(
        "收入", "收到", "进账", "工资", "薪水", "奖金", "报销", "退款", "利息", "分红", "收款"
    )
    private val expenseWords = listOf("支出", "花了", "消费", "付款", "支付", "买了", "付了")
    private val balanceWords = listOf("结余", "余额", "还剩", "剩下")

    private val genericEvents = listOf(
        "体重" to "kg", "喝水" to "杯", "睡眠" to "小时", "运动" to "分钟", "跑步" to "公里"
    )

    fun parse(raw: String, now: Long = System.currentTimeMillis()): ParseResult {
        val input = raw.trim()
        if (input.isEmpty()) return ParseResult(null, "说一句就能记，比如「昨天午饭 25 微信」")

        val time = parseTime(input, now)
        val channel = channels.firstOrNull { input.contains(it) }
        val amountHit = findAmount(input)
        val isBalance = balanceWords.any { input.contains(it) }
        val category = categoryWords.firstOrNull { input.contains(it.first) }?.second

        // 「体重 62.5」「跑步 5 公里」这类不是花销，先走通用事件，别当成账目。
        if (amountHit == null || !isMoneyContext(input)) {
            val generic = parseGeneric(input, time)
            if (generic != null) return ParseResult(generic, "记好了，看一下对不对：")
            if (amountHit == null) {
                return ParseResult(null, "没看到要记的金额，说一下？比如「昨天午饭 25 微信」")
            }
        }

        if (isBalance) {
            return ParseResult(
                Draft(
                    kind = DraftKind.BALANCE,
                    amount = amountHit.value,
                    channel = channel ?: "现金",
                    category = "其他",
                    time = time,
                    remark = "余额快照"
                ),
                "这是「${channel ?: "现金"}」的余额。和上一次的余额比，差额就是这期间的收支："
            )
        }

        val income = incomeWords.any { input.contains(it) } && !expenseWords.any { input.contains(it) }
        return ParseResult(
            Draft(
                kind = DraftKind.MONEY,
                eventName = "账本",
                amount = amountHit.value,
                category = category ?: "其他",
                channel = channel,
                time = time,
                remark = buildRemark(input, amountHit.token),
                income = income
            ),
            if (income) "这笔收入看着对吗" else "这笔看着对吗"
        )
    }

    class AmountHit(val value: Double, val token: String, val hasUnit: Boolean)

    private fun findAmount(input: String): AmountHit? {
        val hits = numberRe.findAll(input).mapNotNull { m ->
            val base = m.groupValues[1].toDoubleOrNull() ?: return@mapNotNull null
            val mult = when (m.groupValues[2].lowercase()) {
                "万", "w" -> 10_000.0
                "k" -> 1_000.0
                else -> 1.0
            }
            val after = input.substring(m.range.last + 1).take(3)
            val hasUnit = unitWords.any { after.startsWith(it) }
            AmountHit(base * mult, m.value.trim(), hasUnit)
        }.toList()
        if (hits.isEmpty()) return null
        return hits.firstOrNull { it.hasUnit } ?: hits.first()
    }

    /** 判断一句带数字的话是不是在说钱：出现渠道、类别、金额单位或收支词才算。 */
    private fun isMoneyContext(input: String): Boolean {
        if (channels.any { input.contains(it) }) return true
        if (categoryWords.any { input.contains(it.first) }) return true
        if (unitWords.any { input.contains(it) }) return true
        if (incomeWords.any { input.contains(it) }) return true
        if (expenseWords.any { input.contains(it) }) return true
        if (balanceWords.any { input.contains(it) }) return true
        return false
    }

    private fun parseGeneric(input: String, time: Long): Draft? {
        val hit = genericEvents.firstOrNull { input.contains(it.first) } ?: return null
        val amount = numberRe.find(input)?.groupValues?.get(1)?.toDoubleOrNull()
        return Draft(
            kind = DraftKind.GENERIC,
            eventName = hit.first,
            amount = amount,
            unit = hit.second,
            time = time,
            remark = buildRemark(input, null)
        )
    }

    private fun buildRemark(input: String, amountToken: String?): String {
        var s = input
        amountToken?.let { s = s.replace(it, " ") }
        listOf("今天", "昨天", "前天", "大前天", "刚才", "刚刚", "一下", "帮我").forEach {
            s = s.replace(it, " ")
        }
        channels.forEach { s = s.replace(it, " ") }
        categoryWords.forEach { s = s.replace(it.first, " ") }
        return s.replace(Regex("""\s+"""), " ").trim()
    }

    fun parseTime(input: String, now: Long): Long {
        val today = Dates.startOfDay(now)
        when {
            input.contains("大前天") -> return Dates.addDays(today, -3) + 43_200_000L
            input.contains("前天") -> return Dates.addDays(today, -2) + 43_200_000L
            input.contains("昨天") || input.contains("昨晚") -> return Dates.addDays(today, -1) + 43_200_000L
            input.contains("明天") -> return Dates.addDays(today, 1) + 43_200_000L
        }
        Regex("""(\d{1,2})月(\d{1,2})[日号]""").find(input)?.let { m ->
            val month = m.groupValues[1].toIntOrNull()
            val day = m.groupValues[2].toIntOrNull()
            if (month != null && day != null && month in 1..12 && day in 1..31) {
                val year = Dates.year(now)
                var t = Dates.dayMillis(year, month - 1, day)
                if (t > now + 86_400_000L) t = Dates.dayMillis(year - 1, month - 1, day)
                return t
            }
        }
        Regex("""(\d{1,2})[日号]""").find(input)?.let { m ->
            val day = m.groupValues[1].toIntOrNull()
            if (day != null && day in 1..31) {
                val c = Calendar.getInstance().apply { timeInMillis = now }
                c.set(Calendar.DAY_OF_MONTH, day)
                return c.timeInMillis
            }
        }
        val weekdays = listOf(
            "周日" to 1, "周一" to 2, "周二" to 3, "周三" to 4,
            "周四" to 5, "周五" to 6, "周六" to 7
        )
        weekdays.firstOrNull { input.contains(it.first) }?.let { (_, dow) ->
            val c = Calendar.getInstance().apply { timeInMillis = today }
            var delta = dow - c.get(Calendar.DAY_OF_WEEK)
            if (delta >= 0 || input.contains("上")) delta -= 7
            return Dates.addDays(today, delta)
        }
        return now
    }
}
