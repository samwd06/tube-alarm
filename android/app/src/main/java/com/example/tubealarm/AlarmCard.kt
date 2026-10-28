package com.example.tubealarm

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import com.example.tubealarm.ui.theme.TubeRed
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
                if (now == null) {
                    Text("Finding your station...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    TrainTrack(now)
                    if (now.wakeUp) WakeUpBanner(now.nextStopOff) else NextStop(now)
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

@Composable
private fun NextStop(now: Progress) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Get off at", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(now.nextStopOff, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "At ${now.current} · about ${now.minutesLeft} min left",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // How many stops until then, big enough to read at a glance on a moving train
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "${now.stopsToNextStopOff}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    if (now.stopsToNextStopOff == 1) "stop" else "stops",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun WakeUpBanner(station: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(TubeRed, RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Text("WAKE UP", color = Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Text("Get off at $station", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    }
}
