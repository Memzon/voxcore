package com.voxcore.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.voxcore.audio.VoxSettings
import com.voxcore.player.PlayQueue
import com.voxcore.player.RepeatMode
import com.voxcore.player.SoundPlayer
import com.voxcore.player.Track
import com.voxcore.ui.components.*
import com.voxcore.ui.theme.VoxColors
import kotlinx.coroutines.delay

private fun displayName(ctx: Context, uri: Uri): String =
    ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
        if (c.moveToFirst()) c.getString(0) else null
    } ?: (uri.lastPathSegment ?: "Bilinmeyen dosya")

@Composable
fun SoundboardScreen() {
    val ctx = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        uris.forEach { uri ->
            runCatching {
                ctx.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            PlayQueue.add(ctx, Track(uri.toString(), displayName(ctx, uri)))
        }
    }

    var pos by remember { mutableIntStateOf(0) }
    var dragging by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            if (!dragging) pos = SoundPlayer.position()
            delay(250)
        }
    }

    ScreenTitle("Ses Çalar", "Yerel ses kitaplığı")

    Panel("Kitaplık") {
        Button(
            onClick = { picker.launch(arrayOf("audio/mpeg", "audio/wav", "audio/x-wav", "audio/ogg", "application/ogg")) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Ses dosyası ekle (MP3, WAV, OGG)") }
        Spacer(Modifier.height(8.dp))
        if (PlayQueue.tracks.isEmpty()) {
            Text("Henüz dosya yok. Yukarıdan ekleyin.", color = VoxColors.TextDim, style = MaterialTheme.typography.bodyMedium)
        } else {
            PlayQueue.tracks.forEachIndexed { i, t ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text(
                        t.name,
                        color = if (i == PlayQueue.index) VoxColors.Purple else VoxColors.Text,
                        style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { SoundPlayer.playIndex(ctx, i) }) { Text("Çal", color = VoxColors.Purple) }
                    TextButton(onClick = { PlayQueue.remove(ctx, i) }) { Text("Kaldır", color = VoxColors.TextDim) }
                }
            }
        }
    }
    Panel("Oynatma") {
        Text(
            if (SoundPlayer.currentName.isEmpty()) "Çalan parça yok" else SoundPlayer.currentName,
            color = VoxColors.Text, style = MaterialTheme.typography.bodyMedium
        )
        Slider(
            value = pos.coerceAtMost(maxOf(1, SoundPlayer.durationMs)).toFloat(),
            onValueChange = { dragging = true; pos = it.toInt() },
            onValueChangeFinished = { SoundPlayer.seekTo(pos); dragging = false },
            valueRange = 0f..maxOf(1, SoundPlayer.durationMs).toFloat(),
            enabled = SoundPlayer.durationMs > 0,
            colors = SliderDefaults.colors(
                thumbColor = VoxColors.Purple, activeTrackColor = VoxColors.Purple, inactiveTrackColor = VoxColors.Outline
            )
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime(pos), color = VoxColors.TextDim, style = MaterialTheme.typography.labelMedium)
            Text(formatTime(SoundPlayer.durationMs), color = VoxColors.TextDim, style = MaterialTheme.typography.labelMedium)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { SoundPlayer.previous(ctx) }, enabled = PlayQueue.tracks.isNotEmpty()) { Text("Önceki") }
            Button(onClick = { SoundPlayer.togglePause(ctx) }, enabled = PlayQueue.tracks.isNotEmpty() || SoundPlayer.durationMs > 0) {
                Text(if (SoundPlayer.playing) "Duraklat" else "Oynat")
            }
            OutlinedButton(onClick = { SoundPlayer.next(ctx) }, enabled = PlayQueue.tracks.isNotEmpty()) { Text("Sonraki") }
            OutlinedButton(onClick = { SoundPlayer.stop() }) { Text("Durdur") }
        }
        LabeledSlider("Ses", SoundPlayer.volume, { SoundPlayer.updateVolume(it) }, 0f..100f, "%")
        SoundPlayer.error?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        }
        PendingNote("Çalma telefonun medya çıkışına gider. Discord'a otomatik aktarılmaz; ayrıntı için Ayarlar ve Tanılama ekranına bakın.")
    }
}

