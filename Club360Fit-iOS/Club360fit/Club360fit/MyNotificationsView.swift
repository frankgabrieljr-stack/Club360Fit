import Observation
import SwiftUI

/// Member inbox uses `read_at`; coach inbox (viewing a client) uses `coach_read_at` so each side clears independently.
enum MyNotificationsInboxKind: Sendable {
    case member
    case coach
}

/// Mirrors Android `MyNotificationsScreen`.
struct MyNotificationsView: View {
    var inbox: MyNotificationsInboxKind = .member

    @Environment(ClientHomeViewModel.self) private var home: ClientHomeViewModel
    @Environment(\.clientTabRouter) private var tabRouter
    @Environment(\.dismiss) private var dismiss
    @State private var model = MyNotificationsViewModel()
    @State private var replyTarget: ClientNotificationDTO?
    @State private var replyText = ""
    @State private var replyError: String?
    @State private var showReplyError = false
    @State private var actionTarget: ClientNotificationDTO?

    var body: some View {
        Group {
            if home.clientId == nil {
                ContentUnavailableView("No profile", systemImage: "person.crop.circle.badge.xmark")
            } else {
                listBody
            }
        }
        .navigationTitle("Updates")
        .navigationBarTitleDisplayMode(.inline)
        .toolbarBackground(.ultraThinMaterial, for: .navigationBar)
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button("Mark all read") {
                    Task {
                        guard let cid = home.clientId else { return }
                        await model.markAllRead(clientId: cid, inbox: inbox)
                        await home.reloadNotificationsCount()
                    }
                }
                .tint(Club360Theme.tealDark)
            }
        }
        .task(id: home.clientId) {
            guard let cid = home.clientId else { return }
            await model.load(clientId: cid, inbox: inbox)
        }
        .refreshable {
            guard let cid = home.clientId else { return }
            await model.load(clientId: cid, inbox: inbox, showLoading: false)
        }
        .alert("Reply to workout note", isPresented: replySheetPresented) {
            TextField("Write your reply", text: $replyText, axis: .vertical)
            Button("Send") {
                Task { await sendReply() }
            }
            Button("Cancel", role: .cancel) {
                replyTarget = nil
                replyText = ""
            }
        } message: {
            Text("This sends a timestamped reply to the member.")
        }
        .alert("Could not send reply", isPresented: $showReplyError) {
            Button("OK", role: .cancel) {}
        } message: {
            Text(replyError ?? "Please try again.")
        }
        .alert(actionTarget?.title ?? "Update", isPresented: actionPopupPresented) {
            if let target = actionTarget, inbox == .coach, canReply(to: target) {
                Button("Reply") {
                    replyTarget = target
                    replyText = ""
                    actionTarget = nil
                }
            }
            Button("Open") {
                if let target = actionTarget {
                    Task { await openNotification(target) }
                }
            }
            Button("Mark read") {
                if let target = actionTarget {
                    Task { await markNotificationRead(target) }
                }
            }
            Button("Delete", role: .destructive) {
                if let target = actionTarget {
                    Task { await deleteNotificationFromPopup(target) }
                }
            }
            Button("Cancel", role: .cancel) {
                actionTarget = nil
            }
        } message: {
            Text(actionTarget?.body ?? "")
        }
    }

    private var listBody: some View {
        ZStack {
            Club360ScreenBackground()

            Group {
                if model.isLoading {
                    ProgressView()
                        .tint(Club360Theme.tealDark)
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else if model.items.isEmpty {
                    Text("No updates yet.")
                        .foregroundStyle(.secondary)
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else {
                    List {
                        ForEach(model.items, id: \.id) { n in
                            notificationCard(n)
                                .listRowInsets(EdgeInsets(top: 6, leading: 16, bottom: 6, trailing: 16))
                                .listRowSeparator(.hidden)
                                .listRowBackground(Color.clear)
                                .swipeActions(edge: .leading, allowsFullSwipe: false) {
                                    Button {
                                        Task { await markNotificationRead(n) }
                                    } label: {
                                        Label("Read", systemImage: "checkmark.circle")
                                    }
                                    .tint(Club360Theme.tealDark)
                                }
                                .swipeActions(edge: .trailing, allowsFullSwipe: true) {
                                    if inbox == .coach, canReply(to: n) {
                                        Button {
                                            replyTarget = n
                                            replyText = ""
                                        } label: {
                                            Label("Reply", systemImage: "arrowshape.turn.up.left")
                                        }
                                        .tint(Club360Theme.tealDark)
                                    }
                                    Button(role: .destructive) {
                                        Task { await deleteNotification(n) }
                                    } label: {
                                        Label("Delete", systemImage: "trash")
                                    }
                                }
                        }
                    }
                    .listStyle(.plain)
                    .scrollContentBackground(.hidden)
                }
            }
        }
    }

    private func deleteNotification(_ n: ClientNotificationDTO) async {
        guard let id = n.rowId, let cid = home.clientId else { return }
        await model.deleteNotification(notificationId: id, clientId: cid, inbox: inbox)
        await home.reloadNotificationsCount()
    }

    private var actionPopupPresented: Binding<Bool> {
        Binding(
            get: { actionTarget != nil },
            set: { if !$0 { actionTarget = nil } }
        )
    }

    private var replySheetPresented: Binding<Bool> {
        Binding(
            get: { replyTarget != nil },
            set: { if !$0 { replyTarget = nil } }
        )
    }

    private func canReply(to n: ClientNotificationDTO) -> Bool {
        (n.kind ?? "").lowercased() == "workout_session_logged"
    }

    private func sendReply() async {
        guard let target = replyTarget else { return }
        do {
            try await ClientDataService.replyToWorkoutSessionNote(
                clientId: target.clientId,
                workoutSessionLogId: target.refId,
                replyText: replyText
            )
            replyTarget = nil
            replyText = ""
            guard let cid = home.clientId else { return }
            await model.load(clientId: cid, inbox: inbox, showLoading: false)
            await home.reloadNotificationsCount()
        } catch {
            replyError = error.localizedDescription
            showReplyError = true
        }
    }

    private func openNotification(_ n: ClientNotificationDTO) async {
        // Member shell: `openNotification` resets `homePath`, which pops this screen — do not call `dismiss()` too (double-pop / broken toolbar hits).
        // Coach client hub: no tab router — pop with `dismiss()` only.
        if tabRouter != nil {
            tabRouter?.openNotification(n)
        }
        if let id = n.rowId, let cid = home.clientId {
            await model.markOneRead(notificationId: id, clientId: cid, inbox: inbox)
        }
        await home.reloadNotificationsCount()
        actionTarget = nil
        if tabRouter == nil {
            dismiss()
        }
    }

    private func markNotificationRead(_ n: ClientNotificationDTO) async {
        guard let id = n.rowId, let cid = home.clientId else { return }
        await model.markOneRead(notificationId: id, clientId: cid, inbox: inbox)
        await home.reloadNotificationsCount()
        actionTarget = nil
    }

    private func deleteNotificationFromPopup(_ n: ClientNotificationDTO) async {
        guard let id = n.rowId, let cid = home.clientId else { return }
        await model.deleteNotification(notificationId: id, clientId: cid, inbox: inbox)
        await home.reloadNotificationsCount()
        actionTarget = nil
    }

    private func notificationCard(_ n: ClientNotificationDTO) -> some View {
        let showUnreadDot: Bool = {
            switch inbox {
            case .member: return n.readAt == nil
            case .coach: return n.coachReadAt == nil
            }
        }()

        return Button {
            if inbox == .member {
                Task { await openNotification(n) }
            } else {
                actionTarget = n
            }
        } label: {
            HStack(alignment: .top, spacing: 12) {
                if showUnreadDot {
                    Circle()
                        .fill(Club360Theme.peachDeep)
                        .frame(width: 10, height: 10)
                        .padding(.top, 4)
                }
                VStack(alignment: .leading, spacing: 6) {
                    Text(n.title)
                        .font(.headline.weight(.semibold))
                        .foregroundStyle(Club360Theme.cardTitle)
                    Text(n.body)
                        .font(.body)
                        .foregroundStyle(Club360Theme.cardTitle)
                    if let created = n.createdAt {
                        Text(Club360Formatting.formatPaymentInstant(created))
                            .font(.caption)
                            .foregroundStyle(Club360Theme.captionOnGlass)
                    }
                    if inbox == .member {
                        Text("Tap to open · swipe for read / delete")
                            .font(.caption2)
                            .foregroundStyle(Club360Theme.captionOnGlass)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
            .padding(16)
            .club360Glass()
        }
        .buttonStyle(.plain)
        .contextMenu {
            if inbox == .coach, canReply(to: n) {
                Button("Reply") {
                    replyTarget = n
                    replyText = ""
                }
            }
            Button("Mark read") {
                Task { await markNotificationRead(n) }
            }
            Button("Delete", role: .destructive) {
                Task { await deleteNotification(n) }
            }
        }
    }
}

@Observable
@MainActor
private final class MyNotificationsViewModel {
    var isLoading = true
    var items: [ClientNotificationDTO] = []

    /// - Parameter showLoading: When false, avoids swapping `List` for `ProgressView` during swipe / mark-read (prevents UICollectionView update crashes).
    func load(clientId: String, inbox: MyNotificationsInboxKind, showLoading: Bool = true) async {
        if showLoading { isLoading = true }
        defer { if showLoading { isLoading = false } }
        switch inbox {
        case .member:
            items = (try? await ClientDataService.fetchClientNotifications(clientId: clientId)) ?? []
        case .coach:
            items = (try? await ClientDataService.fetchClientNotificationsForCoach(clientId: clientId)) ?? []
        }
    }

    func markOneRead(notificationId: String, clientId: String, inbox: MyNotificationsInboxKind) async {
        switch inbox {
        case .member:
            try? await ClientDataService.markNotificationRead(notificationId: notificationId)
        case .coach:
            try? await ClientDataService.markNotificationReadAsCoach(notificationId: notificationId)
        }
        await load(clientId: clientId, inbox: inbox, showLoading: false)
    }

    func markAllRead(clientId: String, inbox: MyNotificationsInboxKind) async {
        switch inbox {
        case .member:
            try? await ClientDataService.markAllNotificationsRead(clientId: clientId)
        case .coach:
            try? await ClientDataService.markAllCoachNotificationsReadForClient(clientId: clientId)
        }
        await load(clientId: clientId, inbox: inbox, showLoading: false)
    }

    func deleteNotification(notificationId: String, clientId: String, inbox: MyNotificationsInboxKind) async {
        switch inbox {
        case .member:
            try? await ClientDataService.deleteClientNotificationForMember(notificationId: notificationId)
        case .coach:
            try? await ClientDataService.deleteClientNotificationForCoach(notificationId: notificationId)
        }
        await load(clientId: clientId, inbox: inbox, showLoading: false)
    }
}
