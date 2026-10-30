package dev.onboard.app

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    private val bg get() = getColor(R.color.canvas)
    private val panel get() = getColor(R.color.surface)
    private val accent get() = getColor(R.color.accent)
    private val muted get() = getColor(R.color.muted)
    private val ink get() = getColor(R.color.ink)
    private val soft get() = getColor(R.color.soft)
    private val buttonHeight get() = dp((80 * resources.configuration.fontScale.coerceAtLeast(1f)).toInt())
    private lateinit var store: HistoryStore
    private lateinit var addresses: VisitAddressResolver
    private lateinit var content: LinearLayout
    private var scroller: ScrollView? = null
    private var renderedPage = ""
    private val handler = Handler(Looper.getMainLooper())
    private var monitor: MotionMonitor? = null
    private var navigation: NavigationLink? = null
    private var routeMessage = ""
    private var parked: Boolean? = null
    private var motionAt = 0L
    private var demo = false
    private var page = "home"
    private var editingCase: Long? = null
    private var itemDraft = ""
    private var memoryDraft = ""
    private var allVisits = false
    private var itemInput: EditText? = null
    private var noteVisit: Long? = null
    private var replayFrom: Long? = null
    private var filterDay: Long? = null
    private var statusLabel: TextView? = null
    private var activeDialog: Dialog? = null
    private var lastRunning = false
    private var pendingStart = false
    private var lastVisits = ""
    private var touching = false
    private var pendingRender = false
    private val ticker = object : Runnable {
        override fun run() {
            if (motionAt != 0L && SystemClock.elapsedRealtime() - motionAt > 6_000 && parked != null) {
                parked = null
                if (!allowed()) activeDialog?.dismiss()
                render()
            }
            if (pendingStart && parked != null) {
                pendingStart = false
                if (parked == true) startRecording() else toast("정차 후 기록 시작을 다시 눌러주세요")
            }
            statusLabel?.text = statusText()
            if (lastRunning != RecordingService.running && page == "home") render()
            if (page == "home" && allowed() && lastVisits != visitSignature()) render()
            if (!demo && allowed() && (page == "home" || page == "results")) {
                store.visits(false).take(if (page == "home" && !allVisits) 5 else 200).forEach(addresses::request)
            }
            handler.postDelayed(this, 1_000)
        }
    }

    override fun attachBaseContext(newBase: Context) {
        val mode = newBase.getSharedPreferences("MainActivity", 0).getString("appearance", "system")
        val night = when (mode) { "light" -> Configuration.UI_MODE_NIGHT_NO; "dark" -> Configuration.UI_MODE_NIGHT_YES; else -> null }
        val base = if (night == null) newBase else newBase.createConfigurationContext(Configuration(newBase.resources.configuration).apply {
            uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or night
        })
        super.attachBaseContext(base)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = HistoryStore(this)
        addresses = VisitAddressResolver(this) { visit, address ->
            if (store.saveAddress(visit, address) && allowed() && (page == "home" || page == "results")) {
                if (touching || activeDialog?.isShowing == true) pendingRender = true else render()
            }
        }
        demo = savedInstanceState?.getBoolean("demo") ?: getPreferences(0).getBoolean("demo", false)
        if (demo) store.seedDemo()
        page = savedInstanceState?.getString("page") ?: "home"
        editingCase = savedInstanceState?.getLong("editing", -1)?.takeIf { it >= 0 }
        itemDraft = savedInstanceState?.getString("item") ?: ""
        memoryDraft = savedInstanceState?.getString("memory") ?: ""
        allVisits = savedInstanceState?.getBoolean("allVisits") ?: false
        noteVisit = savedInstanceState?.getLong("noteVisit", -1)?.takeIf { it >= 0 }
        replayFrom = savedInstanceState?.getLong("replayFrom", -1)?.takeIf { it >= 0 }
        filterDay = savedInstanceState?.getLong("filterDay", -1)?.takeIf { it >= 0 }
        render()
    }

    override fun onResume() {
        super.onResume()
        if (routeMessage.isNotBlank()) toast(routeMessage)
        parked = null
        monitor = MotionMonitor(this) { value ->
            motionAt = SystemClock.elapsedRealtime()
            if (parked != value) {
                parked = value
                if (!allowed()) activeDialog?.dismiss()
                if (!allowed() || !touching) render() else pendingRender = true
            }
        }.also { it.start() }
        handler.post(ticker)
        render()
    }
    override fun onPause() {
        touching = false; pendingRender = false
        handler.removeCallbacksAndMessages(null)
        monitor?.close(); monitor = null; parked = null
        super.onPause()
    }
    override fun onDestroy() { navigation?.close(); addresses.close(); activeDialog?.dismiss(); store.close(); super.onDestroy() }
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) touching = true
        val handled = super.dispatchTouchEvent(event)
        if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
            touching = false
            if (pendingRender) {
                pendingRender = false
                handler.postDelayed({ if (touching) pendingRender = true else render() }, 150)
            }
        }
        return handled
    }
    override fun onSaveInstanceState(out: Bundle) {
        out.putBoolean("demo", demo); out.putString("page", page)
        out.putLong("editing", editingCase ?: -1)
        out.putString("item", itemDraft); out.putString("memory", memoryDraft)
        out.putBoolean("allVisits", allVisits)
        out.putLong("noteVisit", noteVisit ?: -1); out.putLong("replayFrom", replayFrom ?: -1); out.putLong("filterDay", filterDay ?: -1)
        super.onSaveInstanceState(out)
    }

    private fun allowed() = parked == true || (demo && parked == null)

    private fun render() {
        val savedScroll = if (renderedPage == page) scroller?.scrollY ?: 0 else 0
        renderedPage = page
        itemInput = null
        statusLabel = null
        lastRunning = RecordingService.running
        val wide = resources.configuration.screenWidthDp >= 840
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(if (wide) 32 else 20), dp(20), dp(if (wide) 32 else 20), dp(20)); setBackgroundColor(bg)
        }
        val nav = LinearLayout(this).apply { orientation = if (wide) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, 0, 0, dp(20)) }
        val brand = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        brand.addView(ImageView(this).apply { setImageResource(R.drawable.ic_onboard); importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO },
            LinearLayout.LayoutParams(dp(48), dp(48)).apply { marginEnd = dp(16) })
        brand.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(label(getString(R.string.app_name), 30, ink, true))
            if (demo) addView(label("연습용 기록", 22, muted))
        })
        nav.addView(brand, if (wide) LinearLayout.LayoutParams(0, -2, 1f) else LinearLayout.LayoutParams(-1, -2))
        val tabs = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf("home" to "방문 기록", "cases" to "내 메모", "settings" to "기록 설정").forEachIndexed { index, (key, name) ->
            val selected = page == key || (key == "cases" && page == "edit") || (key == "home" && page == "results")
            val tab = button(name) { leaveEditor(key) }.apply {
                gravity = Gravity.CENTER
                isSelected = selected
                setTextColor(if (selected) panel else ink)
                background = RippleDrawable(ColorStateList.valueOf(getColor(R.color.outline)), shape(if (selected) ink else panel), null)
            }
            tabs.addView(tab,
                if (wide) LinearLayout.LayoutParams(dp((180 * resources.configuration.fontScale.coerceAtLeast(1f)).toInt()), -2).apply { if (index > 0) marginStart = dp(12) }
                else LinearLayout.LayoutParams(0, -2, 1f).apply { if (index > 0) marginStart = dp(8) })
        }
        nav.addView(tabs, if (wide) LinearLayout.LayoutParams(-2, -2) else LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })
        root.addView(nav, LinearLayout.LayoutParams(-1, -2))
        val scroll = ScrollView(this).apply { isFillViewport = true; background = shape(panel); clipToOutline = true }
        scroller = scroll
        scroll.post { if (scroller === scroll) scroll.scrollTo(0, savedScroll) }
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(if (wide) 32 else 20), dp(28), dp(if (wide) 32 else 20), dp(28)) }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        if (!allowed() && page != "home" && page != "settings") {
            heading(if (parked == false) "정차 후 확인해 주세요" else "정차 상태를 확인하고 있어요")
            paragraph("기어 P와 정차 신호가 확인되면 상세 조작을 사용할 수 있습니다.")
            add(button("방문 기록으로") { page = "home"; render() })
            return
        }
        when (page) { "home" -> home(); "cases" -> cases(); "edit" -> editor(); "results" -> results(); else -> settings() }
    }

    private fun home() {
        lastVisits = visitSignature()
        heading("다녀온 곳")
        val dayEnd = filterDay?.let { Calendar.getInstance().apply { timeInMillis = it; add(Calendar.DATE, 1) }.timeInMillis }
        val visits = if (allowed()) store.visits(demo).filter { visit -> filterDay?.let { day -> visit.start >= day && visit.start < dayEnd!! } ?: (visit.start >= System.currentTimeMillis() - 7 * 86_400_000L) } else emptyList()
        paragraph((filterDay?.let { SimpleDateFormat("M월 d일 EEEE", Locale.KOREA).format(Date(it)) } ?: "최근 7일") + if (allowed()) " · 방문 ${visits.size}곳" else " · 정차 후 확인")
        val horizontalDates = resources.configuration.screenWidthDp >= 840
        val days = LinearLayout(this).apply { orientation = if (horizontalDates) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL }
        listOf("오늘" to 0, "어제" to -1).forEach { (title, offset) ->
            days.addView(button(title) {
                if (allowed()) {
                    filterDay = Calendar.getInstance().apply {
                        add(Calendar.DATE, offset); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    allVisits = false; render()
                } else toast("정차 후 날짜를 선택해 주세요")
            }.apply {
                val selected = filterDay?.let { SimpleDateFormat("yyyyMMdd", Locale.KOREA).format(Date(it)) == SimpleDateFormat("yyyyMMdd", Locale.KOREA).format(Calendar.getInstance().apply { add(Calendar.DATE, offset) }.time) } == true
                isSelected = selected
                if (selected) background = RippleDrawable(ColorStateList.valueOf(getColor(R.color.outline)), shape(soft), null)
            }, LinearLayout.LayoutParams(if (horizontalDates) 0 else -1, -2, if (horizontalDates) 1f else 0f).apply {
                if (offset == -1) { if (horizontalDates) marginStart = dp(12) else topMargin = dp(12) }
            })
        }
        days.addView(button(filterDay?.let { SimpleDateFormat("M월 d일", Locale.KOREA).format(Date(it)) + " · 날짜 변경" } ?: "최근 7일 · 날짜 선택") { chooseDay() },
            LinearLayout.LayoutParams(if (horizontalDates) 0 else -1, -2, if (horizontalDates) 2f else 0f).apply { if (horizontalDates) marginStart = dp(12) else topMargin = dp(12) })
        add(days)
        if (filterDay != null) add(button("최근 7일 전체 보기") { filterDay = null; allVisits = false; render() })
        val recording = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL; setPadding(dp(18), dp(8), dp(12), dp(8)); background = shape(soft)
            val summary = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                addView(label(if (demo) "모의 기록" else if (RecordingService.running) "● 기록 중" else "기록이 멈춰 있어요", 18, ink, true))
                statusLabel = label(statusText(), 13, muted).also { addView(it) }
            }
            addView(summary, LinearLayout.LayoutParams(0, -2, 1f))
            if (!demo) addView(button(if (RecordingService.running) "기록 중지" else "기록 시작", !RecordingService.running) {
                if (RecordingService.running) startService(Intent(this@MainActivity, RecordingService::class.java).setAction(RecordingService.STOP)) else requestRecording()
                handler.postDelayed({ render() }, 500)
            }, LinearLayout.LayoutParams(dp((180 * resources.configuration.fontScale.coerceAtLeast(1f)).toInt()), -2))
        }
        if (!demo) add(recording) else statusLabel = null
        if (!demo) {
            if (checkSelfPermission("android.car.permission.CAR_SPEED") != PackageManager.PERMISSION_GRANTED) {
                add(button("차량 상태 권한 허용") {
                    requestPermissions(arrayOf("android.car.permission.CAR_SPEED"), 11)
                })
            }
        }
        section("방문 이력")
        if (!allowed()) paragraph(if (parked == false) "주행 중에는 방문 기록을 표시하지 않습니다." else "정차 신호가 확인되면 기록을 확인할 수 있습니다.")
        else {
            if (!demo) store.gapSummaries(filterDay, dayEnd).forEach { banner(it, accent) }
            if (visits.isEmpty()) card {
                addView(label(if (filterDay == null) "아직 방문 기록이 없어요" else "선택한 날짜에 기록이 없어요", 22, ink, true))
                addView(label(if (demo) "설정에서 실제 차량 기록으로 전환할 수 있어요." else "기록을 시작한 이후의 정차부터 저장됩니다.", 16, muted))
            }
            else {
                if (!allVisits && visits.size > 5) paragraph("최근 방문 5곳을 먼저 보여드려요")
                (if (allVisits) visits else visits.take(5)).forEach { visitCard(it) }
                if (visits.size > 5) add(button(if (allVisits) "최근 5곳만 보기" else "저장된 방문 더 보기 · ${visits.size}곳") {
                    allVisits = !allVisits; render()
                })
            }
        }
    }

    private fun cases() {
        heading("내 메모")
        add(button("새 메모", true) { openNote(null) })
        val visits = store.visits(demo).associateBy { it.id }
        val notes = store.notes(demo)
        if (notes.isEmpty()) paragraph("방문 중 기억할 일을 남겨보세요. 방문 상세에서도 메모할 수 있어요.")
        notes.forEach { note ->
            card {
                addView(label(note.title, 22, ink, true))
                if (note.body.isNotBlank()) addView(label(note.body, 17, muted))
                addView(label(note.visit?.let { visits[it]?.let { v -> "${time(v.start)} · ${v.placeName}" } ?: "연결된 방문 보관 기간 만료" } ?: "일반 메모", 14, muted))
                addView(button("메모 수정") { openNote(note.visit, note) }.apply { contentDescription = "메모 수정 · ${note.title}" })
                visits[note.visit]?.let { visit -> addView(button("방문 보기") { showVisit(visit) }) }
            }
        }
    }

    private fun openNote(visit: Long?, note: VisitNote? = null) {
        if (!allowed()) return
        resetDraft(); noteVisit = visit; editingCase = note?.id
        itemDraft = note?.title ?: ""; memoryDraft = note?.body ?: ""
        page = "edit"; render()
    }

    private fun editor() {
        heading(if (editingCase == null) "메모 남기기" else "메모 수정")
        noteVisit?.let { id -> paragraph(store.visits(demo).find { it.id == id }?.let { "${time(it.start)} · ${it.placeName}" } ?: "연결된 방문 보관 기간 만료") }
        section("제목")
        itemInput = field("예: 카페 방문", itemDraft, 80) { itemDraft = it; itemInput?.error = null }
        section("내용 · 선택")
        field("기억할 일을 남겨보세요", memoryDraft, 500) { memoryDraft = it }
        add(button("메모 저장", true) {
            if (!allowed()) return@button
            if (itemDraft.isBlank()) {
                itemInput?.apply { error = "메모 제목을 입력해 주세요"; requestFocus() }
                return@button
            }
            getSystemService(InputMethodManager::class.java).hideSoftInputFromWindow(itemInput?.windowToken, 0)
            store.saveNote(editingCase, noteVisit, itemDraft.trim(), memoryDraft.trim(), demo)
            resetDraft(); page = "cases"; render(); toast("메모를 저장했어요")
        })
        add(button("취소") { leaveEditor("cases") })
        editingCase?.let { id -> add(button("메모 삭제") {
            AlertDialog.Builder(this).setTitle("이 메모를 삭제할까요?")
                .setPositiveButton("삭제") { _, _ -> if (allowed()) { store.deleteNote(id); resetDraft(); page = "cases"; render() } }
                .setNegativeButton("취소", null).display()
        }) }
    }

    private fun results() {
        heading("이 시점부터 돌아보기")
        paragraph(replayFrom?.let { "${time(it)} 이후 · 시간순" } ?: "저장된 최근 방문 · 시간순")
        add(button("방문 기록으로") { page = "home"; render() })
        val visits = store.visits(demo).filter { v -> replayFrom?.let { from -> v.start >= from || v.end?.let { it > from } == true } ?: true }.sortedBy { it.start }
        if (visits.isEmpty()) paragraph("해당 시점 이후에 저장된 방문이 없어요.")
        if (!demo) store.gapSummaries(replayFrom).forEach { banner(it, accent) }
        visits.forEach { visitCard(it) }
    }

    private fun visitCard(visit: Visit) = visitRow {
        if (!visit.demo) addresses.request(visit)
        val stack = resources.configuration.screenWidthDp < 600 && resources.configuration.fontScale > 1.2f
        val header = LinearLayout(this@MainActivity).apply { gravity = Gravity.CENTER_VERTICAL; orientation = if (stack) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL }
        header.addView(LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            addView(label(clock(visit.start), 30, ink, true).apply { fontFeatureSettings = "tnum" })
            addView(label(SimpleDateFormat("M.dd", Locale.KOREA).format(Date(visit.start)), 22, muted).apply { gravity = Gravity.CENTER })
        }, LinearLayout.LayoutParams(if (stack) -2 else dp((112 * resources.configuration.fontScale.coerceAtLeast(1f)).toInt()), -2).apply { if (stack) bottomMargin = dp(12) else marginEnd = dp(20) })
        header.addView(LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.VERTICAL
            addView(label(visit.placeName, 28, ink, true))
            if (visit.address.isNotBlank() && (visit.demo || visit.label.isNotBlank())) {
                addView(label(if (visit.demo) "예시 주소 · ${visit.address}" else visit.address, 22, muted))
            }
            addView(label(if (visit.reason.contains("시각 불명")) "종료 시각 불명" else visit.end?.let {
                val minutes = ((it - visit.start) / 60_000).coerceAtLeast(0)
                if (minutes == 0L) "1분 미만 정차" else "${minutes}분 정차"
            } ?: "정차 중", 22, muted))
            addView(label(if (visit.demo) "연습용 방문" else if (visit.address.isNotBlank()) "좌표로 확인한 주소" else if (visit.latitude != null) "위치 저장됨 · 장소 이름을 붙일 수 있어요" else "위치 미확정", 22, muted))
        }, LinearLayout.LayoutParams(if (stack) -1 else 0, -2, if (stack) 0f else 1f))
        val wideActions = resources.configuration.screenWidthDp >= 1200 && resources.configuration.fontScale <= 1.3f
        val actions = LinearLayout(this@MainActivity).apply {
            orientation = if (wideActions) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL
            setPadding(0, dp(12), 0, 0)
        }
        fun actionLayout() = LinearLayout.LayoutParams(if (wideActions) 0 else -1, -2, if (wideActions) 1f else 0f).apply {
            if (actions.childCount > 0) { if (wideActions) marginStart = dp(12) else topMargin = dp(8) }
        }
        actions.addView(button("방문 보기") { if (allowed()) showVisit(visit) }
            .apply { contentDescription = "방문 보기 · ${visit.placeName}" }, actionLayout())
        actions.addView(button(if (page == "results") "메모 남기기" else "여기부터 돌아보기") {
            if (allowed()) {
                if (page == "results") openNote(visit.id)
                else { replayFrom = visit.start; page = "results"; render() }
            }
        }.apply { contentDescription = "${if (page == "results") "메모 남기기" else "여기부터 돌아보기"} · ${visit.placeName}" }, actionLayout())
        actions.addView(button(if (visit.demo) "길 안내 알아보기" else "다시 길 안내") {
            requestNavigation(visit)
        }, actionLayout())
        if (resources.configuration.screenWidthDp >= 1400 && resources.configuration.fontScale <= 1.3f) {
            val row = LinearLayout(this@MainActivity).apply { gravity = Gravity.CENTER_VERTICAL }
            row.addView(header, LinearLayout.LayoutParams(0, -2, 1f))
            row.addView(actions, LinearLayout.LayoutParams(dp(if (actions.childCount > 2) 750 else 520), -2).apply { marginStart = dp(24) })
            addView(row)
        } else { addView(header); addView(actions) }
    }

    private fun requestNavigation(visit: Visit) {
        if (!allowed()) return
        activeDialog?.dismiss()
        if (visit.demo) {
            AlertDialog.Builder(this).setTitle("연습용 길 안내")
                .setMessage("${visit.placeName}\n예시 주소: ${visit.address}\n\n가상 주소로 구성한 연습용 방문입니다. 실제 기록에서는 저장된 좌표를 Pleos 내비에 보내 길 안내를 요청합니다.")
                .setPositiveButton("확인", null).display()
            return
        }
        val latitude = visit.latitude
        val longitude = visit.longitude
        if (latitude == null || longitude == null || !latitude.isFinite() || !longitude.isFinite() ||
            latitude !in -90.0..90.0 || longitude !in -180.0..180.0) {
            AlertDialog.Builder(this).setTitle("안내할 위치가 없어요")
                .setMessage("이 방문에는 좌표가 저장되지 않았습니다. 장소 이름이나 주소만으로 경로를 요청하지 않습니다. 위치가 저장된 실제 방문을 선택해 주세요.")
                .setPositiveButton("확인", null).display()
            return
        }
        if (parked != true) { toast("정차 확인 후 사용할 수 있어요"); return }
        AlertDialog.Builder(this).setTitle("기록된 정차 위치로 안내할까요?")
            .setMessage("${visit.placeName}\n${visit.address}\n\n현재 위치에서 새 경로를 요청합니다. 기존 내비 안내가 바뀔 수 있습니다. 기록 좌표는 매장 입구나 주차장 진입점과 다를 수 있습니다.")
            .setPositiveButton("길 안내 요청") { _, _ ->
                if (parked == true && !demo) {
                    navigation?.close()
                    navigation = NavigationLink(this, { message ->
                        routeMessage = message
                        if (hasWindowFocus()) toast(message)
                    }).also {
                        it.navigate(latitude, longitude, visit.placeName, visit.address) {
                            parked == true && !demo && SystemClock.elapsedRealtime() - motionAt <= 6_000
                        }
                        it.start()
                    }
                }
            }.setNegativeButton("취소", null).display()
    }

    private fun showVisit(visit: Visit) {
        if (!allowed()) return
        val location = if (visit.demo) "연습용 방문 · 실제 수집 위치가 아닙니다" else if (visit.latitude != null && visit.longitude != null)
            String.format(Locale.US, "%.5f, %.5f · 정확도 ±%.0fm", visit.latitude, visit.longitude, visit.accuracy)
            else "위치 미확정"
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(24), dp(12), dp(24), dp(12))
            addView(label("${time(visit.start)} · ${visit.end?.let(::clock) ?: "종료 불명 또는 정차 중"}", 18, ink))
            addView(label(location, 15, muted))
            if (visit.address.isNotBlank()) addView(label(
                if (visit.demo) "예시 주소: ${visit.address}" else "좌표 기준 주소: ${visit.address}\n실제 방문한 매장이나 건물을 확인한 것은 아닙니다.", 22, muted))
            store.contextDetails(visit).forEach { addView(label(it, 15, muted)) }
            addView(button("이 시점부터 돌아보기", true) {
                if (allowed()) { activeDialog?.dismiss(); replayFrom = visit.start; page = "results"; render() }
            })
            addView(button("이 방문에 메모 남기기") { if (allowed()) { activeDialog?.dismiss(); openNote(visit.id) } })
            addView(button("이전 이동 위치 보기") {
                if (allowed()) { activeDialog?.dismiss(); AlertDialog.Builder(this@MainActivity).setTitle("이 방문까지 수집한 위치")
                    .setMessage(store.approachLocations(visit).joinToString("\n\n").ifBlank { "수집된 위치가 없습니다. 이동하지 않았다는 뜻은 아닙니다." } + "\n\n이전 정차 종료 이후의 최근 50건입니다. 이전 정차가 없으면 최대 1시간 전부터 표시합니다. 좌표를 이어 실제 도로 경로로 복원하지 않습니다.")
                    .setPositiveButton("닫기", null).display()
                }
            })
            addView(button("자세한 차량 기록 보기") {
                if (allowed()) { activeDialog?.dismiss(); AlertDialog.Builder(this@MainActivity).setTitle("수신한 차량 기록")
                    .setMessage(store.details(visit.id).joinToString("\n\n").ifBlank { "수신한 차량 기록 없음" })
                    .setPositiveButton("닫기", null).display()
                }
            })
            for (index in 0 until childCount) {
                val child = getChildAt(index)
                if (child is Button) child.layoutParams = LinearLayout.LayoutParams(-1, -2).apply {
                    topMargin = dp(16)
                }
            }
        }
        AlertDialog.Builder(this).setTitle(visit.placeName)
            .setView(ScrollView(this).apply { addView(body) })
            .setPositiveButton("닫기", null).setNeutralButton("장소 이름 붙이기") { _, _ ->
                if (allowed()) {
                    val input = EditText(this).apply {
                        textSize = 28f; setTextColor(ink); setHintTextColor(muted)
                        minHeight = buttonHeight; setPadding(dp(24), dp(16), dp(24), dp(16))
                        setSingleLine(true)
                        imeOptions = imeOptions or EditorInfo.IME_FLAG_NO_FULLSCREEN
                        setText(visit.label); hint = "내가 기억하는 장소 이름"
                    }
                    AlertDialog.Builder(this).setTitle("장소 이름").setView(input).setPositiveButton("저장") { _, _ ->
                        if (allowed()) { store.rename(visit.id, input.text.toString()); render() }
                    }.setNegativeButton("취소", null).display()
                }
            }.display()
    }

    private fun settings() {
        heading("기록 설정")
        section("화면")
        card {
            addView(label("화면 모드", 20, ink, true))
            val dark = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
            val appearance = getPreferences(0).getString("appearance", "system")
            val mode = when (appearance) { "light" -> "라이트"; "dark" -> "다크"; else -> "시스템 설정 사용" }
            addView(label("현재 ${if (dark) "다크" else "라이트"} 모드", 16, muted))
            addView(button(mode) {
                val options = object : ArrayAdapter<String>(this@MainActivity, android.R.layout.simple_list_item_single_choice, arrayOf("시스템 설정 사용", "라이트", "다크")) {
                    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View = super.getView(position, convertView, parent).apply {
                        (this as TextView).textSize = 24f
                        minimumHeight = buttonHeight; setPadding(dp(24), dp(20), dp(24), dp(20))
                    }
                }
                AlertDialog.Builder(this@MainActivity).setTitle("화면 모드")
                    .setSingleChoiceItems(options, listOf("system", "light", "dark").indexOf(appearance)) { dialog, index ->
                        getPreferences(0).edit().putString("appearance", listOf("system", "light", "dark")[index]).apply()
                        dialog.dismiss(); recreate()
                    }.setNegativeButton("취소", null).display()
            })
            addView(label("시스템 설정을 선택하면 차량 모드와 함께 바뀝니다.", 14, muted))
        }
        section("저장과 보관")
        add(button("저장 공간 절약 현황") {
            AlertDialog.Builder(this).setTitle("필요한 기록만 가볍게 저장")
                .setMessage("같은 차량 상태는 관측 횟수와 시간으로 묶습니다. 위치는 이동과 시간 변화를 보고 골라 저장합니다.\n\n${store.storageSummary()}\n\n받은 기록 수와 저장 행 수의 비교이며 파일 압축률은 아닙니다.")
                .setPositiveButton("닫기", null).display()
        })
        card {
            addView(label("내 기기에만 보관", 24, ink, true))
            addView(label("방문 기록은 7일, 메모는 직접 삭제할 때까지 보관합니다.", 22, muted))
            addView(button("보관 및 수집 안내") {
                AlertDialog.Builder(this@MainActivity).setTitle("내 기록의 보관과 수집")
                    .setMessage("기록은 기기에 보관하며 별도 앱 암호는 없습니다. 동일 차량의 같은 OS 사용자는 기록을 볼 수 있습니다.\n\n주소는 Android 시스템 또는 Pleos 내비에서 확인하며, 제공자와의 통신이 발생할 수 있습니다. 내비 주소는 기록 좌표와 100m 이내일 때만 사용합니다. 조회되지 않아도 기록한 좌표는 유지합니다.\n\n동의 후 기록 시작 이후만 수집하며, 전원 종료·강제 종료 중에는 중단될 수 있습니다. 자동 재시작하지 않습니다.\n\n전체 삭제 또는 앱 삭제 시 모든 자료를 지웁니다.")
                    .setPositiveButton("닫기", null).display()
            })
        }
        section("연습과 연결 확인")
        add(button(if (demo) "실제 차량 기록으로 전환" else "모의 기록으로 사용해 보기") { switchDemo(!demo) })
        add(button("연결 상태 확인") {
            AlertDialog.Builder(this).setTitle("수집 연결 상태").setMessage(collectionStatus())
                .setPositiveButton("닫기", null).display()
        })
        section("전체 삭제")
        paragraph("수집을 중지하고 방문 기록과 메모를 모두 지웁니다.")
        add(button("전체 기록 삭제") {
            if (!allowed()) { toast("정차 확인 후 삭제할 수 있어요"); return@button }
            AlertDialog.Builder(this).setTitle("모든 기록을 삭제할까요?")
                .setMessage("기록을 중지하고 실제·모의 방문 기록과 모든 메모를 삭제합니다. 되돌릴 수 없습니다.")
                .setPositiveButton("전체 삭제") { _, _ ->
                    if (allowed()) {
                        startService(Intent(this, RecordingService::class.java).setAction(RecordingService.DELETE))
                        resetDraft(); replayFrom = null; filterDay = null
                        handler.postDelayed({ page = "home"; render(); toast("전체 기록을 삭제했습니다") }, 500)
                    }
                }.setNegativeButton("취소", null).display()
        })
    }

    private fun requestRecording() {
        if (demo) { toast("실제 차량 기록 모드로 전환한 후 시작해 주세요"); return }
        if (parked != true) { toast("정차 신호 확인 후 기록을 시작해 주세요"); return }
        AlertDialog.Builder(this).setTitle("차량 이력을 기록할까요?")
            .setMessage("문 상태·정차·위치·차량 거리계·내비 목적지와 도착 알림을 이 기기에 기록합니다. 주소는 Android 시스템 또는 Pleos 내비에서 확인하며 제공자와의 통신이 발생할 수 있습니다. 알림에서 중지할 수 있습니다. 기록 이전이나 전원 종료 중의 이력은 복원하지 않습니다.")
            .setPositiveButton("동의하고 시작") { _, _ ->
                val permissions = mutableListOf<String>()
                if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    permissions += Manifest.permission.ACCESS_FINE_LOCATION; permissions += Manifest.permission.ACCESS_COARSE_LOCATION
                }
                if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) permissions += Manifest.permission.POST_NOTIFICATIONS
                if (permissions.isEmpty()) startRecording() else requestPermissions(permissions.toTypedArray(), 10)
            }.setNegativeButton("취소", null).display()
    }
    override fun onRequestPermissionsResult(code: Int, permissions: Array<out String>, results: IntArray) {
        super.onRequestPermissionsResult(code, permissions, results)
        if (code == 11) {
            toast(if (checkSelfPermission("android.car.permission.CAR_SPEED") == PackageManager.PERMISSION_GRANTED)
                "차량 상태 권한을 허용했습니다. 정차 신호를 확인합니다."
                else "차량 상태 권한이 없어 실제 기록의 상세 조작은 잠깁니다. 앱 설정에서 허용할 수 있어요.")
            render()
        }
        if (code == 10) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                if (parked == true) startRecording() else { pendingStart = true; toast("정차 신호를 다시 확인하고 있습니다") }
            }
            else toast("위치 권한이 없어 기록을 시작하지 않았습니다. 설정에서 권한을 허용할 수 있어요.")
        }
    }
    private fun startRecording() {
        if (parked != true) { toast("정차 상태를 다시 확인한 후 시작해 주세요"); return }
        try { startForegroundService(Intent(this, RecordingService::class.java)); handler.postDelayed({ render() }, 500) }
        catch (_: Exception) { toast("기록을 시작할 수 없습니다. 권한과 기기 상태를 확인해 주세요.") }
    }
    private fun switchDemo(value: Boolean) {
        if (parked == false) { toast("정차 후 사용할 수 있어요"); return }
        if (value && RecordingService.running) startService(Intent(this, RecordingService::class.java).setAction(RecordingService.STOP))
        if (value) store.seedDemo()
        demo = value; getPreferences(0).edit().putBoolean("demo", demo).apply()
        replayFrom = null; filterDay = null; allVisits = false; resetDraft(); page = "home"; render()
    }
    private fun chooseDay() {
        if (!allowed()) { toast("정차 확인 후 사용할 수 있어요"); return }
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val selected = (today.clone() as Calendar).apply { filterDay?.let { timeInMillis = it } }
        val month = (selected.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
        val calendarBody = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(24), dp(8), dp(24), dp(16))
        }
        fun drawCalendar() {
            calendarBody.removeAllViews()
            calendarBody.addView(label(SimpleDateFormat("M월 d일 EEEE", Locale.KOREA).format(selected.time), 30, ink, true))
            val navigation = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
            navigation.addView(button("이전 달") { month.add(Calendar.MONTH, -1); drawCalendar() },
                LinearLayout.LayoutParams(dp(160), -2))
            navigation.addView(label(SimpleDateFormat("yyyy년 M월", Locale.KOREA).format(month.time), 28, ink, true).apply {
                gravity = Gravity.CENTER
            }, LinearLayout.LayoutParams(0, -2, 1f))
            navigation.addView(button("다음 달") { month.add(Calendar.MONTH, 1); drawCalendar() }.apply {
                isEnabled = month.get(Calendar.YEAR) < today.get(Calendar.YEAR) ||
                    (month.get(Calendar.YEAR) == today.get(Calendar.YEAR) && month.get(Calendar.MONTH) < today.get(Calendar.MONTH))
                alpha = if (isEnabled) 1f else 0.35f
            }, LinearLayout.LayoutParams(dp(160), -2))
            calendarBody.addView(navigation, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12); bottomMargin = dp(12) })
            val weekdays = LinearLayout(this)
            listOf("일", "월", "화", "수", "목", "금", "토").forEach { day ->
                weekdays.addView(label(day, 24, muted).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(0, -2, 1f))
            }
            calendarBody.addView(weekdays)
            val offset = month.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY
            val count = month.getActualMaximum(Calendar.DAY_OF_MONTH)
            repeat((offset + count + 6) / 7) { week ->
                val row = LinearLayout(this)
                repeat(7) { weekday ->
                    val day = week * 7 + weekday - offset + 1
                    val cell = if (day in 1..count) {
                        val date = (month.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, day) }
                        val chosen = date.timeInMillis == selected.timeInMillis
                        button(day.toString(), chosen) {
                            selected.timeInMillis = date.timeInMillis
                            drawCalendar()
                        }.apply {
                            contentDescription = SimpleDateFormat("yyyy년 M월 d일", Locale.KOREA).format(date.time)
                            isSelected = chosen; isEnabled = date.timeInMillis <= today.timeInMillis
                            alpha = if (isEnabled) 1f else 0.35f
                            setPadding(0, dp(8), 0, dp(8))
                            maxLines = 1
                        }
                    } else View(this)
                    row.addView(cell, LinearLayout.LayoutParams(0, -2, 1f).apply {
                        marginStart = dp(4); marginEnd = dp(4)
                    })
                }
                calendarBody.addView(row, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
            }
        }
        drawCalendar()
        AlertDialog.Builder(this).setTitle("날짜 선택")
            .setView(ScrollView(this).apply { addView(calendarBody) })
            .setPositiveButton("확인") { _, _ ->
                if (allowed()) { filterDay = selected.timeInMillis; allVisits = false; render() }
            }.setNegativeButton("취소", null).display()
        activeDialog?.window?.setLayout(dp(minOf(1120, resources.configuration.screenWidthDp - 48)), -2)
        (activeDialog as? AlertDialog)?.let { dialog ->
            listOf(AlertDialog.BUTTON_NEGATIVE, AlertDialog.BUTTON_POSITIVE).forEach { which ->
                dialog.getButton(which)?.apply {
                    minWidth = dp(144)
                    setPadding(dp(24), dp(12), dp(24), dp(12))
                    val primary = which == AlertDialog.BUTTON_POSITIVE
                    setTextColor(if (primary) getColor(R.color.on_accent) else ink)
                    background = RippleDrawable(ColorStateList.valueOf(getColor(R.color.outline)),
                        shape(if (primary) accent else panel).apply {
                            if (!primary) setStroke(dp(1), getColor(R.color.outline))
                        }, null)
                    (layoutParams as? ViewGroup.MarginLayoutParams)?.let { params ->
                        params.marginStart = dp(16)
                        layoutParams = params
                    }
                }
            }
        }
    }

    private fun statusText(): String = if (page == "settings") collectionStatus() else when {
        demo -> "실제 차량 기록과 분리되어 있어요"
        checkSelfPermission("android.car.permission.CAR_SPEED") != PackageManager.PERMISSION_GRANTED -> "차량 상태 권한이 필요해요"
        parked == false -> "주행 중 · 상세 조작은 정차 후"
        parked == null -> "정차 상태 확인 중"
        RecordingService.running -> "정차 확인 · ${RecordingService.locationStatus}"
        else -> "지금 시작한 이후의 방문부터 저장해요"
    }
    private fun collectionStatus() = if (RecordingService.running)
        "문 · ${RecordingService.doorStatus}\n위치 · ${RecordingService.locationStatus}\n내비 · ${RecordingService.navigationStatus}\n거리계 · ${RecordingService.odometerStatus}"
        else "수집 중지 · 기록을 시작하면 연결 상태를 확인할 수 있어요"
    private fun leaveEditor(destination: String) {
        if (destination == page) return
        val original = editingCase?.let { id -> store.notes(demo).find { it.id == id } }
        val changed = if (original == null) itemDraft.isNotBlank() || memoryDraft.isNotBlank()
            else itemDraft != original.title || memoryDraft != original.body
        val leave = { page = destination; resetDraft(); render() }
        if (page == "edit" && changed) AlertDialog.Builder(this).setTitle("입력을 그만둘까요?")
            .setMessage("저장하지 않은 내용은 사라집니다.")
            .setPositiveButton("나가기") { _, _ -> leave() }.setNegativeButton("계속 입력", null).display()
        else leave()
    }
    private fun resetDraft() { editingCase = null; noteVisit = null; itemDraft = ""; memoryDraft = "" }
    private fun visitSignature() = store.visits(demo).take(5).joinToString { "${it.id}:${it.doors}:${it.end}:${it.latitude}:${it.label}:${it.address}" }
    private fun AlertDialog.Builder.display() {
        create().also { dialog ->
            activeDialog = dialog; dialog.show()
            dialog.findViewById<TextView>(android.R.id.message)?.apply { textSize = 22f; setLineSpacing(dp(5).toFloat(), 1.15f) }
            listOf(AlertDialog.BUTTON_POSITIVE, AlertDialog.BUTTON_NEGATIVE, AlertDialog.BUTTON_NEUTRAL).forEach { which ->
                dialog.getButton(which)?.apply { textSize = 24f; minHeight = buttonHeight; isAllCaps = false }
            }
        }
    }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun time(value: Long) = SimpleDateFormat("MM.dd HH:mm", Locale.KOREA).format(Date(value))
    private fun clock(value: Long) = SimpleDateFormat("HH:mm", Locale.KOREA).format(Date(value))
    private fun toast(message: String) { Toast.makeText(this, message, Toast.LENGTH_LONG).show() }
    private fun label(text: String, size: Int = 18, color: Int = ink, bold: Boolean = false) = TextView(this).apply {
        this.text = text; textSize = maxOf(size.toFloat(), 22f); setTextColor(color); setPadding(0, dp(8), 0, dp(12))
        includeFontPadding = false
        setLineSpacing(dp(3).toFloat(), 1.1f)
        if (bold) typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }
    private fun button(text: String, primary: Boolean = false, action: () -> Unit) = Button(this).apply {
        this.text = text; textSize = 24f; isAllCaps = false; minHeight = buttonHeight
        setPadding(dp(20), dp(8), dp(20), dp(8)); setTextColor(if (primary) getColor(R.color.on_accent) else ink)
        backgroundTintList = null
        val surface = shape(if (primary) accent else panel).apply { if (!primary) setStroke(dp(1), getColor(R.color.outline)) }
        background = RippleDrawable(ColorStateList.valueOf(getColor(R.color.outline)), surface, null)
        stateListAnimator = null
        setOnClickListener { action() }
    }
    private fun add(view: View) { content.addView(view, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(16) }) }
    private fun heading(text: String) { add(label(text, 38, ink, true)) }
    private fun section(text: String) { add(label(text, 20, ink, true).apply { setPadding(0, dp(16), 0, dp(4)) }) }
    private fun paragraph(text: String) { add(label(text, 17, muted)) }
    private fun banner(text: String, color: Int) { add(label(text, 16, color).apply { setPadding(dp(16), dp(12), dp(16), dp(12)); background = shape(panel) }) }
    private fun card(body: LinearLayout.() -> Unit) { add(LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(24), dp(20), dp(24), dp(20)); background = shape(bg); body() }) }
    private fun visitRow(body: LinearLayout.() -> Unit) { add(LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, dp(12), 0, dp(20)); body() }) }
    private fun shape(color: Int) = GradientDrawable().apply { setColor(color); cornerRadius = dp(8).toFloat() }
    private fun field(hint: String, initial: String, limit: Int, changed: (String) -> Unit): EditText {
        val input = EditText(this).apply {
            this.hint = hint; textSize = 28f; setTextColor(ink); setHintTextColor(muted)
            background = shape(panel).apply { setStroke(dp(1), getColor(R.color.outline)) }
            setPadding(dp(20), dp(16), dp(20), dp(16))
            minHeight = buttonHeight; setSingleLine(false); setText(initial); filters = arrayOf(android.text.InputFilter.LengthFilter(limit))
            imeOptions = imeOptions or EditorInfo.IME_FLAG_NO_FULLSCREEN
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { changed(s.toString()) }
                override fun afterTextChanged(s: Editable?) = Unit
            })
        }
        add(input)
        return input
    }
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        when (page) {
            "edit" -> leaveEditor("cases")
            "results" -> leaveEditor("home")
            "cases", "settings" -> leaveEditor("home")
            else -> super.onBackPressed()
        }
    }
}
