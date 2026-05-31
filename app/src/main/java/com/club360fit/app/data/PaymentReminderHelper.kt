package com.club360fit.app.data

import com.club360fit.app.ui.utils.toDisplayDate
import java.time.LocalDate

/**
 * In-app + device push reminders for coach-scheduled payment due dates
 * (`client_payment_settings.next_due_*`).
 */
object PaymentReminderHelper {

    /** After coach saves payment settings, notify the member when an upcoming due is set. */
    suspend fun notifyMemberAfterCoachSave(settings: ClientPaymentSettingsDto) {
        val due = settings.nextDueDate ?: return
        val amount = settings.nextDueAmount?.trim()?.takeIf { it.isNotBlank() }
        val note = settings.nextDueNote?.trim()?.takeIf { it.isNotBlank() }
        val body = buildString {
            append("Due ${due.toDisplayDate()}")
            amount?.let { append(" — $it") }
            note?.let { append(". $it") }
        }
        ClientNotificationRepository.notifyMemberFromCoach(
            clientId = settings.clientId,
            kind = "payment_reminder",
            title = "Upcoming payment",
            body = body,
            refType = "payment",
            refId = settings.clientId,
            dedupeKey = "payment_scheduled:${settings.clientId}:$due"
        )
    }

    /** Member app: remind on due date and one day before (deduped). */
    suspend fun remindMemberIfDueSoon(clientId: String, canViewPayments: Boolean) {
        if (!canViewPayments) return
        val settings = PaymentSettingsRepository.getForClient(clientId) ?: return
        val due = settings.nextDueDate ?: return
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)
        val amount = settings.nextDueAmount?.trim()?.takeIf { it.isNotBlank() } ?: "your coach"
        when (due) {
            today -> ClientNotificationRepository.notifyMemberFromCoach(
                clientId = clientId,
                kind = "payment_reminder",
                title = "Payment due today",
                body = "Amount: $amount. Open Payments to confirm or pay.",
                refType = "payment",
                refId = clientId,
                dedupeKey = "payment_due_today:$clientId:$due"
            )
            tomorrow -> ClientNotificationRepository.notifyMemberFromCoach(
                clientId = clientId,
                kind = "payment_reminder",
                title = "Payment due tomorrow",
                body = "Due ${due.toDisplayDate()} — $amount",
                refType = "payment",
                refId = clientId,
                dedupeKey = "payment_due_tomorrow:$clientId:$due"
            )
        }
    }

    /** Coach app: surface clients with payment due today in coach notification inbox. */
    suspend fun remindCoachOfClientsDueToday() {
        val today = LocalDate.now()
        val assigned = ClientRepository.getClients().filter { it.coachId != null }
        for (client in assigned) {
            val clientId = client.id ?: continue
            val settings = PaymentSettingsRepository.getForClient(clientId) ?: continue
            val due = settings.nextDueDate ?: continue
            if (due != today) continue
            val name = client.fullName?.trim()?.takeIf { it.isNotEmpty() } ?: "Client"
            val amount = settings.nextDueAmount?.trim()?.takeIf { it.isNotBlank() }
            val body = buildString {
                append(name)
                amount?.let { append(" — $it") }
            }
            ClientNotificationRepository.notifyCoachAboutClient(
                clientId = clientId,
                kind = "payment_reminder",
                title = "Payment due today",
                body = body.ifBlank { name },
                refType = "payment",
                refId = clientId,
                dedupeKey = "coach_payment_due_today:$clientId:$due"
            )
        }
    }
}
