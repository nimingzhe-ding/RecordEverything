package me.nimingzhe.recordeverything.util

import java.util.Calendar
import java.util.Locale

object Dates {

    private val weekdayNames = arrayOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")

    fun calendar(): Calendar = Calendar.getInstance(Locale.CHINA)

    fun startOfDay(millis: Long): Long {
        val c = calendar()
        c.timeInMillis = millis
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    fun addDays(millis: Long, days: Int): Long {
        val c = calendar()
        c.timeInMillis = millis
        c.add(Calendar.DAY_OF_MONTH, days)
        return c.timeInMillis
    }

    fun year(millis: Long): Int = calendar().apply { timeInMillis = millis }.get(Calendar.YEAR)

    fun month0(millis: Long): Int = calendar().apply { timeInMillis = millis }.get(Calendar.MONTH)

    fun dayOfMonth(millis: Long): Int = calendar().apply { timeInMillis = millis }.get(Calendar.DAY_OF_MONTH)

    fun daysBetween(a: Long, b: Long): Int =
        Math.round((startOfDay(b) - startOfDay(a)) / 86_400_000.0).toInt()

    fun formatDate(millis: Long): String {
        val c = calendar().apply { timeInMillis = millis }
        return "%d年%d月%d日".format(
            c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun formatDateShort(millis: Long): String {
        val c = calendar().apply { timeInMillis = millis }
        return "%d月%d日".format(c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
    }

    fun formatTime(millis: Long): String {
        val c = calendar().apply { timeInMillis = millis }
        return String.format(Locale.US, "%02d:%02d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE))
    }

    fun weekdayName(millis: Long): String {
        val c = calendar().apply { timeInMillis = millis }
        return weekdayNames[(c.get(Calendar.DAY_OF_WEEK) - 1).coerceIn(0, 6)]
    }

    /** 聊天气泡上的日期标签：今天 / 昨天 / 前天 / 10月3日。 */
    fun chip(millis: Long, now: Long = System.currentTimeMillis()): String {
        val diff = daysBetween(millis, now)
        return when (diff) {
            0 -> "今天"
            1 -> "昨天"
            2 -> "前天"
            -1 -> "明天"
            -2 -> "后天"
            -3 -> "大后天"
            else -> formatDateShort(millis)
        }
    }

    fun monthTitle(year: Int, month0: Int): String = "${year}年${month0 + 1}月"

    fun daysInMonth(year: Int, month0: Int): Int {
        val c = calendar()
        c.clear()
        c.set(year, month0, 1)
        return c.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    /** 返回该月 1 号是周几，用 Calendar.SUNDAY..SATURDAY（1..7）。 */
    fun firstWeekday(year: Int, month0: Int): Int {
        val c = calendar()
        c.clear()
        c.set(year, month0, 1)
        return c.get(Calendar.DAY_OF_WEEK)
    }

    fun dayMillis(year: Int, month0: Int, day: Int): Long {
        val c = calendar()
        c.clear()
        c.set(year, month0, day, 12, 0, 0)
        return c.timeInMillis
    }

    fun monthStart(year: Int, month0: Int): Long = dayMillis(year, month0, 1)

    fun nextMonth(year: Int, month0: Int): Pair<Int, Int> =
        if (month0 == 11) year + 1 to 0 else year to month0 + 1

    fun prevMonth(year: Int, month0: Int): Pair<Int, Int> =
        if (month0 == 0) year - 1 to 11 else year to month0 - 1

    /** 连续记录天数（今天或昨天有记录才会计入）。 */
    fun streak(times: List<Long>): Int {
        val days = times.map { startOfDay(it) }.distinct().sortedDescending()
        if (days.isEmpty()) return 0
        var cursor = startOfDay(System.currentTimeMillis())
        if (days.first() != cursor) {
            cursor = addDays(cursor, -1)
            if (days.first() != cursor) return 0
        }
        var count = 0
        for (d in days) {
            if (d == cursor) {
                count++
                cursor = addDays(cursor, -1)
            } else if (d < cursor) {
                break
            }
        }
        return count
    }
}
