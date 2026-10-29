package dev.onboard.app

import org.junit.Assert.*
import org.junit.Test

class EvidencePolicyTest {
    @Test fun firstValueAndRepeatsAreNotMovementEvidence() {
        assertEquals("baseline", EvidencePolicy.transition(null, 1, 0, 1))
        assertEquals("repeat", EvidencePolicy.transition(1, 1, 0, 1))
        assertEquals("transition", EvidencePolicy.transition(0, 1, 0, 1))
    }
    @Test fun unavailableAndOutOfRangeMustNotBecomeTransitions() {
        assertEquals("unavailable", EvidencePolicy.transition(0, null, 0, 1))
        assertEquals("unavailable", EvidencePolicy.transition(0, 4, 0, 1))
    }
    @Test fun staleFutureAndInaccurateLocationsAreRejected() {
        assertTrue(EvidencePolicy.locationUsable(70_000, 20_000, 30f))
        assertFalse(EvidencePolicy.locationUsable(70_000, 1_000, 30f))
        assertFalse(EvidencePolicy.locationUsable(70_000, 71_000, 30f))
        assertFalse(EvidencePolicy.locationUsable(70_000, 20_000, 101f))
        assertFalse(EvidencePolicy.locationUsable(70_000, 20_000, Float.NaN))
    }
    @Test fun parkedRequiresBothAvailableSpeedAndParkGear() {
        assertEquals(true, EvidencePolicy.parked(0f, 4))
        assertEquals(false, EvidencePolicy.parked(2f, 4))
        assertEquals(false, EvidencePolicy.parked(0f, 8))
        assertNull(EvidencePolicy.parked(null, 4))
        assertNull(EvidencePolicy.parked(Float.NaN, 4))
    }
    @Test fun memoryBeforeAnchorIsRetainedEvenWhenCertain() {
        assertTrue(EvidencePolicy.memoryRelation(1, 2, true).contains("유지"))
        assertTrue(EvidencePolicy.memoryRelation(1, 2, false).contains("함께 확인"))
        assertEquals("마지막 기억 이후", EvidencePolicy.memoryRelation(3, 2, true))
        assertEquals("마지막 확인 시점 모름", EvidencePolicy.memoryRelation(3, null, true))
    }
}
