package dev.onboard.app

import org.junit.Assert.*
import org.junit.Test

class CoverageTrackerTest {
    @Test fun movingWithoutFixProducesOneGrowingGap() {
        val tracker = CoverageTracker()
        assertNull(tracker.sample(100f, 0, 1000, null))
        val first = tracker.sample(100.2f, 20_000, 21_000, null)!!
        val next = tracker.sample(100.4f, 40_000, 41_000, null)!!
        assertEquals(first.startElapsed, next.startElapsed)
        assertTrue(next.distanceKm > first.distanceKm)
    }
    @Test fun freshCoordinatesAndStationaryCounterProduceNoGap() {
        val tracker = CoverageTracker()
        tracker.sample(100f, 0, 1000, 0)
        assertNull(tracker.sample(101f, 20_000, 21_000, 20_000))
        assertNull(tracker.sample(101f, 40_000, 41_000, null))
    }
    @Test fun resetUnknownCounterAndLongInterruptionsDoNotInventDistance() {
        val tracker = CoverageTracker()
        tracker.sample(100f, 0, 1000, null)
        assertNull(tracker.sample(99f, 20_000, 21_000, null))
        assertNull(tracker.sample(null, 40_000, 41_000, null))
        assertNull(tracker.sample(200f, 60_000, 61_000, null))
        assertNull(tracker.sample(201f, 200_000, 201_000, null))
    }
}
