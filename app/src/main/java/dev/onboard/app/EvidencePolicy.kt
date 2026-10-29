package dev.onboard.app

object EvidencePolicy {
    fun transition(previous: Int?, value: Int?, minimum: Int, maximum: Int): String = when {
        value == null || value !in minimum..maximum -> "unavailable"
        previous == null -> "baseline"
        previous == value -> "repeat"
        else -> "transition"
    }

    fun locationUsable(nowMs: Long, measuredMs: Long, accuracy: Float): Boolean =
        nowMs - measuredMs in 0..60_000 && accuracy.isFinite() && accuracy in 0f..100f

    fun parked(speed: Float?, gear: Int?): Boolean? = when {
        speed == null || !speed.isFinite() || gear == null -> null
        else -> kotlin.math.abs(speed) < 0.1f && gear == 4
    }

    fun memoryRelation(visitTime: Long, anchor: Long?, certain: Boolean): String = when {
        anchor == null -> "마지막 확인 시점 모름"
        visitTime >= anchor -> "마지막 기억 이후"
        certain -> "마지막 기억 이전 · 비교용으로 유지"
        else -> "불확실한 기억 이전 · 함께 확인"
    }
}
