package dev.onboard.app

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.CheckBox
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
    private val bg = Color.rgb(16, 29, 36)
    private val panel = Color.rgb(28, 44, 52)
    private val accent = Color.rgb(156, 230, 197)
    private val muted = Color.rgb(173, 190, 196)
    private lateinit var store: HistoryStore
    private lateinit var content: LinearLayout
    private val handler = Handler(Looper.getMainLooper())
    private var monitor: MotionMonitor? = null
    private var parked: Boolean? = null
    private var motionAt = 0L
    private var demo = false
    private var page = "home"
    private var selectedCase: Long? = null
    private var editingCase: Long? = null
    private var itemDraft = ""
    private var memoryDraft = ""
    private var areaDraft = ""
    private var anchorDraft: Long? = null
    private var certainDraft = false
    private var statusLabel: TextView? = null
    private var activeDialog: Dialog? = null
    private var lastRunning = false
    private var pendingStart = false
    private var lastVisits = ""
    private val doorOptions = linkedMapOf("" to "모름 · 모든 문 기록 보기", "ROW_1_LEFT" to "앞줄 왼쪽 문",
        "ROW_1_RIGHT" to "앞줄 오른쪽 문", "ROW_2_LEFT" to "뒷줄 왼쪽 문", "ROW_2_RIGHT" to "뒷줄 오른쪽 문")
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
            handler.postDelayed(this, 1_000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = HistoryStore(this)
        demo = savedInstanceState?.getBoolean("demo") ?: getPreferences(0).getBoolean("demo", false)
        page = savedInstanceState?.getString("page") ?: "home"
        selectedCase = savedInstanceState?.getLong("selected", -1)?.takeIf { it >= 0 }
        editingCase = savedInstanceState?.getLong("editing", -1)?.takeIf { it >= 0 }
        itemDraft = savedInstanceState?.getString("item") ?: ""
        memoryDraft = savedInstanceState?.getString("memory") ?: ""
        areaDraft = savedInstanceState?.getString("area") ?: ""
        anchorDraft = savedInstanceState?.getLong("anchor", -1)?.takeIf { it >= 0 }
        certainDraft = savedInstanceState?.getBoolean("certain") ?: false
        render()
    }

    override fun onResume() {
        super.onResume()
        parked = null
        monitor = MotionMonitor(this) { value ->
            motionAt = SystemClock.elapsedRealtime()
            if (parked != value) {
                parked = value
                if (!allowed()) activeDialog?.dismiss()
                render()
            }
        }.also { it.start() }
        handler.post(ticker)
        render()
    }
    override fun onPause() {
        handler.removeCallbacksAndMessages(null)
        monitor?.close(); monitor = null; parked = null
        super.onPause()
    }
    override fun onDestroy() { activeDialog?.dismiss(); store.close(); super.onDestroy() }
    override fun onSaveInstanceState(out: Bundle) {
        out.putBoolean("demo", demo); out.putString("page", page)
        out.putLong("selected", selectedCase ?: -1); out.putLong("editing", editingCase ?: -1)
        out.putString("item", itemDraft); out.putString("memory", memoryDraft); out.putString("area", areaDraft)
        out.putLong("anchor", anchorDraft ?: -1); out.putBoolean("certain", certainDraft)
        super.onSaveInstanceState(out)
    }

    private fun allowed() = parked == true || (demo && parked == null)

    private fun render() {
        statusLabel = null
        lastRunning = RecordingService.running
        val wide = resources.configuration.screenWidthDp >= 840
        val root = LinearLayout(this).apply {
            orientation = if (wide) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL
            setPadding(dp(24), dp(20), dp(24), dp(16)); setBackgroundColor(bg)
        }
        val nav = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, 0, dp(if (wide) 24 else 0), dp(12)) }
        nav.addView(label("ONBOARD", 22, accent, true))
        nav.addView(label(if (demo) "시연 모드 · 모의 기록" else "분실물 역추적", 14, muted))
        val tabs = LinearLayout(this).apply { orientation = if (wide) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL }
        listOf("home" to "오늘의 기록", "cases" to "물건 찾기", "settings" to "기록 설정").forEach { (key, name) ->
            tabs.addView(button(name, page == key) { page = key; render() },
                if (wide) LinearLayout.LayoutParams(-1, dp(58)).apply { topMargin = dp(10) }
                else LinearLayout.LayoutParams(0, dp(56), 1f).apply { marginEnd = dp(8) })
        }
        nav.addView(tabs)
        root.addView(nav, if (wide) LinearLayout.LayoutParams(dp(220), -1) else LinearLayout.LayoutParams(-1, -2))
        val scroll = ScrollView(this).apply { isFillViewport = true }
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(4), 0, dp(4), dp(24)) }
        scroll.addView(content)
        root.addView(scroll, if (wide) LinearLayout.LayoutParams(0, -1, 1f) else LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        if (demo) banner("시연용 가상 기록입니다. 실제 차량 기록과 섞이지 않습니다.", accent)
        if (!allowed() && page != "home" && page != "settings") {
            heading(if (parked == false) "정차 후 확인해 주세요" else "정차 상태를 확인하고 있어요")
            paragraph("기어 P와 정차 신호가 확인되면 상세 조작을 사용할 수 있습니다.")
            add(button("오늘의 기록으로") { page = "home"; render() })
            return
        }
        when (page) { "home" -> home(); "cases" -> cases(); "edit" -> editor(); "results" -> results(); else -> settings() }
    }

    private fun home() {
        lastVisits = visitSignature()
        heading("어디서 마지막으로\n보셨나요?")
        paragraph("기억과 차량 기록을 나란히 보고, 다시 확인할 곳을 정리해 보세요.")
        card {
            addView(label(if (RecordingService.running) "● 차량 이력 기록 중" else "○ 차량 이력 기록 중지", 20, accent, true))
            statusLabel = label(statusText(), 16, muted).also { addView(it) }
            if (checkSelfPermission("android.car.permission.CAR_SPEED") != PackageManager.PERMISSION_GRANTED) {
                addView(button("차량 상태 권한 허용") {
                    requestPermissions(arrayOf("android.car.permission.CAR_SPEED"), 11)
                })
            }
            addView(button(if (RecordingService.running) "기록 중지" else "기록 시작", true) {
                if (RecordingService.running) startService(Intent(this@MainActivity, RecordingService::class.java).setAction(RecordingService.STOP)) else requestRecording()
                handler.postDelayed({ render() }, 500)
            })
        }
        add(button("물건 찾기 시작", true) {
            if (allowed()) { resetDraft(); page = "edit"; render() } else toast("정차 신호 확인 후 사용할 수 있어요")
        })
        section("최근 방문 기록")
        if (!allowed()) paragraph(if (parked == false) "주행 중에는 방문 기록을 표시하지 않습니다." else "정차 신호가 확인되면 기록을 확인할 수 있습니다.")
        else {
            val visits = store.visits(demo)
            if (visits.isEmpty()) paragraph("아직 기록이 없어요. 기록 시작 이후의 정차 구간부터 남습니다.")
            visits.take(5).forEach { visitCard(it, null) }
        }
        if (!demo && parked != false) add(button("모의 기록으로 사용해 보기") { switchDemo(true) })
    }

    private fun cases() {
        heading("찾고 있는 물건")
        paragraph("확인 결과와 기억을 저장해 두고 이어서 찾아보세요.")
        add(button("새 물건 찾기", true) { resetDraft(); page = "edit"; render() })
        val cases = store.cases(demo)
        if (cases.isEmpty()) paragraph("진행 중인 탐색이 없어요. 물건 이름만으로도 시작할 수 있습니다.")
        cases.forEach { search -> card {
            addView(label(search.item, 25, Color.WHITE, true))
            addView(label("${search.state} · ${search.anchor?.let(::time) ?: "마지막 시점 모름"}", 16, muted))
            addView(button("복기 이어보기") { selectedCase = search.id; page = "results"; render() })
        } }
    }

    private fun editor() {
        heading(if (editingCase == null) "마지막 기억을 남겨주세요" else "기억 수정하기")
        paragraph("정확히 기억나지 않아도 괜찮아요. 물건 위치를 미리 등록할 필요는 없습니다.")
        section("어떤 물건을 찾으세요?")
        field("예: 지갑, 안경, 서류", itemDraft, 80) { itemDraft = it }
        section("마지막으로 본 시점")
        add(button(anchorDraft?.let(::time) ?: "모름 · 시점을 선택하려면 누르세요") { chooseAnchor() })
        add(CheckBox(this).apply {
            text = "이 시점에 물건을 본 것이 확실해요"; textSize = 18f; setTextColor(Color.WHITE)
            isChecked = certainDraft; minHeight = dp(56)
            setOnCheckedChangeListener { _, checked -> certainDraft = checked }
        })
        section("장소나 상황에 대한 기억")
        field("예: 마트 계산대에서 지갑을 꺼냈어요", memoryDraft, 300) { memoryDraft = it }
        section("관련된 문 · 선택")
        store.observedDoorAreas().forEach { if (it !in doorOptions) doorOptions[it] = "$it · 차량 제공 영역" }
        add(button(doorOptions[areaDraft] ?: "관련 문: $areaDraft") {
            AlertDialog.Builder(this).setTitle("기억과 관련된 문")
                .setItems(doorOptions.values.toTypedArray()) { _, index ->
                    areaDraft = doorOptions.keys.elementAt(index); render()
                }.display()
        })
        paragraph("문 개방만으로 분실 장소를 정하지 않습니다. 기억 이전의 기록도 비교할 수 있게 남겨둡니다.")
        add(button("방문 기록과 대조하기", true) {
            if (!allowed()) return@button
            if (itemDraft.isBlank()) { toast("찾는 물건 이름을 입력해 주세요"); return@button }
            selectedCase = store.saveCase(editingCase, itemDraft.trim(), anchorDraft, certainDraft, memoryDraft.trim(), areaDraft.trim(), demo)
            page = "results"; render()
        })
        add(button("취소") { page = "cases"; render() })
    }

    private fun results() {
        val search = store.cases(demo).find { it.id == selectedCase }
        if (search == null) { page = "cases"; cases(); return }
        heading("${search.item}\n확인할 곳을 정리해요")
        paragraph("${search.state} · ${search.anchor?.let(::time) ?: "마지막 확인 시점 모름"}")
        if (search.memory.isNotBlank()) banner(search.memory, muted)
        if (search.area.isNotBlank()) paragraph("관련 문에 대한 기억: ${doorOptions[search.area] ?: search.area}")
        paragraph("최근 방문순입니다. 분실 가능성 순위가 아닙니다. 차내 잔류와 미수집 구간도 함께 확인해 주세요.")
        add(button("기억 수정") {
            editingCase = search.id; itemDraft = search.item; anchorDraft = search.anchor
            certainDraft = search.certain; memoryDraft = search.memory; areaDraft = search.area; page = "edit"; render()
        })
        val visits = store.visits(demo)
        if (visits.isEmpty()) paragraph("대조할 방문 기록이 없습니다. 기록 시작 전의 이력은 복원할 수 없어요.")
        else paragraph("저장된 최근 방문 최대 200개를 표시합니다.")
        visits.forEach { paragraph(EvidencePolicy.memoryRelation(it.start, search.anchor, search.certain)); visitCard(it, search) }
        if (search.state == "탐색 중") {
            add(button("물건을 찾았어요", true) { finish(search, "찾음") })
            add(button("못 찾았지만 탐색 종료") { finish(search, "미발견 종료") })
        }
        add(button("탐색 목록으로") { page = "cases"; render() })
    }

    private fun visitCard(visit: Visit, search: SearchCase?) = card {
        addView(label(visit.label.ifBlank { "정차 기록 · 장소명 없음" }, 22, Color.WHITE, true))
        addView(label("${time(visit.start)} · ${if (visit.reason.contains("시각 불명")) "종료 시각 불명" else visit.end?.let { "종료 ${clock(it)}" } ?: "관측 중"}", 16, muted))
        addView(label(if (visit.latitude != null && visit.longitude != null)
            String.format(Locale.US, "%.5f, %.5f · 당시 정확도 ±%.0fm", visit.latitude, visit.longitude, visit.accuracy)
            else "위치 미확정 · 실내나 상점 위치를 알 수 없어요", 16, muted))
        addView(label("문 상태 변경 ${visit.doors}건 · 물건 이동의 증거는 아닙니다", 16, accent))
        if (search != null && search.area.isNotBlank()) {
            val related = store.relatedDoorCount(visit.id, search.area)
            addView(label(if (related > 0) "기억과 관련된 문: 상태 변경 ${related}건 관측"
                else "관련 문 변경 기록 없음 · 확인 대상에서 제외하지 않음", 17, accent))
        }
        if (visit.reason.isNotBlank()) addView(label(visit.reason, 14, muted))
        search?.let { store.checks(it.id)[visit.id]?.let { result -> addView(label("확인 결과: $result", 18, accent)) } }
        addView(button("상세 기록 보기") { if (allowed()) showVisit(visit, search) })
    }

    private fun showVisit(visit: Visit, search: SearchCase?) {
        val details = store.details(visit.id).joinToString("\n\n").ifBlank { "수신한 문 기록 없음. 문이 열리지 않았다는 뜻은 아닙니다." }
        AlertDialog.Builder(this).setTitle(visit.label.ifBlank { time(visit.start) })
            .setMessage("$details\n\n문 값은 차량 원시 상태입니다. 미수집 구간은 별도 확인이 필요합니다.")
            .setPositiveButton(if (search == null) "닫기" else "확인 결과 남기기") { _, _ ->
                if (allowed() && search != null) AlertDialog.Builder(this).setTitle("확인 결과")
                    .setItems(arrayOf("확인했지만 못 찾음", "이곳에서 찾음", "아직 확인하지 않음")) { _, which ->
                        if (allowed()) {
                            store.check(search.id, visit.id, listOf("확인했지만 못 찾음", "이곳에서 찾음", "아직 확인하지 않음")[which])
                            if (which == 1) store.finishCase(search.id, "찾음")
                            render()
                        }
                    }.display()
            }.setNeutralButton("장소 이름 붙이기") { _, _ ->
                if (allowed()) {
                    val input = EditText(this).apply { setText(visit.label); hint = "내가 기억하는 장소 이름" }
                    AlertDialog.Builder(this).setTitle("장소 이름").setView(input).setPositiveButton("저장") { _, _ ->
                        if (allowed()) { store.rename(visit.id, input.text.toString()); render() }
                    }.setNegativeButton("취소", null).display()
                }
            }.setNegativeButton("돌아가기", null).display()
    }

    private fun finish(search: SearchCase, state: String) {
        AlertDialog.Builder(this).setTitle(if (state == "찾음") "물건을 찾으셨나요?" else "탐색을 종료할까요?")
            .setMessage("확인 결과는 이 기기에 저장됩니다.").setPositiveButton("저장") { _, _ ->
                if (allowed()) { store.finishCase(search.id, state); render() }
            }.setNegativeButton("취소", null).display()
    }

    private fun settings() {
        heading("내 기록, 내 기기에")
        paragraph("서버로 전송하지 않습니다. 같은 차량의 다른 사용자가 기록을 볼 수 있으므로 공유 차량에서는 기록과 삭제에 유의해 주세요.")
        section("보관과 수집")
        paragraph("차량·위치 원시 기록과 방문 이력은 7일간 보관하며 다음 기록 시작 시 오래된 자료를 삭제합니다. 탐색 메모·결과는 전체 삭제 전까지 남습니다. 앱 삭제 시 모두 삭제됩니다. 기기 기본 보안 저장소를 사용하며 별도 앱 암호는 제공하지 않습니다.")
        paragraph("동의 후 기록을 시작한 이후만 수집합니다. 전원 종료·앱 강제 종료 동안에는 중단될 수 있습니다. 자동 재시작하지 않으며 다시 시작하면 중단 구간을 구분합니다.")
        add(button(if (demo) "실제 차량 기록으로 전환" else "모의 기록으로 시연하기") { switchDemo(!demo) })
        section("기록 삭제")
        add(button("전체 기록 삭제") {
            if (!allowed()) { toast("정차 확인 후 삭제할 수 있어요"); return@button }
            AlertDialog.Builder(this).setTitle("모든 기록을 삭제할까요?")
                .setMessage("기록을 중지하고 실제·모의 방문 이력, 탐색 메모와 결과를 삭제합니다. 되돌릴 수 없습니다.")
                .setPositiveButton("전체 삭제") { _, _ ->
                    if (allowed()) {
                        startService(Intent(this, RecordingService::class.java).setAction(RecordingService.DELETE))
                        resetDraft(); selectedCase = null
                        handler.postDelayed({ page = "home"; render(); toast("전체 기록을 삭제했습니다") }, 500)
                    }
                }.setNegativeButton("취소", null).display()
        })
    }

    private fun requestRecording() {
        if (demo) { toast("실제 차량 기록 모드로 전환한 후 시작해 주세요"); return }
        if (parked != true) { toast("정차 신호 확인 후 기록을 시작해 주세요"); return }
        AlertDialog.Builder(this).setTitle("차량 이력을 기록할까요?")
            .setMessage("문 상태·정차·위치를 이 기기에 기록합니다. 알림에서 중지할 수 있습니다. 기록 이전이나 전원 종료 중의 이력은 복원하지 않습니다.")
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
        selectedCase = null; resetDraft(); page = "home"; render()
    }
    private fun chooseAnchor() {
        val visits = store.visits(demo)
        val options = arrayOf("모름", "직접 날짜와 시각 선택") + visits.map { "${time(it.start)} · ${it.label.ifBlank { "정차 기록" }}" }
        AlertDialog.Builder(this).setTitle("마지막으로 본 시점").setItems(options) { _, which ->
            when (which) {
                0 -> { anchorDraft = null; render() }
                1 -> {
                    val cal = Calendar.getInstance()
                    DatePickerDialog(this, { _, y, m, d ->
                        cal.set(y, m, d)
                        TimePickerDialog(this, { _, hour, minute ->
                            cal.set(Calendar.HOUR_OF_DAY, hour); cal.set(Calendar.MINUTE, minute); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                            if (cal.timeInMillis > System.currentTimeMillis()) toast("미래 시각은 선택할 수 없어요") else anchorDraft = cal.timeInMillis
                            render()
                        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).also { activeDialog = it }.show()
                    }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).also { activeDialog = it }.show()
                }
                else -> { anchorDraft = visits[which - 2].start; render() }
            }
        }.display()
    }

    private fun statusText(): String = if (checkSelfPermission("android.car.permission.CAR_SPEED") != PackageManager.PERMISSION_GRANTED)
        "차량 상태 권한을 허용하면 정차를 확인할 수 있습니다."
        else if (RecordingService.running)
        "${RecordingService.doorStatus}\n${RecordingService.locationStatus}\n${if (parked == true) "정차 확인" else "정차 신호 확인 필요"}"
        else "기록 시작 이후의 이력만 남습니다.\n${if (parked == true) "기어 P · 정차 확인" else "정차 신호 확인 필요"}"
    private fun resetDraft() { editingCase = null; itemDraft = ""; memoryDraft = ""; areaDraft = ""; anchorDraft = null; certainDraft = false }
    private fun visitSignature() = store.visits(demo).take(5).joinToString { "${it.id}:${it.doors}:${it.end}:${it.latitude}:${it.label}" }
    private fun AlertDialog.Builder.display() { create().also { activeDialog = it }.show() }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun time(value: Long) = SimpleDateFormat("MM.dd HH:mm", Locale.KOREA).format(Date(value))
    private fun clock(value: Long) = SimpleDateFormat("HH:mm", Locale.KOREA).format(Date(value))
    private fun toast(message: String) { Toast.makeText(this, message, Toast.LENGTH_LONG).show() }
    private fun label(text: String, size: Int = 18, color: Int = Color.WHITE, bold: Boolean = false) = TextView(this).apply {
        this.text = text; textSize = size.toFloat(); setTextColor(color); setPadding(0, dp(6), 0, dp(10))
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }
    private fun button(text: String, primary: Boolean = false, action: () -> Unit) = Button(this).apply {
        this.text = text; textSize = 18f; isAllCaps = false; minHeight = dp(56)
        setPadding(dp(16), dp(8), dp(16), dp(8)); setTextColor(if (primary) bg else Color.WHITE)
        backgroundTintList = ColorStateList.valueOf(if (primary) accent else Color.rgb(43, 61, 70))
        setOnClickListener { action() }
    }
    private fun add(view: View) { content.addView(view, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) }) }
    private fun heading(text: String) { add(label(text, 32, Color.WHITE, true)) }
    private fun section(text: String) { add(label(text, 20, accent, true)) }
    private fun paragraph(text: String) { add(label(text, 17, muted)) }
    private fun banner(text: String, color: Int) { add(label(text, 16, color).apply { setPadding(dp(16), dp(12), dp(16), dp(12)); background = shape(panel) }) }
    private fun card(body: LinearLayout.() -> Unit) { add(LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(16), dp(20), dp(16)); background = shape(panel); body() }) }
    private fun shape(color: Int) = GradientDrawable().apply { setColor(color); cornerRadius = dp(16).toFloat() }
    private fun field(hint: String, initial: String, limit: Int, changed: (String) -> Unit) {
        add(EditText(this).apply {
            this.hint = hint; textSize = 20f; setTextColor(Color.WHITE); setHintTextColor(muted)
            minHeight = dp(60); setSingleLine(false); setText(initial); filters = arrayOf(android.text.InputFilter.LengthFilter(limit))
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { changed(s.toString()) }
                override fun afterTextChanged(s: Editable?) = Unit
            })
        })
    }
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() { if (page != "home") { page = "home"; render() } else super.onBackPressed() }
}
