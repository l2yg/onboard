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
        store = HistoryStore(InstrumentationRegistry.getInstrumentation().targetContext, "onboard-history-test.db")
        store.deleteAll()
    }
    @After fun finish() { store.deleteAll(); store.close() }

    @Test fun addressPersistsWithoutOverwritingCustomName() {
        val id = store.beginVisit(store.startSession())
        store.location(id, 37.5, 127.0, 5f)
        val visit = store.visits(false).single()
        assertTrue(store.saveAddress(visit, "서울시 예시 주소"))
        assertEquals("서울시 예시 주소", store.visits(false).single().placeName)
        store.rename(id, "내가 붙인 이름")
        store.close()
        store = HistoryStore(InstrumentationRegistry.getInstrumentation().targetContext, "onboard-history-test.db")
        assertEquals("내가 붙인 이름", store.visits(false).single().placeName)
        store.rename(id, "")
        assertEquals("서울시 예시 주소", store.visits(false).single().placeName)
        assertFalse(store.saveAddress(visit.copy(start = visit.start - 1), "오래된 요청"))
        store.location(id, 37.6, 127.0, 5f)
        assertEquals("", store.visits(false).single().address)
        assertFalse(store.saveAddress(visit, "이전 좌표의 응답"))
    }

    @Test fun versionFiveUpgradeAddsAddressWithoutLosingVisitOrNote() {
        val id = store.beginVisit(store.startSession())
        store.saveNote(null, id, "보존할 메모", "내용", false)
        store.writableDatabase.version = 5
        store.close()
        store = HistoryStore(InstrumentationRegistry.getInstrumentation().targetContext, "onboard-history-test.db")
        assertEquals(id, store.visits(false).single().id)
        assertEquals("", store.visits(false).single().address)
        assertEquals(id, store.notes(false).single().visit)
    }

    @Test fun versionThreeUpgradeKeepsOldRecordsAndPersistsNewNotes() {
        val visit = store.beginVisit(store.startSession())
        val legacy = store.saveCase(null, "기존 자료", null, false, "보존", "", false)
        store.writableDatabase.execSQL("DROP TABLE visit_notes")
        store.writableDatabase.version = 3
        store.close()
        store = HistoryStore(InstrumentationRegistry.getInstrumentation().targetContext, "onboard-history-test.db")
        assertEquals(visit, store.visits(false).single().id)
        assertEquals(legacy, store.cases(false).single().id)
        val note = store.saveNote(null, visit, "방문", "메모", false)
        store.close()
        store = HistoryStore(InstrumentationRegistry.getInstrumentation().targetContext, "onboard-history-test.db")
        assertEquals(note, store.notes(false).single().id)
        assertEquals(visit, store.notes(false).single().visit)
        assertTrue(store.notes(true).isEmpty())
    }

    @Test fun expiredVisitsDetachNotesBeforeIdsCanBeReused() {
        val visit = store.beginVisit(store.startSession(), System.currentTimeMillis() - 9 * 86_400_000L)
        store.saveNote(null, visit, "남겨둘 메모", "방문은 만료", false)
        store.startSession()
        assertTrue(store.visits(false).isEmpty())
        assertNull(store.notes(false).single().visit)
        store.saveNote(null, null, "시연 메모", "", true)
        assertEquals(1, store.notes(false).size)
        assertEquals(1, store.notes(true).size)
        store.deleteAll()
        assertTrue(store.notes(false).isEmpty())
        assertTrue(store.notes(true).isEmpty())
    }

    @Test fun versionTwoUpgradePreservesExistingVisits() {
        val id = store.beginVisit(store.startSession())
        store.writableDatabase.execSQL("DROP TABLE navigation_events")
        store.writableDatabase.execSQL("DROP TABLE route_gaps")
        store.writableDatabase.execSQL("DROP TABLE visit_notes")
        store.writableDatabase.version = 2
        store.close()
        store = HistoryStore(InstrumentationRegistry.getInstrumentation().targetContext, "onboard-history-test.db")
        assertEquals(id, store.visits(false).single().id)
        assertTrue(store.gapSummaries().isEmpty())
    }

    @Test fun navigationContextAndGapRemainLocalAndDeleteTogether() {
        val session = store.startSession()
        val id = store.beginVisit(session)
        store.navigation(session, id, "내비 목적지 도착 알림", "도서관", "주소")
        val visit = store.visits(false).single()
        assertTrue(store.contextDetails(visit).single().contains("도서관"))
        store.seedDemo()
        assertTrue(store.contextDetails(store.visits(true).first()).isEmpty())
        store.gap(session, CoverageGap(1, 20_001, 100, 0.2f))
        store.gap(session, CoverageGap(1, 40_001, 100, 0.4f))
        assertEquals(1, store.gapSummaries().size)
        store.deleteAll()
        assertTrue(store.gapSummaries().isEmpty())
        assertTrue(store.contextDetails(visit).isEmpty())
    }

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
        store = HistoryStore(InstrumentationRegistry.getInstrumentation().targetContext, "onboard-history-test.db")
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
