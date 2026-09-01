package com.rustech.subscribertracker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val title = inputData.getString(KEY_TITLE) ?: "Payment Reminder"
        val message = inputData.getString(KEY_MESSAGE) ?: "You have an upcoming due date."
        val notifId = inputData.getInt(KEY_NOTIF_ID, 1001)

        NotificationHelper.createChannel(applicationContext)
        NotificationHelper.showReminder(applicationContext, title, message, notifId)

        return Result.success()
    }

    companion object {
        const val KEY_TITLE = "title"
        const val KEY_MESSAGE = "message"
        const val KEY_NOTIF_ID = "notif_id"
    }
}
