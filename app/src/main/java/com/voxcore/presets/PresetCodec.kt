package com.voxcore.presets

import com.voxcore.audio.VoxSettings
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

data class PresetValues(
    val effect: String,
    val pitch: Float, val bass: Float, val treble: Float,
    val echoMix: Float, val echoDelayMs: Float, val echoFeedback: Float, val echoRepeats: Float,
    val reverbMix: Float, val master: Float,
    val eq: List<Float>,
    val gateEnabled: Boolean, val gateDb: Float,
    val compEnabled: Boolean, val compThresholdDb: Float, val compRatio: Float,
)

data class Preset(val name: String, val values: PresetValues)

/** Ön ayarın JSON biçimi, doğrulaması ve ayarlara uygulanması. */
object PresetCodec {
    const val FORMAT = "voxcore-preset"
    const val VERSION = 1

    fun capture(): PresetValues = VoxSettings.let {
        PresetValues(
            effect = it.effect, pitch = it.pitch, bass = it.bass, treble = it.treble,
            echoMix = it.echoMix, echoDelayMs = it.echoDelayMs, echoFeedback = it.echoFeedback,
            echoRepeats = it.echoRepeats, reverbMix = it.reverbMix, master = it.master,
            eq = it.eq.toList(),
            gateEnabled = it.gateEnabled, gateDb = it.gateDb,
            compEnabled = it.compEnabled, compThresholdDb = it.compThresholdDb, compRatio = it.compRatio,
        )
    }

    fun apply(v: PresetValues) {
        val s = VoxSettings
        s.effect = v.effect; s.pitch = v.pitch; s.bass = v.bass; s.treble = v.treble
        s.echoMix = v.echoMix; s.echoDelayMs = v.echoDelayMs; s.echoFeedback = v.echoFeedback
        s.echoRepeats = v.echoRepeats; s.reverbMix = v.reverbMix; s.master = v.master
        for (i in 0 until 10) s.eq[i] = v.eq[i]
        s.gateEnabled = v.gateEnabled; s.gateDb = v.gateDb
        s.compEnabled = v.compEnabled; s.compThresholdDb = v.compThresholdDb; s.compRatio = v.compRatio
    }

    fun encode(name: String, v: PresetValues): String {
        val o = JSONObject()
        o.put("format", FORMAT)
        o.put("version", VERSION)
        o.put("name", name)
        o.put("effect", v.effect)
        o.put("pitch", v.pitch.toDouble())
        o.put("bass", v.bass.toDouble())
        o.put("treble", v.treble.toDouble())
        o.put("echoMix", v.echoMix.toDouble())
        o.put("echoDelayMs", v.echoDelayMs.toDouble())
        o.put("echoFeedback", v.echoFeedback.toDouble())
        o.put("echoRepeats", v.echoRepeats.toDouble())
        o.put("reverbMix", v.reverbMix.toDouble())
        o.put("master", v.master.toDouble())
        val eq = JSONArray()
        v.eq.forEach { eq.put(it.toDouble()) }
        o.put("eq", eq)
        o.put("gateEnabled", v.gateEnabled)
        o.put("gateDb", v.gateDb.toDouble())
        o.put("compEnabled", v.compEnabled)
        o.put("compThresholdDb", v.compThresholdDb.toDouble())
        o.put("compRatio", v.compRatio.toDouble())
        return o.toString(2)
    }

    /** Geçersizse Failure(IllegalArgumentException(Türkçe mesaj)) döner. Sayılar izinli aralığa kırpılır. */
    fun decode(text: String): Result<Preset> = try {
        Result.success(parse(text))
    } catch (e: JSONException) {
        Result.failure(IllegalArgumentException("Dosya geçerli bir JSON değil."))
    } catch (e: IllegalArgumentException) {
        Result.failure(e)
    }

    private fun parse(text: String): Preset {
        val o = JSONObject(text)
        require(o.optString("format") == FORMAT) { "Bu bir VOXCORE ön ayar dosyası değil." }
        val ver = o.optInt("version", -1)
        require(ver in 1..VERSION) { "Desteklenmeyen ön ayar sürümü ($ver)." }
        val name = o.optString("name").trim()
        require(name.isNotEmpty() && name.length <= 60) { "Ön ayar adı boş ya da 60 karakterden uzun." }
        val effect = o.optString("effect", "Yok")
        require(effect in VoxSettings.effectNames) { "Bilinmeyen efekt: $effect" }
        val arr = o.optJSONArray("eq")
        require(arr != null && arr.length() == 10) { "Ekolayzır tam 10 değer içermeli." }
        val eq = (0 until 10).map { i ->
            val d = arr.optDouble(i, Double.NaN)
            require(!d.isNaN() && !d.isInfinite()) { "Ekolayzır değeri geçerli bir sayı değil (bant ${i + 1})." }
            d.toFloat().coerceIn(-12f, 12f)
        }
        val v = PresetValues(
            effect = effect,
            pitch = o.num("pitch", -12f, 12f, 0f),
            bass = o.num("bass", -12f, 12f, 0f),
            treble = o.num("treble", -12f, 12f, 0f),
            echoMix = o.num("echoMix", 0f, 100f, 0f),
            echoDelayMs = o.num("echoDelayMs", 20f, 1000f, 250f),
            echoFeedback = o.num("echoFeedback", 0f, 90f, 40f),
            echoRepeats = o.num("echoRepeats", 1f, 10f, 4f),
            reverbMix = o.num("reverbMix", 0f, 100f, 0f),
            master = o.num("master", 0f, 100f, 80f),
            eq = eq,
            gateEnabled = o.optBoolean("gateEnabled", false),
            gateDb = o.num("gateDb", -90f, -20f, -60f),
            compEnabled = o.optBoolean("compEnabled", false),
            compThresholdDb = o.num("compThresholdDb", -60f, 0f, -20f),
            compRatio = o.num("compRatio", 1f, 10f, 2f),
        )
        return Preset(name, v)
    }

    private fun JSONObject.num(key: String, lo: Float, hi: Float, def: Float): Float {
        if (!has(key)) return def
        val d = optDouble(key, Double.NaN)
        require(!d.isNaN() && !d.isInfinite()) { "'$key' alanı geçerli bir sayı değil." }
        return d.toFloat().coerceIn(lo, hi)
    }
}
