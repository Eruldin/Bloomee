package com.bloomee.app.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.bloomee.app.MainActivity
import com.bloomee.app.R

object Notifications {

    const val CHANNEL_HYDRATION = "bloomee_hydration"
    const val CHANNEL_CYCLE = "bloomee_cycle"
    const val CHANNEL_MEDICATION = "bloomee_medication"

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        listOf(
            Triple(CHANNEL_HYDRATION, "Su hatırlatıcısı", "Gün içinde su içme hatırlatmaları"),
            Triple(CHANNEL_CYCLE, "Döngü hatırlatıcısı", "Yaklaşan regl ve yumurtlama bildirimleri"),
            Triple(CHANNEL_MEDICATION, "İlaç hatırlatıcısı", "Günlük ilaç ve takviye hatırlatmaları")
        ).forEach { (id, name, description) ->
            manager.createNotificationChannel(
                NotificationChannel(id, name, NotificationManager.IMPORTANCE_DEFAULT).apply {
                    this.description = description
                }
            )
        }
    }

    fun show(context: Context, channelId: String, notificationId: Int, title: String, text: String) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val intent = PendingIntent.getActivity(
            context,
            notificationId,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(intent)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }
}
