package com.example.tubealarm

import android.Manifest
import android.annotation.SuppressLint
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.VibrationEffect
import android.os.Vibrator
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

// Location is only read after the permission has been granted, which lint can't see
@SuppressLint("MissingPermission")
@Composable
fun AlarmCard(start: String, end: String) {
    val context = LocalContext.current
    var running by remember { mutableStateOf(false) }
    var location by remember { mutableStateOf<Location?>(null) }
    var progress by remember { mutableStateOf<Progress?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    val askForLocation = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        running = granted
        error = if (granted) null else "The alarm needs your location to know which station you're at"
    }

    if (running) {
        DisposableEffect(Unit) {
            val manager = context.getSystemService(LocationManager::class.java)
            val listener = LocationListener { location = it }
            manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10_000L, 0f, listener)
            onDispose { manager.removeUpdates(listener) }
        }

        LaunchedEffect(Unit) {
            while (true) {
                val here = location
                if (here == null) {
                    delay(2_000)
                    continue
                }
                try {
                    progress = withContext(Dispatchers.IO) {
                        fetchProgress(start, end, fetchNearest(here.latitude, here.longitude))
                    }
                    error = null
                } catch (e: Exception) {
                    error = e.message ?: "Couldn't reach the server"
                }
                delay(30_000)
            }
        }

        LaunchedEffect(progress?.wakeUp) {
            if (progress?.wakeUp == true) {
                val buzz = VibrationEffect.createWaveform(longArrayOf(0, 800, 400, 800, 400, 800), -1)
                context.getSystemService(Vibrator::class.java).vibrate(buzz)
            }
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Alarm", style = MaterialTheme.typography.titleSmall)

            if (!running) {
                Text("Wakes you up one stop before you need to get off")
                Button(
                    onClick = { askForLocation.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
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
                    onClick = {
                        running = false
                        progress = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Stop alarm") }
            }

            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
}
