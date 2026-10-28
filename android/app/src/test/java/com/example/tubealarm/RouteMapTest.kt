package com.example.tubealarm

import org.junit.Assert.assertEquals
import org.junit.Test

class RouteMapTest {
    private val brixtonToBank = listOf(
        Leg("victoria", listOf("Brixton", "Stockwell")),
        Leg("northern", listOf("Stockwell", "Oval", "Kennington", "Bank")),
    )

    @Test
    fun changeStationsOnlyAppearOnce() {
        val names = routeStops(brixtonToBank).map { it.name }
        assertEquals(listOf("Brixton", "Stockwell", "Oval", "Kennington", "Bank"), names)
    }

    @Test
    fun changeHasALineEachSide() {
        val stockwell = routeStops(brixtonToBank)[1]
        assertEquals("victoria", stockwell.lineAbove)
        assertEquals("northern", stockwell.lineBelow)
        assertEquals(false, stockwell.middle)
    }

    @Test
    fun endsOnlyHaveOneSide() {
        val stops = routeStops(brixtonToBank)
        assertEquals(null, stops.first().lineAbove)
        assertEquals("victoria", stops.first().lineBelow)
        assertEquals("northern", stops.last().lineAbove)
        assertEquals(null, stops.last().lineBelow)
    }

    @Test
    fun middleStopsBelongToTheirLeg() {
        val middles = routeStops(brixtonToBank).filter { it.middle }
        assertEquals(listOf("Oval", "Kennington"), middles.map { it.name })
        assertEquals(listOf(1, 1), middles.map { it.leg })
    }
}
