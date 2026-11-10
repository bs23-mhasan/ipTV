package com.mhasancse15.iptv.presentation.settings

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExpandedDropdownMenu
import androidx.compose.material3.ListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier.fillMaxSize(),
    onRefreshPlaylist: () -> Unit,
    onClearCache: () -> Unit,
    onEpgRefresh: () -> Unit,
    onThemeChange: (String) -> Unit
) {
    var isDarkTheme by remember { mutableStateOf(true) }
    var autoPlayNext by remember { mutableStateOf(true) }
    var expanded by remember { mutableStateOf(false) }

    val themes = listOf("System", "Light", "Dark")

    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        // Playlist settings
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Playlist",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                ListItem(
                    headlineText = { Text("Refresh Playlist") },
                    supportingText = { Text("Download latest channel list") },
                    trailingContent = {
                        Text(
                            text = "Refresh",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier.clickable(onClick = onRefreshPlaylist)
                )

                ListItem(
                    headlineText = { Text("Clear Cache") },
                    supportingText = { Text("Remove cached playlist and logos") },
                    trailingContent = {
                        Text(
                            text = "Clear",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier.clickable(onClick = onClearCache)
                )
            }
        }

        // Playback settings
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Playback",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                ListItem(
                    headlineText = { Text("Auto Play Next Channel") },
                    trailingContent = {
                        Switch(
                            checked = autoPlayNext,
                            onCheckedChange = { autoPlayNext = it }
                        )
                    }
                )
            }
        }

        // Theme settings
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Appearance",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Box {
                    TextField(
                        value = themes[if (isDarkTheme) 2 else 1],
                        onValueChange = { },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Theme") },
                        trailingIcon = { /* Arrow icon */ },
                        enabled = false,
                        singleLine = true
                    )
                    ExpandedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        themes.forEachIndexed { index, theme ->
                            DropdownMenuItem(
                                text = { Text(theme) },
                                onClick = {
                                    onThemeChange(theme)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // EPG settings
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "TV Guide",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                ListItem(
                    headlineText = { Text("Refresh EPG") },
                    supportingText = { Text("Download latest programme guide") },
                    trailingContent = {
                        Text(
                            text = "Refresh",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier.clickable(onClick = onEpgRefresh)
                )
            }
        }

        // About
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "About",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                ListItem(
                    headlineText = { Text("BDIX TV") },
                    supportingText = { Text("Version ${BuildConfig.VERSION_NAME}") }
                )

                ListItem(
                    headlineText = { Text("Playlist Sources") },
                    supportingText = { Text("BDIX IPTV Repository") }
                )
            }
        }
    }
}