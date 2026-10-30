package com.voxcore.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.voxcore.audio.VoxSettings
import java.util.Locale

/**
 * Akıllı kelime eko: Android SpeechRecognizer (tr-TR) ile konuşmayı dinler,
 * her kelimeyi Android TextToSpeech (tr-TR) ile belirlenen kez yeniden söyler.
 * Sınırlar: tanıma kalitesi ve çevrimdışı destek cihaza bağlıdır; ses motoru ile aynı anda
 * mikrofon kullanımı bazı cihazlarda çalışmayabilir. Tüm çağrılar ana iş parçacığından yapılmalı.
 */
object WordEcho {
    var active by mutableStateOf(false)
        private set
    var status by mutableStateOf("Kapalı")
        private set
    var lastHeard by mutableStateOf("")
        private set

    private const val END_ID = "vox-end"
    private val main = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var listening = false
    private var appCtx: Context? = null
    private val guardRunnable = Runnable { listen() }

    fun start(context: Context) {
        if (active) return
        val ctx = context.applicationContext
        if (!SpeechRecognizer.isRecognitionAvailable(ctx)) {
            status = "Bu cihazda konuşma tanıma servisi yok."
            return
        }
        appCtx = ctx
        active = true
        listening = false
        ttsReady = false
        lastHeard = ""
        status = "Başlatılıyor"
        tts = TextToSpeech(ctx) { code ->
            main.post {
                if (!active) return@post
                if (code != TextToSpeech.SUCCESS) { fail("Metni konuşmaya çevirme motoru açılamadı."); return@post }
                val r = tts?.setLanguage(Locale("tr", "TR"))
                if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
                    fail("Cihazda Türkçe ses verisi yok. Ayarlar > Metin okuma bölümünden Türkçe sesi indirin.")
                    return@post
                }
                tts?.setOnUtteranceProgressListener(utteranceListener)
                ttsReady = true
                recognizer = SpeechRecognizer.createSpeechRecognizer(ctx).also { it.setRecognitionListener(recListener) }
                listen()
            }
        }
    }

    fun stop() {
        active = false
        listening = false
        ttsReady = false
        main.removeCallbacksAndMessages(null)
        recognizer?.let { runCatching { it.cancel() }; runCatching { it.destroy() } }
        recognizer = null
        tts?.let { runCatching { it.stop() }; runCatching { it.shutdown() } }
        tts = null
        status = "Kapalı"
    }

    private fun fail(msg: String) {
        stop()
        status = msg
    }

    private fun listenLater(ms: Long) {
        main.postDelayed({ listen() }, ms)
    }

    private fun listen() {
        if (!active || listening) return
        val r = recognizer ?: return
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }
        listening = true
        try {
            r.startListening(intent)
        } catch (e: Exception) {
            listening = false
            fail("Tanıma başlatılamadı: ${e.message}")
        }
    }

    private fun speakWords(text: String) {
        val words = text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        val t = tts
        if (words.isEmpty() || t == null || !ttsReady) { listenLater(200); return }
        status = "Tekrarlıyor"
        t.setSpeechRate(VoxSettings.wordSpeed.coerceIn(0.5f, 2f))
        val reps = VoxSettings.wordRepeats.toInt().coerceIn(1, 6)
        val pause = VoxSettings.wordPauseMs.toLong().coerceAtLeast(0L)
        var n = 0
        words.forEachIndexed { wi, w ->
            for (r in 0 until reps) {
                val last = wi == words.lastIndex && r == reps - 1
                val id = if (last) END_ID else "vox-${n++}"
                t.speak(w, TextToSpeech.QUEUE_ADD, null, id)
            }
            if (pause > 0L && wi != words.lastIndex) {
                t.playSilentUtterance(pause, TextToSpeech.QUEUE_ADD, "vox-p${n++}")
            }
        }
        // Konuşma bitiş bildirimi gelmezse takılmamak için emniyet zamanlayıcısı.
        val guard = words.size * reps * 1500L + words.size * pause + 4000L
        main.removeCallbacks(guardRunnable)
        main.postDelayed(guardRunnable, guard)
    }

    private val utteranceListener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {}
        override fun onDone(utteranceId: String?) {
            if (utteranceId == END_ID) main.post { main.removeCallbacks(guardRunnable); listen() }
        }

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) {
            if (utteranceId == END_ID) main.post { main.removeCallbacks(guardRunnable); listen() }
        }
    }

    private val recListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) { status = "Dinliyor" }
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() { status = "İşleniyor" }
        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}

        override fun onError(error: Int) {
            listening = false
            if (!active) return
            when (error) {
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> fail("Mikrofon izni yok.")
                SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> {
                    status = "Ağ gerekiyor olabilir, yeniden deniyor"
                    listenLater(1500)
                }
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> listenLater(1000)
                else -> listenLater(400)
            }
        }

        override fun onResults(results: Bundle?) {
            listening = false
            if (!active) return
            val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
            lastHeard = text
            speakWords(text)
        }
    }
}
