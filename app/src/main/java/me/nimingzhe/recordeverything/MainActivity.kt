package me.nimingzhe.recordeverything

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import me.nimingzhe.recordeverything.ui.AppViewModel
import me.nimingzhe.recordeverything.ui.CalendarScreen
import me.nimingzhe.recordeverything.ui.ChatScreen
import me.nimingzhe.recordeverything.ui.Palette
import me.nimingzhe.recordeverything.ui.RecordEverythingTheme
import me.nimingzhe.recordeverything.ui.SettingsScreen
import me.nimingzhe.recordeverything.ui.StatsScreen
import me.nimingzhe.recordeverything.ui.Tab

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RecordEverythingTheme {
                Root()
            }
        }
    }
}

@Composable
private fun Root(vm: AppViewModel = viewModel()) {
    val state by vm.state.collectAsState()

    Column(
        Modifier
            .fillMaxSize()
            .background(Palette.Bg)
            .statusBarsPadding()
    ) {
        Box(Modifier.weight(1f)) {
            when (state.tab) {
                Tab.CHAT -> ChatScreen(
                    state = state,
                    onSend = vm::send,
                    onDraftChange = vm::updateDraft,
                    onConfirm = vm::confirmDraft,
                    onCancel = vm::cancelDraft
                )
                Tab.CALENDAR -> CalendarScreen(
                    state = state,
                    onPrevMonth = { vm.shiftMonth(-1) },
                    onNextMonth = { vm.shiftMonth(1) },
                    onSelectDay = vm::selectDay,
                    onToday = vm::jumpToToday,
                    onDelete = vm::deleteRecord,
                    onQuickAdd = vm::addMoney
                )
                Tab.STATS -> StatsScreen(
                    state = state,
                    onPrevMonth = { vm.shiftMonth(-1) },
                    onNextMonth = { vm.shiftMonth(1) }
                )
                Tab.SETTINGS -> SettingsScreen(
                    state = state,
                    onExportJson = vm::exportJson,
                    onExportCsv = vm::exportCsv,
                    onClear = vm::clearAll
                )
            }
        }
        BottomBar(current = state.tab, onSelect = vm::setTab)
    }
}

private data class TabItem(val tab: Tab, val label: String, val icon: String)

private val tabs = listOf(
    TabItem(Tab.CHAT, "聊天", "💬"),
    TabItem(Tab.CALENDAR, "日历", "📅"),
    TabItem(Tab.STATS, "统计", "📊"),
    TabItem(Tab.SETTINGS, "设置", "⚙️")
)

@Composable
private fun BottomBar(current: Tab, onSelect: (Tab) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Palette.Bg)
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEach { item ->
            val selected = item.tab == current
            Column(
                Modifier
                    .weight(1f)
                    .clip(CircleShape)
                    .clickable { onSelect(item.tab) }
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (selected) Palette.Gold.copy(alpha = 0.16f) else Color.Transparent),
                    contentAlignment = Alignment.Center
                ) { Text(item.icon, fontSize = 14.sp) }
                Spacer(Modifier.height(2.dp))
                Text(
                    item.label,
                    color = if (selected) Palette.Gold else Palette.TextDim,
                    fontSize = 10.sp,
                    fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
                )
            }
        }
        Spacer(Modifier.width(2.dp))
    }
}