@Composable
fun MixerScreen() {
    val bands = listOf("31", "62", "125", "250", "500", "1k", "2k", "4k", "8k", "16k")

    ScreenTitle("Mikser")

    Panel("Ana ses") {
        LabeledSlider("Ana ses", VoxSettings.master, { VoxSettings.master = it }, 0f..100f, "%")
    }
    Panel("10 bantlı ekolayzır") {
        bands.forEachIndexed { i, hz ->
            LabeledSlider("$hz Hz", VoxSettings.eq[i], { VoxSettings.eq[i] = it }, -12f..12f, " dB", 1)
        }
    }
    Panel("Gürültü kapısı") {
        ToggleRow("Kapıyı aç", VoxSettings.gateEnabled) { VoxSettings.gateEnabled = it }
        LabeledSlider("Eşik", VoxSettings.gateDb, { VoxSettings.gateDb = it }, -90f..-20f, " dB", enabled = VoxSettings.gateEnabled)
        PendingNote("Mikrofon bu eşiğin altına inince ses yumuşakça kısılır (100 ms bekleme). Eşik çok yüksekse sesin başı kesilir.")
    }
    Panel("Kompresör") {
        ToggleRow("Kompresörü aç", VoxSettings.compEnabled) { VoxSettings.compEnabled = it }
        LabeledSlider("Eşik", VoxSettings.compThresholdDb, { VoxSettings.compThresholdDb = it }, -60f..0f, " dB", enabled = VoxSettings.compEnabled)
        LabeledSlider("Oran", VoxSettings.compRatio, { VoxSettings.compRatio = it }, 1f..10f, ":1", 1, enabled = VoxSettings.compEnabled)
        PendingNote("Tüm kaydırıcılar canlı motora bağlıdır ve Ses Değiştirici ekranıyla aynı ayarı paylaşır. Kompresör ekolayzırdan sonra çalışır, hafif otomatik kazanç telafisi uygular.")
    }
}

@Composable
fun QueueScreen() {
    val ctx = LocalContext.current
    ScreenTitle("Çalma Sırası", "Kitaplıkla aynı liste")
    Panel("Sıra") {
        if (PlayQueue.tracks.isEmpty()) {
            Text("Sıra boş. Ses Çalar'dan dosya ekleyin.", color = VoxColors.TextDim, style = MaterialTheme.typography.bodyMedium)
        } else {
            PlayQueue.tracks.forEachIndexed { i, t ->
                Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(
                        "${i + 1}. ${t.name}",
                        color = if (i == PlayQueue.index) VoxColors.Purple else VoxColors.Text,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = { SoundPlayer.playIndex(ctx, i) }) { Text("Çal", color = VoxColors.Purple) }
                        TextButton(onClick = { PlayQueue.move(ctx, i, -1) }, enabled = i > 0) { Text("Yukarı", color = VoxColors.Purple) }
                        TextButton(onClick = { PlayQueue.move(ctx, i, 1) }, enabled = i < PlayQueue.tracks.lastIndex) { Text("Aşağı", color = VoxColors.Purple) }
                        TextButton(onClick = { PlayQueue.remove(ctx, i) }) { Text("Kaldır", color = VoxColors.TextDim) }
                    }
                }
            }
            TextButton(onClick = { PlayQueue.clear(ctx) }) { Text("Sırayı temizle", color = VoxColors.TextDim) }
        }
    }
    Panel("Tekrar modu") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RepeatMode.entries.forEach { m ->
                FilterChip(
                    selected = PlayQueue.repeatMode == m,
                    onClick = { PlayQueue.repeatMode = m; PlayQueue.save(ctx) },
                    label = { Text(m.label) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = VoxColors.PurpleDim, selectedLabelColor = VoxColors.Text)
                )
            }
        }
        ToggleRow("Karışık çalma", PlayQueue.shuffle) { PlayQueue.shuffle = it; PlayQueue.save(ctx) }
        PendingNote("Parça bitince sıradaki otomatik çalar. Karışık çalmada sıra rastgele seçilir; tekrar kapalıysa da durmaz.")
    }
}
