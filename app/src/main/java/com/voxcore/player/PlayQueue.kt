package com.voxcore.player

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

data class Track(val uri: String, val name: String)

enum class RepeatMode(val label: String) {
    Off("Kapalı"), One("Birini tekrarla"), All("Hepsini tekrarla")
}

/** Ses kitaplığı ve çalma sırası (aynı liste). Kalıcıdır: SharedPreferences içinde JSON. */
object PlayQueue {
    val tracks = mutableStateListOf<Track>()
    var index by mutableIntStateOf(-1)
    var repeatMode by mutableStateOf(RepeatMode.Off)
    var shuffle by mutableStateOf(false)

    private var loaded = false
    private const val PREFS = "voxcore_queue"

    fun load(ctx: Context) {
        if (loaded) return
        loaded = true
        val p = ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        runCatching {
            val arr = JSONArray(p.getString("tracks", "[]"))
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                tracks.add(Track(o.getString("uri"), o.getString("name")))
            }
        }
        repeatMode = RepeatMode.entries.firstOrNull { it.name == p.getString("repeat", "Off") } ?: RepeatMode.Off
        shuffle = p.getBoolean("shuffle", false)
    }

    fun save(ctx: Context) {
        val arr = JSONArray()
        tracks.forEach { arr.put(JSONObject().put("uri", it.uri).put("name", it.name)) }
        ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("tracks", arr.toString())
            .putString("repeat", repeatMode.name)
            .putBoolean("shuffle", shuffle)
            .apply()
    }

    fun add(ctx: Context, t: Track) {
        if (tracks.none { it.uri == t.uri }) tracks.add(t)
        save(ctx)
    }

    fun remove(ctx: Context, i: Int) {
        if (i !in tracks.indices) return
        val wasCurrent = i == index
        tracks.removeAt(i)
        if (wasCurrent) { SoundPlayer.stop(); index = -1 } else if (i < index) index -= 1
        save(ctx)
    }

    fun clear(ctx: Context) {
        SoundPlayer.stop()
        tracks.clear()
        index = -1
        save(ctx)
    }

    /** Bir adım yukarı (-1) ya da aşağı (+1) taşır. */
    fun move(ctx: Context, from: Int, delta: Int) {
        val to = from + delta
        if (from !in tracks.indices || to !in tracks.indices) return
        val t = tracks.removeAt(from)
        tracks.add(to, t)
        index = when (index) {
            from -> to
            to -> from
            else -> index
        }
        save(ctx)
    }

    /** Sıradaki parça. auto = parça kendiliğinden bitti. Null = çalmayı durdur. */
    fun nextIndex(auto: Boolean): Int? {
        val n = tracks.size
        if (n == 0) return null
        if (auto && repeatMode == RepeatMode.One && index in 0 until n) return index
        if (shuffle && n > 1) {
            var r: Int
            do { r = Random.nextInt(n) } while (r == index)
            return r
        }
        val nx = index + 1
        if (nx < n) return nx
        return if (repeatMode == RepeatMode.All || !auto) 0 else null
    }

    fun previousIndex(): Int? {
        val n = tracks.size
        if (n == 0) return null
        return if (index <= 0) n - 1 else index - 1
    }
}
