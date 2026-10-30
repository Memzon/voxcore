package com.voxcore.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object VoxColors {
    val Black = Color(0xFF0E0E10)
    val Charcoal = Color(0xFF18181C)
    val Surface = Color(0xFF212127)
    val Outline = Color(0xFF34343C)
    val Purple = Color(0xFF9B7BFF)
    val PurpleDim = Color(0xFF5B4A99)
    val Text = Color(0xFFECECF1)
    val TextDim = Color(0xFF9A9AA6)
}

private val Scheme = darkColorScheme(
    primary = VoxColors.Purple,
    onPrimary = VoxColors.Black,
    primaryContainer = VoxColors.PurpleDim,
    background = VoxColors.Black,
    onBackground = VoxColors.Text,
    surface = VoxColors.Charcoal,
    onSurface = VoxColors.Text,
    surfaceVariant = VoxColors.Surface,
    onSurfaceVariant = VoxColors.TextDim,
    outline = VoxColors.Outline,
)

private val Type = Typography(
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Light, fontSize = 30.sp, letterSpacing = (-0.5).sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 12.sp),
)

private val VoxShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(22.dp),
)

@Composable
fun VoxcoreTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, typography = Type, shapes = VoxShapes, content = content)
}
