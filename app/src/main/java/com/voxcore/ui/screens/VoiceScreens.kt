package com.voxcore.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.voxcore.audio.AudioIo
import com.voxcore.audio.VoxEngine
import com.voxcore.audio.VoxSettings
import com.voxcore.speech.WordEcho
import com.voxcore.ui.components.*
import com.voxcore.ui.theme.VoxColors

@Composable
fun VoiceChangerScreen() {
    val eqLabels = listOf("31", "62", "125", "250", "500", "1k", "2k", "4k", "8k", "16k")
    val startEngine = rememberEngineStarter()

    ScreenTitle("Ses Değiştirici", "Canlı mikrofon işleme")

    Panel("Canlı çalışma") {
        Button(
            onClick = { if (VoxEngine.running) VoxEngine.stop() else startEngine() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (VoxEngine.running) "Durdur" else "Başlat")
        }
        Spacer(Modifier.height(8.dp))
        if (VoxEngine.running) {
            Text(
                "Çalışıyor: ${VoxEngine.activeSampleRate} Hz, tahmini gecikme ${VoxEngine.estimatedLatencyMs} ms",
                style = MaterialTheme.typography.bodyMedium, color = VoxColors.TextDim
            )
        }
        VoxEngine.error?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        }
        if (!AudioIo.headphonesConnected()) {
            PendingNote("Kulaklık algılanmadı. Hoparlörde çalıştırırsanız mikrofon sesi geri yakalar ve ıslık (geri besleme) olur. Kulaklık takın.")
        }
    }
    Panel("Efekt") {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VoxSettings.effectNames.forEach { e ->
                FilterChip(
                    selected = VoxSettings.effect == e, onClick = { VoxSettings.effect = e }, label = { Text(e) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = VoxColors.PurpleDim, selectedLabelColor = VoxColors.Text
                    )
                )
            }
        }
    }
    Panel("Ayarlar") {
        LabeledSlider("Perde", VoxSettings.pitch, { VoxSettings.pitch = it }, -12f..12f, " yarım ton", 1)
        LabeledSlider("Bas", VoxSettings.bass, { VoxSettings.bass = it }, -12f..12f, " dB", 1)
        LabeledSlider("Tiz", VoxSettings.treble, { VoxSettings.treble = it }, -12f..12f, " dB", 1)
        LabeledSlider("Eko", VoxSettings.echoMix, { VoxSettings.echoMix = it }, 0f..100f, "%")
        LabeledSlider("Yankı", VoxSettings.reverbMix, { VoxSettings.reverbMix = it }, 0f..100f, "%")
        LabeledSlider("Ana ses", VoxSettings.master, { VoxSettings.master = it }, 0f..100f, "%")
    }
    Panel("Ekolayzır (10 bant)") {
        eqLabels.forEachIndexed { i, hz ->
            LabeledSlider("$hz Hz", VoxSettings.eq[i], { VoxSettings.eq[i] = it }, -12f..12f, " dB", 1)
        }
        TextButton(onClick = { for (i in 0 until 10) VoxSettings.eq[i] = 0f }) {
            Text("Sıfırla", color = VoxColors.Purple)
        }
    }
}

@Composable
fun EchoScreen() {
    val ctx = LocalContext.current
    val startEngine = rememberEngineStarter()
    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) WordEcho.start(ctx)
    }

    ScreenTitle("Eko Motoru")

    Panel("Gerçek zamanlı eko") {
        LabeledSlider("Gecikme", VoxSettings.echoDelayMs, { VoxSettings.echoDelayMs = it }, 20f..1000f, " ms")
        LabeledSlider("Geri besleme (her tekrarın gücü)", VoxSettings.echoFeedback, { VoxSettings.echoFeedback = it }, 0f..90f, "%")
        LabeledSlider("Yoğunluk", VoxSettings.echoMix, { VoxSettings.echoMix = it }, 0f..100f, "%")
        LabeledSlider("Tekrar sayısı", VoxSettings.echoRepeats, { VoxSettings.echoRepeats = it }, 1f..10f)
        Button(
            onClick = { if (VoxEngine.running) VoxEngine.stop() else startEngine() },
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (VoxEngine.running) "Durdur" else "Başlat") }
        if (VoxEngine.running) {
            Text(
                "Çalışıyor: ${VoxEngine.activeSampleRate} Hz, tahmini gecikme ${VoxEngine.estimatedLatencyMs} ms",
                style = MaterialTheme.typography.bodyMedium, color = VoxColors.TextDim
            )
        }
        VoxEngine.error?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        }
        PendingNote("Ekoyu duymak için yoğunluğu artırın. Bu ekran Ses Değiştirici ile aynı motoru ve aynı eko ayarlarını kullanır. Kulaklık kullanın.")
    }
    Panel("Akıllı kelime eko") {
        Text(
            "Türkçe konuşmayı tanır, her kelimeyi yeniden söyler.",
            style = MaterialTheme.typography.bodyMedium, color = VoxColors.TextDim
        )
        LabeledSlider("Kelime tekrarı", VoxSettings.wordRepeats, { VoxSettings.wordRepeats = it }, 1f..6f)
        LabeledSlider("Tekrar hızı", VoxSettings.wordSpeed, { VoxSettings.wordSpeed = it }, 0.5f..2f, "x", 1)
        LabeledSlider("Kelimeler arası duraklama", VoxSettings.wordPauseMs, { VoxSettings.wordPauseMs = it }, 0f..1000f, " ms")
        Button(
            onClick = {
                if (WordEcho.active) WordEcho.stop()
                else if (hasMicPermission(ctx)) WordEcho.start(ctx)
                else micLauncher.launch(Manifest.permission.RECORD_AUDIO)
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (WordEcho.active) "Tanımayı durdur" else "Tanımayı başlat") }
        Spacer(Modifier.height(8.dp))
        Text("Durum: ${WordEcho.status}", style = MaterialTheme.typography.bodyMedium, color = VoxColors.Text)
        if (WordEcho.lastHeard.isNotEmpty()) {
            Text("Duyulan: ${WordEcho.lastHeard}", style = MaterialTheme.typography.bodyMedium, color = VoxColors.TextDim)
        }
        PendingNote(
            "Android SpeechRecognizer (tr-TR) ve cihazın Türkçe metin okuma sesi kullanılır. " +
                "Tanıma kalitesi ve çevrimdışı çalışma cihaza bağlıdır, internet gerekebilir. " +
                "Kelimeler hoparlörden çalınır, ses motorunun çıkışına karışmaz. " +
                "Ses motoruyla aynı anda mikrofon paylaşımı bazı cihazlarda çalışmaz; sorun olursa birini durdurun. " +
                "Tanıyıcı her dinlemeye başlarken bip sesi çıkarabilir."
        )
    }
}
