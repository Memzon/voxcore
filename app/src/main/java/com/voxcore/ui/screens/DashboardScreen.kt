package com.voxcore.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import com.voxcore.audio.VoxEngine
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.voxcore.ui.Dest
import com.voxcore.ui.components.*
import com.voxcore.ui.theme.VoxColors

@Composable
fun DashboardScreen(onOpen: (Dest) -> Unit) {
    ScreenTitle("VOXCORE", "Profesyonel ses stüdyosu")

    Panel("Ses motoru") {
        val levels = remember { mutableStateListOf(*Array(24) { 0f }) }
        LaunchedEffect(Unit) {
            while (true) {
                delay(50)
                levels.removeAt(0)
                levels.add(VoxEngine.level)
            }
        }
        Text(
            if (VoxEngine.running) "Durum: çalışıyor" else "Durum: kapalı",
            color = VoxColors.Text, style = MaterialTheme.typography.bodyMedium
        )
        LevelBars(levels.toList())
        if (!VoxEngine.running) PendingNote("Mikrofon girişini görmek için Ses Değiştirici ekranından başlatın.")
    }

    val tiles = listOf(
        Dest.Voice to "Canlı mikrofon efektleri",
        Dest.Echo to "Gerçek zamanlı ve akıllı kelime eko",
        Dest.Soundboard to "MP3, WAV, OGG içe aktar",
        Dest.Mixer to "10 bantlı ekolayzır, kapı, kompresör",
        Dest.Presets to "Ayarları kaydet ve yükle",
        Dest.Recorder to "Kayıt ve WAV dışa aktarma",
        Dest.Queue to "Çalma sırası yönetimi",
        Dest.Settings to "İzinler ve gecikme tanılaması",
    )
    tiles.chunked(2).forEach { row ->
        Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            row.forEach { (dest, desc) -> Tile(dest.title, desc, Modifier.weight(1f)) { onOpen(dest) } }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun Tile(title: String, desc: String, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .heightIn(min = 104.dp)
            .background(VoxColors.Charcoal, MaterialTheme.shapes.medium)
            .border(1.dp, VoxColors.Outline, MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = VoxColors.Text)
        Spacer(Modifier.height(4.dp))
        Text(desc, style = MaterialTheme.typography.bodyMedium, color = VoxColors.TextDim)
    }
}
