package com.akrishna87.expiro.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.akrishna87.expiro.ExpiroApp
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/**
 * Looks at every available item and sends at most two notifications per item:
 *  - stage 1: within the configured number of days before expiry
 *  - stage 2: on the expiry day (or later, if the app missed it)
 */
class ExpiryCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as ExpiroApp
        val dao = app.db.itemDao()
        val reminderDays = app.settings.reminderDays.first()
        val today = LocalDate.now()

        for (item in dao.getAvailable()) {
            val daysLeft = item.daysLeft(today)
            val stage = when {
                daysLeft <= 0 -> 2
                daysLeft <= reminderDays -> 1
                else -> 0
            }
            if (stage > item.notifiedStage) {
                Notifications.notifyExpiry(applicationContext, item, daysLeft)
                dao.setNotifiedStage(item.id, stage)
            }
        }
        return Result.success()
    }
}
