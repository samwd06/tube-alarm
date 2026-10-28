package com.example.tubealarm

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Roundel()
            Column {
                Text("Tube Alarm", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    "Never sleep past your stop",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        RoundCard {
            Text("Where are you going?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            StationField(start, { start = it }, "From", MaterialTheme.colorScheme.primary)
            StationField(end, { end = it }, "To", MaterialTheme.colorScheme.secondary)
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
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text(if (loading) "Planning..." else "Plan journey", fontWeight = FontWeight.SemiBold)
            }
        }

        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        journey?.let {
            JourneyResult(it)
            if (it.minutes > 0) {
                // A new journey gets a fresh alarm rather than carrying on the old one
                key(it) { AlarmCard(it.legs.first().stops.first(), it.legs.last().stops.last()) }
            }
        }
    }
}

// The rounded white card everything on the screen sits in
@Composable
fun RoundCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            content()
        }
    }
}

@Composable
private fun StationField(value: String, onChange: (String) -> Unit, label: String, dot: Color) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        leadingIcon = { Box(Modifier.size(12.dp).background(dot, CircleShape)) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
fun JourneyResult(journey: Journey) {
    RoundCard {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("${journey.minutes}", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            Text("min", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 6.dp))
        }

        journey.warnings.forEach { Warning(it) }

        RouteMap(journey.legs)
    }
}

@Composable
fun Warning(text: String) {
    Text(
        text,
        color = MaterialTheme.colorScheme.onErrorContainer,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

fun lineName(line: String?): String = when (line) {
    null -> "You're already there"
    "walk" -> "Walk"
    "hammersmith-city" -> "Hammersmith & City line"
    "waterloo-city" -> "Waterloo & City line"
    else -> line.replaceFirstChar { it.uppercase() } + " line"
}

fun stopsText(count: Int): String = if (count == 1) "1 stop" else "$count stops"

// e.g. "Victoria line to Oxford Circus, then Central line to Bank (about 20 minutes)"
fun rerouteText(reroute: Reroute): String =
    reroute.legs.joinToString(", then ") { "${lineName(it.line)} to ${it.stops.last()}" } +
        " (about ${reroute.minutes} minutes)"

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
