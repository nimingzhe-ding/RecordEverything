package me.nimingzhe.recordeverything.util

import android.icu.util.ChineseCalendar
import java.util.Calendar

/** 农历（API 24+ 的 android.icu.util.ChineseCalendar 提供，免维护查找表）。 */
object Lunar {

    private val dayNames = listOf(
        "初一", "初二", "初三", "初四", "初五", "初六", "初七", "初八", "初九", "初十",
        "十一", "十二", "十三", "十四", "十五", "十六", "十七", "十八", "十九", "二十",
        "廿一", "廿二", "廿三", "廿四", "廿五", "廿六", "廿七", "廿八", "廿九", "三十"
    )

    private val monthNames = listOf(
        "正月", "二月", "三月", "四月", "五月", "六月",
        "七月", "八月", "九月", "十月", "冬月", "腊月"
    )

    /** 日历格子里的农历小字：初一显示月份，其余显示日。 */
    fun short(millis: Long): String = try {
        val cc = ChineseCalendar()
        cc.timeInMillis = millis
        val day = cc.get(Calendar.DAY_OF_MONTH)
        val month = cc.get(Calendar.MONTH)
        if (day == 1) monthNames[month.coerceIn(0, 11)] else dayNames[(day - 1).coerceIn(0, 29)]
    } catch (t: Throwable) {
        ""
    }

    fun full(millis: Long): String = try {
        val cc = ChineseCalendar()
        cc.timeInMillis = millis
        val day = cc.get(Calendar.DAY_OF_MONTH)
        val month = cc.get(Calendar.MONTH)
        val leap = cc.get(ChineseCalendar.IS_LEAP_MONTH) == 1
        "农历" + (if (leap) "闰" else "") + monthNames[month.coerceIn(0, 11)] + dayNames[(day - 1).coerceIn(0, 29)]
    } catch (t: Throwable) {
        ""
    }
}
