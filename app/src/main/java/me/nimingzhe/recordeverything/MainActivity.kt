package me.nimingzhe.recordeverything

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import me.nimingzhe.recordeverything.data.Todo
import me.nimingzhe.recordeverything.ui.AllScreen
import me.nimingzhe.recordeverything.ui.AppViewModel
import me.nimingzhe.recordeverything.ui.CalendarScreen
import me.nimingzhe.recordeverything.ui.EditDialog
import me.nimingzhe.recordeverything.ui.Palette
import me.nimingzhe.recordeverything.ui.Radius
import me.nimingzhe.recordeverything.ui.RecordEverythingTheme
import me.nimingzhe.recordeverything.ui.SettingsScreen
import me.nimingzhe.recordeverything.ui.Tab
import me.nimingzhe.recordeverything.ui.TodayScreen
import me.nimingzhe.recordeverything.ui.Type

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
    var editing by remember { mutableStateOf<Todo?>(null) }

    Box(
        Modifier
            .fillMaxSize()
            // 背景从最顶端开始铺，状态栏区域与页面顶部的暖色渐变接得上
            .background(Brush.verticalGradient(listOf(Palette.BgTop, Palette.Bg), 0f, 520f))
            .statusBarsPadding()
    ) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                when (state.tab) {
                    Tab.TODAY -> TodayScreen(
                        state = state,
                        overdue = vm.overdue(),
                        todayList = vm.todayList(),
                        onToggle = vm::toggle,
                        onEdit = { editing = it },
                        onTomorrow = vm::snoozeToTomorrow,
                        onToday = { vm.setDue(it, System.currentTimeMillis()) },
                        onDelete = vm::delete,
                        onAdd = vm::compose,
                        onDraftChange = vm::updateDraft,
                        onConfirm = vm::confirmDraft,
                        onCancel = vm::cancelDraft
                    )
                    Tab.CALENDAR -> CalendarScreen(
                        state = state,
                        onPrevMonth = { vm.shiftMonth(-1) },
                        onNextMonth = { vm.shiftMonth(1) },
                        onSelectDay = vm::selectDay,
                        onToggle = vm::toggle,
                        onEdit = { editing = it },
                        onTomorrow = vm::snoozeToTomorrow,
                        onToday = { vm.setDue(it, System.currentTimeMillis()) },
                        onDelete = vm::delete,
                        onAdd = vm::compose,
                        onDraftChange = vm::updateDraft,
                        onConfirm = vm::confirmDraft,
                        onCancel = vm::cancelDraft
                    )
                    Tab.ALL -> AllScreen(
                        state = state,
                        list = vm.allFiltered(),
                        counts = vm.counts(),
                        onFilter = vm::setFilter,
                        onGroup = vm::setGroup,
                        onToggle = vm::toggle,
                        onEdit = { editing = it },
                        onTomorrow = vm::snoozeToTomorrow,
                        onToday = { vm.setDue(it, System.currentTimeMillis()) },
                        onDelete = vm::delete,
                        onAdd = vm::compose,
                        onDraftChange = vm::updateDraft,
                        onConfirm = vm::confirmDraft,
                        onCancel = vm::cancelDraft
                    )
                    Tab.SETTINGS -> SettingsScreen(
                        state = state,
                        counts = vm.counts(),
                        streak = vm.streak(),
                        onExportMarkdown = vm::exportMarkdown,
                        onExportJson = vm::exportJson,
                        onExportCsv = vm::exportCsv,
                        onClear = vm::clearAll
                    )
                }
            }
            BottomPill(current = state.tab, onSelect = vm::setTab)
        }
    }

    editing?.let { todo ->
        EditDialog(
            todo = todo,
            onDismiss = { editing = null },
            onSave = {
                vm.update(it)
                editing = null
            },
            onDelete = {
                vm.delete(it.id)
                editing = null
            }
        )
    }
}

private data class TabItem(
    val tab: Tab,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

private val tabs = listOf(
    TabItem(Tab.TODAY, "今天", Icons.Filled.Home),
    TabItem(Tab.CALENDAR, "日历", Icons.Filled.DateRange),
    TabItem(Tab.ALL, "全部", Icons.Filled.List),
    TabItem(Tab.SETTINGS, "设置", Icons.Filled.Settings)
)

/**
 * 悬浮胶囊导航。不是通栏白条：它浮在内容之上、有描边，
 * 选中项是一枚金色实心圆 + 深色字形。这样底部不会和输入条打架。
 */
@Composable
private fun BottomPill(current: Tab, onSelect: (Tab) -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 10.dp)
            .navigationBarsPadding()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Radius.xl))
                .background(Palette.SurfaceHi.copy(alpha = 0.96f))
                .border(1.dp, Palette.Line, RoundedCornerShape(Radius.xl))
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { item ->
                val selected = item.tab == current
                val bg by animateColorAsState(
                    if (selected) Palette.Gold else Color.Transparent,
                    tween(180),
                    label = "tabBg"
                )
                val fg by animateColorAsState(
                    if (selected) Color(0xFF231A0A) else Palette.TextDim,
                    tween(180),
                    label = "tabFg"
                )
                val scale by animateFloatAsState(if (selected) 1f else 0.92f, label = "tabScale")
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .clickable { onSelect(item.tab) }
                        .padding(vertical = 5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        Modifier
                            .size(28.dp)
                            .scale(scale)
                            .clip(CircleShape)
                            .background(bg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(item.icon, contentDescription = item.label, tint = fg, modifier = Modifier.size(15.dp))
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(
                        item.label,
                        color = if (selected) Palette.Gold else Palette.TextDim,
                        style = Type.nav,
                        fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
                    )
                }
            }
        }
    }
}
