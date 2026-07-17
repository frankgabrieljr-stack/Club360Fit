import Foundation
import Supabase

// MARK: - DTOs

struct CommunityPostDTO: Decodable, Sendable, Identifiable, Hashable {
    let rowId: String?
    let clientId: String
    let coachId: String
    let authorDisplayName: String
    let coachDisplayName: String?
    let category: String
    let body: String
    let createdAt: String?

    enum CodingKeys: String, CodingKey {
        case rowId = "id"
        case clientId = "client_id"
        case coachId = "coach_id"
        case authorDisplayName = "author_display_name"
        case coachDisplayName = "coach_display_name"
        case category, body
        case createdAt = "created_at"
    }

    var id: String { rowId ?? "\(clientId)-\(createdAt ?? body)" }

    var coachLabel: String {
        let name = (coachDisplayName ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
        return name.isEmpty ? "Coach" : name
    }

    var categoryLabel: String {
        switch category.lowercased() {
        case "win": return "Win"
        case "question": return "Question"
        case "encouragement": return "Cheer"
        default: return "Tip"
        }
    }
}

struct CommunityCommentDTO: Decodable, Sendable, Identifiable, Hashable {
    let rowId: String?
    let postId: String
    let clientId: String?
    let authorUserId: String?
    let isCoachReply: Bool
    let authorDisplayName: String
    let body: String
    let createdAt: String?

    enum CodingKeys: String, CodingKey {
        case rowId = "id"
        case postId = "post_id"
        case clientId = "client_id"
        case authorUserId = "author_user_id"
        case isCoachReply = "is_coach_reply"
        case authorDisplayName = "author_display_name"
        case body
        case createdAt = "created_at"
    }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        rowId = try c.decodeIfPresent(String.self, forKey: .rowId)
        postId = try c.decode(String.self, forKey: .postId)
        clientId = try c.decodeIfPresent(String.self, forKey: .clientId)
        authorUserId = try c.decodeIfPresent(String.self, forKey: .authorUserId)
        isCoachReply = try c.decodeIfPresent(Bool.self, forKey: .isCoachReply) ?? false
        authorDisplayName = try c.decodeIfPresent(String.self, forKey: .authorDisplayName) ?? "Member"
        body = try c.decodeIfPresent(String.self, forKey: .body) ?? ""
        createdAt = try c.decodeIfPresent(String.self, forKey: .createdAt)
    }

    var id: String { rowId ?? "\(postId)-\(createdAt ?? body)" }
}

struct CommunityMemberDTO: Decodable, Sendable, Identifiable, Hashable {
    let clientId: String
    let userId: String?
    let memberDisplayName: String
    let coachId: String
    let coachDisplayName: String
    let avatarUrl: String?

    enum CodingKeys: String, CodingKey {
        case clientId = "client_id"
        case userId = "user_id"
        case memberDisplayName = "member_display_name"
        case coachId = "coach_id"
        case coachDisplayName = "coach_display_name"
        case avatarUrl = "avatar_url"
    }

    init(
        clientId: String,
        userId: String?,
        memberDisplayName: String,
        coachId: String,
        coachDisplayName: String,
        avatarUrl: String?
    ) {
        self.clientId = clientId
        self.userId = userId
        self.memberDisplayName = memberDisplayName
        self.coachId = coachId
        self.coachDisplayName = coachDisplayName
        self.avatarUrl = avatarUrl
    }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        clientId = try c.decode(String.self, forKey: .clientId)
        userId = try c.decodeIfPresent(String.self, forKey: .userId)
        memberDisplayName = try c.decodeIfPresent(String.self, forKey: .memberDisplayName) ?? "Member"
        coachId = try c.decode(String.self, forKey: .coachId)
        coachDisplayName = try c.decodeIfPresent(String.self, forKey: .coachDisplayName) ?? "Coach"
        avatarUrl = try c.decodeIfPresent(String.self, forKey: .avatarUrl)
    }

    var id: String { clientId }

    /// Best available public photo URL (profile metadata, then storage path).
    var resolvedAvatarURL: URL? {
        if let raw = avatarUrl?.trimmingCharacters(in: .whitespacesAndNewlines), !raw.isEmpty,
           let url = URL(string: raw) {
            return url
        }
        if let uid = userId, !uid.isEmpty {
            return ClientDataService.publicAvatarURLForAuthUserId(uid)
        }
        return nil
    }
}

private struct CommunityPostInsert: Encodable, Sendable {
    let client_id: String
    let coach_id: String
    let author_display_name: String
    let coach_display_name: String
    let category: String
    let body: String
}

private struct CommunityCommentInsert: Encodable, Sendable {
    let post_id: String
    let client_id: String?
    let author_user_id: String
    let is_coach_reply: Bool
    let author_display_name: String
    let body: String
}

// MARK: - API

extension ClientDataService {
    /// Same pattern as other extensions — `db` on the main type is `private`.
    private static var communityDb: SupabaseClient { Club360FitSupabase.shared }

    /// App-wide peer feed (newest first). Each post includes the author's coach name.
    static func fetchCommunityPosts(limit: Int = 50) async throws -> [CommunityPostDTO] {
        try await communityDb
            .from("community_posts")
            .select()
            .order("created_at", ascending: false)
            .limit(limit)
            .execute()
            .value
    }

    /// One member's community posts (newest first).
    static func fetchCommunityPosts(forClientId clientId: String, limit: Int = 50) async throws -> [CommunityPostDTO] {
        try await communityDb
            .from("community_posts")
            .select()
            .eq("client_id", value: clientId)
            .order("created_at", ascending: false)
            .limit(limit)
            .execute()
            .value
    }

