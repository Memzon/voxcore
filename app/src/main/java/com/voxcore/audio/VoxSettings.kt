package com.voxcore.audio

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Ekran ve ses motorunun paylaştığı tek ayar kaynağı. Ön ayarlar bunu kaydeder ve yükler. */
object VoxSettings {
    val effectNames = listOf("Yok", "Derin ses", "İnce ses", "Robot", "Uzaylı", "Telsiz", "Canavar", "Mağara", "Telefon")

    var effect by mutableStateOf("Yok")
    var pitch by mutableFloatStateOf(0f)         // yarım ton
    var bass by mutableFloatStateOf(0f)          // dB
    var treble by mutableFloatStateOf(0f)        // dB
    var echoMix by mutableFloatStateOf(0f)       // %
    var echoDelayMs by mutableFloatStateOf(250f)
    var echoFeedback by mutableFloatStateOf(40f) // % (her tekrarın öncekine oranı)
    var echoRepeats by mutableFloatStateOf(4f)   // tekrar sayısı 1..10
    var reverbMix by mutableFloatStateOf(0f)     // %
    var master by mutableFloatStateOf(80f)       // %
    val eq = mutableStateListOf(*Array(10) { 0f }) // dB, 31 Hz .. 16 kHz

    // Faz 5: kapı ve kompresör
    var gateEnabled by mutableStateOf(false)
    var gateDb by mutableFloatStateOf(-60f)
    var compEnabled by mutableStateOf(false)
    var compThresholdDb by mutableFloatStateOf(-20f)
    var compRatio by mutableFloatStateOf(2f)

    // Faz 3: akıllı kelime eko
    var wordRepeats by mutableFloatStateOf(2f)
    var wordSpeed by mutableFloatStateOf(1f)
    var wordPauseMs by mutableFloatStateOf(200f)

    // Motor ve ses yolu (sonraki başlatmada uygulanır)
    var sampleRate by mutableIntStateOf(48000)
    var blockFrames by mutableIntStateOf(256)
    var inputSource by mutableIntStateOf(0)      // 0 = mikrofon, 1 = ses iletişimi
    var inputDeviceId by mutableIntStateOf(0)    // 0 = sistem varsayılanı
    var outputDeviceId by mutableIntStateOf(0)   // 0 = sistem varsayılanı

    val eqFreqs = doubleArrayOf(31.0, 62.0, 125.0, 250.0, 500.0, 1000.0, 2000.0, 4000.0, 8000.0, 16000.0)
}
