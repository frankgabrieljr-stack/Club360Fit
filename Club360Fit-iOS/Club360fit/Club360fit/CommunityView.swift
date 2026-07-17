import Auth
import Observation
import Supabase
import SwiftUI

enum CommunityViewerMode: Sendable {
    /// Member shell: post + reply.
    case member
    /// Coach shell: browse + reply (Coach badge); no new posts.
    case coach
}

private enum CommunityPane: String, CaseIterable, Identifiable {
    case feed
    case members

    var id: String { rawValue }

    var title: String {
        switch self {
        case .feed: return "Feed"
        case .members: return "Members"
        }
    }
}

// MARK: - Shell entry

struct CommunityView: View {
    var mode: CommunityViewerMode = .member

    var body: some View {
        Group {
            switch mode {
            case .member:
                MemberCommunityGate()
            case .coach:
                CommunityHubView(mode: .coach)
            }
        }
        .navigationTitle("Community")
        .navigationBarTitleDisplayMode(.large)
        .toolbarBackground(.ultraThinMaterial, for: .navigationBar)
    }
}

private struct MemberCommunityGate: View {
    @Environment(ClientHomeViewModel.self) private var home

    var body: some View {
        if home.clientId == nil {
            ContentUnavailableView("No profile", systemImage: "person.crop.circle.badge.xmark")
        } else {
            CommunityHubView(mode: .member, home: home)
        }
    }
}

// MARK: - Hub (Feed + Members)

private struct CommunityHubView: View {
    let mode: CommunityViewerMode
    var home: ClientHomeViewModel? = nil
    @State private var model = CommunityViewModel()
    @State private var pane: CommunityPane = .feed
    @State private var showCompose = false
    @State private var selectedPost: CommunityPostDTO?
    @State private var selectedMember: CommunityMemberDTO?

    private var isCoachMode: Bool { mode == .coach }

    var body: some View {
        ZStack {
            Club360ScreenBackground()

            if model.isLoading, model.posts.isEmpty, model.members.isEmpty {
                ProgressView("Loading community…")
                    .tint(Club360Theme.tealDark)
            } else {
                VStack(spacing: 0) {
                    Picker("Community", selection: $pane) {
                        ForEach(CommunityPane.allCases) { p in
                            Text(p.title).tag(p)
                        }
                    }
                    .pickerStyle(.segmented)
                    .padding(.horizontal, 18)
                    .padding(.top, 8)
                    .padding(.bottom, 12)

                    switch pane {
                    case .feed:
                        feedBody
                    case .members:
                        membersBody
                    }
                }
            }
        }
        .task(id: taskKey) {
            await model.load(mode: mode, home: home)
        }
        .refreshable {
            await model.load(mode: mode, home: home, showLoading: false)
        }
        .sheet(isPresented: $showCompose) {
            CommunityComposeSheet(
                categories: CommunityViewModel.categories,
                coachName: model.coachDisplayName,
                isSaving: model.isSaving,
                onSave: { category, body in
                    Task {
                        let ok = await model.createPost(category: category, body: body)
                        if ok { showCompose = false }
                    }
                },
                onDismiss: { showCompose = false }
            )
        }
        .navigationDestination(item: $selectedPost) { post in
            CommunityPostDetailView(
                post: post,
                mode: mode,
                ownClientId: model.clientId,
                ownUserId: model.authorUserId,
                authorDisplayName: model.authorDisplayName,
                onOpenAuthor: { openAuthor(of: post) }
            )
        }
        .navigationDestination(item: $selectedMember) { member in
            CommunityMemberProfileView(
                member: member,
                mode: mode,
                ownClientId: model.clientId,
                ownUserId: model.authorUserId,
                authorDisplayName: model.authorDisplayName
            )
        }
        .alert("Could not save", isPresented: Binding(
            get: { model.errorMessage != nil },
            set: { if !$0 { model.errorMessage = nil } }
        )) {
            Button("OK", role: .cancel) { model.errorMessage = nil }
        } message: {
            Text(model.errorMessage ?? "")
        }
    }

