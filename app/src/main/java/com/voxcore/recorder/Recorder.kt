package com.voxcore.recorder

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * İşlenmiş (efektli) sesi 16 bit mono WAV olarak kaydeder.
 * Ses motoru çalışırken kayıt başlar; motor kapanırsa kayıt da düzgünce kapanır.
 */
object Recorder {
    var armed by mutableStateOf(false)
        private set
    var recording by mutableStateOf(false)
        private set
    var seconds by mutableIntStateOf(0)
        private set
    var error by mutableStateOf<String?>(null)
    val files = mutableStateListOf<File>()

    private var dir: File? = null
    private var raf: RandomAccessFile? = null
    private var sr = 48000
    private var samples = 0L
    private var bytes = ByteArray(0)

    fun init(ctx: Context) {
        dir = File(ctx.applicationContext.filesDir, "recordings").also { it.mkdirs() }
        refresh()
    }

    fun refresh() {
        val list = dir?.listFiles { f -> f.isFile && f.name.endsWith(".wav") }?.sortedByDescending { it.name } ?: emptyList()
        files.clear()
        files.addAll(list)
    }

    /** Kullanıcı "kaydı başlat" dedi. Motor çalışıyorsa hemen, değilse motor açılınca başlar. */
    fun arm(engineRunningRate: Int) {
        error = null
        armed = true
        if (engineRunningRate > 0) begin(engineRunningRate)
    }

    fun disarm() { armed = false }

    fun onEngineStarted(rate: Int) { if (armed) begin(rate) }

    fun onEngineStopped() { stop() }

    @Synchronized
    private fun begin(rate: Int) {
        if (recording) return
        val d = dir
        if (d == null) { error = "Kayıt klasörü hazır değil."; armed = false; return }
        val name = "kayit-" + SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date()) + ".wav"
        try {
            val r = RandomAccessFile(File(d, name), "rw")
            r.setLength(0)
            r.write(header(rate, 0L))
            raf = r
            sr = rate
            samples = 0L
            seconds = 0
            recording = true
        } catch (e: IOException) {
            error = "Kayıt dosyası açılamadı: ${e.message}"
        }
        armed = false
    }

    @Synchronized
    fun write(buf: ShortArray, n: Int) {
        val r = raf ?: return
        if (bytes.size < n * 2) bytes = ByteArray(n * 2)
        for (i in 0 until n) {
            val v = buf[i].toInt()
            bytes[2 * i] = (v and 0xFF).toByte()
            bytes[2 * i + 1] = ((v shr 8) and 0xFF).toByte()
        }
        try {
            r.write(bytes, 0, n * 2)
            samples += n
            seconds = (samples / sr).toInt()
        } catch (e: IOException) {
            error = "Diske yazılamadı: ${e.message}"
            stop()
        }
    }

    @Synchronized
    fun stop() {
        armed = false
        val r = raf ?: run { recording = false; return }
        raf = null
        recording = false
        try {
            r.seek(0)
            r.write(header(sr, samples * 2))
        } catch (e: IOException) {
            error = "WAV başlığı yazılamadı: ${e.message}"
        } finally {
            runCatching { r.close() }
        }
        refresh()
    }

    private fun header(rate: Int, dataBytes: Long): ByteArray {
        val b = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        b.put("RIFF".toByteArray(Charsets.US_ASCII))
        b.putInt((36 + dataBytes).toInt())
        b.put("WAVE".toByteArray(Charsets.US_ASCII))
        b.put("fmt ".toByteArray(Charsets.US_ASCII))
        b.putInt(16)
        b.putShort(1)          // PCM
        b.putShort(1)          // mono
        b.putInt(rate)
        b.putInt(rate * 2)
        b.putShort(2)
        b.putShort(16)
        b.put("data".toByteArray(Charsets.US_ASCII))
        b.putInt(dataBytes.toInt())
        return b.array()
    }

    /** Dosya başlığından süre (sn) ve örnekleme hızı. Okunamazsa null. */
    fun info(f: File): Pair<Int, Int>? = try {
        RandomAccessFile(f, "r").use { r ->
            if (r.length() < 44) null else {
                val h = ByteArray(44)
                r.readFully(h)
                val bb = ByteBuffer.wrap(h).order(ByteOrder.LITTLE_ENDIAN)
                val rate = bb.getInt(24)
                val data = bb.getInt(40).toLong() and 0xFFFFFFFFL
                if (rate <= 0) null else Pair((data / 2 / rate).toInt(), rate)
            }
        }
    } catch (e: IOException) { null }
}
