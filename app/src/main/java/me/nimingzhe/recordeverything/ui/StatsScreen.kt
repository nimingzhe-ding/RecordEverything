package me.nimingzhe.recordeverything.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.nimingzhe.recordeverything.data.Ledger
import me.nimingzhe.recordeverything.util.Dates

@Composable
fun StatsScreen(state: UiState, onPrevMonth: () -> Unit, onNextMonth: () -> Unit) {
    val totals = remember(state.records, state.year, state.month0) {
        val events = state.events.associateBy { it.id }
        val out = HashMap<Int, Pair<Double, Double>>()
        state.records.forEach { r ->
            if (Dates.year(r.time) != state.year || Dates.month0(r.time) != state.month0) return@forEach
            val ev = events[r.eventId] ?: return@forEach
            if (!Ledger.isMoney(ev)) return@forEach
            val amount = Ledger.amount(r, ev) ?: return@forEach
            val day = Dates.dayOfMonth(r.time)
            val prev = out[day] ?: (0.0 to 0.0)
            out[day] = if (Ledger.isIncome(r, ev)) prev.first to (prev.second + amount)
            else (prev.first - amount) to prev.second
        }
        out
    }
    val spend = -totals.values.sumOf { it.first }
    val income = totals.values.sumOf { it.second }
    val balance = income - spend

    val categories = remember(state.records, state.year, state.month0) { categoryTotals(state) }
    val channels = remember(state.records, state.year, state.month0) { channelTotals(state) }
    val days = remember(state.records, state.year, state.month0) { dayList(state) }
    val maxDay = days.maxOfOrNull { it.second }?.takeIf { it > 0 } ?: 1.0

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 30.dp)
    ) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("统计", color = Palette.Text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        Dates.monthTitle(state.year, state.month0),
                        color = Palette.TextDim, fontSize = 11.sp
                    )
                }
                Chip("‹", onClick = onPrevMonth)
                Spacer(Modifier.width(8.dp))
                Chip("›", onClick = onNextMonth)
            }
        }

        item {
            Column(
                Modifier
                    .padding(horizontal = 14.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.verticalGradient(listOf(Palette.SurfaceHi, Palette.Surface)))
                    .padding(18.dp)
            ) {
                Text("本月结余", color = Palette.TextDim, fontSize = 11.sp)
                Spacer(Modifier.height(6.dp))
                Text(
                    (if (balance >= 0) "+" else "-") + "¥" + Ledger.money(kotlin.math.abs(balance)),
                    color = if (balance >= 0) Palette.Income else Palette.Expense,
                    fontSize = 30.sp, fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(14.dp))
                Row {
                    Column(Modifier.weight(1f)) {
                        Text("支出", color = Palette.TextDim, fontSize = 10.sp)
                        Text("¥" + Ledger.money(spend), color = Palette.Expense, fontSize = 15.sp)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("收入", color = Palette.TextDim, fontSize = 10.sp)
                        Text("¥" + Ledger.money(income), color = Palette.Income, fontSize = 15.sp)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("笔数", color = Palette.TextDim, fontSize = 10.sp)
                        Text("${days.sumOf { it.third }}", color = Palette.Text, fontSize = 15.sp)
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(18.dp))
            Text(
                "每日支出", modifier = Modifier.padding(horizontal = 18.dp),
                color = Palette.Text, fontSize = 14.sp, fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(10.dp))
            BarChart(days, maxDay)
        }

        if (categories.isNotEmpty()) {
            item {
                Spacer(Modifier.height(20.dp))
                Text(
                    "花在哪了", modifier = Modifier.padding(horizontal = 18.dp),
                    color = Palette.Text, fontSize = 14.sp, fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(10.dp))
            }
            items(categories.size) { i ->
                val (name, value) = categories[i]
                val pct = if (spend > 0) value / spend else 0.0
                CategoryRow(name, value, pct)
            }
        }

        if (channels.isNotEmpty()) {
            item {
                Spacer(Modifier.height(20.dp))
                Text(
                    "从哪花出去", modifier = Modifier.padding(horizontal = 18.dp),
                    color = Palette.Text, fontSize = 14.sp, fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(10.dp))
            }
            items(channels.size) { i ->
                val (name, value) = channels[i]
                val pct = if (spend > 0) value / spend else 0.0
                ChannelRow(name, value, pct)
            }
        }

        if (categories.isEmpty()) {
            item { EmptyHint("这个月还没有账，去聊天页记一笔吧") }
        }
    }
}

