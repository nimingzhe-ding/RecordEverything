package me.nimingzhe.recordeverything.ui

import androidx.compose.foundation.background
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Chip(
    text: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    icon: String? = null,
    onClick: (() -> Unit)? = null
) {
    val bg = if (selected) Palette.Gold.copy(alpha = 0.18f) else Palette.SurfaceHi
    val fg = if (selected) Palette.Gold else Palette.TextDim
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(bg)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Text(icon, fontSize = 12.sp)
            Spacer(Modifier.width(4.dp))
        }
        Text(text, color = fg, fontSize = 12.sp)
    }
}

@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Palette.Surface)
            .padding(16.dp)
    ) { content() }
}

@Composable
fun MoneyText(
    value: Double,
    modifier: Modifier = Modifier,
    colored: Boolean = true,
    prefix: String = "",
    fontSize: Int = 15
) {
    val sign = when {
        value > 0 -> "+"
        value < 0 -> "-"
        else -> ""
    }
    val color = if (!colored) Palette.Text else if (value > 0) Palette.Income else if (value < 0) Palette.Expense else Palette.TextDim
    Text(
        text = (if (prefix.isNotEmpty()) prefix else sign) + me.nimingzhe.recordeverything.data.Ledger.money(kotlin.math.abs(value)),
        color = color,
        fontSize = fontSize.sp,
        fontWeight = if (colored) FontWeight.Medium else FontWeight.Normal,
        modifier = modifier
    )
}

@Composable
fun EmptyHint(text: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🌙", fontSize = 28.sp)
        Spacer(Modifier.height(10.dp))
        Text(text, color = Palette.TextDim, fontSize = 13.sp)
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
