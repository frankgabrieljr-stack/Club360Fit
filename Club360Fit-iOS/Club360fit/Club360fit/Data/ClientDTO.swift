import Foundation

/// Mirrors Android `ClientDto` / `clients` table.
struct ClientDTO: Decodable, Sendable {
    let id: String?
    let userId: String
    let fullName: String?
    let age: Int?
    let heightCm: Int?
    let weightKg: Int?
    let goal: String?
    let canViewNutrition: Bool
    let canViewWorkouts: Bool
    let canViewPayments: Bool
    let canViewEvents: Bool
    /// When this `clients` row was created (member since); ISO-8601 from Supabase.
    let createdAt: String?

    enum CodingKeys: String, CodingKey {
        case id
        case userId = "user_id"
        case fullName = "full_name"
        case age
        case heightCm = "height_cm"
        case weightKg = "weight_kg"
        case goal
        case canViewNutrition = "can_view_nutrition"
        case canViewWorkouts = "can_view_workouts"
        case canViewPayments = "can_view_payments"
        case canViewEvents = "can_view_events"
        case createdAt = "created_at"
    }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = try c.decodeIfPresent(String.self, forKey: .id)
        userId = try c.decode(String.self, forKey: .userId)
        fullName = try c.decodeIfPresent(String.self, forKey: .fullName)
        age = try c.decodeIfPresent(Int.self, forKey: .age)
        heightCm = try c.decodeIfPresent(Int.self, forKey: .heightCm)
        weightKg = try c.decodeIfPresent(Int.self, forKey: .weightKg)
        goal = try c.decodeIfPresent(String.self, forKey: .goal)
        canViewNutrition = try c.decodeIfPresent(Bool.self, forKey: .canViewNutrition) ?? false
        canViewWorkouts = try c.decodeIfPresent(Bool.self, forKey: .canViewWorkouts) ?? false
        canViewPayments = try c.decodeIfPresent(Bool.self, forKey: .canViewPayments) ?? false
        canViewEvents = try c.decodeIfPresent(Bool.self, forKey: .canViewEvents) ?? false
        createdAt = try c.decodeIfPresent(String.self, forKey: .createdAt)
    }

    /// Stable row identity for lists (`clients.id` preferred).
    var stableId: String {
        if let s = id, !s.isEmpty { return s }
        return userId
    }

    /// One line for coach client cards: age, height, weight, goal (matches Android `buildClientMemberSummaryLine`).
    var memberSummaryLine: String {
        var parts: [String] = []
        if let age { parts.append("Age \(age)") }
        if let hi = Club360Units.feetInchesLabel(fromCm: heightCm) { parts.append(hi) }
        if let w = weightKg, let lbs = Club360Units.displayPoundsFromKg(Double(w)) { parts.append(lbs) }
        let g = goal?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        if !g.isEmpty { parts.append("Goal: \(g)") }
        return parts.joined(separator: " · ")
    }
}
