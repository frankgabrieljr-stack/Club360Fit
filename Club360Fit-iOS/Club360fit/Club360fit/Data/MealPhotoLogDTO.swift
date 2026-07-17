import Foundation

/// Which meal of the day a photo belongs to (DB `meal_photo_logs.meal_slot`).
enum MealPhotoSlot: String, CaseIterable, Codable, Sendable, Identifiable {
    case breakfast
    case lunch
    case dinner
    case snack
    case other

    var id: String { rawValue }

    var label: String {
        switch self {
        case .breakfast: return "Breakfast"
        case .lunch: return "Lunch"
        case .dinner: return "Dinner"
        case .snack: return "Snack"
        case .other: return "Other"
        }
    }

    var systemImage: String {
        switch self {
        case .breakfast: return "cup.and.saucer.fill"
        case .lunch: return "fork.knife"
        case .dinner: return "moon.stars.fill"
        case .snack: return "leaf.fill"
        case .other: return "square.grid.2x2.fill"
        }
    }

    /// Stable sort within a day: meals first, then snacks, then other.
    var sortRank: Int {
        switch self {
        case .breakfast: return 0
        case .lunch: return 1
        case .dinner: return 2
        case .snack: return 3
        case .other: return 4
        }
    }

    static func parse(_ raw: String?) -> MealPhotoSlot {
        guard let raw, let slot = MealPhotoSlot(rawValue: raw.lowercased()) else { return .other }
        return slot
    }
}

/// One calendar day of meal photos (newest days first when built via `MealPhotoDayGroup.grouped`).
struct MealPhotoDayGroup: Identifiable, Sendable {
    let logDate: String
    let logs: [MealPhotoLogDTO]

    var id: String { logDate }

    var displayTitle: String {
        Self.friendlyDayTitle(logDate: logDate)
    }

    var mealCountLabel: String {
        let n = logs.count
        return n == 1 ? "1 photo" : "\(n) photos"
    }

    /// Distinct slots logged that day (for the day summary chips).
    var slotsPresent: [MealPhotoSlot] {
        var seen = Set<MealPhotoSlot>()
        var ordered: [MealPhotoSlot] = []
        for log in logs {
            let slot = log.resolvedSlot
            if seen.insert(slot).inserted {
                ordered.append(slot)
            }
        }
        return ordered.sorted { $0.sortRank < $1.sortRank }
    }

    static func grouped(from logs: [MealPhotoLogDTO]) -> [MealPhotoDayGroup] {
        let byDay = Dictionary(grouping: logs) { $0.logDate }
        let dayOrder = byDay.keys.sorted(by: >)
        return dayOrder.map { day in
            let dayLogs = (byDay[day] ?? []).sorted { a, b in
                if a.resolvedSlot.sortRank != b.resolvedSlot.sortRank {
                    return a.resolvedSlot.sortRank < b.resolvedSlot.sortRank
                }
                return (a.createdAt ?? "") < (b.createdAt ?? "")
            }
            return MealPhotoDayGroup(logDate: day, logs: dayLogs)
        }
    }

    static func friendlyDayTitle(logDate: String) -> String {
        guard let date = Club360DateFormats.postgresDay.date(from: logDate) else {
            return Club360DateFormats.displayDay(fromPostgresDay: logDate)
        }
        let cal = Calendar.current
        if cal.isDateInToday(date) { return "Today" }
        if cal.isDateInYesterday(date) { return "Yesterday" }
        let fmt = DateFormatter()
        fmt.locale = .current
        fmt.setLocalizedDateFormatFromTemplate("EEE MMM d")
        return fmt.string(from: date)
    }
}

/// Mirrors Android `MealPhotoLogDto` / `meal_photo_logs`.
struct MealPhotoLogDTO: Decodable, Sendable {
    let id: String?
    let clientId: String
    let logDate: String
    let storagePath: String
    let notes: String?
    let mealSlot: String?
    let createdAt: String?
    let coachFeedback: String?
    let coachFeedbackUpdatedAt: String?

    enum CodingKeys: String, CodingKey {
        case id
        case clientId = "client_id"
        case logDate = "log_date"
        case storagePath = "storage_path"
        case notes
        case mealSlot = "meal_slot"
        case createdAt = "created_at"
        case coachFeedback = "coach_feedback"
        case coachFeedbackUpdatedAt = "coach_feedback_updated_at"
    }

    var rowIdentity: String {
        if let id, !id.isEmpty { return id }
        return "\(clientId)-\(logDate)-\(storagePath)"
    }

    var resolvedSlot: MealPhotoSlot { MealPhotoSlot.parse(mealSlot) }

    var needsCoachFeedback: Bool {
        (coachFeedback ?? "").trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }
}

struct MealPhotoLogInsert: Encodable, Sendable {
    let clientId: String
    let logDate: String
    let storagePath: String
    let notes: String
    let mealSlot: String

    enum CodingKeys: String, CodingKey {
        case clientId = "client_id"
        case logDate = "log_date"
        case storagePath = "storage_path"
        case notes
        case mealSlot = "meal_slot"
    }
}
