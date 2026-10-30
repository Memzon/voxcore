package com.voxcore.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.voxcore.audio.AudioIo
import com.voxcore.player.PlayQueue
import com.voxcore.recorder.Recorder
import com.voxcore.ui.screens.*
import com.voxcore.ui.theme.VoxColors
import com.voxcore.ui.theme.VoxcoreTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AudioIo.init(applicationContext)
        Recorder.init(applicationContext)
        PlayQueue.load(applicationContext)
        setContent { VoxcoreTheme { VoxcoreApp() } }
    }
}

@Composable
fun VoxcoreApp() {
    var current by rememberSaveable { mutableStateOf(Dest.Dashboard) }

    Scaffold(
        containerColor = VoxColors.Black,
        bottomBar = {
            NavigationBar(containerColor = VoxColors.Charcoal) {
                Dest.entries.filter { it.inBottomBar }.forEach { d ->
                    NavigationBarItem(
                        selected = current == d,
                        onClick = { current = d },
                        icon = { Text(d.short.take(1), color = if (current == d) VoxColors.Purple else VoxColors.TextDim) },
                        label = { Text(d.short) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedTextColor = VoxColors.Purple,
                            unselectedTextColor = VoxColors.TextDim,
                            indicatorColor = VoxColors.PurpleDim.copy(alpha = 0.35f)
                        )
                    )
                }
            }
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            if (!current.inBottomBar) {
                TextButton(onClick = { current = Dest.Dashboard }) { Text("‹ Panele dön", color = VoxColors.Purple) }
            }
            when (current) {
                Dest.Dashboard -> DashboardScreen(onOpen = { current = it })
                Dest.Voice -> VoiceChangerScreen()
                Dest.Echo -> EchoScreen()
                Dest.Soundboard -> SoundboardScreen()
                Dest.Mixer -> MixerScreen()
                Dest.Presets -> PresetsScreen()
                Dest.Recorder -> RecorderScreen()
                Dest.Queue -> QueueScreen()
                Dest.Settings -> SettingsScreen()
            }
        }
    }
}
