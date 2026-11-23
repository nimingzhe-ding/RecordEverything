package me.nimingzhe.recordeverything.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.nimingzhe.recordeverything.data.Draft
import me.nimingzhe.recordeverything.data.DraftKind
import me.nimingzhe.recordeverything.data.Ledger
import me.nimingzhe.recordeverything.data.Record
import me.nimingzhe.recordeverything.nl.ChatParser
import me.nimingzhe.recordeverything.util.Dates
import me.nimingzhe.recordeverything.util.Lunar

private val weekLabels = listOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")

@Composable
fun CalendarScreen(
    state: UiState,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDay: (Long?) -> Unit,
    onToday: () -> Unit,
    onDelete: (Record) -> Unit,
    onQuickAdd: (Double, String, String?, Long, String, Boolean) -> Unit
) {
    val totals = remember(state.records, state.year, state.month0) { buildTotals(state) }
    val spend = totals.values.sumOf { it.first }
    val income = totals.values.sumOf { it.second }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        Dates.monthTitle(state.year, state.month0),
                        color = Palette.Text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("支出 ", color = Palette.TextDim, fontSize = 11.sp)
                        Text("¥" + Ledger.money(-spend), color = Palette.Expense, fontSize = 11.sp)
                        Spacer(Modifier.width(8.dp))
                        Text("收入 ", color = Palette.TextDim, fontSize = 11.sp)
                        Text("¥" + Ledger.money(income), color = Palette.Income, fontSize = 11.sp)
                    }
                }
                NavRound("‹", onPrevMonth)
                Spacer(Modifier.width(8.dp))
                NavRound("›", onNextMonth)
            }
        }

        item {
            Column(Modifier.padding(horizontal = 12.dp)) {
                Row(Modifier.fillMaxWidth()) {
                    weekLabels.forEach { label ->
                        Text(
                            label,
                            modifier = Modifier.weight(1f),
                            color = Palette.TextDim,
                            fontSize = 11.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                MonthGrid(
                    state = state,
                    totals = totals,
                    onSelectDay = onSelectDay
                )
            }
        }

        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val day = state.selectedDay
                Text(
                    if (day != null) Dates.formatDate(day) + " " + Dates.weekdayName(day) else "点一个日子看明细",
                    color = Palette.Text, fontSize = 14.sp, fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.weight(1f))
                Chip("回到今天", icon = "↺", onClick = onToday)
            }
        }

        val day = state.selectedDay
        if (day != null) {
            val dayRecords = state.records.filter {
                it.time >= Dates.startOfDay(day) && it.time < Dates.addDays(Dates.startOfDay(day), 1)
            }
            if (dayRecords.isEmpty()) {
                item { EmptyHint("这天还没有记录") }
            } else {
                items(dayRecords, key = { it.id }) { r ->
                    DayRecordRow(r, state, onDelete)
                }
            }
            item {
                QuickAddBlock(day) { amount, category, channel, time, remark, income ->
                    onQuickAdd(amount, category, channel, time, remark, income)
                }
            }
        } else {
            item { EmptyHint("从上面选一天，看当天的每一笔") }
        }
    }
}

@Composable
private fun NavRound(text: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(Palette.Gold)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) { Text(text, color = Color(0xFF221A0C), fontSize = 15.sp, fontWeight = FontWeight.Bold) }
}

@Composable
private fun MonthGrid(
    state: UiState,
    totals: Map<Int, Pair<Double, Double>>,
    onSelectDay: (Long?) -> Unit
) {
    val firstDow = Dates.firstWeekday(state.year, state.month0) // 1=周日
    val daysInMonth = Dates.daysInMonth(state.year, state.month0)
    val lead = firstDow - 1
    val totalCells = ((lead + daysInMonth + 6) / 7) * 7
    val today = Dates.startOfDay(System.currentTimeMillis())

    Column {
        var index = 0
        while (index < totalCells) {
            Row(Modifier.fillMaxWidth()) {
                for (i in 0 until 7) {
                    val cell = index + i
                    val day = cell - lead + 1
                    Box(Modifier.weight(1f).height(62.dp).padding(2.dp)) {
                        if (day in 1..daysInMonth) {
                            DayCell(
                                state = state,
                                day = day,
                                totals = totals,
                                today = today,
                                onSelectDay = onSelectDay
                            )
                        }
                    }
                }
            }
            index += 7
        }
    }
}

