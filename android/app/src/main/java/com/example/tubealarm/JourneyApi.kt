package com.example.tubealarm

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

// 10.0.2.2 on the emulator, or whatever backend.url says in local.properties
const val BACKEND_URL = BuildConfig.BACKEND_URL

data class Leg(val line: String?, val stops: List<String>)

data class Journey(val minutes: Int, val legs: List<Leg>, val warnings: List<String>)

fun parseJourney(json: String): Journey {
    val body = JSONObject(json)
    val legs = body.getJSONArray("legs")

    return Journey(
        minutes = body.getInt("minutes"),
        legs = parseLegs(legs),
        warnings = parseWarnings(body.getJSONArray("warnings")),
    )
}

private fun parseLegs(legs: JSONArray): List<Leg> =
    (0 until legs.length()).map { i ->
        val leg = legs.getJSONObject(i)
        val stops = leg.getJSONArray("stops")
        Leg(
            line = if (leg.isNull("line")) null else leg.getString("line"),
            stops = (0 until stops.length()).map { stops.getString(it) },
        )
    }

// Turned into text straight away, e.g. "Victoria line: Minor Delays"
private fun parseWarnings(warnings: JSONArray): List<String> =
    (0 until warnings.length()).map { i ->
        val warning = warnings.getJSONObject(i)
        "${lineName(warning.getString("line"))}: ${warning.getString("status")}"
    }

data class Progress(
    val current: String,
    val stopsLeft: Int,
    val nextStopOff: String,
    val stopsToNextStopOff: Int,
    val wakeUp: Boolean,
    val minutesLeft: Int,
    val warnings: List<String>,
    val reroute: Reroute?,
)

// Another way to the destination from where the passenger is now, around lines that are badly disrupted
data class Reroute(val avoid: List<String>, val minutes: Int, val legs: List<Leg>)

fun parseProgress(json: String): Progress {
    val body = JSONObject(json)
    return Progress(
        current = body.getString("current"),
        stopsLeft = body.getInt("stops_left"),
        nextStopOff = body.getString("next_stop_off"),
        stopsToNextStopOff = body.getInt("stops_to_next_stop_off"),
        wakeUp = body.getBoolean("wake_up"),
        minutesLeft = body.getInt("minutes_left"),
        // A backend from before 25 Oct doesn't send these
        warnings = body.optJSONArray("warnings")?.let(::parseWarnings) ?: emptyList(),
        reroute = body.optJSONObject("reroute")?.let { reroute ->
            val avoid = reroute.getJSONArray("avoid")
            Reroute(
                avoid = (0 until avoid.length()).map { avoid.getString(it) },
                minutes = reroute.getInt("minutes"),
                legs = parseLegs(reroute.getJSONArray("legs")),
            )
        },
    )
}

// Both of these block while they wait for the server, so only call them off the main thread
fun fetchJourney(start: String, end: String): Journey =
    parseJourney(get("/journey?start=${encode(start)}&end=${encode(end)}"))

fun fetchProgress(start: String, end: String, current: String, avoid: List<String> = emptyList()): Progress {
    var path = "/progress?start=${encode(start)}&end=${encode(end)}&current=${encode(current)}"
    if (avoid.isNotEmpty()) path += "&avoid=${encode(avoid.joinToString(","))}"
    return parseProgress(get(path))
}

// The backend sorts by distance, so the first name is the closest station
fun parseNearest(json: String): String = JSONArray(json).getJSONObject(0).getString("name")

fun fetchNearest(lat: Double, lon: Double): String =
    parseNearest(get("/nearest?lat=$lat&lon=$lon&limit=1"))

private fun get(path: String): String {
    val connection = URL("$BACKEND_URL$path").openConnection() as HttpURLConnection
    connection.connectTimeout = 5_000
    connection.readTimeout = 10_000

    try {
        if (connection.responseCode != 200) {
            // The backend explains problems like an unknown station in a "detail" field
            val error = connection.errorStream?.bufferedReader()?.readText().orEmpty()
            val detail = runCatching { JSONObject(error).getString("detail") }.getOrNull()
            throw IllegalArgumentException(detail ?: "The server returned ${connection.responseCode}")
        }
        return connection.inputStream.bufferedReader().readText()
    } finally {
        connection.disconnect()
    }
}

private fun encode(text: String): String = URLEncoder.encode(text, "UTF-8")
