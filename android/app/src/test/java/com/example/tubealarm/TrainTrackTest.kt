package com.example.tubealarm

import org.junit.Assert.assertEquals
import org.junit.Test

class TrainTrackTest {
    private val brixtonToBank = listOf(
        Leg("victoria", listOf("Brixton", "Stockwell")),
        Leg("northern", listOf("Stockwell", "Oval", "Kennington", "Bank")),
    )

    @Test
    fun countsTheChangeStationOnce() {
        // Brixton 0, Stockwell 1, Oval 2
        assertEquals(2, trainIndex(brixtonToBank, "Oval"))
    }

    @Test
    fun ignoresCase() {
        assertEquals(4, trainIndex(brixtonToBank, "bank"))
    }

    @Test
    fun nowhereIfNotOnTheRoute() {
        assertEquals(null, trainIndex(brixtonToBank, "Victoria"))
    }
}
