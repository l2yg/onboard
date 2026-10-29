package dev.onboard.app

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import java.util.Locale
import java.util.concurrent.Executors

/** Best-effort address lookup; never infers a business visit from a coordinate. */
class VisitAddressResolver(context: Context, private val resolved: (Visit, String) -> Unit) {
    private val geocoder = Geocoder(context.applicationContext, Locale.KOREA)
    private val handler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()
    private val attempts = mutableMapOf<Pair<Long, Long>, Long>()
    private val pending = mutableSetOf<Pair<Long, Long>>()
    private var closed = false

    fun request(visit: Visit) {
        val lat = visit.latitude ?: return
        val lon = visit.longitude ?: return
        if (closed || visit.demo || visit.address.isNotBlank() || !lat.isFinite() || !lon.isFinite() ||
            lat !in -90.0..90.0 || lon !in -180.0..180.0 || !Geocoder.isPresent()) return
        val key = visit.id to visit.start
        val now = SystemClock.elapsedRealtime()
        if (pending.size >= 2 || key in pending || attempts[key]?.let { now - it < 600_000 } == true) return
        attempts[key] = now
        pending.add(key)
        val timeout = Runnable { pending.remove(key) }
        handler.postDelayed(timeout, 15_000)
        fun finish(addresses: List<Address>) {
            val address = addresses.firstOrNull()?.getAddressLine(0)?.trim().orEmpty()
            handler.post {
                if (!closed && pending.remove(key)) {
                    handler.removeCallbacks(timeout)
                    if (address.isNotBlank()) resolved(visit, address)
                }
            }
        }
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
        handler.removeCallbacksAndMessages(null)
        executor.shutdownNow()
    }
}
