package com.example.moneymaplk.core.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.moneymaplk.R
import com.example.moneymaplk.domain.model.PaymentFollowUp
import com.example.moneymaplk.domain.model.RecurringPayment
import com.example.moneymaplk.domain.model.TransactionType

object RecurrentNotificationHelper {
    private const val CHANNEL_ID = "recurrent_due_channel"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Recurrent reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Reminders for overdue recurrent income and payments."
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    fun canPostNotifications(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
    }

    fun notifyOverdue(context: Context, payment: RecurringPayment) {
        if (!canPostNotifications(context)) return
        createChannel(context)
        val typeLabel = if (payment.type == TransactionType.INCOME) "income" else "payment"
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Overdue recurrent $typeLabel")
            .setContentText("${payment.title} needs confirmation or skip.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            ) {
                NotificationManagerCompat.from(context).notify(payment.paymentId.hashCode(), notification)
            }
        } else {
            NotificationManagerCompat.from(context).notify(payment.paymentId.hashCode(), notification)
        }
    }

    fun notifyOverdueFollowUp(context: Context, followUp: PaymentFollowUp) {
        if (!canPostNotifications(context)) return
        createChannel(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Overdue expected item")
            .setContentText("${followUp.title} needs confirmation or skip.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            ) {
                NotificationManagerCompat.from(context).notify(followUp.followUpId.hashCode(), notification)
            }
        } else {
            NotificationManagerCompat.from(context).notify(followUp.followUpId.hashCode(), notification)
        }
    }
}
