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
import androidx.compose.foundation.lazy.LazyRow
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

private val filterLabels = listOf(
    Filter.OPEN to "未完成",
    Filter.TODAY to "今天",
    Filter.WEEK to "本周",
    Filter.DONE to "已完成",
    Filter.ALL to "全部"
)

/** 全部：按筛选看清单，支持按分组过滤。 */
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
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                Column(Modifier.padding(horizontal = 6.dp, vertical = 4.dp)) {
                    Text("全部", color = Palette.Text, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${counts.first} 件未完成 · 今天 ${counts.second} 件 · 过期 ${counts.third} 件",
                        color = Palette.TextDim,
                        fontSize = 11.sp
                    )
                }
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(filterLabels) { (f, label) ->
                        Chip(label, selected = state.filter == f) { onFilter(f) }
                    }
                }
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item { Chip("全部分组", selected = state.group == null) { onGroup(null) } }
                    items(Todo.GROUPS) { g ->
                        Chip(g, selected = state.group == g) { onGroup(g) }
                    }
                }
            }

            if (list.isEmpty()) {
                item { EmptyHint("这里空的") }
            } else {
                // 按天分组显示
                val grouped = list.groupBy { it.due }
                    .toSortedMap(compareBy { it ?: Long.MAX_VALUE })
                grouped.forEach { (day, todos) ->
                    item(key = "h-$day") {
                        SectionHeader(
                            day?.let { Dates.formatDateShort(it) + " " + Dates.weekdayName(it) } ?: "收集箱",
                            "${todos.size} 件"
                        )
                    }
                    items(todos, key = { it.id }) { t ->
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