@Composable
private fun BarChart(days: List<Triple<Int, Double, Int>>, maxValue: Double) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(120.dp)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        days.forEach { (_, value, _) ->
            val ratio = (value / maxValue).coerceIn(0.0, 1.0).toFloat()
            Box(
                Modifier
                    .weight(1f)
                    .height((112 * ratio).dp.coerceAtLeast(2.dp))
                    .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                    .background(
                        if (ratio > 0.66f) Palette.Gold
                        else if (ratio > 0.33f) Palette.Gold.copy(alpha = 0.7f)
                        else Palette.Gold.copy(alpha = 0.4f)
                    )
            )
        }
    }
}

@Composable
private fun CategoryRow(name: String, value: Double, pct: Double) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Palette.Surface)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(32.dp).clip(RoundedCornerShape(9.dp)).background(Palette.SurfaceHi),
            contentAlignment = Alignment.Center
        ) { Text(categoryEmoji(name), fontSize = 14.sp) }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name, color = Palette.Text, fontSize = 14.sp)
                Spacer(Modifier.weight(1f))
                Text("¥" + Ledger.money(value), color = Palette.Text, fontSize = 13.sp)
            }
            Spacer(Modifier.height(6.dp))
            Box(
                Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp))
                    .background(Palette.SurfaceHi)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(pct.toFloat().coerceIn(0f, 1f))
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Palette.Gold.copy(alpha = 0.85f))
                )
            }
        }
    }
}

@Composable
private fun ChannelRow(name: String, value: Double, pct: Double) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Palette.Surface)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(channelEmoji(name), fontSize = 15.sp)
        Spacer(Modifier.width(10.dp))
        Text(name, color = Palette.Text, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text("${(pct * 100).toInt()}%", color = Palette.TextDim, fontSize = 11.sp)
        Spacer(Modifier.width(10.dp))
        Text("¥" + Ledger.money(value), color = Palette.Text, fontSize = 13.sp)
    }
}

private fun categoryTotals(state: UiState): List<Pair<String, Double>> {
    val events = state.events.associateBy { it.id }
    val map = LinkedHashMap<String, Double>()
    state.records.forEach { r ->
        if (Dates.year(r.time) != state.year || Dates.month0(r.time) != state.month0) return@forEach
        val ev = events[r.eventId] ?: return@forEach
        if (!Ledger.isMoney(ev) || Ledger.isIncome(r, ev)) return@forEach
        val amount = Ledger.amount(r, ev) ?: return@forEach
        val key = Ledger.categoryName(r, ev).ifBlank { "其他" }
        map[key] = (map[key] ?: 0.0) + amount
    }
    return map.entries.sortedByDescending { it.value }.map { it.key to it.value }
}

private fun channelTotals(state: UiState): List<Pair<String, Double>> {
    val events = state.events.associateBy { it.id }
    val map = LinkedHashMap<String, Double>()
    state.records.forEach { r ->
        if (Dates.year(r.time) != state.year || Dates.month0(r.time) != state.month0) return@forEach
        val ev = events[r.eventId] ?: return@forEach
        if (!Ledger.isMoney(ev) || Ledger.isIncome(r, ev)) return@forEach
        val amount = Ledger.amount(r, ev) ?: return@forEach
        val key = Ledger.channelName(r, ev).ifBlank { "未标注" }
        map[key] = (map[key] ?: 0.0) + amount
    }
    return map.entries.sortedByDescending { it.value }.map { it.key to it.value }
}

/** 当月每天的支出（正数）与笔数。 */
private fun dayList(state: UiState): List<Triple<Int, Double, Int>> {
    val events = state.events.associateBy { it.id }
    val map = HashMap<Int, Pair<Double, Int>>()
    val daysInMonth = Dates.daysInMonth(state.year, state.month0)
    state.records.forEach { r ->
        if (Dates.year(r.time) != state.year || Dates.month0(r.time) != state.month0) return@forEach
        val ev = events[r.eventId] ?: return@forEach
        if (!Ledger.isMoney(ev) || Ledger.isIncome(r, ev)) return@forEach
        val amount = Ledger.amount(r, ev) ?: return@forEach
        val day = Dates.dayOfMonth(r.time)
        val prev = map[day] ?: (0.0 to 0)
        map[day] = (prev.first + amount) to (prev.second + 1)
    }
    return (1..daysInMonth).map { d ->
        val v = map[d] ?: (0.0 to 0)
        Triple(d, v.first, v.second)
    }
}
