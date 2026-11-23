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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.nimingzhe.recordeverything.data.Todo
import me.nimingzhe.recordeverything.util.Dates
import me.nimingzhe.recordeverything.util.Lunar

private val weekHeaders = listOf("日", "一", "二", "三", "四", "五", "六")

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
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 20.dp)
        ) {
            item {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        Dates.monthTitle(state.year, state.month0),
                        color = Palette.Text,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.weight(1f))
                    NavButton("‹") { onPrevMonth() }
                    Spacer(Modifier.width(6.dp))
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable {
                                val now = System.currentTimeMillis()
                                onSelectDay(Dates.startOfDay(now))
                            }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) { Text("今天", color = Palette.Gold, fontSize = 12.sp) }
                    Spacer(Modifier.width(6.dp))
                    NavButton("›") { onNextMonth() }
                }
            }

            item {
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    weekHeaders.forEach { w ->
                        Text(
                            w,
                            Modifier.weight(1f),
                            color = Palette.TextDim,
                            fontSize = 11.sp,
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
                    EmptyHint("点一个日期看看")
                } else {
                    val list = state.todos.filter { it.due == Dates.startOfDay(day) }
                        .sortedWith(compareBy({ it.done }, { it.time ?: Int.MAX_VALUE }))
                    Column(Modifier.padding(top = 14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                Dates.formatDate(day) + " " + Dates.weekdayName(day),
                                color = Palette.Text,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (list.isEmpty()) "空" else "${list.size} 件",
                                color = Palette.TextDim,
                                fontSize = 11.sp
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        if (list.isEmpty()) {
                            EmptyHint("这天还没安排")
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                list.forEach { t ->
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

            item { Spacer(Modifier.height(20.dp)) }
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
private fun NavButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(Palette.Surface)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) { Text(label, color = Palette.TextDim, fontSize = 14.sp) }
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

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Palette.Surface)
            .padding(6.dp)
    ) {
        for (row in 0 until total / 7) {
            Row(Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val index = row * 7 + col
                    val day = index - lead + 1
                    Box(Modifier.weight(1f).height(60.dp).padding(2.dp)) {
                        if (day in 1..days) {
                            DayCell(
                                year = year,
                                month0 = month0,
                                day = day,
                                count = counts[day],
                                isToday = Dates.dayMillis(year, month0, day).let {
                                    Dates.startOfDay(it) == today
                                },
                                isSelected = selected != null && Dates.startOfDay(selected) ==
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
    val border = when {
        isSelected -> Palette.Gold.copy(alpha = 0.7f)
        isToday -> Palette.Gold.copy(alpha = 0.35f)
        else -> Color.Transparent
    }
    Column(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(
                when {
                    isSelected -> Palette.Gold.copy(alpha = 0.12f)
                    isToday -> Palette.SurfaceHi.copy(alpha = 0.7f)
                    else -> Color.Transparent
                }
            )
            .border(1.dp, border, RoundedCornerShape(12.dp))
            .clickable { onSelect(millis) }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            day.toString(),
            color = when {
                isSelected -> Palette.Gold
                isToday -> Palette.Gold
                else -> Palette.Text
            },
            fontSize = 13.sp,
            fontWeight = if (isSelected || isToday) FontWeight.SemiBold else FontWeight.Normal
        )
        Text(Lunar.short(millis), color = Palette.TextDim.copy(alpha = 0.65f), fontSize = 8.sp)
        Spacer(Modifier.weight(1f))
        val open = count?.first ?: 0
        val done = count?.second ?: 0
        Row(
            Modifier.height(6.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            repeat(open.coerceAtMost(3)) { Dot(Palette.Gold, 4) }
            if (open > 3) Dot(Palette.Gold.copy(alpha = 0.5f), 3)
            repeat(done.coerceAtMost(2)) { Dot(Palette.Income, 4) }
        }
        Spacer(Modifier.height(2.dp))
    }
}
