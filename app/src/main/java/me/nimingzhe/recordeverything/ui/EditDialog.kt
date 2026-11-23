package me.nimingzhe.recordeverything.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import me.nimingzhe.recordeverything.data.Todo
import me.nimingzhe.recordeverything.nl.TodoParser
import me.nimingzhe.recordeverything.util.Dates

/**
 * 编辑一条待办。做成底部抽屉的气质（深色卡 + 顶端拖拽条 + 底部主按钮），
 * 而不是 AlertDialog——后者在深色主题下永远是"系统弹窗"，很出戏。
 */
@Composable
fun EditDialog(
    todo: Todo,
    onDismiss: () -> Unit,
    onSave: (Todo) -> Unit,
    onDelete: (Todo) -> Unit = {}
) {
    var title by remember(todo.id) { mutableStateOf(todo.title) }
    var note by remember(todo.id) { mutableStateOf(todo.note) }
    var due by remember(todo.id) { mutableStateOf(todo.due) }
    var time by remember(todo.id) { mutableStateOf(todo.time) }
    var group by remember(todo.id) { mutableStateOf(todo.group) }
    var important by remember(todo.id) { mutableStateOf(todo.important) }

    val today = Dates.startOfDay(System.currentTimeMillis())
    val tomorrow = Dates.addDays(today, 1)
    val dayAfter = Dates.addDays(today, 2)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp)
                .clip(RoundedCornerShape(Radius.xl))
                .background(Palette.SurfaceHi)
                .border(1.dp, Palette.Line, RoundedCornerShape(Radius.xl))
                .padding(horizontal = 18.dp, vertical = 14.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(34.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Palette.Line)
            )
            Spacer(Modifier.height(16.dp))

            Eyebrow("改 一 改")
            Spacer(Modifier.height(10.dp))

            BasicTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(
                    color = Palette.Text,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium
                ),
                cursorBrush = SolidColor(Palette.Gold),
                maxLines = 3
            )
            Spacer(Modifier.height(8.dp))
            BasicTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(color = Palette.TextMid, fontSize = 13.sp),
                cursorBrush = SolidColor(Palette.Gold),
                maxLines = 3,
                decorationBox = { inner ->
                    Box {
                        if (note.isEmpty()) Text("加个备注", color = Palette.TextDim, fontSize = 13.sp)
                        inner()
                    }
                }
            )

            Spacer(Modifier.height(20.dp))
            FieldLabel("日期")
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Chip("今天", selected = due == today) { due = today }
                Chip("明天", selected = due == tomorrow) { due = tomorrow }
                Chip("后天", selected = due == dayAfter) { due = dayAfter }
                Chip("不排期", selected = due == null) { due = null; time = null }
            }
            if (due != null && due != today && due != tomorrow && due != dayAfter) {
                Spacer(Modifier.height(6.dp))
                Text(Dates.formatDate(due!!), color = Palette.Gold, fontSize = 11.sp)
            }

            Spacer(Modifier.height(16.dp))
            FieldLabel("时间")
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf(9 to "9:00", 12 to "12:00", 18 to "18:00", 20 to "20:00").forEach { (h, label) ->
                    Chip(label, selected = time == h * 60) {
                        time = h * 60
                        if (due == null) due = today
                    }
                }
                Chip("无", selected = time == null) { time = null }
            }

            Spacer(Modifier.height(16.dp))
            FieldLabel("分组")
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Todo.GROUPS.forEach { g ->
                    Chip(g, selected = group == g) { group = g }
                }
            }

            Spacer(Modifier.height(16.dp))
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (important) Palette.Gold.copy(alpha = 0.18f) else Palette.SurfaceMax
                    )
                    .border(
                        1.dp,
                        if (important) Palette.Gold.copy(alpha = 0.5f) else Palette.Line,
                        RoundedCornerShape(50)
                    )
                    .clickable { important = !important }
                    .padding(horizontal = 13.dp, vertical = 7.dp)
            ) {
                Text(
                    "⚡ 重要",
                    color = if (important) Palette.Gold else Palette.TextDim,
                    fontSize = 12.sp
                )
            }

            if (time != null && due != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "定在 " + Dates.formatDateShort(due!!) + " " + TodoParser.fmtMinute(time!!),
                    color = Palette.TextDim,
                    fontSize = 10.sp
                )
            }

            Spacer(Modifier.height(22.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "删除",
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { onDelete(todo) }
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    color = Palette.Danger,
                    fontSize = 13.sp
                )
                Spacer(Modifier.weight(1f))
                GhostButton("取消", onClick = onDismiss)
                Spacer(Modifier.width(4.dp))
                GoldButton("保存", enabled = title.isNotBlank()) {
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
                }
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text,
        color = Palette.TextDim,
        fontSize = 10.sp,
        letterSpacing = 1.4.sp,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}
