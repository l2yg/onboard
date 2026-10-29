package dev.onboard.probe

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.listener.SteeringWheelAngleListener
import android.app.Activity
import android.car.Car
import android.car.VehiclePropertyIds
import android.car.hardware.property.CarPropertyManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.widget.TextView

class ProbeActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var vehicle: Vehicle
    private lateinit var output: TextView
    private lateinit var doorProbe: DoorProbe
    private var car: Car? = null
    private var subscribed = false
    @Volatile private var closed = false
    private val angleListener = object : SteeringWheelAngleListener {
        override fun onSteeringWheelAngleUpdated(angle: Float?) {
            report("steering.callback value=$angle receivedMs=${SystemClock.elapsedRealtime()}")
        }

        override fun onFailed(e: Exception) {
            report("steering.callback failed=${e.javaClass.name}: ${e.message}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        output = TextView(this).apply {
            text = "Read-only vehicle signal probe\n"
            textSize = 20f
        }
        setContentView(output)
        vehicle = Vehicle(this)
        doorProbe = DoorProbe(vehicle, ::report)
        report("begin package=$packageName sdk=Vehicle-2.3.2")
        report("permission.CAR_DRIVE=${checkSelfPermission("pleos.car.permission.CAR_DRIVE")}")
        report("permission.CAR_DOORS=${checkSelfPermission("pleos.car.permission.CAR_DOORS")}")
        report("permission.CAR_SPEED=${checkSelfPermission("android.car.permission.CAR_SPEED")}")
        report("permission.CAR_POWERTRAIN=${checkSelfPermission("android.car.permission.CAR_POWERTRAIN")}")
        vehicle.initialize()
        handler.postDelayed({ inspectSteering() }, 2_000)
        handler.postDelayed({ doorProbe.start() }, 2_000)
        handler.postDelayed({ Thread { inspectStandardProperties() }.start() }, 3_000)
        handler.postDelayed({
            report("observation.window.end")
            finish()
        }, 120_000)
    }

    private fun inspectSteering() {
        try {
            vehicle.getSteeringWheel().checkSteeringWheelAngleCapabilities(
                onComplete = capability@ { config, bounds ->
                    if (closed) return@capability
                    report("steering.capability access=${config.accessType} bounds=$bounds")
                    try {
                        vehicle.getSteeringWheel().getCurrentSteeringWheelAngle(
                            onComplete = { report("steering.get value=$it") },
                            onFailed = { report("steering.get failed=${it.javaClass.name}: ${it.message}") }
                        )
                    } catch (e: Exception) {
                        report("steering.get failed=${e.javaClass.name}: ${e.message}")
                    }
                    try {
                        vehicle.getSteeringWheel().registerSteeringWheelAngle(angleListener)
                        subscribed = true
                        report("steering.register returned; callback still unproven")
                    } catch (e: Exception) {
                        report("steering.register failed=${e.javaClass.name}: ${e.message}")
                    }
                },
                onFailed = { report("steering.capability failed=${it.javaClass.name}: ${it.message}") }
            )
        } catch (e: Exception) {
            report("steering.call failed=${e.javaClass.name}: ${e.message}")
        }
    }

    private fun inspectStandardProperties() {
        if (closed) return
        try {
            car = Car.createCar(this)
            if (closed) {
                car?.disconnect()
                return
            }
            val manager = car?.getCarManager(Car.PROPERTY_SERVICE) as? CarPropertyManager
            if (manager == null) {
                report("aaos.manager unavailable")
                return
            }
            for (property in intArrayOf(VehiclePropertyIds.PERF_VEHICLE_SPEED, VehiclePropertyIds.GEAR_SELECTION)) {
                try {
                    val config = manager.getCarPropertyConfig(property)
                    report("aaos.config property=$property present=${config != null}")
                    if (property == VehiclePropertyIds.PERF_VEHICLE_SPEED) {
                        val value = manager.getProperty(Float::class.javaObjectType, property, 0)
                        report("aaos.speed value=${value?.value} status=${value?.status} timestampNs=${value?.timestamp}")
                    } else {
                        val value = manager.getProperty(Int::class.javaObjectType, property, 0)
                        report("aaos.gear value=${value?.value} status=${value?.status} timestampNs=${value?.timestamp}")
                    }
                } catch (e: Exception) {
                    report("aaos.property=$property failed=${e.javaClass.name}: ${e.message}")
                }
            }
        } catch (e: LinkageError) {
            report("aaos.library unavailable=${e.javaClass.name}: ${e.message}")
        } catch (e: Exception) {
            report("aaos.connect failed=${e.javaClass.name}: ${e.message}")
        }
    }

    private fun report(message: String) {
        Log.i("OnboardRuntimeProbe", message)
        handler.post { if (!closed) output.append("$message\n") }
    }

    override fun onDestroy() {
        closed = true
        handler.removeCallbacksAndMessages(null)
        doorProbe.close()
        if (subscribed) {
            try {
                vehicle.getSteeringWheel().unregisterSteeringWheelAngle(angleListener)
            } catch (e: Exception) {
                Log.w("OnboardRuntimeProbe", "steering.unregister failed", e)
            }
        }
        vehicle.release()
        try {
            car?.disconnect()
        } catch (e: LinkageError) {
            Log.i("OnboardRuntimeProbe", "aaos.library unavailable during cleanup")
        }
        super.onDestroy()
    }
}
