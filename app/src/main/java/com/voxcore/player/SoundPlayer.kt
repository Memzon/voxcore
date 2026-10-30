package com.voxcore.player

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * MP3/WAV/OGG oynatıcı (Android MediaPlayer). Çıkış cihazın normal medya çıkışıdır;
 * Discord'a veya mikrofon girişine otomatik aktarılmaz.
 */
object SoundPlayer {
    var playing by mutableStateOf(false)
        private set
    var durationMs by mutableIntStateOf(0)
        private set
    var currentName by mutableStateOf("")
        private set
    var error by mutableStateOf<String?>(null)
    var volume by mutableFloatStateOf(80f)
        private set

    private var mp: MediaPlayer? = null
    private var app: Context? = null

    fun playIndex(ctx: Context, i: Int) {
        val t = PlayQueue.tracks.getOrNull(i) ?: return
        PlayQueue.index = i
        start(ctx, Uri.parse(t.uri), t.name)
    }

    /** Kuyrukta olmayan dosya (ör. kayıt geçmişi) çalar. */
    fun playExternal(ctx: Context, uri: Uri, name: String) {
        PlayQueue.index = -1
        start(ctx, uri, name)
    }

    private fun start(ctx: Context, uri: Uri, name: String) {
        release()
        error = null
        val c = ctx.applicationContext
        app = c
        currentName = name
        val p = MediaPlayer()
        mp = p
        try {
            p.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
            )
            p.setDataSource(c, uri)
            p.setOnPreparedListener { m ->
                durationMs = m.duration
                m.setVolume(volume / 100f, volume / 100f)
                m.start()
                playing = true
            }
            p.setOnCompletionListener { onCompleted() }
            p.setOnErrorListener { _, what, extra ->
                error = "Çalma hatası ($what/$extra). Dosya silinmiş, taşınmış ya da desteklenmiyor olabilir."
                release()
                true
            }
            p.prepareAsync()
        } catch (e: Exception) {
            error = "Dosya açılamadı: ${e.message}"
            release()
        }
    }

    private fun onCompleted() {
        playing = false
        val c = app ?: return
        if (PlayQueue.index < 0) return
        val n = PlayQueue.nextIndex(auto = true) ?: return
        playIndex(c, n)
    }

    fun togglePause(ctx: Context) {
        val p = mp
        if (p == null) {
            if (PlayQueue.tracks.isNotEmpty()) playIndex(ctx, PlayQueue.index.coerceAtLeast(0))
            return
        }
        try {
            if (p.isPlaying) { p.pause(); playing = false } else { p.start(); playing = true }
        } catch (e: IllegalStateException) {
            release()
        }
    }

    fun next(ctx: Context) {
        PlayQueue.nextIndex(auto = false)?.let { playIndex(ctx, it) }
    }

    fun previous(ctx: Context) {
        if (position() > 3000) { seekTo(0); return }
        PlayQueue.previousIndex()?.let { playIndex(ctx, it) }
    }

    fun stop() {
        release()
        currentName = ""
    }

    fun updateVolume(v: Float) {
        volume = v.coerceIn(0f, 100f)
        try { mp?.setVolume(volume / 100f, volume / 100f) } catch (e: IllegalStateException) { }
    }

    fun position(): Int = try { mp?.currentPosition ?: 0 } catch (e: IllegalStateException) { 0 }

    fun seekTo(ms: Int) {
        try { mp?.seekTo(ms) } catch (e: IllegalStateException) { }
    }

    private fun release() {
        runCatching { mp?.release() }
        mp = null
        playing = false
        durationMs = 0
    }
}
