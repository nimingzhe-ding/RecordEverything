package me.nimingzhe.recordeverything

import me.nimingzhe.recordeverything.nl.TodoParser
import me.nimingzhe.recordeverything.util.Dates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TodoParserTest {

    // 2026-11-23 周一 12:00
    private val now = 1_795_406_400_000L

    @Test fun todayAllDay() {
        val r = TodoParser.parse("今天交房租", now)
        assertNotNull(r.draft)
        assertEquals("交房租", r.draft!!.title)
        assertEquals(Dates.startOfDay(now), r.draft!!.due)
        assertNull(r.draft!!.time)
    }

    @Test fun tomorrowAfternoon() {
        val r = TodoParser.parse("明天下午三点开会", now)
        val d = r.draft!!
        assertEquals("开会", d.title)
        assertEquals(Dates.addDays(Dates.startOfDay(now), 1), d.due)
        assertEquals(15 * 60, d.time)
        assertEquals("工作", d.group)
    }

    @Test fun eveningDefault() {
        val r = TodoParser.parse("今晚八点写周报", now)
        val d = r.draft!!
        assertEquals(20 * 60, d.time)
        assertEquals(Dates.startOfDay(now), d.due)
    }

    @Test fun explicitColonTime() {
        val r = TodoParser.parse("后天 14:30 体检", now)
        val d = r.draft!!
        assertEquals(14 * 60 + 30, d.time)
        assertEquals(Dates.addDays(Dates.startOfDay(now), 2), d.due)
        assertEquals("生活", d.group)
    }

    @Test fun weekdayNextWeek() {
        val r = TodoParser.parse("下周一交报告", now)
        val d = r.draft!!
        assertEquals("交报告", d.title)
        assertTrue(d.due!! > Dates.startOfDay(now))
        assertEquals(7, Dates.daysBetween(Dates.startOfDay(now), d.due!!))
    }

    @Test fun noDateGoesToInbox() {
        val r = TodoParser.parse("买一台咖啡机", now)
        assertNull(r.draft!!.due)
        assertNull(r.draft!!.time)
        assertEquals("买一台咖啡机", r.draft!!.title)
    }

    @Test fun monthDay() {
        val r = TodoParser.parse("12月3日 交材料", now)
        val d = r.draft!!
        assertEquals(11, Dates.month0(d.due!!))
        assertEquals(3, Dates.dayOfMonth(d.due!!))
    }

    @Test fun importantFlag() {
        val r = TodoParser.parse("明天重要 面谈", now)
        assertTrue(r.draft!!.important)
        assertEquals("面谈", r.draft!!.title)
    }

    @Test fun blankInputGivesHint() {
        val r = TodoParser.parse("   ", now)
        assertNull(r.draft)
        assertTrue(r.hint.isNotEmpty())
    }

    @Test fun minuteFormatter() {
        assertEquals("09:05", TodoParser.fmtMinute(9 * 60 + 5))
        assertEquals("00:00", TodoParser.fmtMinute(0))
    }
}