    private var taskKey: String {
        switch mode {
        case .member: return home?.clientId ?? "member-none"
        case .coach: return "coach-\(model.authorUserId ?? "pending")"
        }
    }

    private var feedBody: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 18) {
                headerCard

                if isCoachMode {
                    Text("Browse the peer feed and reply with encouragement. Replies show a Coach badge. Members keep posting — coaches don’t start new posts here.")
                        .font(.footnote)
                        .foregroundStyle(Club360Theme.captionOnGlass)
                        .padding(14)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .club360Glass(cornerRadius: 18)
                } else if !model.hasCoach {
                    Text("You can browse and reply once you have a profile. Posting unlocks after a coach claims you.")
                        .font(.footnote)
                        .foregroundStyle(Club360Theme.captionOnGlass)
                        .padding(14)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .club360Glass(cornerRadius: 18)
                }

                if model.posts.isEmpty {
                    ContentUnavailableView(
                        isCoachMode ? "No posts yet" : "Be the first to post",
                        systemImage: "bubble.left.and.bubble.right.fill",
                        description: Text(
                            isCoachMode
                                ? "When members share tips, wins, or questions, they appear here for you to encourage."
                                : "Share a tip, celebrate a win, or ask a question. Every member can see posts and which coach you train with."
                        )
                    )
                    .padding(.top, 12)
                } else {
                    LazyVStack(spacing: 12) {
                        ForEach(model.posts) { post in
                            CommunityPostCard(
                                post: post,
                                isOwnPost: post.clientId == model.clientId,
                                onOpenAuthor: { openAuthor(of: post) },
                                onOpenPost: { selectedPost = post }
                            )
                        }
                    }
                }
            }
            .padding(.horizontal, 18)
            .padding(.bottom, model.hasCoach && !isCoachMode ? 88 : 28)
        }
        .overlay(alignment: .bottomTrailing) {
            if model.hasCoach, !isCoachMode {
                Button {
                    showCompose = true
                } label: {
                    Image(systemName: "plus")
                        .font(.title2.weight(.semibold))
                        .foregroundStyle(.white)
                        .frame(width: 56, height: 56)
                        .background(Club360Theme.primaryButtonGradient, in: Circle())
                        .shadow(color: Club360Theme.burgundy.opacity(0.35), radius: 12, y: 6)
                }
                .padding(.trailing, 22)
                .padding(.bottom, 18)
                .accessibilityLabel("New post")
            }
        }
    }

    private var membersBody: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 18) {
                VStack(alignment: .leading, spacing: 8) {
                    Text("Members & coaches")
                        .font(.subheadline.weight(.bold))
                        .foregroundStyle(Club360Theme.cardTitle)
                        .textCase(.uppercase)
                    Text(
                        isCoachMode
                            ? "Tap a member to see their public community profile and posts."
                            : "Tap a member to see their photo and community posts. Only names and public photos — never private health details."
                    )
                        .font(.footnote)
                        .foregroundStyle(Club360Theme.captionOnGlass)
                        .fixedSize(horizontal: false, vertical: true)
                }
                .padding(16)
                .frame(maxWidth: .infinity, alignment: .leading)
                .club360Glass(cornerRadius: 22)

                if model.members.isEmpty {
                    ContentUnavailableView(
                        "No members yet",
                        systemImage: "person.2",
                        description: Text("When members are assigned to coaches, they’ll show up here.")
                    )
                } else {
                    ForEach(model.membersByCoach, id: \.coachId) { group in
                        VStack(alignment: .leading, spacing: 10) {
                            Text("Coach · \(group.coachName)")
                                .font(.caption.weight(.bold))
                                .foregroundStyle(Club360Theme.burgundy)
                                .textCase(.uppercase)
                            ForEach(group.members) { member in
                                Button {
                                    selectedMember = member
                                } label: {
                                    CommunityMemberRow(
                                        member: member,
                                        isYou: member.clientId == model.clientId
                                    )
                                }
                                .buttonStyle(.plain)
                            }
                        }
                    }
                }
            }
            .padding(.horizontal, 18)
            .padding(.bottom, 28)
        }
    }

    private func openAuthor(of post: CommunityPostDTO) {
        if let match = model.members.first(where: { $0.clientId == post.clientId }) {
            selectedMember = match
            return
        }
        selectedMember = CommunityMemberDTO(
            clientId: post.clientId,
            userId: nil,
            memberDisplayName: post.authorDisplayName,
            coachId: post.coachId,
            coachDisplayName: post.coachLabel,
            avatarUrl: nil
        )
    }

    private var headerCard: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(isCoachMode ? "Community" : "Peer support")
                .font(.subheadline.weight(.bold))
                .foregroundStyle(Club360Theme.cardTitle)
                .textCase(.uppercase)
            Text(
                isCoachMode
                    ? "See what members are sharing. Reply to encourage them — your name shows with a Coach badge."
                    : "Connect with members across Club360Fit. Posts show your name and coach so people know who trains with whom. Keep it supportive — no medical advice or private health details."
            )
                .font(.footnote)
                .foregroundStyle(Club360Theme.captionOnGlass)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .club360Glass(cornerRadius: 22)
    }
}

