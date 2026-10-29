package dev.onboard.app

import android.graphics.Bitmap
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withHint
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.FileOutputStream
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppFlowTest {
    @Test fun visitReplayAndLinkedNoteCanBeCreatedRestoredEditedAndDeleted() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.getSharedPreferences("MainActivity", 0).edit().putBoolean("demo", true).commit()
        val visit = HistoryStore(context).use { it.seedDemo(); it.visits(true).first() }
        val title = "방문 시험 ${System.nanoTime()}"
        val updatedTitle = "수정한 $title"
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            onView(withText("물건 찾기")).check(doesNotExist())
            capture("home")
            onView(withText("최근 7일 · 날짜 선택")).perform(scrollTo(), click())
            onView(withId(android.R.id.button1)).perform(click())
            onView(withText("최근 7일 전체 보기")).perform(scrollTo(), click())
            onView(withContentDescription("여기부터 돌아보기 · ${visit.label}")).perform(scrollTo(), click())
            onView(withText("이 시점부터 돌아보기")).check(matches(isDisplayed()))
            capture("results")
            onView(withContentDescription("메모 남기기 · ${visit.label}")).perform(scrollTo(), click())
            onView(withHint("예: 카페 방문")).perform(replaceText(title))
            onView(withHint("기억할 일을 남겨보세요")).perform(replaceText("다음에 다시 방문"))
            closeSoftKeyboard()
            scenario.recreate()
            onView(withText("다음에 다시 방문")).check(matches(isDisplayed()))
            capture("memory")
            onView(withText("메모 저장")).perform(scrollTo(), click())
            val note = HistoryStore(context).use { store -> store.notes(true).first { it.title == title }.also { assertEquals(visit.id, it.visit) } }
            onView(withContentDescription("메모 수정 · $title")).perform(scrollTo(), click())
            onView(withHint("예: 카페 방문")).perform(replaceText(updatedTitle))
            closeSoftKeyboard()
            onView(withText("방문 기록")).perform(click())
            onView(withText("계속 입력")).perform(click())
            onView(withText("메모 저장")).perform(scrollTo(), click())
            HistoryStore(context).use { store -> assertEquals(updatedTitle, store.notes(true).first { it.id == note.id }.title) }
            onView(withContentDescription("메모 수정 · $updatedTitle")).perform(scrollTo(), click())
            onView(withText("메모 삭제")).perform(scrollTo(), click())
            onView(withText("삭제")).perform(click())
            HistoryStore(context).use { store -> assertEquals(false, store.notes(true).any { it.id == note.id }) }
        }
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
