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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.nimingzhe.recordeverything.data.Todo
import me.nimingzhe.recordeverything.nl.TodoParser
import me.nimingzhe.recordeverything.util.Dates

/** 编辑一条待办：标题、备注、日期、时间、分组、重要。 */
@Composable
fun EditDialog(
    todo: Todo,
    onDismiss: () -> Unit,
    onSave: (Todo) -> Unit
) {
    var title by remember(todo.id) { mutableStateOf(todo.title) }
    var note by remember(todo.id) { mutableStateOf(todo.note) }
    var due by remember(todo.id) { mutableStateOf(todo.due) }
    var time by remember(todo.id) { mutableStateOf(todo.time) }
    var group by remember(todo.id) { mutableStateOf(todo.group) }
    var important by remember(todo.id) { mutableStateOf(todo.important) }

    val today = Dates.startOfDay(System.currentTimeMillis())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("改一改", fontSize = 16.sp) },
        text = {
            Column {
                BasicTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(color = Palette.Text, fontSize = 16.sp),
                    cursorBrush = SolidColor(Palette.Gold)
                )
                Spacer(Modifier.height(10.dp))
                BasicTextField(
                    value = note,
                    onValueChange = { note = it },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(color = Palette.TextDim, fontSize = 13.sp),
                    cursorBrush = SolidColor(Palette.Gold),
                    decorationBox = { inner ->
                        Box {
                            if (note.isEmpty()) Text("备注", color = Palette.Divider, fontSize = 13.sp)
                            inner()
                        }
                    }
                )
                Spacer(Modifier.height(14.dp))

                Text("日期", color = Palette.TextDim, fontSize = 11.sp)
                Spacer(Modifier.height(5.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Chip("今天", selected = due == today) { due = today }
                    Chip("明天", selected = due == Dates.addDays(today, 1)) {
                        due = Dates.addDays(today, 1)
                    }
                    Chip("后天", selected = due == Dates.addDays(today, 2)) {
                        due = Dates.addDays(today, 2)
                    }
                    Chip("不排期", selected = due == null) { due = null; time = null }
                }
                if (due != null && due != today &&
                    due != Dates.addDays(today, 1) && due != Dates.addDays(today, 2)
                ) {
                    Spacer(Modifier.height(4.dp))
                    Text(Dates.formatDate(due!!), color = Palette.Gold, fontSize = 11.sp)
                }

                Spacer(Modifier.height(12.dp))
                Text("时间", color = Palette.TextDim, fontSize = 11.sp)
                Spacer(Modifier.height(5.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(9 to "9:00", 12 to "12:00", 18 to "18:00", 20 to "20:00").forEach { (h, label) ->
                        Chip(label, selected = time == h * 60) {
                            time = h * 60
                            if (due == null) due = today
                        }
                    }
                    Chip("无", selected = time == null) { time = null }
                }

                Spacer(Modifier.height(12.dp))
                Text("分组", color = Palette.TextDim, fontSize = 11.sp)
                Spacer(Modifier.height(5.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Todo.GROUPS.forEach { g ->
                        Chip(g, selected = group == g) { group = g }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (important) Palette.Gold.copy(alpha = 0.18f) else Palette.SurfaceHi
                        )
                        .clickable { important = !important }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        "⚡ 重要",
                        color = if (important) Palette.Gold else Palette.TextDim,
                        fontSize = 12.sp
                    )
                }

                if (time != null && due != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "提醒：" + Dates.formatDateShort(due!!) + " " + TodoParser.fmtMinute(time!!),
                        color = Palette.TextDim,
                        fontSize = 10.sp
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    todo.copy(
                        title = title.trim(),
                        note = note.trim(),
                        due = due,
                        time = time,
                        group = group,
                        important = important
                    )
                )
            }) { Text("保存", color = Palette.Gold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
