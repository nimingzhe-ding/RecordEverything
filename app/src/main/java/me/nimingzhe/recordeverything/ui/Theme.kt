package me.nimingzhe.recordeverything.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 设计系统 · 深色暖金
 *
 * 底色是"炭黑偏暖"，不带蓝，避免冷调科技感；金色只做强调，不做大面积填充。
 * 层级靠 three 层 surface 拉开（Surface → SurfaceHi → SurfaceMax）+ 1px 发丝线，
 * 而不是靠阴影——深色界面上的阴影几乎看不见，反而会糊成一团。
 */
object Palette {
    val Bg = Color(0xFF0D0B09)
    val BgTop = Color(0xFF151009)
    val Surface = Color(0xFF17130F)
    val SurfaceHi = Color(0xFF201A14)
    val SurfaceMax = Color(0xFF2A2219)
    val Line = Color(0xFF2A221A)
    val LineSoft = Color(0xFF221C15)

    val Gold = Color(0xFFE5C07A)
    val GoldSoft = Color(0xFFC8A25D)
    val GoldDeep = Color(0xFF7E6231)

    val Text = Color(0xFFF3ECE0)
    val TextMid = Color(0xFFB6A996)
    val TextDim = Color(0xFF776B5A)

    val Danger = Color(0xFFD8735F)
    val Success = Color(0xFF6FB57C)
}

/** 圆角尺度：卡片 20、内层 14、控件 12。 */
object Radius {
    val sm = 12.dp
    val md = 16.dp
    val lg = 22.dp
    val xl = 28.dp
}

private val Colors = darkColorScheme(
    primary = Palette.Gold,
    onPrimary = Color(0xFF1C1408),
    primaryContainer = Palette.SurfaceMax,
    onPrimaryContainer = Palette.Gold,
    secondary = Palette.GoldSoft,
    onSecondary = Palette.Text,
    background = Palette.Bg,
    onBackground = Palette.Text,
    surface = Palette.Surface,
    onSurface = Palette.Text,
    surfaceVariant = Palette.SurfaceHi,
    onSurfaceVariant = Palette.TextMid,
    error = Palette.Danger,
    outline = Palette.Line
)

/**
 * 字体层级。日期与月份用衬线（Android 上是 Noto Serif CJK），
 * 让"今天"这种大标题有一点编辑部气质；正文保持无衬线，密度优先。
 */
private val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontSize = 44.sp,
        lineHeight = 48.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = (-1).sp,
        color = Palette.Text
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.Serif,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = (-0.5).sp,
        color = Palette.Text
    ),
    titleLarge = TextStyle(
        fontSize = 19.sp,
        lineHeight = 26.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.2.sp,
        color = Palette.Text
    ),
    titleMedium = TextStyle(
        fontSize = 15.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.Medium,
        color = Palette.Text
    ),
    bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 23.sp, color = Palette.Text),
    bodyMedium = TextStyle(fontSize = 13.sp, lineHeight = 20.sp, color = Palette.TextMid),
    bodySmall = TextStyle(fontSize = 11.sp, lineHeight = 16.sp, color = Palette.TextDim),
    labelSmall = TextStyle(
        fontSize = 10.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 1.2.sp,
        color = Palette.TextDim
    )
)

@Composable
fun RecordEverythingTheme(content: @Composable () -> Unit) {
    @Suppress("UNUSED_EXPRESSION")
    isSystemInDarkTheme()
    // 关键：M3 的 Text 默认继承 bodyLarge（lineHeight 23sp）。只设 fontSize 不设
    // lineHeight 时，8sp 的小字仍占 23sp 行高，会把日历格子这类紧排内容撑爆。
    // 这里把局部默认样式换成"只有颜色、没有字号与行高"，让每处 Text 自己决定。
    CompositionLocalProvider(
        LocalTextStyle provides TextStyle(color = Palette.Text)
    ) {
        MaterialTheme(colorScheme = Colors, typography = AppTypography, content = content)
    }
}
