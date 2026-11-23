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
    /**
     * 未选中态控件的描边。比 Line 亮一档：Line 只做卡片边与分隔线，
     * 而勾选圈这类"要能看出是个控件"的边界需要 3:1（WCAG 1.4.11），
     * 这个值在三个底色上是 3.55 / 3.34 / 3.11。
     */
    val Stroke = Color(0xFF756656)
    val LineSoft = Color(0xFF221C15)

    val Gold = Color(0xFFE5C07A)
    val GoldSoft = Color(0xFFC8A25D)
    val GoldDeep = Color(0xFF7E6231)

    val Text = Color(0xFFF3ECE0)
    val TextMid = Color(0xFFB6A996)
    val TextDim = Color(0xFF8E8170)

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
 * 文字尺度。
 *
 * 全 App 只有这九档，任何 Text 都必须从这里取，不再写裸 fontSize——
 * 上一版散落了 18 种字号（8/10/11/12/13/14/14.5/15/16/17/18/19/24/26/30/40/44/62），
 * 其中 8sp 在高密度屏上几乎不可读、14.5sp 是手滑。
 *
 * 衬线只给三类"主角"：日期、月份、统计数字。其余全部无衬线。
 */
object Type {
    /** 今天页的日号。整个界面唯一的 60+ 字号。 */
    val display = TextStyle(
        fontFamily = FontFamily.Serif, fontSize = 62.sp, lineHeight = 68.sp,
        fontWeight = FontWeight.Normal, letterSpacing = (-2).sp
    )
    /** 「全部」页的计数。 */
    val number = TextStyle(
        fontFamily = FontFamily.Serif, fontSize = 40.sp, lineHeight = 44.sp,
        fontWeight = FontWeight.Normal, letterSpacing = (-1).sp
    )
    /** 日历页的月份。 */
    val month = TextStyle(
        fontFamily = FontFamily.Serif, fontSize = 30.sp, lineHeight = 34.sp,
        fontWeight = FontWeight.Normal, letterSpacing = (-0.5).sp
    )
    /** 设置页的统计数字。 */
    val stat = TextStyle(
        fontFamily = FontFamily.Serif, fontSize = 22.sp, lineHeight = 26.sp,
        fontWeight = FontWeight.Normal
    )

    /** 页面主标题（设置页）。 */
    val screen = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Medium)
    /** 草稿卡标题这类"正在输入的东西"。 */
    val title = TextStyle(fontSize = 17.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium)
    /** 列表行标题、正文。 */
    val body = TextStyle(fontSize = 14.sp, lineHeight = 21.sp)
    /** 一行里的第二层信息：日期、分组、备注。 */
    val meta = TextStyle(fontSize = 12.sp, lineHeight = 17.sp)
    /** 最弱的一层：说明文字、页脚。 */
    val caption = TextStyle(fontSize = 11.sp, lineHeight = 15.sp)
    /** 分区标题：字距拉开的全宽小字。11sp 而不是 10sp，小屏上才站得住。 */
    val eyebrow = TextStyle(
        fontSize = 11.sp, lineHeight = 14.sp,
        fontWeight = FontWeight.Medium, letterSpacing = 1.5.sp
    )
    /** 日历格子里那行农历。上一版是 8sp + 55% 透明，对比度只有 1.9:1，等于看不见。 */
    val lunar = TextStyle(fontSize = 10.sp, lineHeight = 12.sp)

    /** 底部导航的文字。上一版 9sp，是这套界面里最小的字，读起来费劲。 */
    val nav = TextStyle(fontSize = 10.sp, lineHeight = 13.sp, letterSpacing = 0.4.sp)

    /** 胶囊、按钮。 */
    val label = TextStyle(fontSize = 12.sp, lineHeight = 16.sp)
    val labelStrong = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium)
    val button = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold)
}

/** M3 的 typography 也指向同一套，避免两套字号体系打架。 */
private val AppTypography = Typography(
    displayLarge = Type.display,
    displaySmall = Type.month,
    titleLarge = Type.screen,
    titleMedium = Type.title,
    bodyLarge = Type.body,
    bodyMedium = Type.meta,
    bodySmall = Type.caption,
    labelSmall = Type.eyebrow
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
