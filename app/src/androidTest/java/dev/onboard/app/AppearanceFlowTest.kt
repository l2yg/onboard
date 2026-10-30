package dev.onboard.app

import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.EditText
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withHint
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppearanceFlowTest {
    @Test fun memoInputKeepsLargeAppFieldsWhileKeyboardIsOpen() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        ActivityScenario.launch<MainActivity>(context.packageManager.getLaunchIntentForPackage(context.packageName)!!).use { scenario ->
            fun ready(view: View): Boolean = (view is TextView && view.text.toString().contains(" · 방문 ")) ||
                (view is ViewGroup && (0 until view.childCount).any { ready(view.getChildAt(it)) })
            var parked = false
            val until = SystemClock.elapsedRealtime() + 8_000
            while (!parked && SystemClock.elapsedRealtime() < until) {
                scenario.onActivity { parked = ready(it.findViewById(android.R.id.content)) }
                if (!parked) SystemClock.sleep(100)
            }
            assertTrue("정차 확인 필요", parked)
            onView(withText("내 메모")).perform(click())
            onView(withText("새 메모")).perform(scrollTo(), click())
            onView(withHint("예: 카페 방문")).perform(scrollTo(), click(), typeText("Visit memo"))
            onView(withText("메모 남기기")).check(matches(isDisplayed()))
            onView(withHint("기억할 일을 남겨보세요")).perform(scrollTo(), click(), typeText("Remember this stop."))
            onView(withHint("기억할 일을 남겨보세요")).check { view, error ->
                if (error != null) throw error
                val input = view as EditText
                assertTrue("작은 입력 글자", input.textSize / input.resources.displayMetrics.scaledDensity >= 28f)
                assertTrue("전체 화면 편집 방지", input.imeOptions and EditorInfo.IME_FLAG_NO_FULLSCREEN != 0)
            }
            scenario.onActivity {
                assertFalse("키보드 전체 화면 편집기가 열림", it.getSystemService(InputMethodManager::class.java).isFullscreenMode)
            }
            capture("memo-input-large")
        }
    }

    @Test fun datePickerHasLargeTargetsAndSupportsMonthNavigation() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val today = Calendar.getInstance()
        val description = SimpleDateFormat("yyyy년 M월 d일", Locale.KOREA).format(today.time)
        ActivityScenario.launch<MainActivity>(context.packageManager.getLaunchIntentForPackage(context.packageName)!!).use { scenario ->
            fun ready(view: View): Boolean = (view is TextView && view.text.toString().contains(" · 방문 ")) ||
                (view is ViewGroup && (0 until view.childCount).any { ready(view.getChildAt(it)) })
            var parked = false
            val until = SystemClock.elapsedRealtime() + 8_000
            while (!parked && SystemClock.elapsedRealtime() < until) {
                scenario.onActivity { parked = ready(it.findViewById(android.R.id.content)) }
                if (!parked) SystemClock.sleep(100)
            }
            assertTrue("정차 확인 후 날짜 선택이 가능해야 합니다", parked)
            onView(withText("최근 7일 · 날짜 선택")).perform(scrollTo(), click())
            onView(withContentDescription(description)).check { view, error ->
                if (error != null) throw error
                val target = view as Button
                val density = target.resources.displayMetrics.density
                assertTrue("날짜 터치 폭", target.width >= 80 * density)
                assertTrue("날짜 터치 높이", target.height >= 80 * density)
                assertTrue("날짜 글자 크기", target.textSize / target.resources.displayMetrics.scaledDensity >= 24f)
            }
            onView(withText("이전 달")).perform(click())
            val previous = (today.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
            onView(withText(SimpleDateFormat("yyyy년 M월", Locale.KOREA).format(previous.time))).check(matches(isDisplayed()))
            onView(withText("다음 달")).perform(click())
            onView(withContentDescription(description)).perform(click())
            onView(withId(android.R.id.button1)).check { view, error ->
                if (error != null) throw error
                assertTrue("확인 버튼 터치 폭", view.width >= 120 * view.resources.displayMetrics.density)
            }
            capture("date-picker-large")
            onView(withId(android.R.id.button1)).perform(click())
            onView(withText("최근 7일 전체 보기")).check(matches(isDisplayed()))
        }
    }

    @Test fun defaultTextAndButtonsAreLargeEnoughForTheSimplifiedInterface() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            onView(withText("방문 기록")).perform(click())
            scenario.onActivity { activity ->
                assertEquals("차량 발자취", activity.getString(R.string.app_name))
                fun inspect(view: View) {
                    if (view is TextView && view.text.isNotEmpty()) {
                        val sp = view.textSize / activity.resources.displayMetrics.scaledDensity
                        assertTrue("작은 글자: ${view.text}", sp >= 21.9f)
                    }
                    if (view is Button) {
                        assertTrue("작은 버튼 글자: ${view.text}", view.textSize / activity.resources.displayMetrics.scaledDensity >= 23.9f)
                        assertTrue("작은 터치 높이: ${view.text}", view.measuredHeight >= 80 * activity.resources.displayMetrics.density)
                    }
                    if (view is ViewGroup) for (i in 0 until view.childCount) inspect(view.getChildAt(i))
                }
                inspect(activity.findViewById(android.R.id.content))
            }
        }
    }
    @Test fun lightDarkAndSystemSelectionsPersistAcrossRecreation() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val preferences = context.getSharedPreferences("MainActivity", 0)
        val original = preferences.getString("appearance", "system")
        preferences.edit().putString("appearance", "system").apply()
        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                onView(withText("기록 설정")).perform(click())
                onView(withText("시스템 설정 사용")).perform(scrollTo(), click())
                onView(withText("라이트")).perform(click())
                scenario.onActivity {
                    assertEquals(Configuration.UI_MODE_NIGHT_NO, it.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK)
                    assertEquals(0xFFEFF3FA.toInt(), it.getColor(R.color.canvas))
                }
                capture("light-settings")
                onView(withText("방문 기록")).perform(click())
                capture("light-home")
                onView(withText("기록 설정")).perform(click())
                onView(withText("라이트")).perform(scrollTo(), click())
                onView(withText("다크")).perform(click())
                scenario.recreate()
                scenario.onActivity {
                    assertEquals(Configuration.UI_MODE_NIGHT_YES, it.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK)
                    assertEquals(0xFF101217.toInt(), it.getColor(R.color.canvas))
                }
                capture("dark-settings")
                onView(withText("방문 기록")).perform(click())
                capture("dark-home")
                onView(withText("기록 설정")).perform(click())
                onView(withText("다크")).perform(scrollTo(), click())
                onView(withText("시스템 설정 사용")).perform(click())
                scenario.onActivity {
                    assertEquals("system", preferences.getString("appearance", ""))
                    assertEquals(context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK,
                        it.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK)
                }
            }
        } finally { preferences.edit().putString("appearance", original).commit() }
    }

    private fun capture(name: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val screenshot = instrumentation.uiAutomation.takeScreenshot() ?: error("화면 캡처 실패")
        FileOutputStream(File(instrumentation.targetContext.filesDir, "onboard-$name.png")).use {
            screenshot.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        screenshot.recycle()
    }
}
