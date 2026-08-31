package com.replaylead.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val DeepInk = Color(0xFF102A43)
val Coral = Color(0xFFFF735C)
val Mint = Color(0xFF49C6A7)
val Paper = Color(0xFFF7F5EF)

private val ReplayLeadColors = lightColorScheme(
    primary = Coral,
    onPrimary = Color.White,
    secondary = Mint,
    background = Paper,
    onBackground = DeepInk,
    surface = Color.White,
    onSurface = DeepInk,
)

@Composable
fun ReplayLeadTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ReplayLeadColors,
        typography = Typography(),
        content = content,
    )
}
