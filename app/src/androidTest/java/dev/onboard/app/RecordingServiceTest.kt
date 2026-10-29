package dev.onboard.app

import android.Manifest
import android.content.Intent
import android.os.SystemClock
import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecordingServiceTest {
    @Test fun realDoorSnapshotsPersistWhileActivityIsBackgrounded() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        instrumentation.uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.ACCESS_FINE_LOCATION)
        instrumentation.uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.ACCESS_COARSE_LOCATION)
        instrumentation.uiAutomation.grantRuntimePermission(context.packageName, "android.car.permission.CAR_SPEED")
        context.getSharedPreferences("MainActivity", 0).edit().putBoolean("demo", false).commit()
        HistoryStore(context).use { store ->
            store.deleteAll()
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                scenario.onActivity { it.startForegroundService(Intent(it, RecordingService::class.java)) }
                await("기록 서비스가 시작되지 않음") { RecordingService.running }
                await("실제 문 SDK 응답 없음: ${RecordingService.doorStatus}") { count(store, "events", "kind='snapshot' AND value IS NOT NULL") > 0 }
                val before = count(store, "events", "kind='snapshot' AND value IS NOT NULL")
                scenario.moveToState(Lifecycle.State.CREATED)
                await("앱을 벗어난 뒤 문 조회 기록이 증가하지 않음") { count(store, "events", "kind='snapshot' AND value IS NOT NULL") > before }
                assertTrue(RecordingService.running)
                assertTrue("P 기어 정차 방문 기록 없음", store.visits(false).isNotEmpty())
                await("위치 공급자로부터 실제 측정값을 받지 못함") { count(store, "locations", "accuracy <= 100") > 0 }
                context.startService(Intent(context, RecordingService::class.java).setAction(RecordingService.STOP))
                await("기록 중지 실패") { !RecordingService.running }
                assertTrue(store.visits(false).all { it.end != null })
                scenario.moveToState(Lifecycle.State.RESUMED)
                SystemClock.sleep(3_000)
                instrumentation.uiAutomation.takeScreenshot()?.let { screenshot ->
                    FileOutputStream(File(context.filesDir, "onboard-live.png")).use { screenshot.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    screenshot.recycle()
                }
            }
        }
    }

    private fun count(store: HistoryStore, table: String, condition: String): Long =
        store.readableDatabase.rawQuery("SELECT COUNT(*) FROM $table WHERE $condition", null).use { it.moveToFirst(); it.getLong(0) }

    private fun await(message: String, condition: () -> Boolean) {
        val until = SystemClock.elapsedRealtime() + 25_000
        while (!condition() && SystemClock.elapsedRealtime() < until) SystemClock.sleep(200)
        assertTrue(message, condition())
    }
}
