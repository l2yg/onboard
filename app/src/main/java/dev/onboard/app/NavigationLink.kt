package dev.onboard.app

import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import ai.pleos.playground.navi.data.DestinationInfo
import ai.pleos.playground.navi.data.DestinationArrivedInfo
import ai.pleos.playground.navi.data.WaypointArrivedInfo
import ai.pleos.playground.navi.data.RouteInfo
import ai.pleos.playground.navi.data.NavigationScreenRequest
import android.content.Context
import android.os.Handler
import android.os.Looper

class NavigationLink(context: Context, private val status: (String) -> Unit,
                     private val event: (String, String, String) -> Unit = { _, _, _ -> }) {
    private val helper = NaviHelper(context.applicationContext)
    private val handler = Handler(Looper.getMainLooper())
    private var closed = false
    private var connected = false
    private var pending: (() -> Unit)? = null
    private val listener = object : NaviHelperEventListener {
        override fun onDestinationInfo(info: DestinationInfo) = dispatch {
            event("내비 목적지 설정", info.name, info.address)
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
    fun refresh() { if (connected && !closed) dispatch { helper.getDestinationInfo() } }
    fun start() {
        try {
            helper.initialize(onConnected = { dispatch {
                connected = true
                helper.addListener(listener)
                status("내비 연결됨 · 도착 알림 대기")
                helper.getDestinationInfo()
                pending?.invoke(); pending = null
            } }, onFailed = { error -> dispatch { pending = null; status("내비 연결 확인 필요: ${error.javaClass.simpleName}") } })
        } catch (error: Exception) { status("내비 사용 불가: ${error.javaClass.simpleName}") }
        handler.postDelayed({ if (!closed && !connected) { pending = null; status("내비 연결 지연 · 내비 앱 상태를 확인해 주세요") } }, 15_000)
    }
    fun navigate(latitude: Double, longitude: Double, name: String, permitted: () -> Boolean) {
        val request = {
            if (!permitted()) status("정차 상태가 바뀌어 길 안내를 요청하지 않았습니다")
            else try {
                helper.requestRoute(RouteInfo(latitude = latitude, longitude = longitude, poiName = name,
                    poiId = "", address = "", poiSubId = ""))
                helper.requestNavigationScreen(NavigationScreenRequest.Foreground)
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
