package dev.onboard.app

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withHint
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppFlowTest {
    @Test fun demoSearchCanBeCreatedRestoredEditedAndFinished() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        HistoryStore(context).use { it.deleteAll() }
        context.getSharedPreferences("MainActivity", 0).edit().clear().commit()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            onView(withText("모의 기록으로 사용해 보기")).perform(scrollTo(), click())
            capture("home")
            onView(withText("물건 찾기 시작")).perform(scrollTo(), click())
            onView(withHint("예: 지갑, 안경, 서류")).perform(scrollTo(), replaceText("지갑"))
            closeSoftKeyboard()
            scenario.recreate()
            onView(withText("지갑")).check(matches(isDisplayed()))
            capture("memory")
            onView(withText("방문 기록과 대조하기")).perform(scrollTo(), click())
            capture("results")
            onView(withText("기억 수정")).perform(scrollTo(), click())
            onView(withHint("예: 지갑, 안경, 서류")).perform(scrollTo(), replaceText("안경"))
            closeSoftKeyboard()
            onView(withText("방문 기록과 대조하기")).perform(scrollTo(), click())
            onView(withText("물건을 찾았어요")).perform(scrollTo(), click())
            onView(withText("저장")).perform(click())
            HistoryStore(context).use { store -> assertEquals("찾음", store.cases(true).single().state) }
        }
    }

    private fun capture(name: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val screenshot = instrumentation.uiAutomation.takeScreenshot() ?: return
        FileOutputStream(File(instrumentation.targetContext.filesDir, "onboard-$name.png")).use {
            screenshot.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        screenshot.recycle()
    }
}
