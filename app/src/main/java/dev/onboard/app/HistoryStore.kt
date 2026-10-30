package dev.onboard.app

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.location.Location
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Visit(
    val id: Long, val start: Long, val end: Long?, val label: String,
    val demo: Boolean, val latitude: Double?, val longitude: Double?,
    val accuracy: Float?, val reason: String, val doors: Int, val address: String = ""
) {
    val placeName: String get() = label.ifBlank { address }.ifBlank { "정차 장소" }
}

data class SearchCase(
    val id: Long, val item: String, val anchor: Long?, val certain: Boolean,
    val memory: String, val area: String, val state: String, val demo: Boolean
)

data class VisitNote(val id: Long, val visit: Long?, val title: String, val body: String, val demo: Boolean)

class HistoryStore(context: Context, databaseName: String = "onboard-history.db", private val compact: Boolean = true) : SQLiteOpenHelper(context, databaseName, null, 6) {
    private var locationPolicy = LocationStoragePolicy()
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE sessions(id INTEGER PRIMARY KEY, started INTEGER NOT NULL, ended INTEGER, reason TEXT NOT NULL DEFAULT '')")
        db.execSQL("CREATE TABLE visits(id INTEGER PRIMARY KEY, session INTEGER, started INTEGER NOT NULL, ended INTEGER, label TEXT NOT NULL DEFAULT '', demo INTEGER NOT NULL DEFAULT 0, lat REAL, lon REAL, accuracy REAL, reason TEXT NOT NULL DEFAULT '')")
        db.execSQL("CREATE TABLE events(id INTEGER PRIMARY KEY, session INTEGER, visit INTEGER, wall INTEGER NOT NULL, elapsed INTEGER NOT NULL, area TEXT NOT NULL, value INTEGER, kind TEXT NOT NULL, detail TEXT NOT NULL)")
        db.execSQL("CREATE INDEX events_visit ON events(visit)")
        db.execSQL("CREATE TABLE searches(id INTEGER PRIMARY KEY, item TEXT NOT NULL, anchor INTEGER, certain INTEGER NOT NULL, memory TEXT NOT NULL, area TEXT NOT NULL, demo INTEGER NOT NULL DEFAULT 0, state TEXT NOT NULL DEFAULT '탐색 중')")
        db.execSQL("CREATE TABLE checks(search_id INTEGER NOT NULL, visit_id INTEGER NOT NULL, result TEXT NOT NULL, PRIMARY KEY(search_id,visit_id))")
        createLocations(db)
        createContext(db)
        createNotes(db)
        createStorage(db)
        createAddresses(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) createLocations(db)
        if (oldVersion < 3) createContext(db)
        if (oldVersion < 4) createNotes(db)
        if (oldVersion < 5) createStorage(db)
        if (oldVersion < 6) createAddresses(db)
    }

    private fun createAddresses(db: SQLiteDatabase) {
        val columns = db.rawQuery("PRAGMA table_info(visits)", null).use { c -> buildSet { while (c.moveToNext()) add(c.getString(1)) } }
        if ("address" !in columns) db.execSQL("ALTER TABLE visits ADD COLUMN address TEXT NOT NULL DEFAULT ''")
        if ("address_lat" !in columns) db.execSQL("ALTER TABLE visits ADD COLUMN address_lat REAL")
        if ("address_lon" !in columns) db.execSQL("ALTER TABLE visits ADD COLUMN address_lon REAL")
    }

    private fun nearby(lat: Double?, lon: Double?, otherLat: Double?, otherLon: Double?): Boolean {
        if (lat == null || lon == null || otherLat == null || otherLon == null) return false
        val distance = FloatArray(1)
        Location.distanceBetween(lat, lon, otherLat, otherLon, distance)
        return distance[0] <= 100f
    }

    fun saveAddress(visit: Visit, address: String): Boolean {
        if (visit.demo || address.isBlank()) return false
        val current = visits(false).firstOrNull { it.id == visit.id && it.start == visit.start } ?: return false
        if (!nearby(current.latitude, current.longitude, visit.latitude, visit.longitude)) return false
        return writableDatabase.update("visits", ContentValues().apply {
            put("address", address.trim().take(240)); put("address_lat", visit.latitude); put("address_lon", visit.longitude)
        }, "id=? AND started=? AND demo=0", arrayOf(visit.id.toString(), visit.start.toString())) == 1
    }

    private fun createStorage(db: SQLiteDatabase) {
        val columns = db.rawQuery("PRAGMA table_info(events)", null).use { c -> buildSet { while (c.moveToNext()) add(c.getString(1)) } }
        if ("last_wall" !in columns) db.execSQL("ALTER TABLE events ADD COLUMN last_wall INTEGER")
        if ("occurrences" !in columns) db.execSQL("ALTER TABLE events ADD COLUMN occurrences INTEGER NOT NULL DEFAULT 1")
        db.execSQL("CREATE TABLE IF NOT EXISTS storage_stats(kind TEXT PRIMARY KEY, received INTEGER NOT NULL, saved INTEGER NOT NULL)")
        db.execSQL("CREATE INDEX IF NOT EXISTS events_area_session ON events(session,area,id)")
    }

    private fun countStorage(kind: String, saved: Boolean) {
        writableDatabase.execSQL("INSERT OR REPLACE INTO storage_stats(kind,received,saved) VALUES(?,COALESCE((SELECT received FROM storage_stats WHERE kind=?),0)+1,COALESCE((SELECT saved FROM storage_stats WHERE kind=?),0)+?)",
            arrayOf<Any>(kind, kind, kind, if (saved) 1 else 0))
    }

    fun storageSummary(): String = readableDatabase.rawQuery("SELECT * FROM storage_stats ORDER BY kind", null).use { c ->
        buildList { while (c.moveToNext()) add("${if (c.text("kind") == "location") "위치" else "차량 상태"}: 받은 기록 ${c.long("received")}건 → 저장 ${c.long("saved")}행") }.joinToString("\n").ifBlank { "새로 수집한 기록부터 절약 현황을 표시합니다." }
    }

    private fun createNotes(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE visit_notes(id INTEGER PRIMARY KEY, visit INTEGER, title TEXT NOT NULL, body TEXT NOT NULL, demo INTEGER NOT NULL, updated INTEGER NOT NULL)")
    }

    fun notes(demo: Boolean): List<VisitNote> = readableDatabase.rawQuery(
        "SELECT * FROM visit_notes WHERE demo=? ORDER BY updated DESC LIMIT 200", arrayOf(if (demo) "1" else "0")
    ).use { c -> buildList { while (c.moveToNext()) add(VisitNote(c.long("id"), c.optionalLong("visit"), c.text("title"), c.text("body"), c.long("demo") == 1L)) } }

    fun saveNote(id: Long?, visit: Long?, title: String, body: String, demo: Boolean): Long {
        val values = ContentValues().apply {
            put("visit", visit); put("title", title.take(80)); put("body", body.take(500))
            put("demo", if (demo) 1 else 0); put("updated", System.currentTimeMillis())
        }
        if (id != null) { writableDatabase.update("visit_notes", values, "id=?", arrayOf(id.toString())); return id }
        return writableDatabase.insertOrThrow("visit_notes", null, values)
    }

    fun deleteNote(id: Long) { writableDatabase.delete("visit_notes", "id=?", arrayOf(id.toString())) }

    fun approachLocations(visit: Visit): List<String> = readableDatabase.rawQuery(
        "SELECT * FROM locations WHERE session=(SELECT session FROM visits WHERE id=?) AND measured<=? AND measured>=COALESCE((SELECT MAX(ended) FROM visits WHERE demo=0 AND ended<=? AND session=(SELECT session FROM visits WHERE id=?)),?) ORDER BY measured DESC LIMIT 50",
        arrayOf(visit.id.toString(), visit.start.toString(), visit.start.toString(), visit.id.toString(), (visit.start - 3_600_000).toString())
    ).use { c -> buildList { while (c.moveToNext()) add("${displayTime(c.long("measured"))} · ${String.format(Locale.US, "%.5f, %.5f", c.optionalDouble("lat"), c.optionalDouble("lon"))} · 정확도 ${c.optionalDouble("accuracy")?.toInt()?.let { "±${it}m" } ?: "불명"}") } }

    private fun createContext(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE navigation_events(id INTEGER PRIMARY KEY, session INTEGER, visit INTEGER, wall INTEGER NOT NULL, kind TEXT NOT NULL, name TEXT NOT NULL, address TEXT NOT NULL)")
        db.execSQL("CREATE TABLE route_gaps(session INTEGER, started INTEGER NOT NULL, ended INTEGER NOT NULL, elapsed INTEGER NOT NULL, distance REAL NOT NULL, PRIMARY KEY(session,elapsed))")
    }

    fun navigation(session: Long, visit: Long?, kind: String, name: String, address: String) {
        writableDatabase.insertOrThrow("navigation_events", null, ContentValues().apply {
            put("session", session); put("visit", visit); put("wall", System.currentTimeMillis())
            put("kind", kind); put("name", name); put("address", address)
        })
    }

    fun gap(session: Long, gap: CoverageGap) {
        writableDatabase.execSQL("INSERT OR REPLACE INTO route_gaps VALUES(?,?,?,?,?)",
            arrayOf<Any>(session, gap.started, gap.ended, gap.startElapsed, gap.distanceKm))
    }

    fun contextDetails(visit: Visit): List<String> = readableDatabase.rawQuery(
        "SELECT * FROM navigation_events WHERE session=(SELECT session FROM visits WHERE id=?) AND wall BETWEEN ? AND ? ORDER BY wall",
        arrayOf(visit.id.toString(), (visit.start - 120_000).toString(), (visit.end ?: System.currentTimeMillis()).toString())
    ).use { c -> buildList { while (c.moveToNext()) add("${c.text("kind")} · ${c.text("name")} ${c.text("address")}\n수신 시각: ${displayTime(c.long("wall"))} · 근접 시각 정보이며 실제 방문 증명이 아닙니다.") } }

    fun gapSummaries(from: Long? = null, until: Long? = null): List<String> = readableDatabase.rawQuery(
        "SELECT * FROM route_gaps WHERE ended>=? AND started<? ORDER BY started DESC LIMIT 5",
        arrayOf((from ?: 0L).toString(), (until ?: Long.MAX_VALUE).toString())
    ).use { c ->
        buildList { while (c.moveToNext()) add("${displayTime(c.long("started"))}~${displayTime(c.long("ended"))} · 거리계 증가 약 ${String.format(Locale.US, "%.1f", c.getDouble(c.getColumnIndexOrThrow("distance")))}km · 경로 미기록 의심") }
    }
    private fun displayTime(value: Long) = SimpleDateFormat("MM.dd HH:mm:ss", Locale.KOREA).format(Date(value))

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
        val keep = !compact || locationPolicy.keep(session, visit, elapsed, latitude, longitude, accuracy, provider)
        countStorage("location", keep)
        if (!keep) return
        writableDatabase.insertOrThrow("locations", null, ContentValues().apply {
            put("session", session); put("visit", visit); put("received", System.currentTimeMillis())
            put("measured", measured); put("measured_elapsed", elapsed); put("lat", latitude)
            put("lon", longitude); put("accuracy", accuracy); put("provider", provider)
        })
    }

    fun event(session: Long, visit: Long?, area: String, value: Int?, kind: String, detail: String,
              elapsed: Long, wall: Long = System.currentTimeMillis()) {
        if (compact && kind in setOf("snapshot", "repeat", "status")) {
            var same = readableDatabase.rawQuery("SELECT * FROM events WHERE session=? AND area=? AND kind=? ORDER BY id DESC LIMIT 1", arrayOf(session.toString(), area, kind)).use { c ->
                if (c.moveToFirst() && c.optionalLong("visit") == visit && c.optionalLong("value") == value?.toLong() &&
                    c.text("detail") == detail && elapsed - c.long("elapsed") in 0..60_000 && wall >= (c.optionalLong("last_wall") ?: c.long("wall"))) c.long("id") else null
            }
            if (same != null) readableDatabase.rawQuery(
                "SELECT 1 FROM events WHERE session=? AND area=? AND id>? AND (kind IN ('baseline','transition','unavailable') OR COALESCE(value,-2147483649)<>CAST(? AS INTEGER) OR COALESCE(visit,-1)<>CAST(? AS INTEGER)) LIMIT 1",
                arrayOf(session.toString(), area, same.toString(), value?.toString() ?: "-2147483649", visit?.toString() ?: "-1")
            ).use { if (it.moveToFirst()) same = null }
            val mergeId = same
            if (mergeId != null) {
                writableDatabase.execSQL("UPDATE events SET last_wall=?,occurrences=occurrences+1 WHERE id=?", arrayOf<Any>(wall, mergeId))
                countStorage("event", false)
                return
            }
        }
        countStorage("event", true)
        writableDatabase.insertOrThrow("events", null, ContentValues().apply {
            put("session", session); put("visit", visit); put("wall", wall); put("elapsed", elapsed)
            put("area", area); put("value", value); put("kind", kind); put("detail", detail)
            put("last_wall", wall); put("occurrences", 1)
        })
    }

    fun visits(demo: Boolean): List<Visit> = readableDatabase.rawQuery(
        "SELECT v.*, (SELECT COUNT(*) FROM events e WHERE e.visit=v.id AND e.kind='transition') AS doors FROM visits v WHERE demo=? ORDER BY started DESC LIMIT 200",
        arrayOf(if (demo) "1" else "0")
    ).use { c -> buildList { while (c.moveToNext()) add(Visit(c.long("id"), c.long("started"), c.optionalLong("ended"), c.text("label"), c.long("demo") == 1L, c.optionalDouble("lat"), c.optionalDouble("lon"), c.optionalDouble("accuracy")?.toFloat(), c.text("reason"), c.long("doors").toInt(),
        if (c.long("demo") == 1L || nearby(c.optionalDouble("lat"), c.optionalDouble("lon"), c.optionalDouble("address_lat"), c.optionalDouble("address_lon"))) c.text("address") else "")) } }

    fun details(visitId: Long): List<String> = readableDatabase.rawQuery(
        "SELECT * FROM events WHERE visit=? ORDER BY id DESC LIMIT 100", arrayOf(visitId.toString())
    ).use { c -> buildList { while (c.moveToNext()) add("${displayTime(c.long("wall"))}~${displayTime(c.optionalLong("last_wall") ?: c.long("wall"))} | ${c.text("area")} | ${c.text("kind")} | 값=${c.optionalLong("value")} | ${c.long("occurrences")}회 관측 | ${c.text("detail")}") } }

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
        val existing = visits(true)
        if (existing.isNotEmpty()) {
            existing.sortedBy { it.start }.forEachIndexed { index, visit ->
                if (visit.address.isBlank()) writableDatabase.execSQL(
                    "UPDATE visits SET address=? WHERE id=? AND demo=1",
                    arrayOf<Any>(demoAddress(index), visit.id))
            }
            return
        }
        val now = System.currentTimeMillis()
        listOf("마트", "카페", "우체국").forEachIndexed { index, name ->
            val time = now - (3 - index) * 3_600_000L
            val id = beginVisit(0, time, "$name · 예시", true)
            writableDatabase.execSQL("UPDATE visits SET address=? WHERE id=? AND demo=1", arrayOf<Any>(demoAddress(index), id))
            endVisit(id, "모의 기록", time + 600_000)
            if (index != 1) {
                event(0, id, "ROW_1_LEFT", 1, "transition", "모의 입력 · 실제 수집 아님", index.toLong(), time + 1000)
            }
        }
    }

    private fun demoAddress(index: Int) = listOf(
        "서울특별시 가상로 101 (가상 주소)",
        "서울특별시 가상로 205 (가상 주소)",
        "서울특별시 가상로 310 (가상 주소)"
    )[index % 3]

    fun deleteAll() {
        locationPolicy = LocationStoragePolicy()
        writableDatabase.beginTransaction()
        try {
            listOf("checks", "searches", "visit_notes", "storage_stats", "events", "locations", "navigation_events", "route_gaps", "visits", "sessions").forEach { writableDatabase.delete(it, null, null) }
            writableDatabase.setTransactionSuccessful()
        } finally { writableDatabase.endTransaction() }
    }

    private fun purgeOld() {
        val cutoff = System.currentTimeMillis() - 7 * 86_400_000L
        writableDatabase.beginTransaction()
        try {
            writableDatabase.execSQL("DELETE FROM checks WHERE visit_id IN (SELECT id FROM visits WHERE started < ?)", arrayOf(cutoff))
            writableDatabase.execSQL("UPDATE visit_notes SET visit=NULL WHERE visit IN (SELECT id FROM visits WHERE started < ?)", arrayOf(cutoff))
            writableDatabase.execSQL("DELETE FROM events WHERE wall < ?", arrayOf(cutoff))
            writableDatabase.execSQL("DELETE FROM locations WHERE received < ?", arrayOf(cutoff))
            writableDatabase.execSQL("DELETE FROM navigation_events WHERE wall < ?", arrayOf(cutoff))
            writableDatabase.execSQL("DELETE FROM route_gaps WHERE started < ?", arrayOf(cutoff))
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
