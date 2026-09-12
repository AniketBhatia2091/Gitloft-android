package com.example.gitloftandroid.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object GitloftColors {
    // Core Stark Cyber-Mono tokens
    val Background = Color(0xFF000000)       // Pure OLED black
    val Surface = Color(0xFF121214)          // Carbon charcoal
    val Surface2 = Color(0xFF1C1C1E)         // Container border background
    val Surface3 = Color(0xFF2C2C30)         // Active/selected background
    val Volt = Color(0xFFD4FF5F)             // Neon volt green
    val Steel = Color(0xFF3A3A3E)            // Secondary interactive
    val Border = Color(0xFF2A2A2E)           // Solid container borders
    val Text = Color(0xFFFFFFFF)             // Stark white text
    val TextSecondary = Color(0xFF8E8E94)    // Cool technical gray text
    val Error = Color(0xFFFF453A)            // System red
    val Success = Color(0xFF30D158)          // Neon cyber green success

    // Language colors
    fun colorForLanguage(language: String?): Color {
        return when (language?.lowercase()?.trim()) {
            "swift" -> Color(0xFFF05138)
            "kotlin" -> Color(0xFFA97BFF)
            "python" -> Color(0xFF3572A5)
            "typescript" -> Color(0xFF3178C6)
            "javascript" -> Color(0xFFF1E05A)
            "ruby" -> Color(0xFFCC342D)
            "go" -> Color(0xFF00ADD8)
            "html" -> Color(0xFFE34C26)
            "css" -> Color(0xFF563D7C)
            "c++", "cpp" -> Color(0xFFF34B7D)
            "c" -> Color(0xFF555555)
            "c#", "csharp" -> Color(0xFF178600)
            "rust" -> Color(0xFFDEA584)
            "dart" -> Color(0xFF00B4D8)
            "java" -> Color(0xFFB07219)
            "shell", "bash" -> Color(0xFF89E051)
            "php" -> Color(0xFF4F5D95)
            "lua" -> Color(0xFF000080)
            "r" -> Color(0xFF198CE7)
            "scala" -> Color(0xFFC82829)
            else -> Color(0xFF64748B)
        }
    }
}

private val DarkColorScheme = darkColorScheme(
    primary = GitloftColors.Volt,
    onPrimary = Color.Black,
    secondary = GitloftColors.Steel,
    onSecondary = Color.White,
    background = GitloftColors.Background,
    onBackground = GitloftColors.Text,
    surface = GitloftColors.Surface,
    onSurface = GitloftColors.Text,
    surfaceVariant = GitloftColors.Surface2,
    onSurfaceVariant = GitloftColors.TextSecondary,
    outline = GitloftColors.Border,
    error = GitloftColors.Error,
    onError = Color.White
)

val GitloftTypography = Typography(
    titleLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Black,
        fontSize = 22.sp,
        letterSpacing = 1.5.sp,
        color = GitloftColors.Text
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        letterSpacing = 1.2.sp,
        color = GitloftColors.Text
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        letterSpacing = 1.0.sp,
        color = GitloftColors.Text
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        color = GitloftColors.Text
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        color = GitloftColors.TextSecondary
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        letterSpacing = 1.0.sp,
        color = GitloftColors.TextSecondary
    )
)

@Composable
fun GitloftTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = GitloftTypography,
        content = content
    )
}
