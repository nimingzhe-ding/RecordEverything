package me.nimingzhe.recordeverything.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import me.nimingzhe.recordeverything.util.Dates
import java.io.File

@Composable
fun SettingsScreen(
    state: UiState,
    counts: Triple<Int, Int, Int>,
    streak: Int,
    onExportMarkdown: () -> String,
    onExportJson: () -> String,
    onExportCsv: () -> String,
    onClear: () -> Unit
) {
    val context = LocalContext.current
    var confirmClear by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    fun share(name: String, content: String, mime: String) {
        try {
            val dir = File(context.cacheDir, "export").apply { mkdirs() }
            val f = File(dir, name)
            f.writeText(content)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", f)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mime
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "导出 $name"))
        } catch (t: Throwable) {
            message = "导出失败：${t.message}"
        }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 28.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)
    ) {
        item {
            Column(Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
                Text("设置", color = Palette.Text, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                Text("数据都在你自己的手机上", color = Palette.TextDim, fontSize = 11.sp)
            }
        }

        item {
            SectionCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Palette.SurfaceHi),
                        contentAlignment = Alignment.Center
                    ) { Text("📓", fontSize = 20.sp) }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("RecordEverything", color = Palette.Text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        Text("记录 · 待办 · 日历，全部离线", color = Palette.TextDim, fontSize = 11.sp)
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth()) {
                    Stat("未完成", "${counts.first}")
                    Stat("今天", "${counts.second}")
                    Stat("连续", "${streak}天")
                }
            }
        }

        item {
            SectionCard(Modifier.fillMaxWidth()) {
                Text("导出", color = Palette.Text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(10.dp))
                SettingRow("Markdown 清单", "带勾选框，可以直接贴进笔记") {
                    share("recordeverything.md", onExportMarkdown(), "text/markdown")
                }
                SettingRow("JSON 数据", "完整备份，字段与原 App 对齐") {
                    share("recordeverything.json", onExportJson(), "application/json")
                }
                SettingRow("CSV 表格", "标题 / 日期 / 时间 / 分组 / 状态 / 备注") {
                    share("recordeverything.csv", onExportCsv(), "text/csv")
                }
            }
        }

        item {
            SectionCard(Modifier.fillMaxWidth()) {
                Text("关于", color = Palette.Text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(10.dp))
                InfoRow("数据存储", "应用私有目录下的一个 JSON")
                InfoRow("网络", "不联网，没有账号，没有会员")
                InfoRow("来源", "对照 MarkTimes 的本地记录能力重写")
            }
        }

        item {
            SectionCard(Modifier.fillMaxWidth()) {
                Text("清除", color = Palette.Expense, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(6.dp))
                Text("会删掉本地所有待办，不可恢复。", color = Palette.TextDim, fontSize = 11.sp)
                Spacer(Modifier.height(10.dp))
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Palette.Expense.copy(alpha = 0.14f))
                        .clickable { confirmClear = true }
                        .padding(horizontal = 16.dp, vertical = 9.dp)
                ) {
                    Text("清空全部数据", color = Palette.Expense, fontSize = 12.sp)
                }
            }
        }

        item {
            Column(Modifier.padding(top = 4.dp, bottom = 24.dp)) {
                Text(
                    "本地优先 · 一个 JSON 装下全部记录",
                    color = Palette.TextDim.copy(alpha = 0.7f),
                    fontSize = 10.sp
                )
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("清空全部数据？") },
            text = { Text("所有待办都会被删除，无法恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    onClear()
                    message = "已清空"
                }) { Text("清空", color = Palette.Expense) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("算了") }
            }
        )
    }

    message?.let {
        AlertDialog(
            onDismissRequest = { message = null },
            confirmButton = { TextButton(onClick = { message = null }) { Text("好") } },
            text = { Text(it) }
        )
    }
}

@Composable
private fun RowScope.Stat(label: String, value: String) {
    Column(Modifier.weight(1f)) {
        Text(value, color = Palette.Text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Text(label, color = Palette.TextDim, fontSize = 10.sp)
    }
}

@Composable
private fun SettingRow(title: String, desc: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Palette.Text, fontSize = 13.sp)
            Text(desc, color = Palette.TextDim, fontSize = 10.sp)
        }
        Text("›", color = Palette.TextDim, fontSize = 15.sp)
    }
}

@Composable
private fun InfoRow(k: String, v: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Text(k, color = Palette.TextDim, fontSize = 11.sp, modifier = Modifier.width(72.dp))
        Text(v, color = Palette.Text, fontSize = 11.sp)
    }
}
