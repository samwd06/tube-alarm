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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

// An activity stops when the screen locks, so the alarm runs here instead,
// with a notification that keeps it alive and shows how far there is to go
class AlarmService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile private var location: Location? = null
    private val listener = LocationListener { location = it }

    override fun onBind(intent: Intent?): IBinder? = null

    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val start = intent?.getStringExtra(EXTRA_START) ?: return START_NOT_STICKY
        val end = intent.getStringExtra(EXTRA_END) ?: return START_NOT_STICKY

        createChannels()
        val first = notification(ONGOING_CHANNEL, "Alarm on: $start to $end", "Finding your station...")
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(ONGOING_ID, first, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(ONGOING_ID, first)
        }

        val manager = getSystemService(LocationManager::class.java)
        manager.removeUpdates(listener)
        manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10_000L, 0f, listener)

        // Starting again for a different journey replaces the old loop
        scope.coroutineContext.cancelChildren()
        journey.value = start to end
        progress.value = null
        scope.launch { follow(start, end) }
        return START_NOT_STICKY
    }

    private suspend fun CoroutineScope.follow(start: String, end: String) {
        var wokenFor: String? = null
        while (isActive) {
            val here = location
            if (here == null) {
                delay(2_000)
                continue
            }
            try {
                val now = fetchProgress(start, end, fetchNearest(here.latitude, here.longitude))
                progress.value = now
                error.value = null
                show(now)
                // Only buzz once for each stop, not every 30 seconds while it's close
                if (now.wakeUp && now.nextStopOff != wokenFor) {
                    wakeUp(now.nextStopOff)
                    wokenFor = now.nextStopOff
                }
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
    }

    override fun onDestroy() {
        getSystemService(LocationManager::class.java).removeUpdates(listener)
        scope.cancel()
        journey.value = null
        progress.value = null
        error.value = null
        super.onDestroy()
    }

    companion object {
        const val EXTRA_START = "start"
        const val EXTRA_END = "end"
        private const val ONGOING_CHANNEL = "progress"
        private const val WAKE_UP_CHANNEL = "wake_up"
        private const val ONGOING_ID = 1
        private const val WAKE_UP_ID = 2

        // The screen watches these, so it can show the alarm even after being closed and reopened
        val journey = MutableStateFlow<Pair<String, String>?>(null)
        val progress = MutableStateFlow<Progress?>(null)
        val error = MutableStateFlow<String?>(null)
    }
}
