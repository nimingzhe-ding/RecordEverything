package me.nimingzhe.recordeverything.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.nimingzhe.recordeverything.util.Dates
import java.io.File

@Composable
fun SettingsScreen(
    state: UiState,
    onExportJson: () -> String,
    onExportCsv: () -> String,
    onClear: () -> Unit
) {
    val context = LocalContext.current
    var confirmClear by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val total = state.records.size
    val moneyCount = state.records.count { r ->
        val ev = state.events.firstOrNull { it.id == r.eventId }
        me.nimingzhe.recordeverything.data.Ledger.amount(r, ev) != null
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 30.dp)) {
        item {
            Column(Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
                Text("设置", color = Palette.Text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Text("数据都在你自己的手机上", color = Palette.TextDim, fontSize = 11.sp)
            }
        }

        item {
            Column(
                Modifier
                    .padding(horizontal = 14.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Palette.Surface)
                    .padding(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                            .background(Palette.SurfaceHi),
                        contentAlignment = Alignment.Center
                    ) { Text("📓", fontSize = 20.sp) }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("记录", color = Palette.Text, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Text(
                            "$total 条记录 · 其中 $moneyCount 笔有金额",
                            color = Palette.TextDim, fontSize = 11.sp
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row {
                    Stat("连续", "${Dates.streak(state.records.map { it.time })} 天")
                    Stat("事件", "${state.events.size} 个")
                    Stat("最早", state.records.minByOrNull { it.time }?.let { Dates.formatDateShort(it.time) } ?: "—")
                }
            }
        }

        item { GroupTitle("导出与备份") }
        item {
            SettingRow("导出 JSON", "完整字段，可自己写脚本处理") {
                saveAndShare(context, "recordeverything.json", onExportJson())
                message = "已导出 JSON"
            }
        }
        item {
            SettingRow("导出 CSV", "直接扔进 Excel / 表格") {
                saveAndShare(context, "recordeverything.csv", onExportCsv())
                message = "已导出 CSV"
            }
        }

        item { GroupTitle("记录方式") }
        item {
            InfoRow("聊天式输入", "一句中文自动拆出金额、类别、渠道、日期")
        }
        item {
            InfoRow("离线运行", "不联网、不上传、不需要账号")
        }
        item {
            InfoRow("农历与红绿", "日历格子带农历，支出红、收入绿")
        }

        item { GroupTitle("危险操作") }
        item {
            SettingRow("清空全部记录", "只删记录，保留事件结构", danger = true) {
                confirmClear = true
            }
        }

        item {
            Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                Text("RecordEverything · 参考 MarkTimes 1.5.1 的结构重写", color = Palette.TextDim.copy(alpha = 0.6f), fontSize = 10.sp)
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("清空全部记录？", color = Palette.Text) },
            text = { Text("这个操作不可撤销，建议先导出一份 JSON。", color = Palette.TextDim) },
            confirmButton = {
                TextButton(onClick = {
                    onClear()
                    confirmClear = false
                    message = "已清空"
                }) { Text("清空", color = Palette.Expense) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("取消", color = Palette.TextDim) }
            },
            containerColor = Palette.Surface
        )
    }

    message?.let {
        AlertDialog(
            onDismissRequest = { message = null },
            title = { Text(it, color = Palette.Text) },
            confirmButton = {
                TextButton(onClick = { message = null }) { Text("好", color = Palette.Gold) }
            },
            containerColor = Palette.Surface
        )
    }
}

@Composable
private fun RowScope.Stat(label: String, value: String) {
    Column(Modifier.weight(1f)) {
        Text(label, color = Palette.TextDim, fontSize = 10.sp)
        Spacer(Modifier.height(3.dp))
        Text(value, color = Palette.Text, fontSize = 14.sp)
    }
}

@Composable
private fun GroupTitle(text: String) {
    Text(
        text,
        modifier = Modifier.padding(start = 22.dp, top = 22.dp, bottom = 8.dp),
        color = Palette.TextDim, fontSize = 11.sp
    )
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String,
    danger: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Palette.Surface)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = if (danger) Palette.Expense else Palette.Text, fontSize = 14.sp)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, color = Palette.TextDim, fontSize = 11.sp)
        }
        Text("›", color = Palette.TextDim, fontSize = 16.sp)
    }
}

@Composable
private fun InfoRow(title: String, subtitle: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Palette.Surface)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Palette.Text, fontSize = 14.sp)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, color = Palette.TextDim, fontSize = 11.sp)
        }
    }
}

private fun saveAndShare(context: Context, name: String, content: String) {
    val dir = File(context.cacheDir, "export")
    if (!dir.exists()) dir.mkdirs()
    val f = File(dir, name)
    f.writeText(content)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = if (name.endsWith("json")) "application/json" else "text/csv"
        putExtra(Intent.EXTRA_STREAM, androidx.core.content.FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", f
        ))
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "导出"))
}
