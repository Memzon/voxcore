package com.voxcore.ui

enum class Dest(val title: String, val short: String, val inBottomBar: Boolean) {
    Dashboard("Panel", "Panel", true),
    Voice("Ses Değiştirici", "Ses", true),
    Echo("Eko Motoru", "Eko", true),
    Soundboard("Ses Çalar", "Çalar", true),
    Mixer("Mikser", "Mikser", true),
    Presets("Ön Ayarlar", "Ayarlar", false),
    Recorder("Kayıt", "Kayıt", false),
    Queue("Çalma Sırası", "Sıra", false),
    Settings("Ayarlar ve Tanılama", "Ayarlar", false),
}
