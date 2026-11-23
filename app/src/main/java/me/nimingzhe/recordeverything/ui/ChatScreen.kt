package me.nimingzhe.recordeverything.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.nimingzhe.recordeverything.data.ChatMessage
import me.nimingzhe.recordeverything.data.Draft
import me.nimingzhe.recordeverything.data.DraftKind
import me.nimingzhe.recordeverything.data.Ledger
import me.nimingzhe.recordeverything.data.Sender
import me.nimingzhe.recordeverything.nl.ChatParser
import me.nimingzhe.recordeverything.util.Dates

@Composable
fun ChatScreen(
    state: UiState,
    onSend: (String) -> Unit,
    onDraftChange: (Draft) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size, state.pending?.id) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    val context = LocalContext.current
    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val said = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!said.isNullOrBlank()) {
                onSend(said)
                input = ""
            }
        }
    }
    val startVoice: () -> Unit = {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "说说这笔账")
        }
        try {
            voiceLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "这台手机没有语音识别服务", Toast.LENGTH_SHORT).show()
        }
    }

    Column(Modifier.fillMaxSize().imePadding()) {
        Header(state)

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 14.dp, end = 14.dp, top = 8.dp, bottom = 12.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (state.messages.isEmpty()) {
                item { WelcomeHint(onSend) }
            }
            items(state.messages, key = { it.id }) { msg ->
                when {
                    msg.sender == Sender.USER -> UserBubble(msg.text)
                    msg.draft != null -> DraftBubble(
                        msg = msg,
                        onChange = onDraftChange,
                        onConfirm = onConfirm,
                        onCancel = onCancel
                    )
                    else -> AppBubble(msg.text)
                }
            }
        }

        InputBar(
            value = input,
            onVoice = startVoice,
            onValueChange = { input = it },
            onSend = {
                if (input.isNotBlank()) {
                    onSend(input)
                    input = ""
                }
            }
        )
    }
}

@Composable
private fun Header(state: UiState) {
    val month = state.records
        .filter { Dates.year(it.time) == state.year && Dates.month0(it.time) == state.month0 }
        .size
    Row(
        Modifier
            .fillMaxWidth()
            .background(Palette.Bg)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("记录", color = Palette.Text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "本月 ${month} 笔 · 连续 ${Dates.streak(state.records.map { it.time })} 天",
                color = Palette.TextDim,
                fontSize = 11.sp
            )
        }
        Box(
            Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Palette.SurfaceHi),
            contentAlignment = Alignment.Center
        ) {
            Text("📓", fontSize = 15.sp)
        }
    }
}

@Composable
private fun WelcomeHint(onSend: (String) -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 26.dp)
    ) {
        Text("今天记点什么？", color = Palette.Text, fontSize = 17.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))
        Text(
            "直接用人话写就行，我会自动拆成金额、类别、渠道和日期。\n比如「昨天午饭 25 微信」「工资 8000 银行卡」。",
            color = Palette.TextDim,
            fontSize = 13.sp,
            lineHeight = 20.sp
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("昨天午饭 25 微信", icon = "🍜", onClick = { onSend("昨天午饭 25 微信") })
            Chip("打车 18 支付宝", icon = "🚕", onClick = { onSend("打车 18 支付宝") })
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("工资 8000", icon = "💸", onClick = { onSend("工资 8000 银行卡") })
            Chip("体重 62.5", icon = "⚖️", onClick = { onSend("体重 62.5") })
        }
    }
}

@Composable
private fun UserBubble(text: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Box(
            Modifier
                .widthIn(max = 270.dp)
                .clip(RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp))
                .background(Palette.Gold.copy(alpha = 0.16f))
                .border(0.5.dp, Palette.Gold.copy(alpha = 0.35f), RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(text, color = Palette.Text, fontSize = 15.sp, lineHeight = 21.sp)
        }
    }
}

@Composable
private fun AppBubble(text: String, card: @Composable (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Column(Modifier.widthIn(max = 300.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Avatar()
                Spacer(Modifier.width(8.dp))
                Column {
                    if (text.isNotEmpty()) {
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp))
                                .background(Palette.Surface)
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text(text, color = Palette.Text, fontSize = 15.sp, lineHeight = 21.sp)
                        }
                    }
                    card?.invoke()
                }
            }
        }
    }
}

@Composable
private fun Avatar() {
    Box(
        Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Palette.SurfaceHi)
            .border(0.5.dp, Palette.Divider, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text("📖", fontSize = 13.sp)
    }
}

@Composable
private fun DraftBubble(
    msg: ChatMessage,
    onChange: (Draft) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    val draft = msg.draft ?: return
    AppBubble(msg.text) {
        DraftCard(draft, onChange, onConfirm, onCancel)
    }
}