// MARK: - Post detail + comments

private struct CommunityPostDetailView: View {
    let post: CommunityPostDTO
    let mode: CommunityViewerMode
    let ownClientId: String?
    let ownUserId: String?
    let authorDisplayName: String
    var onOpenAuthor: (() -> Void)? = nil

    @Environment(\.dismiss) private var dismiss
    @State private var comments: [CommunityCommentDTO] = []
    @State private var isLoading = true
    @State private var isSaving = false
    @State private var errorMessage: String?
    @State private var replyText = ""
    @State private var showDeleteConfirm = false

    private var isOwnPost: Bool {
        guard mode == .member, let ownClientId else { return false }
        return post.clientId == ownClientId
    }

    private var canReply: Bool {
        switch mode {
        case .member: return ownClientId != nil
        case .coach: return ownUserId != nil
        }
    }

    var body: some View {
        ZStack {
            Club360ScreenBackground()

            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    CommunityPostCard(
                        post: post,
                        isOwnPost: isOwnPost,
                        showReplyHint: false,
                        onOpenAuthor: onOpenAuthor
                    )

                    Text("Replies")
                        .font(.subheadline.weight(.bold))
                        .foregroundStyle(Club360Theme.cardTitle)
                        .textCase(.uppercase)

                    if isLoading {
                        ProgressView()
                            .frame(maxWidth: .infinity)
                    } else if comments.isEmpty {
                        Text("No replies yet. Be the first to encourage them.")
                            .font(.footnote)
                            .foregroundStyle(Club360Theme.captionOnGlass)
                    } else {
                        ForEach(comments) { comment in
                            CommunityCommentRow(
                                comment: comment,
                                isOwn: isOwnComment(comment),
                                onDelete: {
                                    Task { await deleteComment(comment) }
                                }
                            )
                        }
                    }

                    replyComposer
                }
                .padding(.horizontal, 18)
                .padding(.bottom, 24)
            }
        }
        .navigationTitle("Post")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            if isOwnPost, post.rowId != nil {
                ToolbarItem(placement: .destructiveAction) {
                    Button("Delete", role: .destructive) {
                        showDeleteConfirm = true
                    }
                }
            }
        }
        .task(id: post.id) {
            await loadComments()
        }
        .alert("Delete post?", isPresented: $showDeleteConfirm) {
            Button("Cancel", role: .cancel) {}
            Button("Delete", role: .destructive) {
                Task { await deletePost() }
            }
        } message: {
            Text("This removes the post and all replies.")
        }
        .alert("Something went wrong", isPresented: Binding(
            get: { errorMessage != nil },
            set: { if !$0 { errorMessage = nil } }
        )) {
            Button("OK", role: .cancel) { errorMessage = nil }
        } message: {
            Text(errorMessage ?? "")
        }
    }

    private var replyComposer: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(mode == .coach ? "Reply as coach" : "Add a reply")
                .font(.caption.weight(.bold))
                .foregroundStyle(Club360Theme.captionOnGlass)
            if mode == .coach {
                Text("Members will see a Coach badge next to your name.")
                    .font(.caption2)
                    .foregroundStyle(Club360Theme.captionOnGlass)
            }
            TextField("Write something supportive…", text: $replyText, axis: .vertical)
                .lineLimit(3 ... 6)
                .padding(12)
                .background(.white.opacity(0.55), in: RoundedRectangle(cornerRadius: 16, style: .continuous))
            Button(isSaving ? "Sending…" : "Send reply") {
                Task { await sendReply() }
            }
            .buttonStyle(Club360PrimaryGradientButtonStyle())
            .disabled(isSaving || replyText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || !canReply)
        }
        .padding(16)
        .club360Glass(cornerRadius: 22)
    }

    private func isOwnComment(_ comment: CommunityCommentDTO) -> Bool {
        if comment.isCoachReply {
            guard let ownUserId, let author = comment.authorUserId else { return false }
            return author.lowercased() == ownUserId.lowercased()
        }
        guard let ownClientId, let cid = comment.clientId else { return false }
        return cid == ownClientId
    }

    private func loadComments() async {
        guard let pid = post.rowId else {
            isLoading = false
            return
        }
        isLoading = true
        defer { isLoading = false }
        comments = (try? await ClientDataService.fetchCommunityComments(postId: pid)) ?? []
    }

    private func sendReply() async {
        guard let pid = post.rowId else { return }
        isSaving = true
        defer { isSaving = false }
        do {
            switch mode {
            case .member:
                guard let cid = ownClientId else { return }
                try await ClientDataService.createCommunityComment(
                    postId: pid,
                    clientId: cid,
                    authorDisplayName: authorDisplayName,
                    body: replyText,
                    postAuthorClientId: post.clientId
                )
            case .coach:
                try await ClientDataService.createCommunityCoachComment(
                    postId: pid,
                    authorDisplayName: authorDisplayName,
                    body: replyText,
                    postAuthorClientId: post.clientId,
                    postAuthorCoachId: post.coachId
                )
            }
            replyText = ""
            await loadComments()
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    private func deletePost() async {
        guard let pid = post.rowId else { return }
        isSaving = true
        defer { isSaving = false }
        do {
            try await ClientDataService.deleteCommunityPost(id: pid)
            dismiss()
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    private func deleteComment(_ comment: CommunityCommentDTO) async {
        guard let cid = comment.rowId else { return }
        do {
            try await ClientDataService.deleteCommunityComment(id: cid)
            await loadComments()
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}

// MARK: - Compose

private struct CommunityComposeSheet: View {
    let categories: [(id: String, label: String)]
    let coachName: String
    let isSaving: Bool
    let onSave: (String, String) -> Void
    let onDismiss: () -> Void

    @State private var category = "tip"
    @State private var bodyText = ""

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    Picker("Category", selection: $category) {
                        ForEach(categories, id: \.id) { item in
                            Text(item.label).tag(item.id)
                        }
                    }
                    .pickerStyle(.segmented)
                } header: {
                    Text("Type")
                }
                Section {
                    TextField("Share a tip, win, or question…", text: $bodyText, axis: .vertical)
                        .lineLimit(4 ... 10)
                } header: {
                    Text("Message")
                } footer: {
                    Text("Visible to all members. Your coach (\(coachName)) will show on the post. Avoid sharing weight, medical info, or meal photos.")
                }
            }
            .club360FormScreen()
            .navigationTitle("New post")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel", action: onDismiss)
                        .disabled(isSaving)
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(isSaving ? "Posting…" : "Post") {
                        onSave(category, bodyText)
                    }
                    .disabled(isSaving || bodyText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                }
            }
        }
        .presentationDetents([.medium, .large])
    }
}

