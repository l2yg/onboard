package dev.onboard.app

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.capability.AccessType
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock

class OdometerCollector(context: Context, private val value: (Float?, Long, Long) -> Unit,
                        private val status: (String) -> Unit) {
    private val vehicle = Vehicle(context.applicationContext)
    private val handler = Handler(Looper.getMainLooper())
    private var closed = false
    private var generation = 0
    private val poll = object : Runnable {
        override fun run() {
            if (closed) return
            val token = ++generation
            try {
                vehicle.getOdometer().checkCurrentDistanceDrivenCapabilities(
                    onComplete = { config -> handler.post {
                        if (!closed && token == generation) {
                            if (config.accessType == AccessType.READ || config.accessType == AccessType.READ_WRITE) read(token)
                            else unavailable("차량 거리 읽기 미지원")
                        }
                    } },
                    onFailed = { error -> handler.post { if (!closed && token == generation) unavailable("거리계 확인 필요: ${error.javaClass.simpleName}") } }
                )
            } catch (error: Exception) { unavailable("거리계 연결 확인 필요: ${error.javaClass.simpleName}") }
            handler.postDelayed(this, 5_000)
        }
    }

    fun start() {
        try { vehicle.initialize(); handler.postDelayed(poll, 1_500) }
        catch (error: Exception) { unavailable("거리계 초기화 실패: ${error.javaClass.simpleName}") }
    }

    private fun read(token: Int) {
        try {
            vehicle.getOdometer().getCurrentDistanceDriven(
                onComplete = { km ->
                    val elapsed = SystemClock.elapsedRealtime()
                    val wall = System.currentTimeMillis()
                    handler.post {
                        if (!closed && token == generation) {
                            value(km, elapsed, wall)
                            status(if (km != null && km.isFinite() && km >= 0) "차량 거리계 수신" else "차량 거리계 값 불명")
                        }
                    }
                },
                onFailed = { error -> handler.post { if (!closed && token == generation) unavailable("거리계 수신 확인 필요: ${error.javaClass.simpleName}") } }
            )
        } catch (error: Exception) { unavailable("거리계 수신 확인 필요: ${error.javaClass.simpleName}") }
    }

    private fun unavailable(message: String) {
        if (closed) return
        value(null, SystemClock.elapsedRealtime(), System.currentTimeMillis())
        status(message)
    }

    fun close() {
        closed = true; handler.removeCallbacksAndMessages(null)
        try { vehicle.release() } catch (_: Exception) { }
    }
}
