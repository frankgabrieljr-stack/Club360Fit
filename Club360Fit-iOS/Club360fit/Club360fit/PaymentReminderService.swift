import Foundation

/// In-app + device push reminders for coach-scheduled payment due dates.
enum PaymentReminderService {
    /// After coach saves payment settings, notify the member when an upcoming due is set.
    static func notifyMemberAfterCoachSave(
        clientId: String,
        nextDueDate: String?,
        nextDueAmount: String?,
        nextDueNote: String?
    ) async {
        guard let due = nextDueDate?.trimmingCharacters(in: .whitespacesAndNewlines), !due.isEmpty else {
            return
        }
        let amount = nextDueAmount?.trimmingCharacters(in: .whitespacesAndNewlines)
        let note = nextDueNote?.trimmingCharacters(in: .whitespacesAndNewlines)
        var body = "Due \(Club360DateFormats.displayDay(fromPostgresDay: due))"
        if let amount, !amount.isEmpty { body += " — \(amount)" }
        if let note, !note.isEmpty { body += ". \(note)" }
        await ClientDataService.notifyMemberFromCoach(
            clientId: clientId,
            kind: "payment_reminder",
            title: "Upcoming payment",
            body: body,
            refType: "payment",
            refId: clientId,
            dedupeKey: "payment_scheduled:\(clientId):\(due)"
        )
    }

    /// Member app: remind on due date and one day before (deduped).
    static func remindMemberIfDueSoon(clientId: String, canViewPayments: Bool) async {
        guard canViewPayments else { return }
        guard let settings = try? await ClientDataService.fetchPaymentSettings(clientId: clientId),
              let dueRaw = settings.nextDueDate,
              let dueDate = Club360DateFormats.postgresDay.date(from: dueRaw) else {
            return
        }
        let cal = Calendar.current
        let today = cal.startOfDay(for: Date())
        let due = cal.startOfDay(for: dueDate)
        let tomorrow = cal.date(byAdding: .day, value: 1, to: today) ?? today
        let amount = settings.nextDueAmount?.trimmingCharacters(in: .whitespacesAndNewlines)
        let amountLabel = (amount?.isEmpty == false) ? amount! : "your coach"
        if due == today {
            await ClientDataService.notifyMemberFromCoach(
                clientId: clientId,
                kind: "payment_reminder",
                title: "Payment due today",
                body: "Amount: \(amountLabel). Open Payments to confirm or pay.",
                refType: "payment",
                refId: clientId,
                dedupeKey: "payment_due_today:\(clientId):\(dueRaw)"
            )
        } else if due == tomorrow {
            await ClientDataService.notifyMemberFromCoach(
                clientId: clientId,
                kind: "payment_reminder",
                title: "Payment due tomorrow",
                body: "Due \(Club360DateFormats.displayDay(fromPostgresDay: dueRaw)) — \(amountLabel)",
                refType: "payment",
                refId: clientId,
                dedupeKey: "payment_due_tomorrow:\(clientId):\(dueRaw)"
            )
        }
    }

    /// Coach app: clients with payment due today → coach notification inbox.
    static func remindCoachOfClientsDueToday() async {
        let todayKey = Club360DateFormats.dayString(Date())
        let clients = (try? await ClientDataService.fetchClientsForCoach()) ?? []
        let assigned = clients.filter {
            !($0.coachId?.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ?? true)
        }
        for client in assigned {
            guard let cid = client.id, !cid.isEmpty,
                  let settings = try? await ClientDataService.fetchPaymentSettings(clientId: cid),
                  settings.nextDueDate == todayKey else {
                continue
            }
            let name = AdminViewModel.listTitle(for: client)
            let amount = settings.nextDueAmount?.trimmingCharacters(in: .whitespacesAndNewlines)
            var body = name
            if let amount, !amount.isEmpty { body += " — \(amount)" }
            await ClientDataService.notifyCoachAboutClient(
                clientId: cid,
                kind: "payment_reminder",
                title: "Payment due today",
                body: body,
                refType: "payment",
                refId: cid,
                dedupeKey: "coach_payment_due_today:\(cid):\(todayKey)"
            )
        }
    }

    /// After coach logs or approves a payment, advance `next_due_date` when recurrence is weekly/monthly.
    static func advanceRecurringDueDate(clientId: String) async {
        guard let settings = try? await ClientDataService.fetchPaymentSettings(clientId: clientId),
              let dueRaw = settings.nextDueDate,
              let dueDate = Club360DateFormats.postgresDay.date(from: dueRaw) else {
            return
        }
        let recurrence = (settings.dueRecurrence ?? "none").lowercased()
        let cal = Calendar.current
        let nextDate: Date?
        switch recurrence {
        case "weekly":
            nextDate = cal.date(byAdding: .weekOfYear, value: 1, to: dueDate)
        case "monthly":
            nextDate = cal.date(byAdding: .month, value: 1, to: dueDate)
        default:
            return
        }
        guard let nextDate else { return }
        let nextStr = Club360DateFormats.dayString(cal.startOfDay(for: nextDate))
        do {
            try await ClientDataService.upsertPaymentSettings(
                clientId: clientId,
                venmoUrl: settings.venmoUrl,
                zelleEmail: settings.zelleEmail,
                zellePhone: settings.zellePhone,
                note: settings.note ?? "",
                nextDueDate: nextStr,
                nextDueAmount: settings.nextDueAmount,
                nextDueNote: settings.nextDueNote,
                dueRecurrence: recurrence
            )
        } catch {
            /* best-effort */
        }
    }
}
