package me.nimingzhe.recordeverything.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.nimingzhe.recordeverything.data.Todo
import me.nimingzhe.recordeverything.util.Dates
import java.util.Calendar

/**
 * 今天。视觉重心是那个衬线的大号日期数字——它回答"现在是哪天"，
 * 比任何标题都直接。下面才是清单。
 */
@Composable
fun TodayScreen(
    state: UiState,
    overdue: List<Todo>,
    todayList: List<Todo>,
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
    val now = System.currentTimeMillis()
    val overdueIds = overdue.map { it.id }.toSet()
    val openToday = todayList.filter { !it.done && it.id !in overdueIds }
    val doneToday = todayList.filter { it.done }
    val total = openToday.size + doneToday.size
    val progress = if (total == 0) 0f else doneToday.size.toFloat() / total

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Palette.BgTop, Palette.Bg), 0f, 520f))
    ) {
        Column(Modifier.fillMaxSize()) {
            LazyColumn(
                Modifier.weight(1f),
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 24.dp)
            ) {
                item {
                    DateHero(
                        now = now,
                        open = openToday.size,
                        done = doneToday.size,
                        over = overdue.size,
                        progress = progress
                    )
                }

                if (overdue.isNotEmpty()) {
                    item { SectionTitle("过 期", overdue.size, Palette.Danger) }
                    item {
                        TodoCard(
                            todos = overdue,
                            showDate = true,
                            onToggle = onToggle,
                            onEdit = onEdit,
                            onTomorrow = onTomorrow,
                            onToday = onToday,
                            onDelete = onDelete,
                            accent = Palette.Danger
                        )
                    }
                }

                item { SectionTitle("今 天", openToday.size) }

                if (openToday.isEmpty() && doneToday.isEmpty()) {
                    item {
                        GroupCard {
                            Column(Modifier.padding(vertical = 8.dp)) {
                                EmptyBlock("今天还空着", "说一句话就能记下来")
                            }
                        }
                    }
                    item {
                        Spacer(Modifier.height(6.dp))
                        Eyebrow("试 试 这 样 说", modifier = Modifier.padding(start = 6.dp, bottom = 8.dp))
                    }
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            samples.chunked(2).forEach { row ->
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    row.forEach { s ->
                                        Chip(s, leading = "↗") { onAdd(s) }
                                    }
                                }
                            }
                        }
                    }
                } else if (openToday.isNotEmpty()) {
                    item {
                        TodoCard(
                            todos = openToday,
                            showDate = false,
                            onToggle = onToggle,
                            onEdit = onEdit,
                            onTomorrow = onTomorrow,
                            onToday = onToday,
                            onDelete = onDelete
                        )
                    }
                }

                if (doneToday.isNotEmpty()) {
                    item { SectionTitle("已 做 完", doneToday.size) }
                    item {
                        TodoCard(
                            todos = doneToday,
                            showDate = false,
                            onToggle = onToggle,
                            onEdit = onEdit,
                            onTomorrow = onTomorrow,
                            onToday = onToday,
                            onDelete = onDelete
                        )
                    }
                }

                item { Spacer(Modifier.height(18.dp)) }
            }

            state.draft?.let { draft ->
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it / 3 }),
                    exit = fadeOut()
                ) {
                    DraftCard(
                        draft = draft,
                        hint = state.draftHint,
                        onChange = onDraftChange,
                        onConfirm = onConfirm,
                        onCancel = onCancel
                    )
                }
            }
            QuickAddBar(onSend = onAdd)
        }
    }
}

/**
 * 顶部日期区：衬线大数字 + 星期 + 完成度细环。
 */
@Composable
private fun DateHero(now: Long, open: Int, done: Int, over: Int, progress: Float) {
    val c = Dates.calendar().apply { timeInMillis = now }
    val month = c.get(Calendar.MONTH) + 1
    val day = c.get(Calendar.DAY_OF_MONTH)
    val weekday = Dates.weekdayName(now)

    Column(Modifier.padding(top = 10.dp, bottom = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Eyebrow("${Dates.year(now)} 年 $month 月  ·  $weekday")
            Spacer(Modifier.weight(1f))
            Ring(progress = progress, size = 30.dp, stroke = 2.5.dp)
            Spacer(Modifier.width(9.dp))
            Text(
                if (done + open == 0) "—" else "${(progress * 100).toInt()}%",
                color = Palette.TextMid,
                style = Type.meta
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(day.toString(), color = Palette.Text, style = Type.display)
        Spacer(Modifier.height(2.dp))
        Text(
            summary(open = open, done = done, over = over),
            color = Palette.TextMid,
            style = Type.meta
        )
    }
}

private val samples = listOf(
    "明天下午三点 开会",
    "后天 14:30 体检",
    "下周一 交报告",
    "买一台咖啡机"
)

fun headline(now: Long): String {
    val c = Dates.calendar().apply { timeInMillis = now }
    val m = c.get(Calendar.MONTH) + 1
    val d = c.get(Calendar.DAY_OF_MONTH)
    return "$m 月 $d 日 · ${Dates.weekdayName(now)}"
}

fun summary(open: Int, done: Int, over: Int): String = buildString {
    if (over > 0) append("$over 件过期没做 · ")
    when {
        open == 0 && done == 0 -> append("今天还没安排")
        open == 0 -> append("今天的事都做完了")
        else -> append("$open 件待办")
    }
    if (done > 0) append(" · 已做 $done 件")
}
