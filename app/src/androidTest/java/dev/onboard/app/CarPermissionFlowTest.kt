package dev.onboard.app

import android.content.pm.PackageManager
import android.accessibilityservice.AccessibilityServiceInfo
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CarPermissionFlowTest {
    @Test fun deniedVehiclePermissionCanBeRequestedFromHome() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        instrumentation.uiAutomation.serviceInfo = instrumentation.uiAutomation.serviceInfo.apply {
            flags = flags or AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        }
        assumeTrue("이미 승인된 기기는 권한 요청 화면 검증 제외", context.checkSelfPermission("android.car.permission.CAR_SPEED") != PackageManager.PERMISSION_GRANTED)
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withText("차량 상태 권한 허용")).perform(scrollTo(), click())
            val until = SystemClock.elapsedRealtime() + 8_000
            var allow: AccessibilityNodeInfo? = null
            while (allow == null && context.checkSelfPermission("android.car.permission.CAR_SPEED") != PackageManager.PERMISSION_GRANTED && SystemClock.elapsedRealtime() < until) {
                allow = findAllow(instrumentation.uiAutomation.rootInActiveWindow)
                if (allow == null) SystemClock.sleep(150)
            }
            // Automotive permission controllers may grant a previously approved group immediately.
            if (allow != null) assertTrue(allow.performAction(AccessibilityNodeInfo.ACTION_CLICK))
            while (context.checkSelfPermission("android.car.permission.CAR_SPEED") != PackageManager.PERMISSION_GRANTED && SystemClock.elapsedRealtime() < until) SystemClock.sleep(100)
            assertEquals("권한 요청 실패: ${describe(instrumentation.uiAutomation.rootInActiveWindow)}", PackageManager.PERMISSION_GRANTED, context.checkSelfPermission("android.car.permission.CAR_SPEED"))
        }
    }

    private fun findAllow(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.viewIdResourceName?.endsWith(":id/permission_allow_button") == true) return node
        for (index in 0 until node.childCount) findAllow(node.getChild(index))?.let { return it }
        return null
    }

    private fun describe(node: AccessibilityNodeInfo?): String {
        if (node == null) return "null"
        return "${node.viewIdResourceName}:${node.text} " + (0 until node.childCount).joinToString { describe(node.getChild(it)) }
    }
}
