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
    fun oneStopIsNotPlural() {
        assertEquals("1 stop", stopsText(1))
        assertEquals("3 stops", stopsText(3))
    }
}
