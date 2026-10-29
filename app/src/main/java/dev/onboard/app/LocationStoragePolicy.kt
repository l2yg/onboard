package dev.onboard.app

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Select observations for storage; never interpolate a road or hide a collection gap. */
class LocationStoragePolicy {
    private data class Fix(val session: Long, val visit: Long?, val elapsed: Long,
        val lat: Double, val lon: Double, val accuracy: Float?, val provider: String)
    private var previous: Fix? = null

    fun keep(session: Long, visit: Long?, elapsed: Long, lat: Double, lon: Double,
             accuracy: Float?, provider: String): Boolean {
        if (!lat.isFinite() || !lon.isFinite() || lat !in -90.0..90.0 || lon !in -180.0..180.0) return false
        val old = previous
        val valid = accuracy != null && accuracy.isFinite() && accuracy in 0f..100f
        val oldValid = old?.accuracy?.let { it.isFinite() && it in 0f..100f } ?: false
        val changed = old == null || old.session != session || old.visit != visit || old.provider != provider || valid != oldValid
        val age = old?.let { elapsed - it.elapsed } ?: 0
        val distance = old?.let { distance(it.lat, it.lon, lat, lon) } ?: 0.0
        val stationary = old != null && distance <= maxOf(25.0, (accuracy ?: 100f).toDouble(), (old.accuracy ?: 100f).toDouble())
        val keep = changed || age < 0 || age >= (if (stationary || !valid) 60_000 else 15_000) ||
            (valid && age >= 5_000 && distance >= 50.0)
        if (keep) previous = Fix(session, visit, elapsed, lat, lon, accuracy, provider)
        return keep
    }

    private fun distance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val a = Math.toRadians(lat1); val b = Math.toRadians(lat2)
        val d = sin((b - a) / 2).let { it * it } + cos(a) * cos(b) * sin(Math.toRadians(lon2 - lon1) / 2).let { it * it }
        return 6_371_000 * 2 * asin(sqrt(d.coerceIn(0.0, 1.0)))
    }
}
