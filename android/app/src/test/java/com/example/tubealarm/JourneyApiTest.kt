package com.example.tubealarm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JourneyApiTest {

    // What the backend sends for Brixton to Bank, cut down a little
    private val brixtonToBank = """
        {
          "start": "Brixton", "end": "Bank", "minutes": 17,
          "legs": [
            {"line": "victoria", "stops": ["Brixton", "Stockwell"]},
            {"line": "northern", "stops": ["Stockwell", "Oval", "Kennington", "Bank"]}
          ],
          "warnings": [{"line": "victoria", "status": "Minor Delays", "reasons": []}]
        }
    """.trimIndent()

    @Test
    fun readsTimeLegsAndWarnings() {
        val journey = parseJourney(brixtonToBank)

        assertEquals(17, journey.minutes)
        assertEquals(listOf("victoria", "northern"), journey.legs.map { it.line })
        assertEquals("Bank", journey.legs.last().stops.last())
        assertEquals(listOf("Victoria line: Minor Delays"), journey.warnings)
    }

    @Test
    fun alreadyThereHasNoLine() {
        val journey = parseJourney(
            """{"minutes": 0, "legs": [{"line": null, "stops": ["Victoria"]}], "warnings": []}"""
        )

        assertNull(journey.legs.single().line)
        assertEquals("You're already there", lineName(journey.legs.single().line))
    }

    @Test
    fun lineNamesReadNicely() {
        assertEquals("Hammersmith & City line", lineName("hammersmith-city"))
        assertEquals("Central line", lineName("central"))
        assertEquals("Walk", lineName("walk"))
    }

    @Test
    fun readsProgress() {
        val progress = parseProgress(
            """
            {"current": "Stockwell", "stops_left": 6, "next_stop_off": "Bank",
             "stops_to_next_stop_off": 6, "wake_up": false, "minutes_left": 12}
            """
        )

        assertEquals("Bank", progress.nextStopOff)
        assertEquals(6, progress.stopsToNextStopOff)
        assertEquals(false, progress.wakeUp)
        assertEquals(12, progress.minutesLeft)
        assertEquals(emptyList<String>(), progress.warnings)
        assertEquals(null, progress.reroute)
        assertEquals(emptyList<Leg>(), progress.legs)
    }

    @Test
    fun readsProgressRoute() {
        val progress = parseProgress(
            """
            {"current": "Oval", "stops_left": 5, "next_stop_off": "Bank",
             "stops_to_next_stop_off": 5, "wake_up": false, "minutes_left": 8, "legs": [
                {"line": "victoria", "stops": ["Brixton", "Stockwell"]},
                {"line": "northern", "stops": ["Stockwell", "Oval", "Bank"]}]}
            """
        )

        assertEquals(listOf("victoria", "northern"), progress.legs.map { it.line })
        assertEquals("Brixton", progress.legs.first().stops.first())
    }

    @Test
    fun readsReroute() {
        val progress = parseProgress(
            """
            {"current": "Brixton", "stops_left": 7, "next_stop_off": "Stockwell",
             "stops_to_next_stop_off": 1, "wake_up": true, "minutes_left": 17, "warnings": [],
             "reroute": {"avoid": ["northern"], "minutes": 21, "legs": [
                {"line": "victoria", "stops": ["Brixton", "Stockwell", "Oxford Circus"]},
                {"line": "central", "stops": ["Oxford Circus", "Bank"]}]}}
            """
        )

        val reroute = progress.reroute!!
        assertEquals(listOf("northern"), reroute.avoid)
        assertEquals(
            "Victoria line to Oxford Circus, then Central line to Bank (about 21 minutes)",
            rerouteText(reroute),
        )
    }

    @Test
    fun readsProgressWarnings() {
        val progress = parseProgress(
            """
            {"current": "Brixton", "stops_left": 7, "next_stop_off": "Stockwell",
             "stops_to_next_stop_off": 1, "wake_up": true, "minutes_left": 17,
             "warnings": [{"line": "victoria", "status": "Minor Delays", "reasons": []}]}
            """
        )

        assertEquals(listOf("Victoria line: Minor Delays"), progress.warnings)
    }

    @Test
    fun nearestTakesTheClosestStation() {
        val json = """[{"name": "Green Park", "metres": 310}, {"name": "Piccadilly Circus", "metres": 480}]"""
        assertEquals("Green Park", parseNearest(json))
    }

    @Test
    fun oneStopIsNotPlural() {
        assertEquals("1 stop", stopsText(1))
        assertEquals("3 stops", stopsText(3))
    }
}
