package com.voxcore.effects

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** RBJ biquad filtresi (Audio EQ Cookbook). Doğrudan form II transpoze. */
class Biquad {
    private var b0 = 1.0; private var b1 = 0.0; private var b2 = 0.0
    private var a1 = 0.0; private var a2 = 0.0
    private var z1 = 0.0; private var z2 = 0.0

    fun process(x: Float): Float {
        val xd = x.toDouble()
        val y = b0 * xd + z1
        z1 = b1 * xd - a1 * y + z2
        z2 = b2 * xd - a2 * y
        return y.toFloat()
    }

    private fun set(nb0: Double, nb1: Double, nb2: Double, a0: Double, na1: Double, na2: Double) {
        b0 = nb0 / a0; b1 = nb1 / a0; b2 = nb2 / a0; a1 = na1 / a0; a2 = na2 / a0
    }

    fun peaking(fs: Int, f0: Double, gainDb: Double, q: Double) {
        val a = 10.0.pow(gainDb / 40.0)
        val w = 2 * PI * f0 / fs
        val alpha = sin(w) / (2 * q)
        val c = cos(w)
        set(1 + alpha * a, -2 * c, 1 - alpha * a, 1 + alpha / a, -2 * c, 1 - alpha / a)
    }

    fun lowShelf(fs: Int, f0: Double, gainDb: Double) {
        val a = 10.0.pow(gainDb / 40.0)
        val w = 2 * PI * f0 / fs
        val c = cos(w)
        val t = 2 * sqrt(a) * (sin(w) / 2 * sqrt(2.0))
        set(
            a * ((a + 1) - (a - 1) * c + t), 2 * a * ((a - 1) - (a + 1) * c), a * ((a + 1) - (a - 1) * c - t),
            (a + 1) + (a - 1) * c + t, -2 * ((a - 1) + (a + 1) * c), (a + 1) + (a - 1) * c - t
        )
    }

    fun highShelf(fs: Int, f0: Double, gainDb: Double) {
        val a = 10.0.pow(gainDb / 40.0)
        val w = 2 * PI * f0 / fs
        val c = cos(w)
        val t = 2 * sqrt(a) * (sin(w) / 2 * sqrt(2.0))
        set(
            a * ((a + 1) + (a - 1) * c + t), -2 * a * ((a - 1) + (a + 1) * c), a * ((a + 1) + (a - 1) * c - t),
            (a + 1) - (a - 1) * c + t, 2 * ((a - 1) - (a + 1) * c), (a + 1) - (a - 1) * c - t
        )
    }

    fun lowpass(fs: Int, f0: Double, q: Double = 0.707) {
        val w = 2 * PI * f0 / fs
        val alpha = sin(w) / (2 * q)
        val c = cos(w)
        set((1 - c) / 2, 1 - c, (1 - c) / 2, 1 + alpha, -2 * c, 1 - alpha)
    }

    fun highpass(fs: Int, f0: Double, q: Double = 0.707) {
        val w = 2 * PI * f0 / fs
        val alpha = sin(w) / (2 * q)
        val c = cos(w)
        set((1 + c) / 2, -(1 + c), (1 + c) / 2, 1 + alpha, -2 * c, 1 - alpha)
    }
}
