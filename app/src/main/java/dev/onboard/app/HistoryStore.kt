package dev.onboard.app

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class Visit(
    val id: Long, val start: Long, val end: Long?, val label: String,
    val demo: Boolean, val latitude: Double?, val longitude: Double?,
    val accuracy: Float?, val reason: String, val doors: Int
)

data class SearchCase(
    val id: Long, val item: String, val anchor: Long?, val certain: Boolean,
    val memory: String, val area: String, val state: String, val demo: Boolean
)

class HistoryStore(context: Context) : SQLiteOpenHelper(context, "onboard-history.db", null, 2) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE sessions(id INTEGER PRIMARY KEY, started INTEGER NOT NULL, ended INTEGER, reason TEXT NOT NULL DEFAULT '')")
        db.execSQL("CREATE TABLE visits(id INTEGER PRIMARY KEY, session INTEGER, started INTEGER NOT NULL, ended INTEGER, label TEXT NOT NULL DEFAULT '', demo INTEGER NOT NULL DEFAULT 0, lat REAL, lon REAL, accuracy REAL, reason TEXT NOT NULL DEFAULT '')")
        db.execSQL("CREATE TABLE events(id INTEGER PRIMARY KEY, session INTEGER, visit INTEGER, wall INTEGER NOT NULL, elapsed INTEGER NOT NULL, area TEXT NOT NULL, value INTEGER, kind TEXT NOT NULL, detail TEXT NOT NULL)")
        db.execSQL("CREATE INDEX events_visit ON events(visit)")
        db.execSQL("CREATE TABLE searches(id INTEGER PRIMARY KEY, item TEXT NOT NULL, anchor INTEGER, certain INTEGER NOT NULL, memory TEXT NOT NULL, area TEXT NOT NULL, demo INTEGER NOT NULL DEFAULT 0, state TEXT NOT NULL DEFAULT '탐색 중')")
        db.execSQL("CREATE TABLE checks(search_id INTEGER NOT NULL, visit_id INTEGER NOT NULL, result TEXT NOT NULL, PRIMARY KEY(search_id,visit_id))")
        createLocations(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) createLocations(db)
    }

    private fun createLocations(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE locations(id INTEGER PRIMARY KEY, session INTEGER, visit INTEGER, received INTEGER NOT NULL, measured INTEGER NOT NULL, measured_elapsed INTEGER NOT NULL, lat REAL NOT NULL, lon REAL NOT NULL, accuracy REAL, provider TEXT NOT NULL)")
        db.execSQL("CREATE INDEX locations_visit ON locations(visit)")
    }

    fun startSession(): Long {
        // An unclosed session has unknown continuity; never fill the missing interval with invented events.
        writableDatabase.execSQL("UPDATE sessions SET reason='중단 후 재시작 · 종료 시각 불명' WHERE ended IS NULL AND reason=''")
        writableDatabase.execSQL("UPDATE visits SET reason='수집 중단 · 종료 시각 불명' WHERE demo=0 AND ended IS NULL AND reason=''")
        purgeOld()
        return writableDatabase.insertOrThrow("sessions", null, ContentValues().apply { put("started", System.currentTimeMillis()) })
    }

    fun endSession(id: Long, reason: String) {
        writableDatabase.execSQL("UPDATE sessions SET ended=?,reason=? WHERE id=?", arrayOf<Any>(System.currentTimeMillis(), reason, id))
    }

    fun beginVisit(session: Long, time: Long = System.currentTimeMillis(), label: String = "", demo: Boolean = false): Long =
        writableDatabase.insertOrThrow("visits", null, ContentValues().apply {
            put("session", session); put("started", time); put("label", label); put("demo", if (demo) 1 else 0)
        })

    fun endVisit(id: Long, reason: String, time: Long = System.currentTimeMillis()) {
        writableDatabase.execSQL("UPDATE visits SET ended=?,reason=? WHERE id=?", arrayOf<Any>(time, reason, id))
    }

    fun location(id: Long, latitude: Double, longitude: Double, accuracy: Float) {
        writableDatabase.execSQL("UPDATE visits SET lat=?,lon=?,accuracy=? WHERE id=?", arrayOf<Any>(latitude, longitude, accuracy, id))
    }

    fun locationSample(session: Long, visit: Long?, measured: Long, elapsed: Long, latitude: Double,
                       longitude: Double, accuracy: Float?, provider: String) {
        writableDatabase.insertOrThrow("locations", null, ContentValues().apply {
            put("session", session); put("visit", visit); put("received", System.currentTimeMillis())
            put("measured", measured); put("measured_elapsed", elapsed); put("lat", latitude)
            put("lon", longitude); put("accuracy", accuracy); put("provider", provider)
        })
    }

    fun event(session: Long, visit: Long?, area: String, value: Int?, kind: String, detail: String,
              elapsed: Long, wall: Long = System.currentTimeMillis()) {
        writableDatabase.insertOrThrow("events", null, ContentValues().apply {
            put("session", session); put("visit", visit); put("wall", wall); put("elapsed", elapsed)
            put("area", area); put("value", value); put("kind", kind); put("detail", detail)
        })
    }

    fun visits(demo: Boolean): List<Visit> = readableDatabase.rawQuery(
        "SELECT v.*, (SELECT COUNT(*) FROM events e WHERE e.visit=v.id AND e.kind='transition') AS doors FROM visits v WHERE demo=? ORDER BY started DESC LIMIT 200",
        arrayOf(if (demo) "1" else "0")
    ).use { c -> buildList { while (c.moveToNext()) add(Visit(c.long("id"), c.long("started"), c.optionalLong("ended"), c.text("label"), c.long("demo") == 1L, c.optionalDouble("lat"), c.optionalDouble("lon"), c.optionalDouble("accuracy")?.toFloat(), c.text("reason"), c.long("doors").toInt())) } }

    fun details(visitId: Long): List<String> = readableDatabase.rawQuery(
        "SELECT * FROM events WHERE visit=? ORDER BY id DESC LIMIT 100", arrayOf(visitId.toString())
    ).use { c -> buildList { while (c.moveToNext()) add("${c.long("wall")} | ${c.text("area")} | ${c.text("kind")} | 값=${c.optionalLong("value")} | ${c.text("detail")}") } }

    fun rename(id: Long, name: String) { writableDatabase.execSQL("UPDATE visits SET label=? WHERE id=?", arrayOf<Any>(name.take(80), id)) }

    fun relatedDoorCount(visit: Long, area: String): Int = readableDatabase.rawQuery(
        "SELECT COUNT(*) FROM events WHERE visit=? AND area=? AND kind='transition'",
        arrayOf(visit.toString(), area)
    ).use { it.moveToFirst(); it.getInt(0) }

    fun observedDoorAreas(): List<String> = readableDatabase.rawQuery(
        "SELECT DISTINCT area FROM events WHERE kind IN ('baseline','snapshot','transition') ORDER BY area", null
    ).use { cursor -> buildList { while (cursor.moveToNext()) add(cursor.getString(0)) } }

    fun saveCase(id: Long?, item: String, anchor: Long?, certain: Boolean, memory: String, area: String, demo: Boolean): Long {
        val values = ContentValues().apply {
            put("item", item.take(80)); put("anchor", anchor); put("certain", if (certain) 1 else 0)
            put("memory", memory.take(300)); put("area", area.take(80)); put("state", "탐색 중")
            put("demo", if (demo) 1 else 0)
        }
        if (id != null) {
            writableDatabase.update("searches", values, "id=?", arrayOf(id.toString()))
            return id
        }
        return writableDatabase.insertOrThrow("searches", null, values)
    }

    fun cases(demo: Boolean): List<SearchCase> = readableDatabase.rawQuery("SELECT * FROM searches WHERE demo=? ORDER BY id DESC LIMIT 100", arrayOf(if (demo) "1" else "0")).use { c ->
        buildList { while (c.moveToNext()) add(SearchCase(c.long("id"), c.text("item"), c.optionalLong("anchor"), c.long("certain") == 1L, c.text("memory"), c.text("area"), c.text("state"), c.long("demo") == 1L)) }
    }

    fun finishCase(id: Long, state: String) { writableDatabase.execSQL("UPDATE searches SET state=? WHERE id=?", arrayOf<Any>(state, id)) }
    fun check(search: Long, visit: Long, result: String) {
        writableDatabase.execSQL("INSERT OR REPLACE INTO checks(search_id,visit_id,result) VALUES(?,?,?)", arrayOf<Any>(search, visit, result))
    }
    fun checks(search: Long): Map<Long, String> = readableDatabase.rawQuery("SELECT * FROM checks WHERE search_id=?", arrayOf(search.toString())).use { c ->
        buildMap { while (c.moveToNext()) put(c.long("visit_id"), c.text("result")) }
    }

    fun seedDemo() {
        if (visits(true).isNotEmpty()) return
        val now = System.currentTimeMillis()
        listOf("마트", "카페", "우체국").forEachIndexed { index, name ->
            val time = now - (3 - index) * 3_600_000L
            val id = beginVisit(0, time, "$name · 예시", true)
            endVisit(id, "모의 기록", time + 600_000)
            if (index != 1) {
                event(0, id, "ROW_1_LEFT", 1, "transition", "모의 입력 · 실제 수집 아님", index.toLong(), time + 1000)
            }
        }
    }

    fun deleteAll() {
        writableDatabase.beginTransaction()
        try {
            listOf("checks", "searches", "events", "locations", "visits", "sessions").forEach { writableDatabase.delete(it, null, null) }
            writableDatabase.setTransactionSuccessful()
        } finally { writableDatabase.endTransaction() }
    }

    private fun purgeOld() {
        val cutoff = System.currentTimeMillis() - 7 * 86_400_000L
        writableDatabase.beginTransaction()
        try {
            writableDatabase.execSQL("DELETE FROM checks WHERE visit_id IN (SELECT id FROM visits WHERE started < ?)", arrayOf(cutoff))
            writableDatabase.execSQL("DELETE FROM events WHERE wall < ?", arrayOf(cutoff))
            writableDatabase.execSQL("DELETE FROM locations WHERE received < ?", arrayOf(cutoff))
            writableDatabase.execSQL("DELETE FROM visits WHERE started < ?", arrayOf(cutoff))
            writableDatabase.execSQL("DELETE FROM sessions WHERE started < ?", arrayOf(cutoff))
            writableDatabase.setTransactionSuccessful()
        } finally { writableDatabase.endTransaction() }
    }

    private fun Cursor.long(name: String) = getLong(getColumnIndexOrThrow(name))
    private fun Cursor.text(name: String) = getString(getColumnIndexOrThrow(name)) ?: ""
    private fun Cursor.optionalLong(name: String): Long? = getColumnIndexOrThrow(name).let { if (isNull(it)) null else getLong(it) }
    private fun Cursor.optionalDouble(name: String): Double? = getColumnIndexOrThrow(name).let { if (isNull(it)) null else getDouble(it) }
}
