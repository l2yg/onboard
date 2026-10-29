package dev.onboard.app

import org.junit.Assert.*
import org.junit.Test

class LocationStoragePolicyTest {
    @Test fun stationaryFixesKeepOneMinuteRepresentatives() {
        val policy = LocationStoragePolicy()
        assertTrue(policy.keep(1, null, 0, 37.0, 127.0, 5f, "gps"))
        assertFalse(policy.keep(1, null, 5_000, 37.00001, 127.0, 5f, "gps"))
        assertTrue(policy.keep(1, null, 60_000, 37.0, 127.0, 5f, "gps"))
    }
    @Test fun movementKeepsTimeAndDistanceChanges() {
        val policy = LocationStoragePolicy()
        policy.keep(1, null, 0, 37.0, 127.0, 5f, "gps")
        assertFalse(policy.keep(1, null, 5_000, 37.0003, 127.0, 5f, "gps"))
        assertTrue(policy.keep(1, null, 15_000, 37.0003, 127.0, 5f, "gps"))
        assertTrue(policy.keep(1, null, 20_000, 37.001, 127.0, 5f, "gps"))
    }
    @Test fun visitSessionProviderAndAccuracyChangesAlwaysRemain() {
        val policy = LocationStoragePolicy()
        policy.keep(1, null, 0, 37.0, 127.0, 5f, "gps")
        assertTrue(policy.keep(1, 1, 1_000, 37.0, 127.0, 5f, "gps"))
        assertTrue(policy.keep(2, 1, 2_000, 37.0, 127.0, 5f, "gps"))
        assertTrue(policy.keep(2, 1, 3_000, 37.0, 127.0, 5f, "network"))
        assertTrue(policy.keep(2, 1, 4_000, 37.0, 127.0, null, "network"))
    }
    @Test fun invalidCoordinatesAreNotSavedAndClockResetStartsAgain() {
        val policy = LocationStoragePolicy()
        assertFalse(policy.keep(1, null, 0, Double.NaN, 127.0, 5f, "gps"))
        assertFalse(policy.keep(1, null, 0, 91.0, 127.0, 5f, "gps"))
        assertTrue(policy.keep(1, null, 20_000, 37.0, 127.0, 5f, "gps"))
        assertTrue(policy.keep(1, null, 1_000, 37.0, 127.0, 5f, "gps"))
    }
}
