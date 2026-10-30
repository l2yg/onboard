package dev.onboard.app

import android.content.Intent
import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PleosAddressTest {
    @Test fun recordingPersistsMatchedNavigationAddressWithoutDeletingOldVisits() {
        assumeTrue("사용자가 실행한 수집은 중단하지 않습니다", !RecordingService.running)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        HistoryStore(context).use { store ->
            val before = store.visits(false).map { it.id }.toSet()
            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)!!
            ActivityScenario.launch<MainActivity>(intent).use { scenario ->
                try {
                    scenario.onActivity { it.startForegroundService(Intent(it, RecordingService::class.java)) }
                    val until = SystemClock.elapsedRealtime() + 25_000
                    while (SystemClock.elapsedRealtime() < until && store.visits(false).none {
                        it.id !in before && it.address.isNotBlank()
                    }) SystemClock.sleep(250)
                    assertTrue("수집 중 내비 주소 저장 실패", store.visits(false).any {
                        it.id !in before && it.address.isNotBlank()
                    })
                    assertTrue("기존 방문이 사라짐", store.visits(false).map { it.id }.containsAll(before))
                } finally {
                    context.startService(Intent(context, RecordingService::class.java).setAction(RecordingService.STOP))
                    val until = SystemClock.elapsedRealtime() + 5_000
                    while (RecordingService.running && SystemClock.elapsedRealtime() < until) SystemClock.sleep(100)
                }
            }
        }
    }

    @Test fun nativeNavigationAddressResolvesWithoutAndroidGeocoder() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val fix = AtomicReference<Triple<Double, Double, String>>()
        val received = CountDownLatch(1)
        lateinit var link: NavigationLink
        instrumentation.runOnMainSync {
            link = NavigationLink(context, {}, location = { latitude, longitude, address ->
                if (address.isNotBlank()) {
                    fix.set(Triple(latitude, longitude, address))
                    received.countDown()
                }
            })
            link.start()
        }
        try { assertTrue("Pleos 내비 주소 응답 없음", received.await(20, TimeUnit.SECONDS)) }
        finally { instrumentation.runOnMainSync { link.close() } }

        val current = fix.get()
        val resolved = CountDownLatch(1)
        lateinit var resolver: VisitAddressResolver
        instrumentation.runOnMainSync {
            resolver = VisitAddressResolver(context) { _, address ->
                if (address.isNotBlank()) resolved.countDown()
            }
            resolver.request(Visit(0, System.currentTimeMillis(), null, "", false,
                current.first, current.second, 5f, "시험 입력", 0))
        }
        try { assertTrue("방문 주소 대체 조회 실패", resolved.await(20, TimeUnit.SECONDS)) }
        finally { instrumentation.runOnMainSync { resolver.close() } }
    }
}
