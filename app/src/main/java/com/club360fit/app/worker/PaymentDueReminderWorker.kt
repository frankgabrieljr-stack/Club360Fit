package com.club360fit.app.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.club360fit.app.data.ClientSelfRepository
import com.club360fit.app.data.PaymentReminderHelper

/**
 * Periodic check for coach-scheduled payment due dates (member + coach inboxes).
 */
class PaymentDueReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        ClientSelfRepository.getOwnClient()?.let { client ->
            val id = client.id
            if (id != null) {
                PaymentReminderHelper.remindMemberIfDueSoon(id, client.canViewPayments)
            }
        }
        PaymentReminderHelper.remindCoachOfClientsDueToday()
        return Result.success()
    }
}
