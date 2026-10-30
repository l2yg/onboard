package dev.onboard.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PleosRouteTest {
    @Test fun navigationCompletesRouteForKoreanTestDestination() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val result = AtomicReference("내비 결과 응답 없음")
        val completed = CountDownLatch(1)
        lateinit var link: NavigationLink
        instrumentation.runOnMainSync {
            link = NavigationLink(instrumentation.targetContext, { message ->
                if (message.startsWith("내비 경로 탐색 완료") || message.contains("실패") ||
                    message.contains("못했어요") || message.contains("사용 불가")) {
                    result.set(message)
                    completed.countDown()
                }
            })
            // Public Korean coordinate used only for this runtime route test; no visit is inserted.
            link.navigate(37.404, 127.112, "경로 연결 시험") { true }
            link.start()
        }
        try {
            assertTrue("경로 결과 30초 초과", completed.await(30, TimeUnit.SECONDS))
            assertTrue(result.get(), result.get().startsWith("내비 경로 탐색 완료"))
        } finally { instrumentation.runOnMainSync { link.close() } }
    }
}
