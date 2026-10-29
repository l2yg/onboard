package dev.onboard.app

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.DoorArea
import ai.pleos.playground.vehicle.api.model.capability.AccessType
import ai.pleos.playground.vehicle.listener.DoorPositionListener
import android.car.Car
import android.car.VehiclePropertyIds
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.SystemClock

class MotionMonitor(context: Context, private val result: (Boolean?) -> Unit) {
    private val context = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private val thread = HandlerThread("OnboardMotion").apply { start() }
    private val worker = Handler(thread.looper)
    private var car: Car? = null
    @Volatile private var closed = false
    private val poll = object : Runnable {
        override fun run() {
            val parked = try {
                val connectedCar = car ?: Car.createCar(context).also { car = it }
                val manager = connectedCar?.getCarManager(Car.PROPERTY_SERVICE) as? CarPropertyManager
                val speed = manager?.getProperty(Float::class.javaObjectType, VehiclePropertyIds.PERF_VEHICLE_SPEED, 0)
                val gear = manager?.getProperty(Int::class.javaObjectType, VehiclePropertyIds.GEAR_SELECTION, 0)
                EvidencePolicy.parked(if (speed?.status == 0) speed.value else null, if (gear?.status == 0) gear.value else null)
            } catch (_: Exception) {
                try { car?.disconnect() } catch (_: Exception) { }
                car = null
                null
            } catch (_: LinkageError) { null }
            if (!closed) {
                main.post { if (!closed) result(parked) }
                worker.postDelayed(this, 2_000)
            }
        }
    }

    fun start() { worker.post(poll) }
    fun close() {
        closed = true
        worker.removeCallbacksAndMessages(null)
        worker.post {
            try { car?.disconnect() } catch (_: Exception) { } catch (_: LinkageError) { }
            thread.quitSafely()
        }
    }
}

class DoorCollector(context: Context, private val record: (String, Int?, String, Long, Long) -> Unit,
                    private val status: (String) -> Unit) {
    private val vehicle = Vehicle(context.applicationContext)
    private val main = Handler(Looper.getMainLooper())
    private val listeners = mutableMapOf<DoorArea, DoorPositionListener>()
    private val values = mutableMapOf<DoorArea, Int?>()
    private var closed = false
    private var pending = false
    private var requestAt = 0L
    private var generation = 0
    private val retry = object : Runnable {
        override fun run() {
            if (closed) return
            if (pending && SystemClock.elapsedRealtime() - requestAt > 8_000) {
                fail(IllegalStateException("문 서비스 응답 지연 · 관측 연속성 불명"))
            }
            if (!pending || SystemClock.elapsedRealtime() - requestAt > 8_000) check()
            main.postDelayed(this, 10_000)
        }
    }

    fun start() {
        try {
            vehicle.initialize()
            main.postDelayed(retry, 1_500)
        } catch (e: Exception) { status("문 서비스 초기화 실패: ${e.javaClass.simpleName}") }
    }

    private fun check() {
        pending = true
        requestAt = SystemClock.elapsedRealtime()
        val request = ++generation
        try {
            vehicle.getDoor().checkDoorPositionCapabilities(
                onComplete = { config, bounds -> main.post {
                    if (!closed && request == generation) {
                        pending = false
                        if (config.accessType != AccessType.READ && config.accessType != AccessType.READ_WRITE) {
                            clearListeners()
                            status("문 상태 읽기 미지원")
                        } else if (bounds.isEmpty()) {
                            clearListeners()
                            status("지원되는 문 영역 없음")
                        } else {
                            listeners.keys.filter { it !in bounds }.forEach { area ->
                                listeners.remove(area)?.let { listener ->
                                    try { vehicle.getDoor().unregisterDoorPosition(area, listener) } catch (_: Exception) { }
                                }
                                values.remove(area)
                            }
                            bounds.forEach { (area, range) ->
                                if (range.minValue <= range.maxValue) {
                                    if (area !in listeners) subscribe(area, range.minValue, range.maxValue)
                                    snapshot(area, request)
                                }
                            }
                        }
                    }
                } },
                onFailed = { e -> main.post { if (!closed && request == generation) fail(e) } }
            )
        } catch (e: Exception) { fail(e) }
    }

    private fun snapshot(area: DoorArea, request: Int) {
        val name = area.values.joinToString(" + ") { it.name }
        try {
            vehicle.getDoor().getCurrentDoorPosition(area,
                onComplete = { value ->
                    val elapsed = SystemClock.elapsedRealtime()
                    val wall = System.currentTimeMillis()
                    main.post {
                        if (!closed && request == generation) {
                            record(name, value, "snapshot", elapsed, wall)
                            status("문 상태 조회 수신 · 변경 알림은 별도 확인")
                        }
                    }
                },
                onFailed = { e -> main.post { if (!closed && request == generation) fail(e) } })
        } catch (e: Exception) { fail(e) }
    }

    private fun subscribe(area: DoorArea, minimum: Int, maximum: Int) {
        val name = area.values.joinToString(" + ") { it.name }
        val listener = object : DoorPositionListener {
            override fun onPositionUpdated(pos: Int?) {
                val elapsed = SystemClock.elapsedRealtime()
                val wall = System.currentTimeMillis()
                main.post {
                    if (!closed && listeners[area] === this) {
                        val kind = EvidencePolicy.transition(values[area], pos, minimum, maximum)
                        values[area] = if (pos != null && pos in minimum..maximum) pos else null
                        record(name, pos, kind, elapsed, wall)
                        status(if (kind == "unavailable") "문 상태 불명" else "문 변경 알림 수신")
                    }
                }
            }
            override fun onFailed(e: Exception) { main.post { if (!closed && listeners[area] === this) fail(e) } }
        }
        listeners[area] = listener
        try {
            vehicle.getDoor().registerDoorPosition(area, listener)
            status("문 알림 구독 요청 · 수신 대기")
        } catch (e: Exception) { fail(e) }
    }

    private fun fail(error: Exception) {
        generation++
        pending = false
        clearListeners()
        status("문 수집 확인 필요: ${error.javaClass.simpleName} · 재시도 중")
    }

    private fun clearListeners() {
        val old = listeners.toMap()
        listeners.clear()
        values.clear()
        old.forEach { (area, listener) ->
            try { vehicle.getDoor().unregisterDoorPosition(area, listener) } catch (_: Exception) { }
        }
    }

    fun close() {
        closed = true
        main.removeCallbacksAndMessages(null)
        clearListeners()
        try { vehicle.release() } catch (_: Exception) { }
    }
}
