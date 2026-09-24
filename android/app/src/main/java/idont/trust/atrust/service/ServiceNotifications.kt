package idont.trust.atrust.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import idont.trust.atrust.MainActivity
import idont.trust.atrust.R
import idont.trust.atrust.logging.Logger

object ServiceNotifications {
    const val VPN_CHANNEL = "distrust_vpn"
    const val PROXY_CHANNEL = "distrust_proxy"
    const val VPN_NOTIFICATION = 1001
    const val PROXY_NOTIFICATION = 1002

    fun createChannels(context: Context) {
        Logger.d("Notifications", "Creating service notification channels")
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                VPN_CHANNEL,
                context.getString(R.string.vpn_notification_channel),
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
        manager.createNotificationChannel(
            NotificationChannel(
                PROXY_CHANNEL,
                context.getString(R.string.proxy_notification_channel),
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
    }

    fun build(
        context: Context,
        channel: String,
        title: String,
        content: String,
        stopIntent: Intent,
    ): Notification {
        Logger.d("Notifications", "Building foreground notification; channel=$channel, title=$title")
        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stop = PendingIntent.getService(
            context,
            channel.hashCode(),
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(content)
            .setContentIntent(openApp)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(0, context.getString(R.string.notification_stop), stop)
            .build()
    }
}
