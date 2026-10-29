package dev.onboard.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HistoryStoreTest {
    private lateinit var store: HistoryStore
    @Before fun prepare() {
        store = HistoryStore(InstrumentationRegistry.getInstrumentation().targetContext)
        store.deleteAll()
    }
    @After fun finish() { store.deleteAll(); store.close() }

    @Test fun demoAndRealEvidenceNeverMix() {
        store.seedDemo()
        val session = store.startSession()
        val id = store.beginVisit(session)
        store.event(session, id, "ROW_1_LEFT", 1, "baseline", "initial", 1)
        store.event(session, id, "ROW_1_LEFT", 0, "transition", "changed", 2)
        assertEquals(3, store.visits(true).size)
        assertEquals(1, store.visits(false).size)
        assertEquals(1, store.visits(false).single().doors)
        assertNull(store.visits(false).single().latitude)
    }
    @Test fun memoryChecksAndCompletionPersistAcrossReopen() {
        store.seedDemo()
        val visit = store.visits(true).first()
        val search = store.saveCase(null, "지갑", null, false, "기억 불확실", "트렁크", true)
        store.check(search, visit.id, "확인했지만 못 찾음")
        store.finishCase(search, "미발견 종료")
        store.close()
        store = HistoryStore(InstrumentationRegistry.getInstrumentation().targetContext)
        assertEquals("미발견 종료", store.cases(true).single().state)
        assertEquals("확인했지만 못 찾음", store.checks(search)[visit.id])
        assertTrue(store.cases(false).isEmpty())
        assertEquals(3, store.visits(true).size)
    }
    @Test fun interruptedSessionMarksUnknownEndRatherThanFillingGap() {
        val first = store.startSession()
        store.beginVisit(first)
        store.startSession()
        assertTrue(store.visits(false).single().reason.contains("종료 시각 불명"))
    }
    @Test fun deletionRemovesCasesEventsAndDemoWithoutReseeding() {
        store.seedDemo()
        store.saveCase(null, "키", null, false, "", "", true)
        store.deleteAll()
        assertTrue(store.visits(true).isEmpty())
        assertTrue(store.visits(false).isEmpty())
        assertTrue(store.cases(true).isEmpty())
    }
}
