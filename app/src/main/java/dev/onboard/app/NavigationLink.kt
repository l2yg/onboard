package dev.onboard.app

import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import ai.pleos.playground.navi.data.DestinationInfo
import ai.pleos.playground.navi.data.DestinationArrivedInfo
import ai.pleos.playground.navi.data.WaypointArrivedInfo
import ai.pleos.playground.navi.data.RouteInfo
import ai.pleos.playground.navi.data.NavigationScreenRequest
import ai.pleos.playground.navi.data.CurrentLocationInfo
import ai.pleos.playground.navi.data.RouteStateInfo
import ai.pleos.playground.navi.constants.NaviErrorCode
import ai.pleos.playground.navi.constants.NaviMode
import android.content.Context
import android.location.Location
import android.os.Handler
import android.os.Looper
import android.util.Log

class NavigationLink(context: Context, private val status: (String) -> Unit,
                     private val location: (Double, Double, String) -> Unit = { _, _, _ -> },
                     private val event: (String, String, String) -> Unit = { _, _, _ -> }) {
    private val helper = NaviHelper(context.applicationContext)
    private val handler = Handler(Looper.getMainLooper())
    private var closed = false
    private var connected = false
    private var pending: (() -> Unit)? = null
    private var requestedDestination: Pair<Double, Double>? = null
    private var destinationMatches = false
    private var routeActive = false
    private var reportedRoute = false
    private var remainingRouteChecks = 0
    private val routeCheck = object : Runnable {
        override fun run() {
            if (closed || requestedDestination == null || reportedRoute) return
            if (remainingRouteChecks-- <= 0) {
                status("내비 응답 확인 지연 · 내비 화면에서 안내 상태를 확인해 주세요")
                return
            }
            dispatch { helper.getRouteStateInfo(); helper.getDestinationInfo() }
            handler.postDelayed(this, 2_000)
        }
    }
    private fun reportRoute() {
        if (routeActive && destinationMatches && !reportedRoute) {
            reportedRoute = true
            status("내비 경로 탐색 완료 · 내비에서 경로를 확인해 주세요")
        }
    }
    private val listener = object : NaviHelperEventListener {
        override fun onCurrentLocationInfo(info: CurrentLocationInfo) = dispatch {
            if (info.latitude.isFinite() && info.longitude.isFinite() &&
                info.latitude in -90.0..90.0 && info.longitude in -180.0..180.0) {
                location(info.latitude, info.longitude, info.address)
            }
        }
        override fun onError(errorCode: NaviErrorCode) = dispatch {
            requestedDestination = null
            status(when (errorCode) {
                NaviErrorCode.CAUSE_SEARCH_ROUTE_TOO_SHORT_DISTANCE -> "목적지가 너무 가까워 경로를 만들지 못했어요"
                NaviErrorCode.CAUSE_SEARCH_ROUTE_OVER_DISTANCE -> "내비 지원 거리를 벗어나 경로를 만들지 못했어요"
                else -> "내비 경로 탐색 실패 · 현재 위치와 내비 연결 상태를 확인해 주세요"
            })
        }
        override fun onRoutingCompleted() = dispatch { helper.getRouteStateInfo(); helper.getDestinationInfo() }
        override fun onRouteStateInfo(routeStateInfo: RouteStateInfo) = dispatch {
            Log.d("OnboardNavigation", "route mode=${routeStateInfo.naviMode}, routing=${routeStateInfo.isRouting}, requested=${requestedDestination != null}")
            routeActive = routeStateInfo.naviMode == NaviMode.ROUTE_GUIDING
            if (routeActive && requestedDestination != null && !reportedRoute) helper.getDestinationInfo()
            reportRoute()
        }
        override fun onDestinationInfo(info: DestinationInfo) = dispatch {
            event("내비 목적지 설정", info.name, info.address)
            requestedDestination?.let { target ->
                val distance = FloatArray(1)
                Location.distanceBetween(target.first, target.second,
                    info.displayLatitude ?: info.latitude, info.displayLongitude ?: info.longitude, distance)
                destinationMatches = distance[0] <= 100f
                Log.d("OnboardNavigation", "destination matches=$destinationMatches, active=$routeActive")
                reportRoute()
            }
        }
        override fun onDestinationArrived(info: DestinationArrivedInfo) = dispatch {
            event("내비 목적지 도착 알림", info.name, info.address)
        }
        override fun onWayPointArrived(info: WaypointArrivedInfo) = dispatch {
            event("내비 경유지 도착 알림", info.name, info.address)
        }
        override fun onDestinationChanged() = dispatch { event("내비 목적지 변경", "", ""); helper.getDestinationInfo() }
        override fun onRouteCancelled() = dispatch { event("내비 안내 취소", "", "") }
    }
    private fun dispatch(action: () -> Unit) { handler.post {
        if (!closed) try { action() } catch (error: Exception) { status("내비 응답 확인 필요: ${error.javaClass.simpleName}") }
    } }
    fun refresh() { if (connected && !closed) dispatch { helper.getDestinationInfo(); helper.getCurrentLocationInfo() } }
    fun refreshLocation() { if (connected && !closed) dispatch { helper.getCurrentLocationInfo() } }
    fun start() {
        try {
            helper.initialize(onConnected = { dispatch {
                connected = true
                helper.addListener(listener)
                status("내비 연결됨 · 도착 알림 대기")
                helper.getDestinationInfo()
                helper.getCurrentLocationInfo()
                pending?.invoke(); pending = null
            } }, onFailed = { error -> dispatch { pending = null; status("내비 연결 확인 필요: ${error.javaClass.simpleName}") } })
        } catch (error: Exception) { status("내비 사용 불가: ${error.javaClass.simpleName}") }
        handler.postDelayed({ if (!closed && !connected) { pending = null; status("내비 연결 지연 · 내비 앱 상태를 확인해 주세요") } }, 15_000)
    }
    fun navigate(latitude: Double, longitude: Double, name: String, address: String = "", permitted: () -> Boolean) {
        val request = {
            if (!permitted()) status("정차 상태가 바뀌어 길 안내를 요청하지 않았습니다")
            else try {
                requestedDestination = latitude to longitude
                destinationMatches = false; routeActive = false; reportedRoute = false
                helper.requestRoute(RouteInfo(latitude = latitude, longitude = longitude, poiName = name,
                    displayLatitude = latitude, displayLongitude = longitude,
                    poiId = "", address = address, poiSubId = ""))
                helper.requestNavigationScreen(NavigationScreenRequest.Foreground)
                handler.removeCallbacks(routeCheck)
                remainingRouteChecks = 15
                handler.postDelayed(routeCheck, 1_000)
                status("길 안내 요청을 보냈습니다. 내비에서 결과를 확인해 주세요.")
            } catch (error: Exception) { status("길 안내 요청 실패: ${error.javaClass.simpleName}") }
        }
        if (connected) request() else { pending = request; status("내비 연결 후 길 안내를 요청합니다") }
    }
    fun close() {
        closed = true; pending = null; handler.removeCallbacksAndMessages(null)
        try { helper.removeListener(listener); helper.release() } catch (_: Exception) { }
    }
}
