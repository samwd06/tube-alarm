package com.example.tubealarm

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// An activity stops when the screen locks, so the alarm runs here instead,
// with a notification that keeps it alive and shows how far there is to go
class AlarmService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile private var location: Location? = null
    private val listener = LocationListener { location = it }

    override fun onBind(intent: Intent?): IBinder? = null

    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val start = intent?.getStringExtra(EXTRA_START)
        val end = intent?.getStringExtra(EXTRA_END)

        // Android kills the app if a foreground service doesn't call startForeground,
        // so that has to happen before giving up on a bad intent
        createChannels()
        val title = if (start != null && end != null) "Alarm on: $start to $end" else "Tube Alarm"
        val first = notification(ONGOING_CHANNEL, title, "Finding your station...")
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(ONGOING_ID, first, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(ONGOING_ID, first)
        }
        if (start == null || end == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        val manager = getSystemService(LocationManager::class.java)
        manager.removeUpdates(listener)
        try {
            manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10_000L, 0f, listener)
        } catch (e: Exception) {
            // No GPS on this device, or location was turned off since the button was pressed
            error.value = "Couldn't get your location"
            stopSelf()
            return START_NOT_STICKY
        }

        // Starting again for a different journey replaces the old loop
        scope.coroutineContext.cancelChildren()
        getSystemService(NotificationManager::class.java).cancel(WAKE_UP_ID)
        getSystemService(NotificationManager::class.java).cancel(DISRUPTION_ID)
        journey.value = start to end
        progress.value = null
        error.value = null
        scope.launch { follow(start, end) }
        return START_NOT_STICKY
    }

    private suspend fun CoroutineScope.follow(start: String, end: String) {
        var wokenFor: String? = null
        var warned: Set<String>? = null
        while (isActive) {
            val here = location
            if (here == null) {
                delay(2_000)
                continue
            }
            try {
                // The fetches block, so cancelling can't stop one halfway. withContext at least
                // throws on the way out, so a stopped alarm doesn't post or buzz afterwards
                val now = withContext(Dispatchers.IO) {
                    fetchProgress(start, end, fetchNearest(here.latitude, here.longitude))
                }
                progress.value = now
                error.value = null
                show(now)
                // Only buzz once for each stop, not every 30 seconds while it's close
                if (now.wakeUp && now.nextStopOff != wokenFor) {
                    wakeUp(now.nextStopOff)
                    wokenFor = now.nextStopOff
                }
                // Whatever was wrong at the start was already on the journey screen,
                // so only a new problem (or one getting worse) is worth a notification
                val fresh = now.warnings - (warned ?: now.warnings.toSet())
                if (fresh.isNotEmpty()) disrupted(fresh)
                warned = now.warnings.toSet()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error.value = e.message ?: "Couldn't reach the server"
            }
            delay(30_000)
        }
    }

    private fun show(now: Progress) {
        val text = "Get off at ${now.nextStopOff}, ${stopsText(now.stopsToNextStopOff)} away"
        val notifications = getSystemService(NotificationManager::class.java)
        notifications.notify(ONGOING_ID, notification(ONGOING_CHANNEL, "At ${now.current}", text))
    }

    private fun wakeUp(station: String) {
        val buzz = VibrationEffect.createWaveform(longArrayOf(0, 800, 400, 800, 400, 800), -1)
        getSystemService(Vibrator::class.java).vibrate(buzz)
        val notifications = getSystemService(NotificationManager::class.java)
        notifications.notify(WAKE_UP_ID, notification(WAKE_UP_CHANNEL, "Wake up!", "Get off at $station"))
    }

    private fun disrupted(warnings: List<String>) {
        val notifications = getSystemService(NotificationManager::class.java)
        val text = warnings.joinToString(", ")
        notifications.notify(DISRUPTION_ID, notification(DISRUPTION_CHANNEL, "Problem on your route", text))
    }

    private fun notification(channel: String, title: String, text: String): Notification {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        return Notification.Builder(this, channel)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(open)
            .setOnlyAlertOnce(channel == ONGOING_CHANNEL)
            .build()
    }

    private fun createChannels() {
        val notifications = getSystemService(NotificationManager::class.java)
        notifications.createNotificationChannel(
            NotificationChannel(ONGOING_CHANNEL, "Journey progress", NotificationManager.IMPORTANCE_LOW)
        )
        notifications.createNotificationChannel(
            NotificationChannel(WAKE_UP_CHANNEL, "Wake up alerts", NotificationManager.IMPORTANCE_HIGH)
        )
        notifications.createNotificationChannel(
            NotificationChannel(DISRUPTION_CHANNEL, "Disruptions on your route", NotificationManager.IMPORTANCE_DEFAULT)
        )
    }

    override fun onDestroy() {
        getSystemService(LocationManager::class.java).removeUpdates(listener)
        getSystemService(NotificationManager::class.java).cancel(WAKE_UP_ID)
        getSystemService(NotificationManager::class.java).cancel(DISRUPTION_ID)
        scope.cancel()
        journey.value = null
        progress.value = null
        super.onDestroy()
    }

    companion object {
        const val EXTRA_START = "start"
        const val EXTRA_END = "end"
        private const val ONGOING_CHANNEL = "progress"
        private const val WAKE_UP_CHANNEL = "wake_up"
        private const val DISRUPTION_CHANNEL = "disruptions"
        private const val ONGOING_ID = 1
        private const val WAKE_UP_ID = 2
        private const val DISRUPTION_ID = 3

        // The screen watches these, so it can show the alarm even after being closed and reopened
        val journey = MutableStateFlow<Pair<String, String>?>(null)
        val progress = MutableStateFlow<Progress?>(null)
        val error = MutableStateFlow<String?>(null)
    }
}
