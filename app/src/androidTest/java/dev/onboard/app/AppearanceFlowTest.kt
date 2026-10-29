package dev.onboard.app

import android.content.res.Configuration
import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.FileOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppearanceFlowTest {
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
