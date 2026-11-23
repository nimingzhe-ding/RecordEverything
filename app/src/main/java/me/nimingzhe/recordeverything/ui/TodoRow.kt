package me.nimingzhe.recordeverything.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.nimingzhe.recordeverything.data.Todo
import me.nimingzhe.recordeverything.nl.TodoParser
import me.nimingzhe.recordeverything.util.Dates

/** 一行待办：勾选圈 + 标题 + 元信息 + 更多。 */
@Composable
fun TodoRow(
    todo: Todo,
    showDate: Boolean = false,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onTomorrow: () -> Unit,
    onDelete: () -> Unit,
    onToday: () -> Unit = {}
) {
    var menu by remember { mutableStateOf(false) }
    val overdue = !todo.done && todo.due != null &&
        todo.due < Dates.startOfDay(System.currentTimeMillis())

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Palette.Surface)
            .clickable { onEdit() }
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CheckCircle(checked = todo.done) { onToggle() }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(
                todo.title,
                color = if (todo.done) Palette.TextDim else Palette.Text,
                fontSize = 14.sp,
                fontWeight = if (todo.important && !todo.done) FontWeight.SemiBold else FontWeight.Normal,
                textDecoration = if (todo.done) TextDecoration.LineThrough else TextDecoration.None,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            val meta = buildList {
                if (todo.important && !todo.done) add("⚡")
                if (showDate && todo.due != null) {
                    add(Dates.formatDateShort(todo.due))
                }
                todo.time?.let { add(TodoParser.fmtMinute(it)) }
                if (todo.group.isNotBlank()) add(todo.group)
                if (todo.note.isNotBlank()) add(todo.note)
            }.joinToString(" · ")
            if (meta.isNotEmpty()) {
                Text(
                    meta,
                    color = if (overdue) Palette.Expense else Palette.TextDim,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Box {
            Box(
                Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(50))
                    .clickable { menu = true },
                contentAlignment = Alignment.Center
            ) { Text("⋯", color = Palette.TextDim, fontSize = 14.sp) }

            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text("改", fontSize = 13.sp) },
                    onClick = { menu = false; onEdit() }
                )
                DropdownMenuItem(
                    text = { Text("今天做", fontSize = 13.sp) },
                    onClick = { menu = false; onToday() }
                )
                DropdownMenuItem(
                    text = { Text("推到明天", fontSize = 13.sp) },
                    onClick = { menu = false; onTomorrow() }
                )
                DropdownMenuItem(
                    text = { Text("删除", fontSize = 13.sp) },
                    onClick = { menu = false; onDelete() }
                )
            }
        }
    }
}

/** 小节标题。 */
@Composable
fun SectionHeader(title: String, trailing: String? = null, accent: androidx.compose.ui.graphics.Color = Palette.TextDim) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 18.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = accent, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.width(8.dp))
        if (trailing != null) Text(trailing, color = Palette.TextDim, fontSize = 11.sp)
        Spacer(Modifier.weight(1f))
    }
}

@Composable
fun TodoGroup(
    todos: List<Todo>,
    showDate: Boolean,
    onToggle: (String) -> Unit,
    onEdit: (Todo) -> Unit,
    onTomorrow: (String) -> Unit,
    onToday: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        todos.forEach { t ->
            TodoRow(
                todo = t,
                showDate = showDate,
                onToggle = { onToggle(t.id) },
                onEdit = { onEdit(t) },
                onTomorrow = { onTomorrow(t.id) },
                onToday = { onToday(t.id) },
                onDelete = { onDelete(t.id) }
            )
        }
    }
}
