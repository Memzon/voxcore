package com.voxcore.audio

import com.voxcore.effects.*
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tanh

private data class Fx(
    val pitch: Float = 0f, val ringHz: Float = 0f, val ringMix: Float = 0f,
    val bandLow: Double = 0.0, val bandHigh: Double = 0.0,
    val bass: Float = 0f, val echoMix: Float = 0f, val echoMs: Float = 0f, val reverb: Float = 0f,
)

private fun fxFor(name: String): Fx = when (name) {
    "Derin ses" -> Fx(pitch = -5f, bass = 4f)
    "İnce ses" -> Fx(pitch = 5f)
    "Robot" -> Fx(ringHz = 55f, ringMix = 1f)
    "Uzaylı" -> Fx(pitch = 3f, ringHz = 70f, ringMix = 0.5f)
    "Telsiz" -> Fx(bandLow = 450.0, bandHigh = 3000.0)
    "Canavar" -> Fx(pitch = -8f, ringHz = 25f, ringMix = 0.3f, bass = 6f)
    "Mağara" -> Fx(echoMix = 0.35f, echoMs = 350f, reverb = 0.6f)
    "Telefon" -> Fx(bandLow = 300.0, bandHigh = 3400.0)
    else -> Fx()
}

/**
 * Tek kanallı gerçek zamanlı efekt zinciri. Ses iş parçacığından çağrılır.
 * Sıra: gürültü kapısı, perde, halka modülasyonu, bant sınırı, bas/tiz, 10 bant EQ,
 * kompresör, eko, yankı, ana ses, yumuşak sınırlayıcı.
 */
class EffectChain(private val sr: Int) {
    private val pitchShifter = PitchShifter(sr)
    private val tapEcho = TapEcho(sr)
    private val reverb = Reverb(sr)
    private val gate = NoiseGate(sr)
    private val comp = Compressor(sr)
    private val bassF = Biquad()
    private val trebleF = Biquad()
    private val eqF = Array(10) { Biquad() }
    private val hp = Biquad()
    private val lp = Biquad()
    private var ringPhase = 0.0

    private var lastBass = Float.NaN
    private var lastTreble = Float.NaN
    private val lastEq = FloatArray(10) { Float.NaN }
    private var lastBandLow = -1.0
    private var lastBandHigh = -1.0

    fun process(buf: FloatArray, n: Int) {
        val s = VoxSettings
        val fx = fxFor(s.effect)

        val bassDb = (s.bass + fx.bass).coerceIn(-18f, 18f)
        if (bassDb != lastBass) { bassF.lowShelf(sr, 120.0, bassDb.toDouble()); lastBass = bassDb }
        val trebleDb = s.treble
        if (trebleDb != lastTreble) { trebleF.highShelf(sr, 6000.0, trebleDb.toDouble()); lastTreble = trebleDb }
        for (i in 0 until 10) {
            val g = s.eq[i]
            if (g != lastEq[i]) { eqF[i].peaking(sr, s.eqFreqs[i], g.toDouble(), 1.4); lastEq[i] = g }
        }
        val useBand = fx.bandHigh > 0.0
        if (useBand && (fx.bandLow != lastBandLow || fx.bandHigh != lastBandHigh)) {
            hp.highpass(sr, fx.bandLow); lp.lowpass(sr, fx.bandHigh)
            lastBandLow = fx.bandLow; lastBandHigh = fx.bandHigh
        }

        val pitchTotal = s.pitch + fx.pitch
        val ratio = 2.0.pow((pitchTotal / 12f).toDouble()).toFloat()
        val usePitch = abs(pitchTotal) > 0.01f
        val echoMix = ((s.echoMix / 100f) + fx.echoMix).coerceIn(0f, 1f)
        val echoMs = if (fx.echoMs > 0f) fx.echoMs else s.echoDelayMs
        val echoSamples = (echoMs * sr / 1000f).toInt()
        val echoRepeats = s.echoRepeats.toInt().coerceIn(1, 10)
        val decay = (s.echoFeedback / 100f).coerceIn(0f, 0.9f)
        val revMix = ((s.reverbMix / 100f) + fx.reverb).coerceIn(0f, 1f)
        val vol = s.master / 100f
        val ringInc = 2 * PI * fx.ringHz / sr

        val gateOn = s.gateEnabled
        val gateLin = 10.0.pow(s.gateDb / 20.0).toFloat()
        val compOn = s.compEnabled
        val compThr = s.compThresholdDb
        val compRatio = s.compRatio

        for (i in 0 until n) {
            var x = buf[i]
            if (gateOn) x = gate.process(x, gateLin)
            if (usePitch) x = pitchShifter.process(x, ratio)
            if (fx.ringMix > 0f) {
                val m = sin(ringPhase).toFloat()
                ringPhase += ringInc
                if (ringPhase > 2 * PI) ringPhase -= 2 * PI
                x = x * (1f - fx.ringMix) + x * m * fx.ringMix
            }
            if (useBand) x = lp.process(hp.process(x)) * 1.6f
            x = bassF.process(x)
            x = trebleF.process(x)
            for (f in eqF) x = f.process(x)
            if (compOn) x = comp.process(x, compThr, compRatio)
            if (echoMix > 0f) x += tapEcho.process(x, echoSamples, echoRepeats, decay) * echoMix
            else tapEcho.process(x, echoSamples, echoRepeats, decay)
            if (revMix > 0f) x += reverb.process(x) * revMix * 1.5f
            x *= vol
            buf[i] = softLimit(x)
        }
    }

    private fun softLimit(x: Float): Float {
        val a = abs(x)
        if (a <= 0.8f) return x
        val y = 0.8f + 0.2f * tanh((a - 0.8f) / 0.2f)
        return if (x < 0) -y else y
    }
}
