package com.example.radar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.radar.ui.screens.RadarMonitorScreen
import com.example.radar.ui.theme.RadarCockpitTheme
import com.example.radar.viewmodel.RadarMonitorViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: RadarMonitorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RadarCockpitTheme {
                RadarMonitorScreen(viewModel = viewModel)
            }
        }
    }
}
