package dev.onboard.app

data class CoverageGap(val started: Long, val ended: Long, val startElapsed: Long, val distanceKm: Float)

/** Counter evidence of insufficient positions, never a reconstruction of the missing road. */
class CoverageTracker {
    private data class Sample(val km: Float, val elapsed: Long, val wall: Long)
    private var previous: Sample? = null
    private var missingFrom: Sample? = null

    fun sample(kilometers: Float?, elapsedMs: Long, wallMs: Long, lastGoodFixMs: Long?): CoverageGap? {
        if (kilometers == null || !kilometers.isFinite() || kilometers < 0) {
            previous = null; missingFrom = null
            return null
        }
        val current = Sample(kilometers, elapsedMs, wallMs)
        val before = previous
        previous = current
        if (before == null) return null
        if (elapsedMs <= before.elapsed || elapsedMs - before.elapsed > 60_000 ||
            kilometers < before.km || kilometers - before.km > 100 || wallMs < before.wall) {
            missingFrom = null
            return null
        }
        val fresh = lastGoodFixMs != null && elapsedMs - lastGoodFixMs in 0..15_000
        if (!fresh && missingFrom == null) missingFrom = before
        val start = missingFrom ?: return null
        val end = if (fresh) before else current
        val result = if (end.elapsed - start.elapsed >= 15_000 && end.km - start.km >= 0.1f)
            CoverageGap(start.wall, end.wall, start.elapsed, end.km - start.km) else null
        if (fresh) missingFrom = null
        return result
    }
}
