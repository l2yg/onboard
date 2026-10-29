package dev.onboard.app

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock

class RecordingService : Service(), LocationListener {
    companion object {
        const val STOP = "dev.onboard.app.STOP"
        const val DELETE = "dev.onboard.app.DELETE"
        @Volatile var running = false
            private set
        @Volatile var doorStatus = "기록 중지"
            private set
        @Volatile var locationStatus = "위치 대기"
            private set
        @Volatile var navigationStatus = "내비 대기"
            private set
        @Volatile var odometerStatus = "거리계 대기"
            private set
        var lastMotionAt = 0L
            private set
    }
    private lateinit var store: HistoryStore
    private var session = 0L
    private var visit: Long? = null
    private var motion: MotionMonitor? = null
    private var doors: DoorCollector? = null
    private var navigation: NavigationLink? = null
    private var odometer: OdometerCollector? = null
    private var coverage = CoverageTracker()
    private var locationManager: LocationManager? = null
    private var latestLocation: Location? = null
    private var parked: Boolean? = null
    private var previousDoorStatus = ""
    private val handler = Handler(Looper.getMainLooper())
    private val watchdog = object : Runnable {
        override fun run() {
            if (!running) return
            if (SystemClock.elapsedRealtime() - lastMotionAt > 6_000) updateMotion(null)
            refreshLocationStatus()
            handler.postDelayed(this, 3_000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        store = HistoryStore(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == STOP || intent?.action == DELETE) {
            stopRecording()
            if (intent.action == DELETE) store.deleteAll()
            stopSelf()
            return START_NOT_STICKY
        }
        if (running) return START_NOT_STICKY
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            doorStatus = "위치 권한을 허용한 뒤 기록을 시작해 주세요"
            stopSelf()
            return START_NOT_STICKY
        }
        try {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(NotificationChannel("recording", "차량 이력 기록", NotificationManager.IMPORTANCE_LOW))
            val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            val stop = PendingIntent.getService(this, 1, Intent(this, RecordingService::class.java).setAction(STOP), PendingIntent.FLAG_IMMUTABLE)
            startForeground(11, Notification.Builder(this, "recording")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation).setContentTitle("${getString(R.string.app_name)} · 방문 기록 중")
                .setContentText("이 기기에만 저장 · 눌러서 기록 상태 확인").setContentIntent(open)
                .addAction(Notification.Action.Builder(null, "기록 중지", stop).build()).setOngoing(true).build())
            session = store.startSession()
            running = true
            doorStatus = "문 서비스 연결 대기"
            lastMotionAt = SystemClock.elapsedRealtime()
            motion = MotionMonitor(this, ::updateMotion).also { it.start() }
            doors = DoorCollector(this, ::recordDoor, ::updateDoorStatus).also { it.start() }
            coverage = CoverageTracker()
            navigation = NavigationLink(this, { navigationStatus = it }) { kind, name, address ->
                if (running) store.navigation(session, visit, kind, name, address)
            }.also { it.start() }
            odometer = OdometerCollector(this, { km, elapsed, wall ->
                if (running) coverage.sample(km, elapsed, wall, usableLocation()?.elapsedRealtimeNanos?.div(1_000_000))
                    ?.let { store.gap(session, it) }
            }, { odometerStatus = it }).also { it.start() }
            subscribeLocations()
            handler.post(watchdog)
        } catch (e: Exception) {
            doorStatus = "기록 시작 실패: ${e.javaClass.simpleName}"
            stopRecording()
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun updateMotion(value: Boolean?) {
        lastMotionAt = SystemClock.elapsedRealtime()
        if (parked != value) {
            visit?.let { store.endVisit(it, if (value == null) "정차 신호 확인 불가" else "정차 종료") }
            visit = null
            parked = value
        }
        if (value == true && visit == null) {
            visit = store.beginVisit(session)
            navigation?.refresh()
        }
        visit?.let { id -> usableLocation()?.let { store.location(id, it.latitude, it.longitude, it.accuracy) } }
    }

    private fun recordDoor(area: String, value: Int?, kind: String, elapsed: Long, wall: Long) {
        if (!running) return
        val currentVisit = if (parked == true && SystemClock.elapsedRealtime() - lastMotionAt <= 6_000) visit else null
        store.event(session, currentVisit, area, value, kind, "원본 시각 미제공 · 앱 수신 시각 기준", elapsed, wall)
    }

    private fun updateDoorStatus(message: String) {
        doorStatus = message
        if (message != previousDoorStatus) {
            store.event(session, null, "수집 상태", null, "status", message, SystemClock.elapsedRealtime())
            previousDoorStatus = message
        }
    }

    private fun subscribeLocations() {
        locationManager = getSystemService(LocationManager::class.java)
        var subscribed = false
        locationManager?.getProviders(true)?.filter { it == LocationManager.GPS_PROVIDER || it == LocationManager.NETWORK_PROVIDER }?.forEach { provider ->
            try {
                locationManager?.requestLocationUpdates(provider, 5_000, 0f, this, Looper.getMainLooper())
                subscribed = true
            } catch (_: SecurityException) { locationStatus = "위치 권한 확인 필요" }
              catch (_: IllegalArgumentException) { locationStatus = "위치 공급자 사용 불가" }
        }
        if (!subscribed) locationStatus = "사용 가능한 위치 공급자 없음"
    }

    private fun usableLocation(): Location? = latestLocation?.takeIf {
        it.hasAccuracy() && EvidencePolicy.locationUsable(SystemClock.elapsedRealtime(), it.elapsedRealtimeNanos / 1_000_000, it.accuracy)
    }

    private fun refreshLocationStatus() {
        val location = usableLocation()
        locationStatus = when {
            location != null -> "위치 수신 · 정확도 ±${location.accuracy.toInt()}m"
            latestLocation != null -> "위치 오래됨 또는 정확도 부족 · 장소 미확정"
            else -> "위치 수신 대기 · 장소 미확정"
        }
    }

    override fun onLocationChanged(location: Location) {
        if (!running) return
        latestLocation = location
        store.locationSample(session, if (parked == true) visit else null, location.time,
            location.elapsedRealtimeNanos / 1_000_000, location.latitude, location.longitude,
            if (location.hasAccuracy()) location.accuracy else null, location.provider ?: "unknown")
        refreshLocationStatus()
        if (parked == true) visit?.let { id -> usableLocation()?.let { store.location(id, it.latitude, it.longitude, it.accuracy) } }
    }
    override fun onProviderDisabled(provider: String) { latestLocation = null; locationStatus = "위치 공급자 꺼짐" }
    override fun onProviderEnabled(provider: String) { locationStatus = "위치 재수신 대기" }
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit

    private fun stopRecording() {
        running = false
        visit?.let { store.endVisit(it, "기록 중지") }; visit = null
        if (session != 0L) store.endSession(session, "기록 중지")
        session = 0
        handler.removeCallbacksAndMessages(null)
        motion?.close(); motion = null
        doors?.close(); doors = null
        navigation?.close(); navigation = null
        odometer?.close(); odometer = null
        locationManager?.removeUpdates(this)
        parked = null
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    override fun onDestroy() { stopRecording(); store.close(); super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null
}
