package com.example.tubealarm

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

// The emulator reaches the Mac's own localhost through this address
const val BACKEND_URL = "http://10.0.2.2:8000"

data class Leg(val line: String?, val stops: List<String>)

data class Journey(val minutes: Int, val legs: List<Leg>, val warnings: List<String>)

fun parseJourney(json: String): Journey {
    val body = JSONObject(json)
    val legs = body.getJSONArray("legs")
    val warnings = body.getJSONArray("warnings")

    return Journey(
        minutes = body.getInt("minutes"),
        legs = (0 until legs.length()).map { i ->
            val leg = legs.getJSONObject(i)
            val stops = leg.getJSONArray("stops")
            Leg(
                line = if (leg.isNull("line")) null else leg.getString("line"),
                stops = (0 until stops.length()).map { stops.getString(it) },
            )
        },
        warnings = (0 until warnings.length()).map { i ->
            val warning = warnings.getJSONObject(i)
            "${lineName(warning.getString("line"))}: ${warning.getString("status")}"
        },
    )
}

data class Progress(
    val current: String,
    val stopsLeft: Int,
    val nextStopOff: String,
    val stopsToNextStopOff: Int,
    val wakeUp: Boolean,
    val minutesLeft: Int,
)

fun parseProgress(json: String): Progress {
    val body = JSONObject(json)
    return Progress(
        current = body.getString("current"),
        stopsLeft = body.getInt("stops_left"),
        nextStopOff = body.getString("next_stop_off"),
        stopsToNextStopOff = body.getInt("stops_to_next_stop_off"),
        wakeUp = body.getBoolean("wake_up"),
        minutesLeft = body.getInt("minutes_left"),
    )
}

// Both of these block while they wait for the server, so only call them off the main thread
fun fetchJourney(start: String, end: String): Journey =
    parseJourney(get("/journey?start=${encode(start)}&end=${encode(end)}"))

fun fetchProgress(start: String, end: String, current: String): Progress =
    parseProgress(get("/progress?start=${encode(start)}&end=${encode(end)}&current=${encode(current)}"))

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
