package me.nimingzhe.recordeverything.ui

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.scale
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

/**
 * 悬浮的一句话输入条。它是这个 App 唯一的入口，所以给足存在感：
 * 胶囊底 + 金色发丝边 + 金色圆形发送键。
 */
@Composable
fun QuickAddBar(
    hint: String = "说一句，比如：明天下午三点 开会",
    onSend: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf("") }
    val active = text.isNotBlank()
    val scale by animateFloatAsState(if (active) 1f else 0.94f, label = "sendScale")

    Row(
        modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color.Transparent, Palette.Bg.copy(alpha = 0.96f)),
                    startY = 0f
                )
            )
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            Modifier
                .weight(1f)
                .clip(RoundedCornerShape(50))
                .background(Palette.SurfaceHi)
                .border(
                    1.dp,
                    if (active) Palette.Gold.copy(alpha = 0.55f) else Palette.Line,
                    RoundedCornerShape(50)
                )
                .padding(start = 18.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = Type.body.copy(color = Palette.Text),
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
                            Text(hint, color = Palette.TextDim, style = Type.body, maxLines = 1)
                        }
                        inner()
                    }
                }
            )
            Box(
                Modifier
                    .size(36.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(
                        if (active) Brush.linearGradient(listOf(Palette.Gold, Palette.GoldSoft))
                        else Brush.linearGradient(listOf(Palette.SurfaceMax, Palette.SurfaceMax))
                    )
                    .clickable {
                        if (text.isNotBlank()) {
                            onSend(text)
                            text = ""
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "↑",
                    color = if (active) Color(0xFF231A0A) else Palette.TextDim,
                    style = Type.title,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * 解析草稿卡。浮在输入条上方，用户确认之前它是"半透明草稿"的身份：
 * 金色描边 + 顶部一行小字说明解析结果。
 */
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
            .padding(horizontal = 18.dp)
            .clip(RoundedCornerShape(Radius.lg))
            .background(Palette.SurfaceHi)
            .border(1.dp, Palette.Gold.copy(alpha = 0.45f), RoundedCornerShape(Radius.lg))
            .padding(16.dp)
    ) {
        if (hint.isNotBlank()) {
            Eyebrow(hint, color = Palette.Gold.copy(alpha = 0.9f))
            Spacer(Modifier.height(8.dp))
        }

        BasicTextField(
            value = draft.title,
            onValueChange = { onChange(draft.copy(title = it)) },
            modifier = Modifier.fillMaxWidth(),
            textStyle = Type.title.copy(color = Palette.Text),
            cursorBrush = SolidColor(Palette.Gold)
        )

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            // 日期
            Box {
                Chip(
                    text = draft.due?.let { Dates.chip(it) } ?: "不排期",
                    selected = draft.due != null,
                    leading = "◷"
                ) { menu = 0 }
                DropdownMenu(
                    expanded = menu == 0,
                    onDismissRequest = { menu = null }
                ) {
                    listOf(
                        "今天" to today,
                        "明天" to Dates.addDays(today, 1),
                        "后天" to Dates.addDays(today, 2),
                        "下周一" to Dates.addDays(today, 7)
                    ).forEach { (label, day) ->
                        DropdownMenuItem(
                            text = { Text(label, style = Type.body) },
                            onClick = {
                                menu = null
                                onChange(draft.copy(due = day))
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("不排期", style = Type.body) },
                        onClick = {
                            menu = null
                            onChange(draft.copy(due = null, time = null))
                        }
                    )
                }
            }
            // 时间
            Box {
                Chip(
                    text = draft.time?.let { TodoParser.fmtMinute(it) } ?: "不设时间",
                    selected = draft.time != null,
                    leading = "◔"
                ) { menu = 1 }
                DropdownMenu(
                    expanded = menu == 1,
                    onDismissRequest = { menu = null }
                ) {
                    listOf(
                        "09:00" to 9 * 60,
                        "12:00" to 12 * 60,
                        "15:00" to 15 * 60,
                        "18:00" to 18 * 60,
                        "20:00" to 20 * 60
                    ).forEach { (label, m) ->
                        DropdownMenuItem(
                            text = { Text(label, style = Type.body) },
                            onClick = {
                                menu = null
                                onChange(draft.copy(time = m, due = draft.due ?: today))
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("不设时间", style = Type.body) },
                        onClick = {
                            menu = null
                            onChange(draft.copy(time = null))
                        }
                    )
                }
            }
            // 分组
            Box {
                Chip(text = draft.group, selected = false, leading = "◈") { menu = 2 }
                DropdownMenu(
                    expanded = menu == 2,
                    onDismissRequest = { menu = null }
                ) {
                    Todo.GROUPS.forEach { g ->
                        DropdownMenuItem(
                            text = { Text(g, style = Type.body) },
                            onClick = {
                                menu = null
                                onChange(draft.copy(group = g))
                            }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (draft.important) Palette.Gold.copy(alpha = 0.18f) else Palette.SurfaceMax
                    )
                    .border(
                        1.dp,
                        if (draft.important) Palette.Gold.copy(alpha = 0.5f) else Palette.Line,
                        RoundedCornerShape(50)
                    )
                    .clickable { onChange(draft.copy(important = !draft.important)) }
                    .padding(horizontal = 13.dp, vertical = 7.dp)
            ) {
                Text(
                    "⚡ 重要",
                    color = if (draft.important) Palette.Gold else Palette.TextDim,
                    style = Type.labelStrong
                )
            }
            Spacer(Modifier.weight(1f))
            GhostButton("取消", onClick = onCancel)
            Spacer(Modifier.width(4.dp))
            GoldButton("加上", enabled = draft.title.isNotBlank(), onClick = onConfirm)
        }
    }
}
