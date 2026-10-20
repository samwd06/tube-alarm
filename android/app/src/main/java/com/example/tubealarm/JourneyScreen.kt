package com.example.tubealarm

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.tubealarm.ui.theme.TubeAlarmTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun JourneyScreen(modifier: Modifier = Modifier) {
    var start by remember { mutableStateOf("") }
    var end by remember { mutableStateOf("") }
    var journey by remember { mutableStateOf<Journey?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Plan a journey", style = MaterialTheme.typography.headlineSmall)

        OutlinedTextField(
            value = start,
            onValueChange = { start = it },
            label = { Text("From") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = end,
            onValueChange = { end = it },
            label = { Text("To") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Button(
            onClick = {
                loading = true
                error = null
                scope.launch {
                    try {
                        // Network calls aren't allowed on the main thread, so do it on the IO one
                        journey = withContext(Dispatchers.IO) { fetchJourney(start.trim(), end.trim()) }
                    } catch (e: Exception) {
                        journey = null
                        error = e.message ?: "Couldn't reach the server"
                    } finally {
                        loading = false
                    }
                }
            },
            enabled = start.isNotBlank() && end.isNotBlank() && !loading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (loading) "Planning..." else "Plan journey")
        }

        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        journey?.let { JourneyResult(it) }
    }
}

@Composable
fun JourneyResult(journey: Journey) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("About ${journey.minutes} minutes", style = MaterialTheme.typography.titleMedium)

        journey.warnings.forEach {
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        journey.legs.forEach { leg ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(lineName(leg.line), style = MaterialTheme.typography.titleSmall)
                    Text("${leg.stops.first()} to ${leg.stops.last()}")
                    if (leg.line != "walk" && leg.stops.size > 1) {
                        Text(stopsText(leg.stops.size - 1), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

fun lineName(line: String?): String = when (line) {
    null -> "You're already there"
    "walk" -> "Walk"
    "hammersmith-city" -> "Hammersmith & City line"
    "waterloo-city" -> "Waterloo & City line"
    else -> line.replaceFirstChar { it.uppercase() } + " line"
}

fun stopsText(count: Int): String = if (count == 1) "1 stop" else "$count stops"

@Preview(showBackground = true)
@Composable
fun JourneyResultPreview() {
    TubeAlarmTheme {
        JourneyResult(
            Journey(
                minutes = 17,
                legs = listOf(
                    Leg("victoria", listOf("Brixton", "Stockwell")),
                    Leg("northern", listOf("Stockwell", "Oval", "Kennington", "Bank")),
                ),
                warnings = listOf("Victoria line: Minor Delays"),
            )
        )
    }
}
