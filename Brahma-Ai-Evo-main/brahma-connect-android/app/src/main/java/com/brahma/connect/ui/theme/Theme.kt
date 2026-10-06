package com.brahma.connect.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import com.brahma.connect.core.AgentStateStore
import com.brahma.connect.core.VisualTheme

@Composable
fun BrahmaConnectTheme(content: @Composable () -> Unit) {
    val theme by AgentStateStore.visualTheme.collectAsState()
    val colors = darkColorScheme(
        primary = Color(theme.primary),
        onPrimary = Color(theme.background),
        secondary = Color(theme.accent),
        background = Color(theme.background),
        surface = Color(theme.surface),
        surfaceVariant = Color(theme.surface + 0x00080B08),
        onSurface = Color(0xFFF4F6F8),
        onSurfaceVariant = Color(0xFFADB5C0),
        error = Color(0xFFFF6B6B),
    )
    MaterialTheme(
        colorScheme = colors,
        typography = androidx.compose.material3.Typography(),
        content = content,
    )
}