@Composable
private fun DayCell(
    state: UiState,
    day: Int,
    totals: Map<Int, Pair<Double, Double>>,
    today: Long,
    onSelectDay: (Long?) -> Unit
) {
    val millis = Dates.dayMillis(state.year, state.month0, day)
    val isToday = Dates.startOfDay(millis) == today
    val selected = state.selectedDay?.let { Dates.startOfDay(it) == Dates.startOfDay(millis) } == true
    val (spend, income) = totals[day] ?: (0.0 to 0.0)
    val hasSpend = spend != 0.0
    val hasIncome = income != 0.0

    Column(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(10.dp))
            .background(
                when {
                    selected -> Palette.Gold.copy(alpha = 0.14f)
                    isToday -> Palette.SurfaceHi
                    else -> Color.Transparent
                }
            )
            .then(
                if (selected) Modifier.border(0.8.dp, Palette.Gold.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                else Modifier
            )
            .clickable { onSelectDay(millis) }
            .padding(vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            day.toString(),
            color = if (isToday) Palette.Gold else Palette.Text,
            fontSize = 13.sp,
            lineHeight = 15.sp,
            fontWeight = if (isToday) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1
        )
        Text(
            Lunar.short(millis),
            color = Palette.TextDim.copy(alpha = 0.7f),
            fontSize = 7.sp,
            lineHeight = 9.sp,
            maxLines = 1
        )
        Text(
            if (hasSpend) shortMoney(spend) else "",
            color = Palette.Expense,
            fontSize = 9.sp,
            lineHeight = 11.sp,
            maxLines = 1,
            fontWeight = FontWeight.Medium
        )
        Text(
            if (hasIncome) shortMoney(income) else "",
            color = Palette.Income,
            fontSize = 9.sp,
            lineHeight = 11.sp,
            maxLines = 1
        )
    }
}

/** 日历格子里的紧凑金额：1.7w / 937 / 0。 */
private fun shortMoney(v: Double): String {
    val abs = kotlin.math.abs(v)
    return when {
        abs == 0.0 -> "0"
        abs >= 10_000 -> String.format("%.1fw", abs / 10_000).replace(".0w", "w")
        abs >= 1_000 -> String.format("%.0f", abs)
        abs % 1.0 == 0.0 -> String.format("%.0f", abs)
        else -> String.format("%.1f", abs)
    }
}

private fun buildTotals(state: UiState): Map<Int, Pair<Double, Double>> {
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
    return out
}

@Composable
private fun DayRecordRow(record: Record, state: UiState, onDelete: (Record) -> Unit) {
    val event = state.events.firstOrNull { it.id == record.eventId }
    val amount = Ledger.amount(record, event)
    val income = Ledger.isIncome(record, event)
    val category = Ledger.categoryName(record, event).ifBlank {
        event?.name ?: "记录"
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Palette.Surface)
            .padding(start = 14.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Palette.SurfaceHi),
            contentAlignment = Alignment.Center
        ) { Text(categoryEmoji(category), fontSize = 16.sp) }

        Spacer(Modifier.width(10.dp))

        Column(Modifier.weight(1f)) {
            Text(category, color = Palette.Text, fontSize = 14.sp)
            Spacer(Modifier.height(2.dp))
            Text(
                buildString {
                    append(Dates.formatTime(record.time))
                    val ch = Ledger.channelName(record, event)
                    if (ch.isNotBlank()) append(" · ").append(ch)
                    if (record.remark.isNotBlank()) append(" · ").append(record.remark)
                },
                color = Palette.TextDim, fontSize = 11.sp, maxLines = 1
            )
        }

        if (amount != null) {
            Text(
                (if (income) "+" else "-") + "¥" + Ledger.money(amount),
                color = if (income) Palette.Income else Palette.Expense,
                fontSize = 14.sp, fontWeight = FontWeight.Medium
            )
        }
        Box(
            Modifier
                .size(30.dp)
                .clip(CircleShape)
                .clickable { onDelete(record) },
            contentAlignment = Alignment.Center
        ) {
            Text("✕", color = Palette.TextDim.copy(alpha = 0.6f), fontSize = 12.sp)
        }
    }
}

@Composable
private fun QuickAddBlock(
    dayMillis: Long,
    onAdd: (Double, String, String?, Long, String, Boolean) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }

    Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
        if (!expanded) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Palette.Gold.copy(alpha = 0.12f))
                    .clickable { expanded = true }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("+ 用一句话补记这一天", color = Palette.Gold, fontSize = 13.sp)
            }
        } else {
            SectionCard(Modifier.fillMaxWidth()) {
                Column {
                    Text("补记 ${Dates.formatDateShort(dayMillis)}", color = Palette.Text, fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Palette.SurfaceHi)
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        if (text.isEmpty()) {
                            Text("午饭 25 微信", color = Palette.TextDim, fontSize = 12.sp)
                        }
                        androidx.compose.foundation.text.BasicTextField(
                            value = text,
                            onValueChange = { text = it },
                            textStyle = androidx.compose.ui.text.TextStyle(color = Palette.Text, fontSize = 13.sp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Spacer(Modifier.weight(1f))
                        Chip("取消", onClick = { expanded = false; text = "" })
                        Chip("记下", selected = true, onClick = {
                            val draft = ChatParser.parse(text, dayMillis).draft
                            if (draft != null) {
                                onAdd(
                                    draft.amount ?: 0.0,
                                    draft.category,
                                    draft.channel,
                                    dayMillis,
                                    draft.remark,
                                    draft.income
                                )
                            }
                            expanded = false
                            text = ""
                        })
                    }
                }
            }
        }
    }
}
