import Foundation
import Supabase

extension ClientDataService {
    private static var notifDb: SupabaseClient { Club360FitSupabase.shared }

    /// Inserts a `client_notifications` row (coach or client, per RLS).
    static func insertClientNotification(_ row: ClientNotificationInsert) async throws {
        try await notifDb
            .from("client_notifications")
            .insert(row)
            .execute()
    }

    /// Coach-facing updates shown in the member’s Updates feed (`visible_to_client` = true).
    static func notifyMemberFromCoach(
        clientId: String,
        kind: String,
        title: String,
        body: String,
        refType: String? = nil,
        refId: String? = nil,
        dedupeKey: String? = nil
    ) async {
        let row = ClientNotificationInsert(
            clientId: clientId,
            kind: kind,
            title: title,
            body: body,
            refType: refType,
            refId: refId,
            dedupeKey: dedupeKey,
            visibleToClient: true
        )
        try? await insertClientNotification(row)
    }

    /// Alerts for the coach when viewing this client (hidden from the member app).
    static func notifyCoachAboutClient(
        clientId: String,
        kind: String,
        title: String,
        body: String,
        refType: String? = nil,
        refId: String? = nil,
        dedupeKey: String? = nil
    ) async {
        let row = ClientNotificationInsert(
            clientId: clientId,
            kind: kind,
            title: title,
            body: body,
            refType: refType,
            refId: refId,
            dedupeKey: dedupeKey,
            visibleToClient: false
        )
        try? await insertClientNotification(row)
    }

    // MARK: - Coach hub (all coached clients; RLS restricts rows)

    /// Every `client_notifications` row visible to this coach (newest first).
    static func fetchNotificationsForCoach(limit: Int = 80) async throws -> [ClientNotificationDTO] {
        try await notifDb
            .from("client_notifications")
            .select()
            .order("created_at", ascending: false)
            .limit(limit)
            .execute()
            .value
    }

    /// Same as [fetchNotificationsForCoach(limit:)] but only rows whose `client_id` is in [clientIds] (defense-in-depth with RLS).
    static func fetchNotificationsForCoach(clientIds: Set<String>, limit: Int = 80) async throws -> [ClientNotificationDTO] {
        guard !clientIds.isEmpty else { return [] }
        let overfetch = min(max(limit * 4, limit), 500)
        let rows = try await fetchNotificationsForCoach(limit: overfetch)
        return Array(rows.filter { clientIds.contains($0.clientId) }.prefix(limit))
    }

    static func unreadNotificationCountForCoach() async throws -> Int {
        let clients = try await fetchClientsForCoach()
        let ids = Set(clients.compactMap { $0.id })
        guard !ids.isEmpty else { return 0 }
        let rows = try await fetchNotificationsForCoach(limit: 500)
        return rows.filter { ids.contains($0.clientId) && $0.coachReadAt == nil }.count
    }

    /// Marks one row read for the **coach** (`coach_read_at`), not the member’s `read_at`.
    static func markNotificationReadAsCoach(notificationId: String) async throws {
        let now = ISO8601DateFormatter().string(from: Date())
        let patch = CoachNotificationReadAtPatch(coachReadAt: now)
        try await notifDb
            .from("client_notifications")
            .update(patch)
            .eq("id", value: notificationId)
            .execute()
    }

    /// Main Coach Hub: clear coach unread across all coached clients.
    static func markAllNotificationsReadForCoach() async throws {
        let clients = try await fetchClientsForCoach()
        let ids = Set(clients.compactMap { $0.id })
        let rows = try await fetchNotificationsForCoach(limit: 500)
        let unread = rows.filter { ids.contains($0.clientId) && $0.coachReadAt == nil }
        for n in unread {
            if let id = n.rowId {
                try await markNotificationReadAsCoach(notificationId: id)
            }
        }
    }

    /// Coach viewing one member: clear coach unread for that client only.
    static func markAllCoachNotificationsReadForClient(clientId: String) async throws {
        let rows = try await fetchClientNotifications(clientId: clientId, limit: 500)
        let unread = rows.filter { $0.coachReadAt == nil }
        for n in unread {
            if let id = n.rowId {
                try await markNotificationReadAsCoach(notificationId: id)
            }
        }
    }
}
