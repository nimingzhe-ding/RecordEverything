package me.nimingzhe.recordeverything.nl

import me.nimingzhe.recordeverything.data.Todo
import me.nimingzhe.recordeverything.util.Dates
import java.util.Calendar

/**
 * 一句话 → 待办。纯函数、无 Android 依赖（除了 java.util.Calendar），可单测。
 *
 * 支持：「明天下午三点开会」「后天还信用卡 交房租」「周五 9点 体检」
 *      「每天八点吃药」「下周一交报告」「3号 交水电费」「今晚写周报」
 * 拆出的是：标题、哪天、当天第几分钟、分组。
 */
object TodoParser {

    data class ParseResult(val draft: Todo?, val hint: String)

    private val groups = listOf(
        "工作" to listOf("开会", "会议", "报告", "周报", "日报", "邮件", "评审", "上线", "客户", "面试", "汇报", "需求"),
        "学习" to listOf("看书", "读书", "背单词", "复习", "作业", "论文", "课程", "考试", "学习", "练题"),
        "生活" to listOf("买菜", "做饭", "洗", "打扫", "缴费", "水电", "房租", "快递", "剪", "健身", "跑步", "打球", "医院", "体检", "取", "还")
    )

    private val weekdayNames = listOf(
        "周日" to 1, "周天" to 1, "周一" to 2, "周二" to 3, "周三" to 4,
        "周四" to 5, "周五" to 6, "周六" to 7,
        "星期日" to 1, "星期一" to 2, "星期二" to 3, "星期三" to 4,
        "星期四" to 5, "星期五" to 6, "星期六" to 7
    )

    /** 中文数字 → 阿拉伯数字，只为「下午三点」「八点」这类时间服务。 */
    private val cnDigits = mapOf(
        '零' to 0, '一' to 1, '两' to 2, '二' to 2, '三' to 3, '四' to 4,
        '五' to 5, '六' to 6, '七' to 7, '八' to 8, '九' to 9, '十' to 10
    )

    private fun cnToInt(s: String): Int? {
        if (s.isEmpty()) return null
        s.toIntOrNull()?.let { return it }
        if (s.length == 1) return cnDigits[s[0]]
        // 十一 / 十二 / 二十 / 二十三
        if (s[0] == '十') return 10 + (cnDigits[s.getOrNull(1)] ?: return null)
        if (s.length == 2 && s[1] == '十') return (cnDigits[s[0]] ?: return null) * 10
        if (s.length == 3 && s[1] == '十') {
            val a = cnDigits[s[0]] ?: return null
            val b = cnDigits[s[2]] ?: return null
            return a * 10 + b
        }
        return null
    }

    fun parse(raw: String, now: Long = System.currentTimeMillis()): ParseResult {
        val input = raw.trim()
        if (input.isEmpty()) return ParseResult(null, "写点什么吧，比如「明天下午三点开会」")

        val due = parseDay(input, now)
        val minute = parseTime(input, due != null)
        val important = listOf("重要", "急", "!!").any { input.contains(it) }
        val group = groups.firstOrNull { (_, words) -> words.any { input.contains(it) } }?.first
            ?: Todo.DEFAULT_GROUP
        val title = cleanTitle(input)

        if (title.isEmpty()) {
            return ParseResult(null, "没看懂要做什么，再写一次？")
        }

        val draft = Todo(
            title = title,
            due = due,
            time = minute,
            important = important,
            group = group
        )

        val when_ = when {
            due == null -> "没排期，放进收集箱"
            minute != null -> Dates.formatDateShort(due) + " " + fmtMinute(minute)
            else -> Dates.formatDateShort(due) + " 全天"
        }
        return ParseResult(draft, "这样对吗 · $when_")
    }

    fun fmtMinute(minute: Int): String =
        String.format("%02d:%02d", (minute / 60) % 24, minute % 60)

    /** 找出日期（不含时间）。返回当天 0 点。 */
    fun parseDay(input: String, now: Long): Long? {
        val today = Dates.startOfDay(now)

        if (input.contains("今天") || input.contains("今晚") || input.contains("今早")) return today
        if (input.contains("后天")) return Dates.addDays(today, 2)
        if (input.contains("明天") || input.contains("明早") || input.contains("明晚")) {
            return Dates.addDays(today, 1)
        }
        if (input.contains("大后天")) return Dates.addDays(today, 3)
        if (input.contains("昨天")) return Dates.addDays(today, -1)

        Regex("""(\d{1,2})月(\d{1,2})[日号]""").find(input)?.let { m ->
            val month = m.groupValues[1].toIntOrNull()
            val day = m.groupValues[2].toIntOrNull()
            if (month != null && day != null && month in 1..12 && day in 1..31) {
                val year = Dates.year(now)
                var t = Dates.dayMillis(year, month - 1, day)
                // 已经过去超过半年的，算明年
                if (t < Dates.addDays(today, -180)) t = Dates.dayMillis(year + 1, month - 1, day)
                return Dates.startOfDay(t)
            }
        }
        Regex("""(\d{1,2})[日号]""").find(input)?.let { m ->
            val day = m.groupValues[1].toIntOrNull()
            if (day != null && day in 1..31) {
                val c = Dates.calendar().apply { timeInMillis = today }
                c.set(Calendar.DAY_OF_MONTH, day)
                var t = Dates.startOfDay(c.timeInMillis)
                if (t < Dates.addDays(today, -2)) {
                    val c2 = Dates.calendar().apply { timeInMillis = t }
                    c2.add(Calendar.MONTH, 1)
                    t = Dates.startOfDay(c2.timeInMillis)
                }
                return t
            }
        }

        // 下周一 / 周三 / 星期五。Calendar 里周日=1..周六=7，这里先换成 周一=0..周日=6
        weekdayNames
            .sortedByDescending { it.first.length }
            .firstOrNull { input.contains(it.first) }
            ?.let { (_, dow) ->
                val c = Dates.calendar().apply { timeInMillis = today }
                val isoToday = (c.get(Calendar.DAY_OF_WEEK) + 5) % 7
                val isoTarget = (dow + 5) % 7
                val nextWeek = input.contains("下个") || input.contains("下周")
                val delta = if (nextWeek) {
                    7 - isoToday + isoTarget
                } else {
                    (isoTarget - isoToday + 7) % 7
                }
                return Dates.addDays(today, delta)
            }

        // 「3天后」
        Regex("""(\d{1,2})\s*天后""").find(input)?.let { m ->
            m.groupValues[1].toIntOrNull()?.let { return Dates.addDays(today, it) }
        }
        return null
    }

