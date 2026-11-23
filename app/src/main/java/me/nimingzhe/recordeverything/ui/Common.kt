package me.nimingzhe.recordeverything.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ───────────────────────── 容器 ─────────────────────────

/**
 * 分组卡：一整个分区（过期 / 今天 / 已做完 / 导出 …）装在一张卡里，
 * 行与行之间用 [RowDivider] 的 1px 发丝线分隔，而不是空隙。
 */
@Composable
fun GroupCard(
    modifier: Modifier = Modifier,
    accent: Color? = null,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.lg))
            .background(Palette.Surface)
            .border(1.dp, accent?.copy(alpha = 0.28f) ?: Palette.Line, RoundedCornerShape(Radius.lg)),
        content = content
    )
}

/** 卡内分隔线。indent 用来让线从文字左边缘起画，而不是从卡片边缘。 */
@Composable
fun RowDivider(indent: Dp = 0.dp) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = indent)
            .height(1.dp)
            .background(Palette.LineSoft)
    )
}

/** 小标题：字距拉开，全小写气质，用于"过期 / 今天 / 已做完"。 */
@Composable
fun Eyebrow(text: String, color: Color = Palette.TextDim, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier,
        color = color,
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 1.4.sp
    )
}

// ───────────────────────── 控件 ─────────────────────────

/** 胶囊标签：筛选、快捷输入、草稿里的日期/时间/分组。 */
@Composable
fun Chip(
    text: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    leading: String? = null,
    onClick: (() -> Unit)? = null
) {
    val bg by animateColorAsState(
        if (selected) Palette.Gold.copy(alpha = 0.16f) else Palette.SurfaceHi,
        tween(160),
        label = "chipBg"
    )
    val line by animateColorAsState(
        if (selected) Palette.Gold.copy(alpha = 0.5f) else Palette.Line,
        tween(160),
        label = "chipLine"
    )
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .border(1.dp, line, RoundedCornerShape(50))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 13.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            Text(leading, fontSize = 10.sp)
            Spacer(Modifier.width(5.dp))
        }
        Text(
            text,
            color = if (selected) Palette.Gold else Palette.TextMid,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            maxLines = 1
        )
    }
}

/** 金色主按钮：渐变底 + 深色字。 */
@Composable
fun GoldButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (enabled) Brush.linearGradient(listOf(Palette.Gold, Palette.GoldSoft))
                else Brush.linearGradient(listOf(Palette.SurfaceMax, Palette.SurfaceMax))
            )
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = if (enabled) Color(0xFF231A0A) else Palette.TextDim,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/** 次级按钮：只有文字。 */
@Composable
fun GhostButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Text(
        text,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 9.dp),
        color = Palette.TextMid,
        fontSize = 13.sp
    )
}

/**
 * 细环：表示当日完成度。金色弧 + 极淡的轨道，线帽圆头。
 */
@Composable
fun Ring(
    progress: Float,
    size: Dp = 34.dp,
    stroke: Dp = 3.dp,
    color: Color = Palette.Gold
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(520),
        label = "ring"
    )
    Canvas(Modifier.size(size)) {
        val s = stroke.toPx()
        val inset = s / 2
        val arcSize = Size(this.size.width - s, this.size.height - s)
        drawArc(
            color = Palette.Line,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = arcSize,
            style = Stroke(width = s, cap = StrokeCap.Round)
        )
        if (animated > 0f) {
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * animated,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = s, cap = StrokeCap.Round)
            )
        }
    }
}

/** 左侧圆形勾选圈，带填充与对勾的动效。 */
@Composable
fun CheckCircle(checked: Boolean, onClick: () -> Unit) {
    val fill by animateColorAsState(
        if (checked) Palette.Gold else Color.Transparent,
        tween(180),
        label = "checkFill"
    )
    val line by animateColorAsState(
        if (checked) Palette.Gold else Palette.TextDim.copy(alpha = 0.45f),
        tween(180),
        label = "checkLine"
    )
    Box(
        Modifier
            .size(21.dp)
            .clip(CircleShape)
            .background(fill)
            .border(1.5.dp, line, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = checked,
            enter = fadeIn(tween(120)) + scaleIn(spring(dampingRatio = 0.5f), initialScale = 0.4f)
        ) {
            Text("✓", color = Color(0xFF231A0A), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/** 圆点，用于日历里的"这天有事"。 */
@Composable
fun Dot(color: Color, size: Int = 5) {
    Box(
        Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(color)
    )
}

/** 空状态：一句诗意的短句 + 一行更弱的补充。 */
@Composable
fun EmptyBlock(
    title: String,
    hint: String? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("✦", color = Palette.GoldDeep, fontSize = 18.sp)
        Spacer(Modifier.height(14.dp))
        Text(
            title,
            color = Palette.TextMid,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
        if (hint != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                hint,
                color = Palette.TextDim,
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** 一组筛选胶囊，横向排布。 */
@Composable
fun ChipRow(
    modifier: Modifier = Modifier,
    spacing: Dp = 8.dp,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit
) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}
