package me.nimingzhe.recordeverything.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** 取自用户截图的深色暖调色板。 */
object Palette {
    val Bg = Color(0xFF12100E)
    val Surface = Color(0xFF1B1610)
    val SurfaceHi = Color(0xFF241D15)
    val Gold = Color(0xFFD8B26A)
    val GoldDim = Color(0xFF9C7F48)
    val Text = Color(0xFFEDE6DA)
    val TextDim = Color(0xFF9A8F7D)
    val Expense = Color(0xFFC9564B)
    val Income = Color(0xFF5FA96B)
    val Divider = Color(0xFF2E2519)
}

private val Colors = darkColorScheme(
    primary = Palette.Gold,
    onPrimary = Color(0xFF20180C),
    primaryContainer = Palette.SurfaceHi,
    onPrimaryContainer = Palette.Gold,
    secondary = Palette.GoldDim,
    onSecondary = Palette.Text,
    background = Palette.Bg,
    onBackground = Palette.Text,
    surface = Palette.Surface,
    onSurface = Palette.Text,
    surfaceVariant = Palette.SurfaceHi,
    onSurfaceVariant = Palette.TextDim,
    error = Palette.Expense,
    outline = Palette.Divider
)

private val AppTypography = Typography(
    bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 22.sp, color = Palette.Text),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, color = Palette.Text),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, color = Palette.TextDim),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Palette.Text),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Palette.Text),
    labelSmall = TextStyle(fontSize = 11.sp, color = Palette.TextDim)
)

@Composable
fun RecordEverythingTheme(content: @Composable () -> Unit) {
    @Suppress("UNUSED_EXPRESSION")
    isSystemInDarkTheme()
    MaterialTheme(colorScheme = Colors, typography = AppTypography, content = content)
}
