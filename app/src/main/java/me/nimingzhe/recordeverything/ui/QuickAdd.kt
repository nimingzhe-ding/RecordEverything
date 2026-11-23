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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.nimingzhe.recordeverything.data.Todo
import me.nimingzhe.recordeverything.nl.TodoParser
import me.nimingzhe.recordeverything.util.Dates

/** 底部一句话输入条。 */
@Composable
fun QuickAddBar(
    hint: String = "说一句：明天下午三点 开会",
    onSend: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf("") }

    Row(
        modifier
            .fillMaxWidth()
            .background(Palette.Bg)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            Modifier
                .weight(1f)
                .clip(RoundedCornerShape(50))
                .background(Palette.Surface)
                .border(1.dp, Palette.Divider, RoundedCornerShape(50))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = TextStyle(color = Palette.Text, fontSize = 14.sp),
                cursorBrush = SolidColor(Palette.Gold),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (text.isNotBlank()) {
                        onSend(text)
                        text = ""
                    }
                }),
                decorationBox = { inner ->
                    Box {
                        if (text.isEmpty()) {
                            Text(hint, color = Palette.TextDim, fontSize = 14.sp, maxLines = 1)
                        }
                        inner()
                    }
                }
            )
        }
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(50))
                .background(Brush.linearGradient(listOf(Palette.Gold, Palette.GoldDim)))
                .clickable {
                    if (text.isNotBlank()) {
                        onSend(text)
                        text = ""
                    }
                },
            contentAlignment = Alignment.Center
        ) { Text("↑", color = Color(0xFF20180C), fontSize = 17.sp, fontWeight = FontWeight.Bold) }
    }
}

/** 解析出的草稿卡片：确认前可以改日期 / 时间 / 分组。 */
@Composable
fun DraftCard(
    draft: Todo,
    hint: String,
    onChange: (Todo) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    var menu by remember { mutableStateOf<Int?>(null) }
    val today = Dates.startOfDay(System.currentTimeMillis())

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Palette.Surface)
            .border(1.dp, Palette.Gold.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        if (hint.isNotBlank()) {
            Text(hint, color = Palette.Gold, fontSize = 11.sp)
            Spacer(Modifier.height(8.dp))
        }

        BasicTextField(
            value = draft.title,
            onValueChange = { onChange(draft.copy(title = it)) },
            modifier = Modifier.fillMaxWidth(),
            textStyle = TextStyle(color = Palette.Text, fontSize = 17.sp, fontWeight = FontWeight.Medium),
            cursorBrush = SolidColor(Palette.Gold)
        )

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // 日期
            Box {
                DraftChip(
                    text = draft.due?.let { Dates.chip(it) } ?: "不排期",
                    active = draft.due != null
                ) { menu = 0 }
                DropdownMenu(expanded = menu == 0, onDismissRequest = { menu = null }) {
                    listOf(
                        "今天" to today,
                        "明天" to Dates.addDays(today, 1),
                        "后天" to Dates.addDays(today, 2),
                        "下周一" to Dates.addDays(today, 7)
                    ).forEach { (label, day) ->
                        DropdownMenuItem(
                            text = { Text(label, fontSize = 13.sp) },
                            onClick = {
                                menu = null
                                onChange(draft.copy(due = day))
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("不排期", fontSize = 13.sp) },
                        onClick = {
                            menu = null
                            onChange(draft.copy(due = null, time = null))
                        }
                    )
                }
            }
            // 时间
            Box {
                DraftChip(
                    text = draft.time?.let { TodoParser.fmtMinute(it) } ?: "不设时间",
                    active = draft.time != null
                ) { menu = 1 }
                DropdownMenu(expanded = menu == 1, onDismissRequest = { menu = null }) {
                    listOf(
                        "09:00" to 9 * 60,
                        "12:00" to 12 * 60,
                        "15:00" to 15 * 60,
                        "18:00" to 18 * 60,
                        "20:00" to 20 * 60
                    ).forEach { (label, m) ->
                        DropdownMenuItem(
                            text = { Text(label, fontSize = 13.sp) },
                            onClick = {
                                menu = null
                                onChange(
                                    draft.copy(
                                        time = m,
                                        due = draft.due ?: today
                                    )
                                )
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("不设时间", fontSize = 13.sp) },
                        onClick = {
                            menu = null
                            onChange(draft.copy(time = null))
                        }
                    )
                }
            }
            // 分组
            Box {
                DraftChip(text = draft.group, active = false) { menu = 2 }
                DropdownMenu(expanded = menu == 2, onDismissRequest = { menu = null }) {
                    Todo.GROUPS.forEach { g ->
                        DropdownMenuItem(
                            text = { Text(g, fontSize = 13.sp) },
                            onClick = {
                                menu = null
                                onChange(draft.copy(group = g))
                            }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (draft.important) Palette.Gold.copy(alpha = 0.18f) else Palette.SurfaceHi
                    )
                    .clickable { onChange(draft.copy(important = !draft.important)) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    "⚡ 重要",
                    color = if (draft.important) Palette.Gold else Palette.TextDim,
                    fontSize = 12.sp
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                "取消",
                color = Palette.TextDim,
                fontSize = 13.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable { onCancel() }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            )
            Spacer(Modifier.width(4.dp))
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Brush.linearGradient(listOf(Palette.Gold, Palette.GoldDim)))
                    .clickable { onConfirm() }
                    .padding(horizontal = 18.dp, vertical = 9.dp)
            ) {
                Text("加上", color = Color(0xFF20180C), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun DraftChip(text: String, active: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (active) Palette.Gold.copy(alpha = 0.14f) else Palette.SurfaceHi)
            .border(
                1.dp,
                if (active) Palette.Gold.copy(alpha = 0.45f) else Palette.Divider,
                RoundedCornerShape(50)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(text, color = if (active) Palette.Gold else Palette.TextDim, fontSize = 12.sp)
    }
}
