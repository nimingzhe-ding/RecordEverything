package me.nimingzhe.recordeverything

import me.nimingzhe.recordeverything.nl.ChatParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/**
 * 中文口语输入的解析用例。纯 JVM 测试，不依赖 Android。
 * 固定“现在”为 2026-11-23 12:00，避免测试随日期漂移。
 */
class ChatParserTest {

    private val now: Long = run {
        val c = Calendar.getInstance()
        c.set(2026, Calendar.NOVEMBER, 23, 12, 0, 0)
        c.set(Calendar.MILLISECOND, 0)
        c.timeInMillis
    }

    private fun day(millis: Long): Int {
        val c = Calendar.getInstance()
        c.timeInMillis = millis
        return c.get(Calendar.DAY_OF_MONTH)
    }

    @Test
    fun `午饭金额渠道`() {
        val r = ChatParser.parse("昨天午饭 25 微信", now)
        val d = r.draft
        assertNotNull(d)
        assertEquals(25.0, d!!.amount!!, 0.001)
        assertEquals("餐饮", d.category)
        assertEquals("微信", d.channel)
        assertEquals("昨天", 22, day(d.time))
    }

    @Test
    fun `带块和元`() {
        assertEquals(18.0, ChatParser.parse("打车 18 块", now).draft!!.amount!!, 0.001)
        assertEquals(30.0, ChatParser.parse("电影 30 元", now).draft!!.amount!!, 0.001)
    }

    @Test
    fun `万与k`() {
        assertEquals(17000.0, ChatParser.parse("房租 1.7万", now).draft!!.amount!!, 0.001)
        assertEquals(1200.0, ChatParser.parse("耳机 1.2k 支付宝", now).draft!!.amount!!, 0.001)
    }

    @Test
    fun `收入识别`() {
        val r = ChatParser.parse("工资 8000 银行卡", now)
        assertTrue(r.draft!!.income)
        assertEquals("工资", r.draft!!.category)
        assertEquals("银行卡", r.draft!!.channel)
        assertEquals("这笔收入看着对吗", r.reply)
    }

    @Test
    fun `没有金额也要给提示`() {
        val r = ChatParser.parse("今天心情不错", now)
        assertNull(r.draft)
        assertTrue(r.reply.contains("金额"))
    }

    @Test
    fun `结余单独成一类`() {
        val r = ChatParser.parse("微信现在结余 530.46", now)
        assertNotNull(r.draft)
        assertEquals(530.46, r.draft!!.amount!!, 0.001)
        assertEquals(me.nimingzhe.recordeverything.data.DraftKind.BALANCE, r.draft!!.kind)
    }

    @Test
    fun `体重走通用事件`() {
        val r = ChatParser.parse("体重 62.5", now)
        assertNotNull(r.draft)
        assertEquals("体重", r.draft!!.eventName)
        assertEquals(62.5, r.draft!!.amount!!, 0.001)
    }

    @Test
    fun `日期写成月日`() {
        val r = ChatParser.parse("10月3日 房租 900", now)
        val c = Calendar.getInstance()
        c.timeInMillis = r.draft!!.time
        assertEquals(Calendar.OCTOBER, c.get(Calendar.MONTH))
        assertEquals(3, c.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `空白输入不崩`() {
        val r = ChatParser.parse("   ", now)
        assertNull(r.draft)
        assertTrue(r.reply.isNotEmpty())
    }
}
