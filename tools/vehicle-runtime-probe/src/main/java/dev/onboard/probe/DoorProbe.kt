package dev.onboard.probe

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.DoorArea
import ai.pleos.playground.vehicle.api.model.capability.AccessType
import ai.pleos.playground.vehicle.listener.DoorPositionListener
import android.os.Handler
import android.os.Looper
import android.os.SystemClock

/** Reads raw door states; neither initial values nor callbacks prove object movement. */
class DoorProbe(
    private val vehicle: Vehicle,
    private val report: (String) -> Unit
) {
    private val handler = Handler(Looper.getMainLooper())
    private val listeners = mutableMapOf<DoorArea, DoorPositionListener>()
    private var closed = false

    fun start() {
        try {
            vehicle.getDoor().checkDoorPositionCapabilities(
                onComplete = { config, bounds ->
                    handler.post {
                        if (!closed) {
                            report("door.capability access=${config.accessType} areas=${bounds.size}")
                            if (config.accessType == AccessType.READ || config.accessType == AccessType.READ_WRITE) {
                                bounds.forEach { (area, range) ->
                                    if (range.minValue <= range.maxValue) {
                                        observe(area, range.minValue, range.maxValue)
                                    } else {
                                        report("door.capability invalidBounds area=${area.values.contentToString()} range=$range")
                                    }
                                }
                            } else {
                                report("door.read unavailable; no subscription requested")
                            }
                        }
                    }
                },
                onFailed = { failure("capability", it) }
            )
        } catch (e: Exception) {
            failure("capability.call", e)
        }
    }

    private fun observe(area: DoorArea, minimum: Int, maximum: Int) {
        val name = area.values.contentToString()
        report("door.area name=$name min=$minimum max=$maximum")
        // Snapshot requests and callbacks may race. Never use a snapshot as a transition baseline.
        try {
            vehicle.getDoor().getCurrentDoorPosition(
                area,
                onComplete = { value ->
                    val receivedMs = SystemClock.elapsedRealtime()
                    handler.post {
                        if (!closed) report("door.snapshot area=$name value=$value receivedMs=$receivedMs")
                    }
                },
                onFailed = { failure("snapshot area=$name", it) }
            )
        } catch (e: Exception) {
            failure("snapshot.call area=$name", e)
        }

        var previous: Int? = null
        val listener = object : DoorPositionListener {
            override fun onPositionUpdated(pos: Int?) {
                val receivedMs = SystemClock.elapsedRealtime()
                val receivedWallMs = System.currentTimeMillis()
                handler.post {
                    if (!closed) {
                        val kind = when {
                            pos == null || pos !in minimum..maximum -> "unavailable"
                            previous == null -> "baseline"
                            previous == pos -> "repeat"
                            else -> "transition"
                        }
                        report("door.callback area=$name value=$pos previous=$previous kind=$kind receivedMs=$receivedMs receivedWallMs=$receivedWallMs sourceTime=unknown")
                        previous = if (pos != null && pos in minimum..maximum) pos else null
                    }
                }
            }

            override fun onFailed(e: Exception) {
                handler.post {
                    if (!closed) {
                        previous = null
                        report("door.subscription failed area=$name type=${e.javaClass.name} message=${e.message}; continuity=unknown")
                    }
                }
            }
        }
        // Keep the same listener and area for cleanup even if registration throws after partial work.
        listeners[area] = listener
        try {
            vehicle.getDoor().registerDoorPosition(area, listener)
            report("door.register returned area=$name; delivery=unproven")
        } catch (e: Exception) {
            failure("register area=$name", e)
        }
    }

    private fun failure(operation: String, error: Exception) {
        handler.post {
            if (!closed) report("door.$operation failed type=${error.javaClass.name} message=${error.message}")
        }
    }

    fun close() {
        closed = true
        handler.removeCallbacksAndMessages(null)
        listeners.forEach { (area, listener) ->
            try {
                vehicle.getDoor().unregisterDoorPosition(area, listener)
            } catch (e: Exception) {
                report("door.unregister failed area=${area.values.contentToString()} type=${e.javaClass.name} message=${e.message}")
            }
        }
        listeners.clear()
    }
}
