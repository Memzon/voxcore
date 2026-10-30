package com.voxcore.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import kotlin.math.max

/** AudioRecord / AudioTrack oluşturma ve ses cihazı listeleme. Motor ve gecikme testi ortak kullanır. */
object AudioIo {
    private var app: Context? = null

    fun init(ctx: Context) { app = ctx.applicationContext }

    class Io(val rec: AudioRecord, val track: AudioTrack, val inBytes: Int, val outBytes: Int) {
        fun release() {
            runCatching { rec.stop() }; runCatching { rec.release() }
            runCatching { track.stop() }; runCatching { track.release() }
        }
    }

    class Dev(val id: Int, val label: String)

    /** Başarısız olursa Türkçe mesajlı IllegalStateException fırlatır. */
    fun open(sr: Int, frames: Int): Io {
        val inMin = AudioRecord.getMinBufferSize(sr, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val inBytes = max(inMin, frames * 2 * 2)
        val rec = createRecord(sr, inBytes, preferCommunication = VoxSettings.inputSource == 1)
            ?: throw IllegalStateException(
                "Mikrofon açılamadı. İzni ve başka bir uygulamanın mikrofonu kullanıp kullanmadığını kontrol edin."
            )
        try {
            deviceById(VoxSettings.inputDeviceId, AudioManager.GET_DEVICES_INPUTS)?.let { rec.setPreferredDevice(it) }
            val outMin = AudioTrack.getMinBufferSize(sr, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
            val outBytes = max(outMin, frames * 2 * 2)
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sr)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build()
                )
                .setBufferSizeInBytes(outBytes)
                .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
                .build()
            deviceById(VoxSettings.outputDeviceId, AudioManager.GET_DEVICES_OUTPUTS)?.let { track.setPreferredDevice(it) }
            return Io(rec, track, inBytes, outBytes)
        } catch (e: Exception) {
            runCatching { rec.release() }
            throw IllegalStateException("Ses çıkışı açılamadı: ${e.message}")
        }
    }

    private fun createRecord(sr: Int, bytes: Int, preferCommunication: Boolean): AudioRecord? {
        val sources = if (preferCommunication)
            listOf(MediaRecorder.AudioSource.VOICE_COMMUNICATION, MediaRecorder.AudioSource.MIC)
        else listOf(MediaRecorder.AudioSource.MIC)
        for (src in sources) {
            val r = AudioRecord(src, sr, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bytes)
            if (r.state == AudioRecord.STATE_INITIALIZED) return r
            r.release()
        }
        return null
    }

    private fun audioManager(): AudioManager? = app?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private fun deviceById(id: Int, flag: Int): AudioDeviceInfo? {
        if (id == 0) return null
        return audioManager()?.getDevices(flag)?.firstOrNull { it.id == id }
    }

    private fun typeName(t: Int): String = when (t) {
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Dahili hoparlör"
        AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> "Ahize"
        AudioDeviceInfo.TYPE_BUILTIN_MIC -> "Dahili mikrofon"
        AudioDeviceInfo.TYPE_WIRED_HEADSET -> "Kablolu kulaklık (mikrofonlu)"
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "Kablolu kulaklık"
        AudioDeviceInfo.TYPE_USB_DEVICE -> "USB cihaz"
        AudioDeviceInfo.TYPE_USB_HEADSET -> "USB kulaklık"
        AudioDeviceInfo.TYPE_USB_ACCESSORY -> "USB aksesuar"
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "Bluetooth (müzik)"
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "Bluetooth (konuşma)"
        AudioDeviceInfo.TYPE_LINE_ANALOG -> "Hat girişi/çıkışı"
        AudioDeviceInfo.TYPE_HDMI -> "HDMI"
        else -> "Diğer (tip $t)"
    }

    private fun list(flag: Int): List<Dev> =
        audioManager()?.getDevices(flag)?.map { d ->
            val name = d.productName?.toString().orEmpty()
            Dev(d.id, typeName(d.type) + if (name.isNotBlank()) " – $name" else "")
        } ?: emptyList()

    fun outputDevices(): List<Dev> = list(AudioManager.GET_DEVICES_OUTPUTS)
    fun inputDevices(): List<Dev> = list(AudioManager.GET_DEVICES_INPUTS)

    fun headphonesConnected(): Boolean =
        audioManager()?.getDevices(AudioManager.GET_DEVICES_OUTPUTS)?.any {
            it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES || it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || it.type == AudioDeviceInfo.TYPE_USB_HEADSET ||
                it.type == AudioDeviceInfo.TYPE_BLE_HEADSET
        } ?: false
}
