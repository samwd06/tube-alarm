package com.example.tubealarm

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun AlarmCard(start: String, end: String) {
    val context = LocalContext.current
    val journey by AlarmService.journey.collectAsState()
    val progress by AlarmService.progress.collectAsState()
    val serviceError by AlarmService.error.collectAsState()
    val avoiding by AlarmService.avoiding.collectAsState()
    var permissionError by remember { mutableStateOf<String?>(null) }
    val running = journey == (start to end)

    val permissions = buildList {
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
    }
    val askForPermissions = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        // Notifications are nice to have, but without location the alarm can't work at all.
        // Approximate isn't enough either: Android only updates it every 10 minutes or so,
        // which is longer than most journeys
        if (granted[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            permissionError = null
            startAlarm(context, start, end)
            if (granted[Manifest.permission.POST_NOTIFICATIONS] == false) {
                permissionError = "Without notifications the alarm can only vibrate when the screen is off"
            }
        } else if (granted[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            permissionError = "The alarm needs precise location. Approximate location only updates " +
                "every few minutes, so it would miss your stop. Tap Start alarm again to allow it"
        } else {
            permissionError = "The alarm needs your location to know which station you're at"
        }
    }

    RoundCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Alarm", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

            if (!running) {
                // Only one alarm can run, so say so before starting this one quietly replaces it
                val other = journey
                if (other != null) {
                    Text("The alarm is already on for ${other.first} to ${other.second}. Starting it here will stop that one")
                } else {
                    Text("Wakes you up one stop before you need to get off, even with the screen locked")
                }
                Button(
                    onClick = { askForPermissions.launch(permissions.toTypedArray()) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (other != null) "Switch alarm to this journey" else "Start alarm") }
            } else {
                val now = progress
                when {
                    now == null -> Text("Finding your station...")
                    now.wakeUp -> Text(
                        "Wake up! Get off at ${now.nextStopOff}",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                    else -> {
                        Text("At ${now.current}")
                        Text("Get off at ${now.nextStopOff}, ${stopsText(now.stopsToNextStopOff)} away")
                        Text("About ${now.minutesLeft} minutes left", style = MaterialTheme.typography.bodySmall)
                    }
                }
                now?.warnings?.forEach { Warning(it) }
                if (avoiding.isNotEmpty()) {
                    Text("Going another way, avoiding the ${avoiding.joinToString(" and ") { lineName(it) }}")
                }
                now?.reroute?.let { reroute ->
                    Text("Another way: ${rerouteText(reroute)}")
                    // Permissions were already given when the alarm started, so this can restart it straight away
                    Button(
                        onClick = { startAlarm(context, start, end, reroute.avoid, reroute.legs.first().stops.first()) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Use this way") }
                }
                OutlinedButton(
                    onClick = { context.stopService(Intent(context, AlarmService::class.java)) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Stop alarm") }
            }

            val error = permissionError ?: serviceError.takeIf { running || journey == null }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
}

private fun startAlarm(
    context: Context, start: String, end: String,
    avoid: List<String> = emptyList(), from: String = start,
) {
    context.startForegroundService(AlarmService.alarmIntent(context, start, end, avoid, from))
}
