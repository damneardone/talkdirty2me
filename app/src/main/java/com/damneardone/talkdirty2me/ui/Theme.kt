package com.damneardone.talkdirty2me.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val AccentPink = Color(0xFFFF2D78)
val GateBackground = Color(0xFF14060D)
val GateCard = Color(0xFF1F0D16)

private val DarkColors = darkColorScheme(
    primary = AccentPink,
    onPrimary = Color.White,
    background = GateBackground,
    onBackground = Color.White,
    surface = GateCard,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF2A121C),
    onSurfaceVariant = Color(0xFFD9C2CC),
    error = Color(0xFFFF8FB5),
)

@Composable
fun Td2mTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColors, content = content)
}