// MARK: - Member profile

private struct CommunityMemberProfileView: View {
    let member: CommunityMemberDTO
    let mode: CommunityViewerMode
    let ownClientId: String?
    let ownUserId: String?
    let authorDisplayName: String

    @State private var posts: [CommunityPostDTO] = []
    @State private var isLoading = true
    @State private var errorMessage: String?
    @State private var selectedPost: CommunityPostDTO?

    private var isYou: Bool {
        guard mode == .member, let ownClientId else { return false }
        return member.clientId == ownClientId
    }

    var body: some View {
        ZStack {
            Club360ScreenBackground()

            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    profileHeader

                    Text("Community activity")
                        .font(.subheadline.weight(.bold))
                        .foregroundStyle(Club360Theme.cardTitle)
                        .textCase(.uppercase)

                    if isLoading {
                        ProgressView()
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 24)
                    } else if let errorMessage {
                        Text(errorMessage)
                            .font(.footnote)
                            .foregroundStyle(.red)
                    } else if posts.isEmpty {
                        Text("No community posts yet.")
                            .font(.footnote)
                            .foregroundStyle(Club360Theme.captionOnGlass)
                    } else {
                        LazyVStack(spacing: 12) {
                            ForEach(posts) { post in
                                CommunityPostCard(
                                    post: post,
                                    isOwnPost: post.clientId == ownClientId,
                                    onOpenPost: { selectedPost = post }
                                )
                            }
                        }
                    }
                }
                .padding(.horizontal, 18)
                .padding(.bottom, 28)
            }
        }
        .navigationTitle(isYou ? "Your profile" : member.memberDisplayName)
        .navigationBarTitleDisplayMode(.inline)
        .task(id: member.clientId) {
            await loadPosts()
        }
        .refreshable {
            await loadPosts()
        }
        .navigationDestination(item: $selectedPost) { post in
            CommunityPostDetailView(
                post: post,
                mode: mode,
                ownClientId: ownClientId,
                ownUserId: ownUserId,
                authorDisplayName: authorDisplayName
            )
        }
    }

    private var profileHeader: some View {
        VStack(spacing: 14) {
            CommunityAvatarView(url: member.resolvedAvatarURL, size: 96)
            Text(member.memberDisplayName)
                .font(.title2.weight(.bold))
                .foregroundStyle(Club360Theme.cardTitle)
            if isYou {
                Text("You")
                    .font(.caption.weight(.bold))
                    .foregroundStyle(Club360Theme.burgundy)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 4)
                    .background(Club360Theme.burgundy.opacity(0.12), in: Capsule())
            }
            HStack(spacing: 6) {
                Image(systemName: "figure.strengthtraining.traditional")
                Text("Trains with \(member.coachDisplayName)")
                    .font(.subheadline.weight(.semibold))
            }
            .foregroundStyle(Club360Theme.captionOnGlass)
            Text("Public community profile — photo and posts only.")
                .font(.caption)
                .foregroundStyle(Club360Theme.captionOnGlass)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .padding(20)
        .club360Glass(cornerRadius: 26)
    }

    private func loadPosts() async {
        isLoading = true
        errorMessage = nil
        defer { isLoading = false }
        do {
            posts = try await ClientDataService.fetchCommunityPosts(forClientId: member.clientId)
        } catch {
            errorMessage = error.localizedDescription
            posts = []
        }
    }
}

