package com.example.tubealarm

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.ui.unit.dp

@Composable
fun AlarmCard(start: String, end: String) {
    val context = LocalContext.current
    val journey by AlarmService.journey.collectAsState()
    val progress by AlarmService.progress.collectAsState()
    val serviceError by AlarmService.error.collectAsState()
    var permissionError by remember { mutableStateOf<String?>(null) }
    val running = journey == (start to end)

    val permissions = buildList {
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
    }
    val askForPermissions = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        // Notifications are nice to have, but without location the alarm can't work at all
        if (granted[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            permissionError = null
            val intent = Intent(context, AlarmService::class.java)
                .putExtra(AlarmService.EXTRA_START, start)
                .putExtra(AlarmService.EXTRA_END, end)
            context.startForegroundService(intent)
            if (granted[Manifest.permission.POST_NOTIFICATIONS] == false) {
                permissionError = "Without notifications the alarm can only vibrate when the screen is off"
            }
        } else {
            permissionError = "The alarm needs your location to know which station you're at"
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Alarm", style = MaterialTheme.typography.titleSmall)

            if (!running) {
                Text("Wakes you up one stop before you need to get off, even with the screen locked")
                Button(
                    onClick = { askForPermissions.launch(permissions.toTypedArray()) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Start alarm") }
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
