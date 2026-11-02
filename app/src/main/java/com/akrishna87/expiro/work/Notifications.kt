package com.akrishna87.expiro.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.akrishna87.expiro.MainActivity
import com.akrishna87.expiro.R
import com.akrishna87.expiro.data.Item

object Notifications {
    const val CHANNEL_ID = "expiry_reminders"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = context.getString(R.string.channel_description) }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    fun notifyExpiry(context: Context, item: Item, daysLeft: Long) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return

        val id = item.id.toInt()
        val text = when {
            daysLeft < 0 -> context.getString(R.string.notif_expired, item.name, -daysLeft)
            daysLeft == 0L -> context.getString(R.string.notif_today, item.name)
            daysLeft == 1L -> context.getString(R.string.notif_tomorrow, item.name)
            else -> context.getString(R.string.notif_in_days, item.name, daysLeft)
        }

        val open = PendingIntent.getActivity(
            context, id,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val markUsed = PendingIntent.getBroadcast(
            context, id,
            Intent(context, MarkUsedReceiver::class.java).putExtra(MarkUsedReceiver.EXTRA_ITEM_ID, item.id),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notif_title))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$text\n${item.amountText()}"))
            .setContentIntent(open)
            .setAutoCancel(true)
            .addAction(0, context.getString(R.string.action_mark_used), markUsed)
            .build()

        NotificationManagerCompat.from(context).notify(id, notification)
    }

    fun cancel(context: Context, itemId: Long) {
        NotificationManagerCompat.from(context).cancel(itemId.toInt())
    }
}
