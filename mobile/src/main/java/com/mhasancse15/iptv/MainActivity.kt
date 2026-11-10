package com.mhasancse15.iptv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.mhasancse15.iptv.navigation.NavGraph
import com.mhasancse15.iptv.presentation.BdixViewModel
import com.mhasancse15.iptv.ui.theme.IPTVTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: BdixViewModel = hiltViewModel()
            val themePreference by viewModel.themePreference.collectAsState()
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (themePreference) {
                "Light" -> false
                "Dark" -> true
                else -> systemDark
            }
            IPTVTheme(darkTheme = darkTheme) {
                NavGraph.BdixNavigation(viewModel = viewModel)
            }
        }
    }
}
