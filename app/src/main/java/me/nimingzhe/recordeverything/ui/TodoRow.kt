package me.nimingzhe.recordeverything.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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

/**
 * 卡内的一行待办。没有自己的卡片背景——它活在 [GroupCard] 里，
 * 与上下行之间靠 1px 发丝线分隔。
 *
 * 层次：标题（15sp）> 元信息（11sp 弱色）。重要的条目在左缘加一条金色竖条，
 * 比在标题前塞 emoji 干净得多。
 */
@Composable
fun TodoRow(
    todo: Todo,
    showDate: Boolean = false,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onTomorrow: () -> Unit,
    onDelete: () -> Unit,
    onToday: () -> Unit = {},
    onSnoozeMenu: Boolean = true
) {
    var menu by remember { mutableStateOf(false) }
    val today = Dates.startOfDay(System.currentTimeMillis())
    val overdue = !todo.done && todo.due != null && todo.due < today
    val highlight = todo.important && !todo.done

    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        verticalAlignment = Alignment.Top
    ) {
        // 重要条：左侧 2dp 金线，高度随行高自适应
        Box(
            Modifier
                .width(2.dp)
                .height(38.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (highlight) Palette.Gold else androidx.compose.ui.graphics.Color.Transparent)
        )
        Row(
            Modifier
                .weight(1f)
                .padding(start = 14.dp, end = 6.dp, top = 13.dp, bottom = 13.dp),
            verticalAlignment = Alignment.Top
        ) {
            Spacer(Modifier.height(1.dp))
            CheckCircle(checked = todo.done) { onToggle() }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    todo.title,
                    color = if (todo.done) Palette.TextDim else Palette.Text,
                    fontSize = 14.5.sp,
                    lineHeight = 21.sp,
                    fontWeight = if (highlight) FontWeight.Medium else FontWeight.Normal,
                    textDecoration = if (todo.done) TextDecoration.LineThrough else TextDecoration.None,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                val meta = buildList {
                    if (showDate && todo.due != null) add(Dates.formatDateShort(todo.due))
                    todo.time?.let { add(TodoParser.fmtMinute(it)) }
                    if (todo.group.isNotBlank()) add(todo.group)
                    if (todo.note.isNotBlank()) add(todo.note)
                }.joinToString("  ·  ")
                if (meta.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        meta,
                        color = if (overdue) Palette.Danger.copy(alpha = 0.85f) else Palette.TextDim,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (onSnoozeMenu) {
                Box {
                    Box(
                        Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(50))
                            .clickable { menu = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⋯", color = Palette.TextDim.copy(alpha = 0.8f), fontSize = 15.sp)
                    }
                    DropdownMenu(
                        expanded = menu,
                        onDismissRequest = { menu = false }
                    ) {
                        MenuItem("改一改") { menu = false; onEdit() }
                        MenuItem("今天做") { menu = false; onToday() }
                        MenuItem("推到明天") { menu = false; onTomorrow() }
                        MenuItem("删除", danger = true) { menu = false; onDelete() }
                    }
                }
            } else {
                Spacer(Modifier.size(30.dp))
            }
        }
    }
}

@Composable
private fun MenuItem(text: String, danger: Boolean = false, onClick: () -> Unit) {
    DropdownMenuItem(
        text = {
            Text(
                text,
                fontSize = 13.sp,
                color = if (danger) Palette.Danger else Palette.Text
            )
        },
        onClick = onClick
    )
}

/** 分区标题：左侧小字距 labels + 右侧数量，不带卡片。 */
@Composable
fun SectionTitle(
    title: String,
    count: Int? = null,
    accent: androidx.compose.ui.graphics.Color = Palette.TextDim,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = 22.dp, bottom = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            color = accent,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.6.sp
        )
        if (count != null) {
            Spacer(Modifier.width(7.dp))
            Text("$count", color = accent.copy(alpha = 0.6f), fontSize = 10.sp, letterSpacing = 1.sp)
        }
        Spacer(Modifier.weight(1f))
    }
}

/** 一组行放进一张卡：自动在行之间画发丝线。 */
@Composable
fun TodoCard(
    todos: List<Todo>,
    showDate: Boolean,
    onToggle: (String) -> Unit,
    onEdit: (Todo) -> Unit,
    onTomorrow: (String) -> Unit,
    onToday: (String) -> Unit,
    onDelete: (String) -> Unit,
    accent: androidx.compose.ui.graphics.Color? = null
) {
    GroupCard(accent = accent) {
        todos.forEachIndexed { i, t ->
            if (i > 0) RowDivider(indent = 16.dp)
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
