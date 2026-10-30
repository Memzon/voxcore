package com.voxcore.audio

import android.os.Process
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.voxcore.recorder.Recorder
import kotlin.math.sqrt

/**
 * Canlı mikrofon -> efekt zinciri -> hoparlör/kulaklık motoru.
 * AudioRecord + AudioTrack (düşük gecikme modu). Oboe ileride değerlendirilebilir.
 */
object VoxEngine {
    var running by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var estimatedLatencyMs by mutableStateOf(0)
        private set
    var activeSampleRate by mutableStateOf(0)
        private set

    @Volatile
    var level = 0f
        private set

    /** Motor durduğunda (kullanıcı veya hata) çağrılır; servis bununla kapanır. */
    @Volatile
    var onStopped: (() -> Unit)? = null

    @Volatile
    private var stopFlag = false
    private var thread: Thread? = null

    @Synchronized
    fun start() {
        if (running) return
        error = null
        stopFlag = false
        running = true
        thread = Thread({ runLoop() }, "voxcore-audio").also { it.start() }
    }

    fun stop() { stopFlag = true }

    private fun runLoop() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
        val sr = VoxSettings.sampleRate
        val frames = VoxSettings.blockFrames.coerceIn(64, 1024)
        var io: AudioIo.Io? = null
        try {
            val opened = AudioIo.open(sr, frames)
            io = opened
            val rec = opened.rec
            val track = opened.track

            activeSampleRate = sr
            // Tahmin: giriş + çıkış arabellekleri. Gerçek uçtan uca gecikme ölçümü değildir (Ayarlar > gecikme testi).
            estimatedLatencyMs = (((opened.inBytes + opened.outBytes) / 2) * 1000f / sr).toInt()
            Recorder.onEngineStarted(sr)

            val chain = EffectChain(sr)
            val shorts = ShortArray(frames)
            val floats = FloatArray(frames)
            rec.startRecording()
            track.play()

            while (!stopFlag) {
                val n = rec.read(shorts, 0, frames)
                if (n < 0) { error = "Mikrofon okuma hatası ($n)."; break }
                if (n == 0) continue
                var sum = 0f
                for (i in 0 until n) { val v = shorts[i] / 32768f; floats[i] = v; sum += v * v }
                level = (sqrt(sum / n) * 4f).coerceIn(0f, 1f)
                chain.process(floats, n)
                for (i in 0 until n) shorts[i] = (floats[i].coerceIn(-1f, 1f) * 32767f).toInt().toShort()
                track.write(shorts, 0, n)
                if (Recorder.recording) Recorder.write(shorts, n)
            }
        } catch (e: IllegalStateException) {
            error = e.message ?: "Ses motoru hatası."
        } catch (e: SecurityException) {
            error = "Mikrofon izni yok."
        } catch (e: Exception) {
            error = "Ses motoru hatası: ${e.message}"
        } finally {
            io?.release()
            Recorder.onEngineStopped()
            level = 0f
            running = false
            onStopped?.invoke()
        }
    }
}
