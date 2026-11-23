package me.nimingzhe.recordeverything.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 圆角胶囊，用于筛选与快捷输入。 */
@Composable
fun Chip(
    text: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) Palette.Gold.copy(alpha = 0.18f) else Palette.SurfaceHi)
            .border(
                1.dp,
                if (selected) Palette.Gold.copy(alpha = 0.55f) else Palette.Divider,
                RoundedCornerShape(50)
            )
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text,
            color = if (selected) Palette.Gold else Palette.TextDim,
            fontSize = 12.sp
        )
    }
}

@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Palette.Surface)
            .padding(16.dp),
        content = content
    )
}

@Composable
fun EmptyHint(text: String, modifier: Modifier = Modifier) {
    Column(
        modifier.padding(vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🌙", fontSize = 26.sp)
        Spacer(Modifier.size(10.dp))
        Text(
            text,
            color = Palette.TextDim,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun Dot(color: Color, size: Int = 6) {
    Box(
        Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(color)
    )
}

/** 左侧圆形勾选圈。 */
@Composable
fun CheckCircle(checked: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(if (checked) Palette.Gold.copy(alpha = 0.22f) else Color.Transparent)
            .border(1.5.dp, if (checked) Palette.Gold else Palette.Divider, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (checked) Text("✓", color = Palette.Gold, fontSize = 12.sp)
    }
}