@Composable
private fun DraftCard(
    draft: Draft,
    onChange: (Draft) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    var remark by remember(draft.time, draft.amount) { mutableStateOf(draft.remark) }
    var categoryOpen by remember { mutableStateOf(false) }
    var channelOpen by remember { mutableStateOf(false) }

    Column(
        Modifier
            .padding(top = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Palette.Surface)
            .border(0.5.dp, Palette.Divider, RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        // 第一行：图标 + 类别 + 类型 + 金额
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(Palette.SurfaceHi),
                contentAlignment = Alignment.Center
            ) { Text(categoryEmoji(draft.category), fontSize = 18.sp) }

            Spacer(Modifier.width(10.dp))

            Text(
                if (draft.kind == DraftKind.MONEY) draft.category else (draft.eventName ?: "记录"),
                color = Palette.Text,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier.clip(CircleShape).background(Palette.SurfaceHi)
                    .padding(horizontal = 7.dp, vertical = 2.dp)
            ) {
                Text(
                    if (draft.income) "收入" else "支出",
                    color = Palette.TextDim,
                    fontSize = 10.sp
                )
            }

            Spacer(Modifier.weight(1f))

            Text(
                (if (draft.income) "+¥" else "¥") + Ledger.money(draft.amount ?: 0.0),
                color = if (draft.income) Palette.Income else Palette.Text,
                fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(Modifier.height(6.dp))
        Box(
            Modifier.fillMaxWidth().height(0.5.dp).background(Palette.Divider)
        )
        Spacer(Modifier.height(10.dp))

        // 第二行：渠道 / 日期 胶囊（可点）
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box {
                Chip(
                    text = draft.channel ?: "现金",
                    icon = channelEmoji(draft.channel),
                    onClick = { channelOpen = true }
                )
                DropdownMenu(channelOpen, onDismissRequest = { channelOpen = false }) {
                    ChatParser.channels.forEach { ch ->
                        DropdownMenuItem(
                            text = { Text("${channelEmoji(ch)}  $ch", color = Palette.Text) },
                            onClick = {
                                onChange(draft.copy(channel = ch))
                                channelOpen = false
                            }
                        )
                    }
                }
            }
            Box {
                Chip(text = Dates.chip(draft.time), icon = "📅", onClick = { categoryOpen = true })
                DropdownMenu(categoryOpen, onDismissRequest = { categoryOpen = false }) {
                    listOf(
                        "今天" to Dates.startOfDay(System.currentTimeMillis()),
                        "昨天" to Dates.addDays(Dates.startOfDay(System.currentTimeMillis()), -1),
                        "前天" to Dates.addDays(Dates.startOfDay(System.currentTimeMillis()), -2)
                    ).forEach { (label, millis) ->
                        DropdownMenuItem(
                            text = { Text(label, color = Palette.Text) },
                            onClick = {
                                onChange(draft.copy(time = millis))
                                categoryOpen = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // 备注输入
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(0.5.dp, Palette.Divider, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            if (remark.isEmpty()) {
                Text("+ 备注，直接写", color = Palette.TextDim, fontSize = 13.sp)
            }
            BasicTextField(
                value = remark,
                onValueChange = {
                    remark = it
                    onChange(draft.copy(remark = it))
                },
                textStyle = TextStyle(color = Palette.Text, fontSize = 13.sp),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(12.dp))

        // 按钮
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.weight(1f))
            Box(
                Modifier.clip(CircleShape).clickable { onCancel() }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text("不对", color = Palette.TextDim, fontSize = 13.sp)
            }
            Box(
                Modifier
                    .clip(CircleShape)
                    .background(Brush.horizontalGradient(listOf(Palette.Gold, Palette.GoldDim)))
                    .clickable { onConfirm() }
                    .padding(horizontal = 18.dp, vertical = 9.dp)
            ) {
                Text("只记这笔", color = Color(0xFF221A0C), fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun InputBar(
    value: String,
    onVoice: () -> Unit,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Palette.Bg)
            .padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Palette.SurfaceHi)
                .clickable { onVoice() },
            contentAlignment = Alignment.Center
        ) { Text("🎙", fontSize = 15.sp) }

        Spacer(Modifier.width(8.dp))

        Box(
            Modifier
                .weight(1f)
                .clip(RoundedCornerShape(20.dp))
                .background(Palette.Surface)
                .border(0.5.dp, Palette.Divider, RoundedCornerShape(20.dp))
                .padding(horizontal = 14.dp, vertical = 11.dp)
        ) {
            if (value.isEmpty()) {
                Text("例如：昨天午饭 25 微信", color = Palette.TextDim, fontSize = 14.sp)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(color = Palette.Text, fontSize = 14.sp),
                maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.width(8.dp))

        Box(
            Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Palette.Gold, Palette.GoldDim)))
                .clickable { onSend() },
            contentAlignment = Alignment.Center
        ) { Text("↑", color = Color(0xFF221A0C), fontSize = 17.sp, fontWeight = FontWeight.Bold) }
    }
}

fun categoryEmoji(category: String): String = when (category) {
    "餐饮" -> "🍜"
    "交通" -> "🚕"
    "购物" -> "🛍"
    "居住" -> "🏠"
    "娱乐" -> "🎬"
    "医疗" -> "💊"
    "学习" -> "📚"
    "人情" -> "🎁"
    "工资" -> "💸"
    "红包" -> "🧧"
    "报销" -> "🧾"
    "退款" -> "↩️"
    else -> "📦"
}

fun channelEmoji(channel: String?): String = when (channel) {
    "微信" -> "💬"
    "支付宝" -> "🅰️"
    "现金" -> "💵"
    "银行卡" -> "💳"
    "信用卡" -> "🏦"
    "花呗" -> "🌸"
    else -> "💵"
}