// MARK: - Cards

private struct CommunityPostCard: View {
    let post: CommunityPostDTO
    var isOwnPost: Bool = false
    var showReplyHint: Bool = true
    var onOpenAuthor: (() -> Void)? = nil
    var onOpenPost: (() -> Void)? = nil

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(alignment: .firstTextBaseline) {
                Button {
                    onOpenAuthor?()
                } label: {
                    HStack(spacing: 6) {
                        Text(post.authorDisplayName)
                            .font(.subheadline.weight(.semibold))
                            .foregroundStyle(Club360Theme.burgundy)
                        if isOwnPost {
                            Text("You")
                                .font(.caption2.weight(.bold))
                                .foregroundStyle(Club360Theme.burgundy)
                                .padding(.horizontal, 8)
                                .padding(.vertical, 3)
                                .background(Club360Theme.burgundy.opacity(0.12), in: Capsule())
                        }
                    }
                }
                .buttonStyle(.plain)
                .disabled(onOpenAuthor == nil)
                Spacer()
                Text(post.categoryLabel)
                    .font(.caption2.weight(.bold))
                    .foregroundStyle(Club360Theme.burgundy)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(Club360Theme.creamWarm.opacity(0.9), in: Capsule())
            }

            HStack(spacing: 6) {
                Image(systemName: "figure.strengthtraining.traditional")
                    .font(.caption2)
                Text("Coach · \(post.coachLabel)")
                    .font(.caption.weight(.semibold))
            }
            .foregroundStyle(Club360Theme.captionOnGlass)

