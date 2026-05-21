package com.example.moneymaplk.core.notifications

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object RecurrentWorkScheduler {
    private const val WORK_NAME = "recurrent_due_daily_check"

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<RecurrentNotificationWorker>(1, TimeUnit.DAYS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
}
