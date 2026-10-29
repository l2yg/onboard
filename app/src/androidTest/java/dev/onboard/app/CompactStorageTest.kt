package dev.onboard.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CompactStorageTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun versionFourRowsRemainReadableAfterUpgrade() {
        val name = "onboard-upgrade-compact-test.db"
        context.deleteDatabase(name)
        try {
            HistoryStore(context, name).use { store ->
                store.writableDatabase.execSQL("DROP INDEX events_visit")
                store.writableDatabase.execSQL("DROP INDEX events_area_session")
                store.writableDatabase.execSQL("DROP TABLE events")
                store.writableDatabase.execSQL("CREATE TABLE events(id INTEGER PRIMARY KEY, session INTEGER, visit INTEGER, wall INTEGER NOT NULL, elapsed INTEGER NOT NULL, area TEXT NOT NULL, value INTEGER, kind TEXT NOT NULL, detail TEXT NOT NULL)")
                store.writableDatabase.execSQL("INSERT INTO events VALUES(1,1,1,1000,1000,'LEFT',1,'transition','old')")
                store.writableDatabase.execSQL("DROP TABLE storage_stats")
                store.writableDatabase.version = 4
            }
            HistoryStore(context, name).use { store ->
                assertEquals(1, count(store, "events"))
                assertTrue(store.details(1).single().contains("1회 관측"))
                assertEquals(1L, scalar(store, "SELECT value FROM events WHERE id=1"))
            }
        } finally { context.deleteDatabase(name) }
    }

    @Test fun alternatingSnapshotsAndRepeatsAreMergedButTransitionsRemain() {
        val name = "onboard-compact-test.db"
        context.deleteDatabase(name)
        try {
            HistoryStore(context, name).use { store ->
                val session = store.startSession(); val visit = store.beginVisit(session)
                repeat(6) { index ->
                    store.event(session, visit, "LEFT", 0, "snapshot", "read", index * 10_000L, 100_000 + index * 10_000L)
                    store.event(session, visit, "LEFT", 0, "repeat", "read", index * 10_000L + 1, 100_001 + index * 10_000L)
                }
                assertEquals(2, count(store, "events"))
                store.event(session, visit, "LEFT", 1, "transition", "read", 55_000, 155_000)
                store.event(session, visit, "LEFT", 0, "transition", "read", 56_000, 156_000)
                store.event(session, visit, "LEFT", 0, "snapshot", "read", 57_000, 157_000)
                assertEquals(5, count(store, "events"))
                assertEquals(2, store.visits(false).single().doors)
                assertTrue(store.details(visit).any { it.contains("6회 관측") })
            }
        } finally { context.deleteDatabase(name) }
    }

    @Test fun syntheticStationaryReplayUsesFewerRowsAndAllocatedBytes() {
        val names = listOf("onboard-full-test.db", "onboard-small-test.db")
        names.forEach { context.deleteDatabase(it) }
        try {
            val results = names.mapIndexed { index, name ->
                HistoryStore(context, name, compact = index == 1).use { store ->
                    val session = store.startSession()
                    val db = store.writableDatabase
                    db.beginTransaction()
                    try {
                        repeat(1_200) { i ->
                            store.locationSample(session, null, 1_000_000 + i * 5_000L, i * 5_000L, 37.0, 127.0, 5f, "gps")
                            store.event(session, null, "LEFT", 0, "snapshot", "received", i * 5_000L, 1_000_000 + i * 5_000L)
                        }
                        db.setTransactionSuccessful()
                    } finally { db.endTransaction() }
                    val bytes = scalar(store, "PRAGMA page_count") * scalar(store, "PRAGMA page_size")
                    Triple(count(store, "locations"), count(store, "events"), bytes)
                }
            }
            assertTrue(results[1].first < results[0].first / 5)
            assertTrue(results[1].second < results[0].second / 5)
            assertTrue(results[1].third < results[0].third)
            InstrumentationRegistry.getInstrumentation().sendStatus(0, android.os.Bundle().apply {
                putString("stream", "\n동일 합성 정차 입력 1200건씩: 일반=${results[0]}, 경량=${results[1]} (위치 행, 차량 행, 할당 바이트)\n")
            })
        } finally { names.forEach { context.deleteDatabase(it) } }
    }
    private fun count(store: HistoryStore, table: String) = scalar(store, "SELECT COUNT(*) FROM $table").toInt()
    private fun scalar(store: HistoryStore, sql: String) = store.readableDatabase.rawQuery(sql, null).use { it.moveToFirst(); it.getLong(0) }
}
