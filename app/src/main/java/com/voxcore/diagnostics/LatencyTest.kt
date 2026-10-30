package com.voxcore.diagnostics

import android.os.Process
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.voxcore.audio.AudioIo
import com.voxcore.audio.VoxEngine
import kotlin.math.abs
import kotlin.math.max

/**
 * Gidiş-dönüş gecikme ölçümü: çıkıştan kısa bir tıklama çalar, mikrofonda ne zaman duyulduğuna bakar.
 * Hoparlör -> mikrofon (sessiz ortam) ya da çıkıştan girişe kablo/loopback gerekir.
 * Kulaklıkta (hoparlör yolu yokken) ölçülemez. Sonuç yaklaşık bir değerdir.
 */
object LatencyTest {
    var running by mutableStateOf(false)
        private set
    var result by mutableStateOf<String?>(null)
        private set

    fun start() {
        if (running) return
        if (VoxEngine.running) { result = "Önce ses motorunu durdurun."; return }
        running = true
        result = "Ölçülüyor... Ortam sessiz olsun; telefonun hoparlörü mikrofona yakın olsun."
        Thread({ run() }, "voxcore-latency").start()
    }

    private fun run() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
        val sr = 48000
        val frames = 256
        var io: AudioIo.Io? = null
        try {
            val o = AudioIo.open(sr, frames)
            io = o
            val shorts = ShortArray(frames)
            val silence = ShortArray(frames)
            val click = ShortArray(frames) { i ->
                if (i < 96) (if ((i / 12) % 2 == 0) 30000 else -30000).toShort() else 0.toShort()
            }
            val warmup = 60
            val total = warmup + 120
            var clickBlock = -1
            var noise = 0
            var detected = -1
            o.rec.startRecording()
            o.track.play()
            for (b in 0 until total) {
                val n = o.rec.read(shorts, 0, frames)
                if (n <= 0) { result = "Mikrofon okunamadı."; return }
                if (b < warmup) {
                    for (i in 0 until n) noise = max(noise, abs(shorts[i].toInt()))
                } else if (detected < 0 && clickBlock >= 0) {
                    val thr = max(noise * 4, 3000)
                    for (i in 0 until n) {
                        if (abs(shorts[i].toInt()) > thr) {
                            detected = (b - (clickBlock + 1)) * frames + i
                            break
                        }
                    }
                }
                val out = if (b == warmup) { clickBlock = b; click } else silence
                o.track.write(out, 0, frames)
            }
            result = if (detected < 0) {
                "Tıklama mikrofonda duyulmadı. Ses seviyesini artırın, hoparlörü mikrofona yaklaştırın. " +
                    "Kulaklık takılıyken ölçüm yapılamaz."
            } else {
                val ms = detected * 1000f / sr
                val blockMs = frames * 1000f / sr
                "Gidiş-dönüş gecikmesi yaklaşık %.0f ms (+ bir blok girişi bekleme, %.0f ms). Yaklaşık değerdir; cihaza ve yola göre değişir."
                    .format(ms, blockMs)
            }
        } catch (e: IllegalStateException) {
            result = e.message ?: "Ölçüm başlatılamadı."
        } catch (e: SecurityException) {
            result = "Mikrofon izni yok."
        } catch (e: Exception) {
            result = "Ölçüm hatası: ${e.message}"
        } finally {
            io?.release()
            running = false
        }
    }
}
