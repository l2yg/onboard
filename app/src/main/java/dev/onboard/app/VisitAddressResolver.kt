package dev.onboard.app

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import java.util.Locale
import java.util.concurrent.Executors

/** Best-effort address lookup; never infers a business visit from a coordinate. */
class VisitAddressResolver(context: Context, private val resolved: (Visit, String) -> Unit) {
    private val appContext = context.applicationContext
    private val geocoder = Geocoder(context.applicationContext, Locale.KOREA)
    private val handler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()
    private val attempts = mutableMapOf<Pair<Long, Long>, Long>()
    private val pending = mutableSetOf<Pair<Long, Long>>()
    private val waiting = mutableMapOf<Pair<Long, Long>, Visit>()
    private var navigation: NavigationLink? = null
    private var closed = false

    fun request(visit: Visit) {
        val lat = visit.latitude ?: return
        val lon = visit.longitude ?: return
        if (closed || visit.demo || visit.address.isNotBlank() || !lat.isFinite() || !lon.isFinite() ||
            lat !in -90.0..90.0 || lon !in -180.0..180.0) return
        val key = visit.id to visit.start
        val now = SystemClock.elapsedRealtime()
        if (pending.size >= 2 || key in pending || attempts[key]?.let { now - it < 600_000 } == true) return
        attempts[key] = now
        pending.add(key)
        waiting[key] = visit
        val timeout = Runnable { pending.remove(key); waiting.remove(key) }
        handler.postDelayed(timeout, 15_000)
        fun finish(addresses: List<Address>) {
            val address = addresses.firstOrNull()?.getAddressLine(0)?.trim().orEmpty()
            handler.post {
                if (!closed && address.isNotBlank() && pending.remove(key)) {
                    waiting.remove(key)
                    handler.removeCallbacks(timeout)
                    if (address.isNotBlank()) resolved(visit, address)
                }
            }
        }
        if (navigation == null) {
            navigation = NavigationLink(context = appContext, status = {}, location = { latitude, longitude, address ->
                if (address.isNotBlank()) {
                    waiting.values.toList().forEach { candidate ->
                        val distance = FloatArray(1)
                        Location.distanceBetween(candidate.latitude!!, candidate.longitude!!, latitude, longitude, distance)
                        if (distance[0] <= 100f) {
                            val candidateKey = candidate.id to candidate.start
                            if (pending.remove(candidateKey)) {
                                waiting.remove(candidateKey)
                                resolved(candidate.copy(latitude = latitude, longitude = longitude), address)
                            }
                        }
                    }
                }
            }).also { it.start() }
        } else navigation?.refreshLocation()
        if (!Geocoder.isPresent()) return
        executor.execute {
            try {
                if (Build.VERSION.SDK_INT >= 33) {
                    geocoder.getFromLocation(lat, lon, 1, object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) = finish(addresses)
                        override fun onError(errorMessage: String?) = finish(emptyList())
                    })
                } else {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(lat, lon, 1).orEmpty()
                    finish(addresses)
                }
            } catch (_: Exception) { finish(emptyList()) }
        }
    }

    fun close() {
        closed = true
        pending.clear()
        waiting.clear()
        navigation?.close(); navigation = null
        handler.removeCallbacksAndMessages(null)
        executor.shutdownNow()
    }
}
