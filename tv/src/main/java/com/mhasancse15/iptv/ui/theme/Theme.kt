package com.mhasancse15.iptv.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val BdixDarkColors = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = androidx.compose.ui.graphics.Color(0xFF0B0B0F),
    surface = androidx.compose.ui.graphics.Color(0xFF18181F)
)

@Composable
fun IPTVTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = BdixDarkColors,
        typography = Typography,
        content = content
    )
}