    /// Privacy-safe directory: member display name + assigned coach name only.
    static func fetchCommunityMemberDirectory() async throws -> [CommunityMemberDTO] {
        try await communityDb
            .rpc("fetch_community_member_directory")
            .execute()
            .value
    }

    static func fetchCommunityComments(postId: String) async throws -> [CommunityCommentDTO] {
        try await communityDb
            .from("community_comments")
            .select()
            .eq("post_id", value: postId)
            .order("created_at", ascending: true)
            .execute()
            .value
    }

    static func createCommunityPost(
        clientId: String,
        coachId: String,
        authorDisplayName: String,
        coachDisplayName: String,
        category: String,
        body: String
    ) async throws {
        let safeCategory = ["tip", "win", "question", "encouragement"].contains(category.lowercased())
            ? category.lowercased()
            : "tip"
        let trimmed = body.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        let row = CommunityPostInsert(
            client_id: clientId,
            coach_id: coachId,
            author_display_name: authorDisplayName,
            coach_display_name: coachDisplayName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
                ? "Coach"
                : coachDisplayName.trimmingCharacters(in: .whitespacesAndNewlines),
            category: safeCategory,
            body: trimmed
        )
        try await communityDb
            .from("community_posts")
            .insert(row)
            .execute()
    }

    /// Member reply on a community post.
    /// - Parameter postAuthorClientId: Post owner's `clients.id` — used to push notify (DB trigger inserts the inbox row).
    static func createCommunityComment(
        postId: String,
        clientId: String,
        authorDisplayName: String,
        body: String,
        postAuthorClientId: String
    ) async throws {
        guard let uid = communityDb.auth.currentSession?.user.id.uuidString else {
            throw NSError(domain: "Community", code: 401, userInfo: [NSLocalizedDescriptionKey: "Not signed in."])
        }
        let trimmed = body.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        let row = CommunityCommentInsert(
            post_id: postId,
            client_id: clientId,
            author_user_id: uid.lowercased(),
            is_coach_reply: false,
            author_display_name: authorDisplayName,
            body: trimmed
        )
        let created: CommunityCommentDTO = try await communityDb
            .from("community_comments")
            .insert(row)
            .select()
            .single()
            .execute()
            .value
        // Self-reply: trigger skips inbox rows; skip push too.
        guard clientId.lowercased() != postAuthorClientId.lowercased() else { return }
        await pushCommunityReplyNotifications(
            postAuthorClientId: postAuthorClientId,
            postId: postId,
            replierDisplayName: authorDisplayName,
            body: trimmed,
            commentId: created.rowId,
            notifyCoach: true
        )
    }

    /// Coach/admin reply — shown with a Coach badge (no clients row required).
    /// - Parameter postAuthorCoachId: Assigned coach on the post — used to avoid pushing the replier about their own reply.
    static func createCommunityCoachComment(
        postId: String,
        authorDisplayName: String,
        body: String,
        postAuthorClientId: String,
        postAuthorCoachId: String
    ) async throws {
        guard let uid = communityDb.auth.currentSession?.user.id.uuidString else {
            throw NSError(domain: "Community", code: 401, userInfo: [NSLocalizedDescriptionKey: "Not signed in."])
        }
        let trimmed = body.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        let name = authorDisplayName.trimmingCharacters(in: .whitespacesAndNewlines)
        let display = name.isEmpty ? "Coach" : name
        let row = CommunityCommentInsert(
            post_id: postId,
            client_id: nil,
            author_user_id: uid.lowercased(),
            is_coach_reply: true,
            author_display_name: display,
            body: trimmed
        )
        let created: CommunityCommentDTO = try await communityDb
            .from("community_comments")
            .insert(row)
            .select()
            .single()
            .execute()
            .value
        let isAssignedCoach = postAuthorCoachId.lowercased() == uid.lowercased()
        await pushCommunityReplyNotifications(
            postAuthorClientId: postAuthorClientId,
            postId: postId,
            replierDisplayName: display,
            body: trimmed,
            commentId: created.rowId,
            notifyCoach: !isAssignedCoach
        )
    }

    /// Fire device pushes after the DB trigger has written `client_notifications` rows.
    private static func pushCommunityReplyNotifications(
        postAuthorClientId: String,
        postId: String,
        replierDisplayName: String,
        body: String,
        commentId: String?,
        notifyCoach: Bool
    ) async {
        let authorId = postAuthorClientId.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !authorId.isEmpty else { return }
        let name = replierDisplayName.trimmingCharacters(in: .whitespacesAndNewlines)
        let preview = String(body.trimmingCharacters(in: .whitespacesAndNewlines).prefix(120))
        let text = (name.isEmpty ? "Someone" : name) + (preview.isEmpty ? "" : ": \(preview)")
        let memberDedupe = commentId.map { "community_reply:\($0)" }
        await triggerDevicePushForNotification(
            ClientNotificationInsert(
                clientId: authorId,
                kind: "community_reply",
                title: "New community reply",
                body: text,
                refType: "community_post",
                refId: postId,
                dedupeKey: memberDedupe,
                visibleToClient: true
            )
        )
        guard notifyCoach, let commentId, !commentId.isEmpty else { return }
        await triggerDevicePushForNotification(
            ClientNotificationInsert(
                clientId: authorId,
                kind: "community_reply",
                title: "Community reply",
                body: text,
                refType: "community_post",
                refId: postId,
                dedupeKey: "community_reply_coach:\(commentId)",
                visibleToClient: false
            )
        )
    }

    static func deleteCommunityPost(id: String) async throws {
        try await communityDb
            .from("community_posts")
            .delete()
            .eq("id", value: id)
            .execute()
    }

    static func deleteCommunityComment(id: String) async throws {
        try await communityDb
            .from("community_comments")
            .delete()
            .eq("id", value: id)
            .execute()
    }
}