            Button {
                onOpenPost?()
            } label: {
                VStack(alignment: .leading, spacing: 10) {
                    Text(post.body)
                        .font(.body)
                        .foregroundStyle(Club360Theme.cardTitle)
                        .multilineTextAlignment(.leading)
                        .fixedSize(horizontal: false, vertical: true)

                    HStack {
                        if let created = CommunityDateFormat.relative(fromISO: post.createdAt) {
                            Text(created)
                                .font(.caption)
                                .foregroundStyle(Club360Theme.captionOnGlass)
                        }
                        Spacer()
                        if showReplyHint {
                            Text("View replies")
                                .font(.caption.weight(.semibold))
                                .foregroundStyle(Club360Theme.burgundy)
                            Image(systemName: "chevron.right")
                                .font(.caption2.weight(.bold))
                                .foregroundStyle(Club360Theme.captionOnGlass)
                        }
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
            .buttonStyle(.plain)
            .disabled(onOpenPost == nil)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .club360Glass(cornerRadius: 22)
    }
}

private struct CommunityMemberRow: View {
    let member: CommunityMemberDTO
    var isYou: Bool = false

    var body: some View {
        HStack(alignment: .center, spacing: 12) {
            CommunityAvatarView(url: member.resolvedAvatarURL, size: 48)
            VStack(alignment: .leading, spacing: 4) {
                HStack(spacing: 8) {
                    Text(member.memberDisplayName)
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(Club360Theme.cardTitle)
                    if isYou {
                        Text("You")
                            .font(.caption2.weight(.bold))
                            .foregroundStyle(Club360Theme.burgundy)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 3)
                            .background(Club360Theme.burgundy.opacity(0.12), in: Capsule())
                    }
                }
                Text("Trains with \(member.coachDisplayName)")
                    .font(.caption)
                    .foregroundStyle(Club360Theme.captionOnGlass)
            }
            Spacer()
            Image(systemName: "chevron.right")
                .font(.caption.weight(.semibold))
                .foregroundStyle(Club360Theme.captionOnGlass)
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .club360Glass(cornerRadius: 18)
    }
}

private struct CommunityAvatarView: View {
    let url: URL?
    var size: CGFloat = 48

    var body: some View {
        Group {
            if let url {
                AsyncImage(url: url) { phase in
                    switch phase {
                    case .empty:
                        ZStack {
                            Circle().fill(Club360Theme.creamWarm)
                            ProgressView().tint(Club360Theme.burgundy)
                        }
                    case .success(let image):
                        image
                            .resizable()
                            .scaledToFill()
                    default:
                        placeholder
                    }
                }
            } else {
                placeholder
            }
        }
        .frame(width: size, height: size)
        .clipShape(Circle())
        .overlay(Circle().stroke(Color.white.opacity(0.7), lineWidth: 1.5))
    }

    private var placeholder: some View {
        ZStack {
            Circle().fill(Club360Theme.creamWarm)
            Image(systemName: "person.fill")
                .font(size >= 72 ? .title : .body)
                .foregroundStyle(Club360Theme.burgundy)
        }
    }
}

private struct CommunityCommentRow: View {
    let comment: CommunityCommentDTO
    let isOwn: Bool
    let onDelete: () -> Void

    var body: some View {
        HStack(alignment: .top, spacing: 10) {
            VStack(alignment: .leading, spacing: 4) {
                HStack(spacing: 6) {
                    Text(comment.authorDisplayName)
                        .font(.caption.weight(.bold))
                        .foregroundStyle(Club360Theme.cardTitle)
                    if comment.isCoachReply {
                        Text("Coach")
                            .font(.caption2.weight(.bold))
                            .foregroundStyle(.white)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 3)
                            .background(Club360Theme.burgundy, in: Capsule())
                    }
                    if isOwn {
                        Text("You")
                            .font(.caption2.weight(.bold))
                            .foregroundStyle(Club360Theme.burgundy)
                    }
                    Spacer()
                    if let created = CommunityDateFormat.relative(fromISO: comment.createdAt) {
                        Text(created)
                            .font(.caption2)
                            .foregroundStyle(Club360Theme.captionOnGlass)
                    }
                }
                Text(comment.body)
                    .font(.subheadline)
                    .foregroundStyle(Club360Theme.cardTitle)
                    .fixedSize(horizontal: false, vertical: true)
            }
            if isOwn, comment.rowId != nil {
                Button(role: .destructive, action: onDelete) {
                    Image(systemName: "trash")
                        .font(.caption)
                }
                .buttonStyle(.borderless)
            }
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .club360Glass(cornerRadius: 18)
    }
}

// MARK: - View model

private struct CommunityCoachGroup {
    let coachId: String
    let coachName: String
    let members: [CommunityMemberDTO]
}

@Observable
@MainActor
private final class CommunityViewModel {
    static let categories: [(id: String, label: String)] = [
        ("tip", "Tip"),
        ("win", "Win"),
        ("question", "Ask"),
        ("encouragement", "Cheer"),
    ]

    var isLoading = false
    var isSaving = false
    var errorMessage: String?
    var posts: [CommunityPostDTO] = []
    var members: [CommunityMemberDTO] = []

    private(set) var clientId: String?
    private(set) var authorUserId: String?
    private(set) var coachId: String?
    private(set) var coachDisplayName = "Coach"
    private(set) var authorDisplayName = "Member"
    private(set) var viewerMode: CommunityViewerMode = .member

    var hasCoach: Bool { coachId != nil && !(coachId?.isEmpty ?? true) }

    var membersByCoach: [CommunityCoachGroup] {
        let grouped = Dictionary(grouping: members, by: \.coachId)
        return grouped
            .map { coachId, list in
                let name = list.first?.coachDisplayName ?? "Coach"
                let sorted = list.sorted {
                    $0.memberDisplayName.localizedCaseInsensitiveCompare($1.memberDisplayName) == .orderedAscending
                }
                return CommunityCoachGroup(coachId: coachId, coachName: name, members: sorted)
            }
            .sorted { $0.coachName.localizedCaseInsensitiveCompare($1.coachName) == .orderedAscending }
    }

    func load(mode: CommunityViewerMode, home: ClientHomeViewModel?, showLoading: Bool = true) async {
        viewerMode = mode
        switch mode {
        case .member:
            await loadAsMember(home: home, showLoading: showLoading)
        case .coach:
            await loadAsCoach(showLoading: showLoading)
        }
    }

    private func loadAsMember(home: ClientHomeViewModel?, showLoading: Bool) async {
        guard let home, let cid = home.clientId else {
            reset()
            return
        }
        if showLoading { isLoading = true }
        errorMessage = nil
        defer { isLoading = false }

        clientId = cid
        authorUserId = Club360FitSupabase.shared.auth.currentSession?.user.id.uuidString.lowercased()
        authorDisplayName = displayName(from: home)

        do {
            let row = try await ClientDataService.fetchClientById(cid)
            coachId = row?.coachId

            async let feed = ClientDataService.fetchCommunityPosts()
            async let directory = ClientDataService.fetchCommunityMemberDirectory()
            posts = try await feed
            members = try await directory

            if let coach = coachId, !coach.isEmpty {
                if let mine = members.first(where: { $0.clientId == cid }) {
                    coachDisplayName = mine.coachDisplayName
                } else if let match = members.first(where: { $0.coachId == coach }) {
                    coachDisplayName = match.coachDisplayName
                }
            }
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    private func loadAsCoach(showLoading: Bool) async {
        if showLoading { isLoading = true }
        errorMessage = nil
        defer { isLoading = false }

        clientId = nil
        coachId = nil
        authorUserId = Club360FitSupabase.shared.auth.currentSession?.user.id.uuidString.lowercased()
        authorDisplayName = "Coach"

        do {
            if let uid = authorUserId,
               let profile = try await ClientDataService.fetchProfileForUser(userId: uid) {
                let name = (profile.full_name ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
                if !name.isEmpty { authorDisplayName = name }
            }

            async let feed = ClientDataService.fetchCommunityPosts()
            async let directory = ClientDataService.fetchCommunityMemberDirectory()
            posts = try await feed
            members = try await directory
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    func createPost(category: String, body: String) async -> Bool {
        guard viewerMode == .member else {
            errorMessage = "Coaches reply on posts instead of starting new ones."
            return false
        }
        guard let cid = clientId, let coach = coachId, !coach.isEmpty else {
            errorMessage = "You need an assigned coach to post."
            return false
        }
        isSaving = true
        errorMessage = nil
        defer { isSaving = false }
        do {
            try await ClientDataService.createCommunityPost(
                clientId: cid,
                coachId: coach,
                authorDisplayName: authorDisplayName,
                coachDisplayName: coachDisplayName,
                category: category,
                body: body
            )
            posts = try await ClientDataService.fetchCommunityPosts()
            return true
        } catch {
            errorMessage = error.localizedDescription
            return false
        }
    }

    private func reset() {
        clientId = nil
        authorUserId = nil
        coachId = nil
        coachDisplayName = "Coach"
        authorDisplayName = "Member"
        posts = []
        members = []
    }

    private func displayName(from home: ClientHomeViewModel) -> String {
        let welcome = home.welcomeName.trimmingCharacters(in: .whitespacesAndNewlines)
        if !welcome.isEmpty, welcome.lowercased() != "there" {
            return welcome
        }
        return "Member"
    }
}

private enum CommunityDateFormat {
    private static let isoParser: ISO8601DateFormatter = {
        let f = ISO8601DateFormatter()
        f.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        return f
    }()

    private static let isoParserNoFrac: ISO8601DateFormatter = {
        let f = ISO8601DateFormatter()
        f.formatOptions = [.withInternetDateTime]
        return f
    }()

    static func relative(fromISO iso: String?) -> String? {
        guard let iso, !iso.isEmpty else { return nil }
        let date = isoParser.date(from: iso) ?? isoParserNoFrac.date(from: iso)
        guard let date else { return nil }
        return date.formatted(.relative(presentation: .named))
    }
}
