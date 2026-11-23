package me.nimingzhe.recordeverything.ui

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.nimingzhe.recordeverything.data.Todo
import me.nimingzhe.recordeverything.util.Dates

private val filterLabels = listOf(
    Filter.OPEN to "未完成",
    Filter.TODAY to "今天",
    Filter.WEEK to "本周",
    Filter.DONE to "已完成",
    Filter.ALL to "全部"
)

/**
 * 全部。筛选条横向排两行（状态 + 分组），
 * 列表按日期分段，每段一张卡。
 */
@Composable
fun AllScreen(
    state: UiState,
    list: List<Todo>,
    counts: Triple<Int, Int, Int>,
    onFilter: (Filter) -> Unit,
    onGroup: (String?) -> Unit,
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
    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 24.dp)
        ) {
            item {
                Column(Modifier.padding(top = 16.dp, bottom = 16.dp)) {
                    Text(
                        "${counts.first}",
                        color = Palette.Text,
                        fontSize = 40.sp,
                        lineHeight = 44.sp,
                        fontFamily = FontFamily.Serif,
                        letterSpacing = (-1).sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "件还没做完 · 今天 ${counts.second} 件 · 过期 ${counts.third} 件",
                        color = Palette.TextMid,
                        fontSize = 12.sp
                    )
                }
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filterLabels) { (f, label) ->
                        Chip(label, selected = state.filter == f) { onFilter(f) }
                    }
                }
            }

            item { Spacer(Modifier.height(8.dp)) }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { Chip("全部分组", selected = state.group == null) { onGroup(null) } }
                    items(Todo.GROUPS) { g ->
                        Chip(g, selected = state.group == g) { onGroup(g) }
                    }
                }
            }

            if (list.isEmpty()) {
                item {
                    Box(Modifier.padding(top = 24.dp)) {
                        GroupCard {
                            EmptyBlock("这里空的", "换个筛选，或者说一句新的")
                        }
                    }
                }
            } else {
                val grouped = list.groupBy { it.due }
                    .toSortedMap(compareBy { it ?: Long.MAX_VALUE })
                grouped.forEach { (day, todos) ->
                    item(key = "h-$day") {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(start = 4.dp, end = 4.dp, top = 22.dp, bottom = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                day?.let { Dates.formatDateShort(it) } ?: "收 集 箱",
                                color = Palette.TextMid,
                                fontSize = 11.sp,
                                letterSpacing = if (day == null) 1.4.sp else 0.sp
                            )
                            if (day != null) {
                                Spacer(Modifier.width(7.dp))
                                Text(
                                    Dates.weekdayName(day),
                                    color = Palette.TextDim.copy(alpha = 0.7f),
                                    fontSize = 10.sp
                                )
                            }
                            Spacer(Modifier.weight(1f))
                            Text("${todos.count { !it.done }}/${todos.size}", color = Palette.TextDim, fontSize = 10.sp)
                        }
                    }
                    item(key = "g-$day") {
                        TodoCard(
                            todos = todos,
                            showDate = false,
                            onToggle = onToggle,
                            onEdit = onEdit,
                            onTomorrow = onTomorrow,
                            onToday = onToday,
                            onDelete = onDelete
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(18.dp)) }
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
