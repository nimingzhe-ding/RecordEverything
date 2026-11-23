package me.nimingzhe.recordeverything.ui

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.nimingzhe.recordeverything.data.Todo
import me.nimingzhe.recordeverything.util.Dates
import java.util.Calendar

/**
 * 今天：过期未完成在最上（红），然后今天要做的，最后是今天已完成的。
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

    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                Column(Modifier.padding(horizontal = 6.dp, vertical = 6.dp)) {
                    Text(
                        headline(now),
                        color = Palette.Text,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(summary(openToday.size, doneToday.size, overdue.size), color = Palette.TextDim, fontSize = 12.sp)
                }
            }

            if (overdue.isNotEmpty()) {
                item { SectionHeader("过期", "${overdue.size} 件", Palette.Expense) }
                items(overdue, key = { it.id }) { t ->
                    TodoRow(
                        todo = t,
                        showDate = true,
                        onToggle = { onToggle(t.id) },
                        onEdit = { onEdit(t) },
                        onTomorrow = { onTomorrow(t.id) },
                        onToday = { onToday(t.id) },
                        onDelete = { onDelete(t.id) }
                    )
                }
            }

            item {
                SectionHeader(
                    "今天",
                    if (openToday.isEmpty()) "空了" else "${openToday.size} 件待办"
                )
            }
            if (openToday.isEmpty()) {
                item {
                    Column(Modifier.padding(horizontal = 4.dp)) {
                        EmptyHint("今天没有待办，舒服")
                        Text(
                            "试试这样说一句：",
                            color = Palette.TextDim,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(start = 8.dp, bottom = 6.dp)
                        )
                        Column(
                            Modifier.padding(horizontal = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            samples.chunked(2).forEach { row ->
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    row.forEach { s -> Chip(s) { onAdd(s) } }
                                }
                            }
                        }
                    }
                }
            } else {
                items(openToday, key = { it.id }) { t ->
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

            if (doneToday.isNotEmpty()) {
                item { SectionHeader("已做完", "${doneToday.size} 件") }
                items(doneToday, key = { it.id }) { t ->
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
    if (over > 0) append("$over 件过期 · ")
    append("今天 $open 件未完成")
    if (done > 0) append(" · 已做 $done 件")
}
