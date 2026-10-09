package com.shilapi.xcertplay

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.shilapi.xcertplay.host.R

/** Keeps an explicitly started connection alive when another car app is in the foreground. */
class DiPlaySessionService : Service() {
    private var notificationKey = ""
    override fun onCreate() { super.onCreate(); live = this }
    override fun onDestroy() { if (live === this) live = null; super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            CarPlayBackgroundSession.stop()
            stopSelf()
            return START_NOT_STICKY
        }
        if (intent?.action == ACTION_MEDIA) CarPlayMediaKeys.command(intent.getIntExtra("button", 0))
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) manager.createNotificationChannel(NotificationChannel(CHANNEL, "CarPlay connection", NotificationManager.IMPORTANCE_LOW))
        val notification = mediaNotification()
        if (Build.VERSION.SDK_INT >= 29) {
            var types = ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            if (Build.VERSION.SDK_INT >= 30 && checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                types = types or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            }
            // Without it Android stops location updates while another car app (the reversing camera,
            // the car's own map) covers CarPlay, and the iPhone gets no position until DiPlay is back.
            if (AirPlayPersistence.loadLocationReportingEnabled(this) &&
                checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                types = types or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            }
            startForeground(1, notification, types)
        } else startForeground(1, notification)
        return START_NOT_STICKY
    }

    private fun mediaNotification(): Notification {
        val snapshot = CarPlayMediaKeys.snapshot
        val title = snapshot?.title?.takeIf { it.isNotBlank() } ?: "CarBridge"
        val artist = snapshot?.artist?.takeIf { it.isNotBlank() } ?: "CarPlay 已连接"
        val open = PendingIntent.getActivity(this, 0, Intent(this, CarPlayHostActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val stop = PendingIntent.getService(this, 1, Intent(this, DiPlaySessionService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val playing = CarPlayMediaKeys.isAudible
        val button = if (playing) com.shilapi.xcertplay.airplay.CarPlayMediaButton.PAUSE else com.shilapi.xcertplay.airplay.CarPlayMediaButton.PLAY
        val media = PendingIntent.getService(this, 10 + button, Intent(this, DiPlaySessionService::class.java)
            .setAction(ACTION_MEDIA).putExtra("button", button), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val builder = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Notification.Builder(this, CHANNEL)
            else { @Suppress("DEPRECATION") Notification.Builder(this).setPriority(Notification.PRIORITY_LOW) }).setSmallIcon(R.drawable.ic_diplay_notification)
            .setContentTitle(title).setContentText(artist).setContentIntent(open).setOngoing(true).setOnlyAlertOnce(true)
            .addAction(Notification.Action.Builder(if (playing) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (playing) "暂停" else "播放", media).build())
            .addAction(Notification.Action.Builder(null, "断开连接", stop).build())
        CarPlayMediaKeys.sessionToken?.let { builder.setStyle(Notification.MediaStyle().setMediaSession(it).setShowActionsInCompactView(0)) }
        notificationKey = "$title|$artist|$playing"
        return builder.build()
    }

    private fun refreshNotification() {
        val state = CarPlayMediaKeys.snapshot
        val key = "${state?.title?.takeIf { it.isNotBlank() } ?: "CarBridge"}|${state?.artist?.takeIf { it.isNotBlank() } ?: "CarPlay 已连接"}|${CarPlayMediaKeys.isAudible}"
        if (notificationKey != key) getSystemService(NotificationManager::class.java).notify(1, mediaNotification())
    }
    override fun onTaskRemoved(rootIntent: Intent?) {
        // BYD's recents force-stops the package ~10 ms after removing the task: end guidance first.
        com.shilapi.xcertplay.hud.BydNavigationOutputs.endNow()
        CarPlayBackgroundSession.stop()
        stopSelf()
    }
    companion object {
        const val ACTION_STOP = "io.github.rhsr1024.carbridge.DISCONNECT"
        private const val ACTION_MEDIA = "io.github.rhsr1024.carbridge.MEDIA"
        private const val CHANNEL = "diplay_connection"
        @Volatile private var live: DiPlaySessionService? = null
        internal fun refreshMediaNotification() { live?.refreshNotification() }
    }
}
