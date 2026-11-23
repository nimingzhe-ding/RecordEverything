package me.nimingzhe.recordeverything.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 设置。分三张卡：概览 / 导出 / 关于。
 * 概览用衬线大数字，和今天页的日期呼应。
 */
@Composable
fun SettingsScreen(
    state: UiState,
    counts: Triple<Int, Int, Int>,
    streak: Int,
    onExportMarkdown: () -> Unit,
    onExportJson: () -> Unit,
    onExportCsv: () -> Unit,
    onClear: () -> Unit
) {
    var confirmClear by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 28.dp)
    ) {
        item {
            Column(Modifier.padding(top = 16.dp, bottom = 18.dp)) {
                Text(
                    "设置",
                    color = Palette.Text,
                    fontSize = 26.sp,
                    lineHeight = 32.sp,
                    fontFamily = FontFamily.Serif,
                    letterSpacing = (-0.5).sp
                )
                Spacer(Modifier.height(4.dp))
                Text("数据都在你自己的手机上", color = Palette.TextMid, fontSize = 12.sp)
            }
        }

        item {
            GroupCard {
                Column(Modifier.padding(horizontal = 18.dp, vertical = 18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(13.dp))
                                .background(
                                    Brush.linearGradient(listOf(Palette.Gold.copy(alpha = 0.22f), Palette.SurfaceMax))
                                ),
                            contentAlignment = Alignment.Center
                        ) { Text("✦", color = Palette.Gold, fontSize = 17.sp) }
                        Spacer(Modifier.width(13.dp))
                        Column {
                            Text(
                                "RecordEverything",
                                color = Palette.Text,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.height(2.dp))
                            Text("记录 · 待办 · 日历，全部离线", color = Palette.TextDim, fontSize = 11.sp)
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                        Stat("未完成", "${counts.first}")
                        Stat("今天", "${counts.second}")
                        Stat("连续", if (streak > 0) "${streak}天" else "—")
                    }
                }
            }
        }

        item { SectionTitle("导 出") }
        item {
            GroupCard {
                ActionRow("Markdown 清单", "带勾选框，可以直接贴进笔记", "⤓", onExportMarkdown)
                RowDivider(indent = 18.dp)
                ActionRow("JSON 数据", "完整备份，一个文件装下全部", "⤓", onExportJson)
                RowDivider(indent = 18.dp)
                ActionRow("CSV 表格", "标题 / 日期 / 时间 / 分组 / 状态", "⤓", onExportCsv)
            }
        }

        item { SectionTitle("关 于") }
        item {
            GroupCard {
                InfoRow("存储", "应用私有目录下的一个 JSON 文件")
                RowDivider(indent = 18.dp)
                InfoRow("网络", "不联网，没有账号，没有会员")
                RowDivider(indent = 18.dp)
                InfoRow("来源", "对照 MarkTimes 的本地记录能力重写")
            }
        }

        item { SectionTitle("危 险 操 作", accent = Palette.Danger.copy(alpha = 0.85f)) }
        item {
            GroupCard(accent = Palette.Danger) {
                Column(Modifier.padding(18.dp)) {
                    Text("清掉全部待办", color = Palette.Text, fontSize = 14.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("删掉之后不可恢复，建议先导出一份 JSON。", color = Palette.TextDim, fontSize = 11.sp)
                    Spacer(Modifier.height(14.dp))
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Palette.Danger.copy(alpha = 0.14f))
                            .clickable { confirmClear = true }
                            .padding(horizontal = 16.dp, vertical = 9.dp)
                    ) {
                        Text("清空", color = Palette.Danger, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 28.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("本地优先 · 一个 JSON 装下全部记录", color = Palette.TextDim, fontSize = 10.sp)
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("确定清空？", fontSize = 16.sp) },
            text = { Text("所有待办会被删掉，无法恢复。", color = Palette.TextMid, fontSize = 13.sp) },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    onClear()
                }) { Text("清空", color = Palette.Danger) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) {
                    Text("算了", color = Palette.TextMid)
                }
            },
            containerColor = Palette.SurfaceHi,
            titleContentColor = Palette.Text,
            textContentColor = Palette.TextMid
        )
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.Stat(label: String, value: String) {
    Column(Modifier.weight(1f)) {
        Text(
            value,
            color = Palette.Text,
            fontSize = 24.sp,
            lineHeight = 28.sp,
            fontFamily = FontFamily.Serif
        )
        Spacer(Modifier.height(3.dp))
        Text(label, color = Palette.TextDim, fontSize = 10.sp, letterSpacing = 1.sp)
    }
}

@Composable
private fun ActionRow(title: String, sub: String, icon: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(Palette.SurfaceMax),
            contentAlignment = Alignment.Center
        ) { Text(icon, color = Palette.GoldSoft, fontSize = 13.sp) }
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Palette.Text, fontSize = 14.sp)
            Spacer(Modifier.height(3.dp))
            Text(sub, color = Palette.TextDim, fontSize = 11.sp)
        }
        Text("›", color = Palette.TextDim, fontSize = 18.sp)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Palette.TextDim, fontSize = 12.sp, modifier = Modifier.width(64.dp))
        Text(value, color = Palette.TextMid, fontSize = 12.sp)
    }
}
