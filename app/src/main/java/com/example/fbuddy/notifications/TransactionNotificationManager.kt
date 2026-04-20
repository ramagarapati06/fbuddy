package com.example.fbuddy.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.fbuddy.MainActivity
import com.example.fbuddy.R
import com.example.fbuddy.data.model.Category

class TransactionNotificationManager(private val context: Context) {

    companion object {
        const val CHANNEL_ID   = "fbuddy_transactions"
        const val CHANNEL_NAME = "Transaction Alerts"
        private var notifId    = 1000
    }

    init {
        createChannel()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "Alerts for new transactions detected via OCR or SMS"
            channel.enableVibration(true)
            channel.setShowBadge(true)

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun showTransactionNotification(
        merchant: String,
        amount: Double,
        category: Category,
        source: String,
        isUnknownMerchant: Boolean = merchant == "Unknown Merchant"
    ) {
        val id = notifId++

        val openIntent = PendingIntent.getActivity(
            context, id,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isUnknownMerchant)
            "⚠️ Unknown Merchant — ₹%.2f via $source".format(amount)
        else
            "💸 ₹%.2f at $merchant".format(amount)

        val body = if (isUnknownMerchant)
            "Tap 'Assign Category' to categorise this transaction."
        else
            "Category: ${category.displayName} · Detected via $source"

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openIntent)

        if (isUnknownMerchant) {
            val categoryIntent = PendingIntent.getActivity(
                context, id + 5000,
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra("open_category_picker", true)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(R.drawable.ic_notification, "Assign Category", categoryIntent)
        }

        try {
            NotificationManagerCompat.from(context).notify(id, builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}