    /** 找出时刻，返回当天第几分钟。 */
    fun parseTime(input: String, hasDay: Boolean): Int? {
        val afternoon = listOf("下午", "傍晚", "晚上", "晚", "今晚", "明晚").any { input.contains(it) }
        val noon = input.contains("中午")
        val morning = listOf("早上", "上午", "早晨", "一早", "今早", "明早").any { input.contains(it) }

        // 14:30 / 14点30 / 14点 / 14:00
        Regex("""(\d{1,2})\s*[:：]\s*(\d{2})""").find(input)?.let { m ->
            val h = m.groupValues[1].toIntOrNull() ?: return@let
            val mi = m.groupValues[2].toIntOrNull() ?: 0
            return clampMinute(adjust(h, afternoon, noon, morning), mi)
        }
        // 三点 / 三点半 / 三点一刻 / 三点二十 / 3点30分
        Regex("""([0-9]{1,2}|[零一两二三四五六七八九十]{1,3})\s*[点时]""").find(input)?.let { m ->
            val h = cnToInt(m.groupValues[1]) ?: return@let
            val rest = input.substring(m.range.last + 1)
            val mi = when {
                rest.startsWith("半") -> 30
                rest.startsWith("一刻") -> 15
                rest.startsWith("三刻") -> 45
                else -> Regex("""^([0-9]{1,2}|[一二三四五六七八九十]{1,3})\s*分?""").find(rest)
                    ?.let { cnToInt(it.groupValues[1]) } ?: 0
            }
            return clampMinute(adjust(h, afternoon, noon, morning), mi)
        }
        // 只有「上午/下午」没写点：给个默认
        if (hasDay) {
            if (afternoon) return 15 * 60
            if (noon) return 12 * 60
            if (morning) return 9 * 60
        }
        return null
    }

    private fun adjust(h: Int, afternoon: Boolean, noon: Boolean, morning: Boolean): Int = when {
        afternoon && h in 1..11 -> h + 12
        noon && h in 1..10 -> h + 12
        !morning && !afternoon && !noon && h in 1..6 -> h + 12 // 「三点」默认下午
        else -> h
    }

    private fun clampMinute(h: Int, m: Int): Int =
        ((h.coerceIn(0, 23)) * 60 + m.coerceIn(0, 59))

    /** 标题 = 原句去掉日期/时刻/语气词/分组词。 */
    fun cleanTitle(input: String): String {
        var s = input

        // 先整体吃掉「下周一 / 本周五 / 星期一」这类复合词，避免「下周」把「周一」的「周」切掉
        s = Regex("""(下个|下|本|这|上)?(周|星期)[日天一二三四五六]""").replace(s, " ")
        // 日期与时刻
        s = Regex("""\d{1,2}月\d{1,2}[日号]""").replace(s, " ")
        s = Regex("""\d{1,2}[日号]""").replace(s, " ")
        s = Regex("""\d{1,2}\s*天后""").replace(s, " ")
        s = Regex("""\d{1,2}\s*[:：]\s*\d{2}""").replace(s, " ")
        s = Regex("""([0-9]{1,2}|[零一两二三四五六七八九十]{1,3})\s*[点时](半|一刻|三刻|[零一两二三四五六七八九十]{1,3}分?)?""").replace(s, " ")

        // 语气词与口语填充词（长词在前，避免「重要」被「要」吃掉）
        val drop = listOf(
            "大后天", "后天", "明天", "今天", "昨天",
            "明晚", "今晚", "明早", "今早",
            "早上", "上午", "中午", "下午", "傍晚", "晚上", "早晨", "一早",
            "下个", "下周", "本周", "这周",
            "提醒我", "提醒", "记得", "别忘了", "别忘", "帮我", "帮我记", "一下",
            "重要", "要去", "想要", "需要", "要去"
        )
        drop.forEach { s = s.replace(it, " ") }

        s = Regex("""\s+""").replace(s, " ").trim()
        s = s.trim('，', ',', '。', '、', ' ', '：', ':', '；', ';')
        return s
    }
}
