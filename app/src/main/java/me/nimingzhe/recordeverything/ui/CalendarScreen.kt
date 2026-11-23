package me.nimingzhe.recordeverything.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.nimingzhe.recordeverything.data.Todo
import me.nimingzhe.recordeverything.util.Dates
import me.nimingzhe.recordeverything.util.Lunar

private val weekHeaders = listOf("日", "一", "二", "三", "四", "五", "六")

/**
 * 月历。刻意去掉格子边框：一屏 42 个框在深色底上会变成一张网，
 * 把日期本身压没了。这里只保留「日号 + 农历 + 圆点」，
 * 选中日用金色实心圆，今天用金色描边圆。
 */
@Composable
fun CalendarScreen(
    state: UiState,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDay: (Long) -> Unit,
    onToggle: (String) -> Unit,
    onEdit: (Todo) -> Unit,
    onTomorrow: (String) -> Unit,
    onToday: (String) -> Unit,
    onDelete: (String) -> Unit,
    onAdd: (String) -> Unit,
    onDraftChange: (Todo) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    val counts = rememberCounts(state)
    val today = Dates.startOfDay(System.currentTimeMillis())
    val selected = state.selectedDay

    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 22.dp)
        ) {
            item {
                MonthHeader(
                    year = state.year,
                    month0 = state.month0,
                    onPrev = onPrevMonth,
                    onNext = onNextMonth,
                    onToday = { onSelectDay(today) }
                )
            }

            item {
                Row(Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                    weekHeaders.forEachIndexed { i, w ->
                        Text(
                            w,
                            Modifier.weight(1f),
                            color = if (i == 0 || i == 6) Palette.TextDim.copy(alpha = 0.7f) else Palette.TextMid,
                            fontSize = 10.sp,
                            letterSpacing = 0.5.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            item {
                MonthGrid(
                    year = state.year,
                    month0 = state.month0,
                    counts = counts,
                    today = today,
                    selected = selected,
                    onSelect = { onSelectDay(it) }
                )
            }

            item {
                val day = selected
                if (day == null) {
                    EmptyBlock("点一天看看", "有安排的日子会亮起来")
                } else {
                    val list = state.todos.filter { it.due == Dates.startOfDay(day) }
                        .sortedWith(compareBy({ it.done }, { it.time ?: Int.MAX_VALUE }))
                    Column(Modifier.padding(top = 18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Eyebrow(
                                Dates.formatDate(day) + "  " + Dates.weekdayName(day),
                                color = Palette.TextMid
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                if (list.isEmpty()) "空" else "${list.count { !it.done }} / ${list.size}",
                                color = Palette.TextDim,
                                fontSize = 10.sp
                            )
                        }
                        Spacer(Modifier.height(9.dp))
                        if (list.isEmpty()) {
                            GroupCard {
                                EmptyBlock("这天还没安排", "下面说一句就能记到这天")
                            }
                        } else {
                            GroupCard {
                                list.forEachIndexed { i, t ->
                                    if (i > 0) RowDivider(indent = 16.dp)
                                    TodoRow(
                                        todo = t,
                                        showDate = false,
                                        onToggle = { onToggle(t.id) },
                                        onEdit = { onEdit(t) },
                                        onTomorrow = { onTomorrow(t.id) },
                                        onToday = { onToday(t.id) },
                                        onDelete = { onDelete(t.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }

        state.draft?.let { draft ->
            DraftCard(
                draft = draft,
                hint = state.draftHint,
                onChange = onDraftChange,
                onConfirm = onConfirm,
                onCancel = onCancel
            )
        }
        QuickAddBar(onSend = onAdd)
    }
}

@Composable
private fun MonthHeader(
    year: Int,
    month0: Int,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                "$year",
                color = Palette.TextDim,
                fontSize = 10.sp,
                letterSpacing = 1.6.sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "${month0 + 1} 月",
                color = Palette.Text,
                fontSize = 30.sp,
                lineHeight = 34.sp,
                fontFamily = FontFamily.Serif,
                letterSpacing = (-0.5).sp
            )
        }
        Spacer(Modifier.weight(1f))
        GhostButton("今天", onClick = onToday)
        Spacer(Modifier.width(2.dp))
        NavButton("‹", onPrev)
        Spacer(Modifier.width(6.dp))
        NavButton("›", onNext)
    }
}

@Composable
private fun NavButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(Palette.SurfaceHi)
            .border(1.dp, Palette.Line, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) { Text(label, color = Palette.TextMid, fontSize = 15.sp) }
}

@Composable
private fun rememberCounts(state: UiState): Map<Int, Pair<Int, Int>> {
    val out = HashMap<Int, Pair<Int, Int>>()
    state.todos.forEach { t ->
        val d = t.due ?: return@forEach
        if (Dates.year(d) != state.year || Dates.month0(d) != state.month0) return@forEach
        val day = Dates.dayOfMonth(t.due!!)
        val prev = out[day] ?: (0 to 0)
        out[day] = if (t.done) prev.first to (prev.second + 1) else (prev.first + 1) to prev.second
    }
    return out
}

@Composable
private fun MonthGrid(
    year: Int,
    month0: Int,
    counts: Map<Int, Pair<Int, Int>>,
    today: Long,
    selected: Long?,
    onSelect: (Long) -> Unit
) {
    val lead = Dates.firstWeekday(year, month0) - 1 // 0-based（周日=0）
    val days = Dates.daysInMonth(year, month0)
    val total = ((lead + days + 6) / 7) * 7

    Column(Modifier.fillMaxWidth()) {
        for (row in 0 until total / 7) {
            Row(Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val index = row * 7 + col
                    val day = index - lead + 1
                    Box(
                        Modifier
                            .weight(1f)
                            .height(60.dp)
                    ) {
                        if (day in 1..days) {
                            DayCell(
                                year = year,
                                month0 = month0,
                                day = day,
                                count = counts[day],
                                isToday = Dates.startOfDay(Dates.dayMillis(year, month0, day)) == today,
                                isSelected = selected != null &&
                                    Dates.startOfDay(selected) ==
                                    Dates.startOfDay(Dates.dayMillis(year, month0, day)),
                                onSelect = onSelect
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    year: Int,
    month0: Int,
    day: Int,
    count: Pair<Int, Int>?,
    isToday: Boolean,
    isSelected: Boolean,
    onSelect: (Long) -> Unit
) {
    val millis = Dates.dayMillis(year, month0, day)
    val open = count?.first ?: 0
    val done = count?.second ?: 0

    Column(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onSelect(millis) }
            .padding(vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(if (isSelected) Palette.Gold else Color.Transparent)
                .then(
                    if (isToday && !isSelected) {
                        Modifier.border(1.dp, Palette.Gold.copy(alpha = 0.6f), CircleShape)
                    } else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                day.toString(),
                color = when {
                    isSelected -> Color(0xFF231A0A)
                    isToday -> Palette.Gold
                    else -> Palette.Text.copy(alpha = 0.88f)
                },
                fontSize = 13.sp,
                lineHeight = 15.sp,
                fontWeight = if (isSelected || isToday) FontWeight.SemiBold else FontWeight.Normal
            )
        }
        Spacer(Modifier.height(1.dp))
        Text(
            Lunar.short(millis),
            color = if (isSelected) Palette.GoldSoft else Palette.TextDim.copy(alpha = 0.55f),
            fontSize = 8.sp,
            lineHeight = 9.sp,
            maxLines = 1
        )
        Spacer(Modifier.height(3.dp))
        Row(
            Modifier.height(5.dp),
            horizontalArrangement = Arrangement.spacedBy(2.5.dp)
        ) {
            if (isSelected) {
                // 选中日：点转成深色，保证在金色底上也看得见
                repeat(open.coerceAtMost(3)) { Dot(Color(0xFF3A2C10), 3) }
                repeat(done.coerceAtMost(2)) { Dot(Color(0xFF1E4A28), 3) }
            } else {
                repeat(open.coerceAtMost(3)) { Dot(Palette.Gold, 3) }
                if (open > 3) Dot(Palette.Gold.copy(alpha = 0.45f), 2)
                repeat(done.coerceAtMost(2)) { Dot(Palette.Success.copy(alpha = 0.8f), 3) }
            }
        }
        Spacer(Modifier.height(2.dp))
    }
}
