package com.voxcore.effects

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin

/** Perde kaydırıcı: iki kademeli, çapraz geçişli gecikme hattı (zaman uzatmasız basit yöntem). */
class PitchShifter(sampleRate: Int) {
    private val size = 16384
    private val buf = FloatArray(size)
    private var w = 0
    private val win = sampleRate * 0.05f   // 50 ms pencere
    private var phase = 0f

    private fun read(delay: Float): Float {
        var pos = w - delay
        while (pos < 0f) pos += size
        val i = pos.toInt()
        val f = pos - i
        val a = buf[i % size]
        val b = buf[(i + 1) % size]
        return a + (b - a) * f
    }

    fun process(x: Float, ratio: Float): Float {
        buf[w] = x
        phase += (1f - ratio) / win
        if (phase >= 1f) phase -= 1f
        if (phase < 0f) phase += 1f
        val p2 = (phase + 0.5f) % 1f
        val s1 = sin(PI.toFloat() * phase)
        val s2 = sin(PI.toFloat() * p2)
        val y = read(phase * win) * (s1 * s1) + read(p2 * win) * (s2 * s2)
        w = (w + 1) % size
        return y
    }
}

class EchoLine(sampleRate: Int) {
    private val buf = FloatArray(sampleRate * 2)
    private var w = 0

    fun process(x: Float, delaySamples: Int, feedback: Float): Float {
        val d = delaySamples.coerceIn(1, buf.size - 1)
        var r = w - d
        if (r < 0) r += buf.size
        val delayed = buf[r]
        buf[w] = x + delayed * feedback
        w = (w + 1) % buf.size
        return delayed
    }
}

private class Comb(val buf: FloatArray, val feedback: Float, val damp: Float) {
    var i = 0; var store = 0f
    fun process(x: Float): Float {
        val out = buf[i]
        store = out * (1 - damp) + store * damp
        buf[i] = x + store * feedback
        i = (i + 1) % buf.size
        return out
    }
}

private class Allpass(val buf: FloatArray) {
    var i = 0
    fun process(x: Float): Float {
        val b = buf[i]
        val out = -x + b
        buf[i] = x + b * 0.5f
        i = (i + 1) % buf.size
        return out
    }
}

/** Freeverb benzeri yankı: 4 comb + 2 allpass. */
class Reverb(sampleRate: Int) {
    private val k = sampleRate / 44100f
    private val combs = listOf(1116, 1188, 1277, 1356).map { Comb(FloatArray((it * k).toInt()), 0.80f, 0.25f) }
    private val allpasses = listOf(556, 441).map { Allpass(FloatArray((it * k).toInt())) }

    fun process(x: Float): Float {
        val inp = x * 0.03f
        var s = 0f
        for (c in combs) s += c.process(inp)
        for (a in allpasses) s = a.process(s)
        return s
    }
}

/** Çok vuruşlu eko: her vuruş bir öncekinin `decay` katı kadar yüksek. En fazla 10 saniyelik toplam gecikme. */
class TapEcho(sampleRate: Int) {
    private val buf = FloatArray(sampleRate * 10 + 8)
    private var w = 0

    fun process(x: Float, delaySamples: Int, repeats: Int, decay: Float): Float {
        val n = repeats.coerceAtLeast(1)
        val d = delaySamples.coerceIn(1, (buf.size - 8) / n)
        buf[w] = x
        var out = 0f
        var gain = 1f
        for (k in 1..n) {
            gain *= decay
            var r = w - k * d
            if (r < 0) r += buf.size
            out += buf[r] * gain
        }
        w = (w + 1) % buf.size
        return out
    }
}

/** Gürültü kapısı: eşiğin altında ses gelirse (bekleme süresinden sonra) yumuşakça kısar. */
class NoiseGate(sr: Int) {
    private val envAttack = exp(-1.0 / (0.001 * sr)).toFloat()
    private val envRelease = exp(-1.0 / (0.030 * sr)).toFloat()
    private val gainOpen = exp(-1.0 / (0.003 * sr)).toFloat()
    private val gainClose = exp(-1.0 / (0.060 * sr)).toFloat()
    private val holdSamples = (0.100 * sr).toInt()
    private var env = 0f
    private var gain = 1f
    private var hold = 0

    fun process(x: Float, threshold: Float): Float {
        val a = abs(x)
        env = if (a > env) a + envAttack * (env - a) else a + envRelease * (env - a)
        val target: Float
        if (env > threshold) {
            hold = holdSamples
            target = 1f
        } else if (hold > 0) {
            hold--
            target = 1f
        } else {
            target = 0f
        }
        val c = if (target > gain) gainOpen else gainClose
        gain = target + c * (gain - target)
        return x * gain
    }
}

/** Basit ileri beslemeli kompresör (dB alanında), yarım kazanç telafisi ile. */
class Compressor(sr: Int) {
    private val attack = exp(-1.0 / (0.005 * sr)).toFloat()
    private val release = exp(-1.0 / (0.120 * sr)).toFloat()
    private var grDb = 0f

    fun process(x: Float, thresholdDb: Float, ratio: Float): Float {
        val r = ratio.coerceAtLeast(1f)
        val level = 20f * log10(max(abs(x), 1e-6f))
        val over = level - thresholdDb
        val target = if (over > 0f) over * (1f - 1f / r) else 0f
        val c = if (target > grDb) attack else release
        grDb = target + c * (grDb - target)
        val makeup = (-thresholdDb * (1f - 1f / r) * 0.5f).coerceAtLeast(0f)
        return x * 10f.pow((makeup - grDb) / 20f)
    }
}
