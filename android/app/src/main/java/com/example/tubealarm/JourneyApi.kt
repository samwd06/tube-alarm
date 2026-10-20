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
            "${warning.getString("line")}: ${warning.getString("status")}"
        },
    )
}

// This blocks while it waits for the server, so only call it off the main thread
fun fetchJourney(start: String, end: String): Journey {
    val query = "start=${encode(start)}&end=${encode(end)}"
    val connection = URL("$BACKEND_URL/journey?$query").openConnection() as HttpURLConnection
    connection.connectTimeout = 5_000
    connection.readTimeout = 10_000

    try {
        if (connection.responseCode != 200) {
            // The backend explains problems like an unknown station in a "detail" field
            val error = connection.errorStream?.bufferedReader()?.readText().orEmpty()
            val detail = runCatching { JSONObject(error).getString("detail") }.getOrNull()
            throw IllegalArgumentException(detail ?: "The server returned ${connection.responseCode}")
        }
        return parseJourney(connection.inputStream.bufferedReader().readText())
    } finally {
        connection.disconnect()
    }
}

private fun encode(text: String): String = URLEncoder.encode(text, "UTF-8")
