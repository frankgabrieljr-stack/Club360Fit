import Observation
import SwiftUI

/// Shared coach Updates inbox (Hub + client hub). Grouped by member; optional `initialClientId` deep-links into one client.
struct CoachHubNotificationsView: View {
    let clientNameById: [String: String]
    /// When set (e.g. from client hub bell), opens that member’s thread first.
    var initialClientId: String? = nil
    var onUnreadChanged: () -> Void = {}

    @State private var model = CoachHubNotificationsModel()
    @State private var resolvedNames: [String: String] = [:]
    @State private var replyTarget: ClientNotificationDTO?
    @State private var replyText = ""
    @State private var replyError: String?
    @State private var showReplyError = false
    @State private var actionTarget: ClientNotificationDTO?
    @State private var selectedClientId: String?
    @State private var openClientHub: CoachUpdatesClientRef?

    init(
        clientNameById: [String: String],
        initialClientId: String? = nil,
        onUnreadChanged: @escaping () -> Void = {}
    ) {
        self.clientNameById = clientNameById
        self.initialClientId = initialClientId
        self.onUnreadChanged = onUnreadChanged
        _selectedClientId = State(initialValue: initialClientId)
        _resolvedNames = State(initialValue: clientNameById)
    }

    private var names: [String: String] {
        resolvedNames.merging(clientNameById) { current, _ in current }
    }

    private var notificationGroups: [CoachNotificationClientGroup] {
        var order: [String] = []
        var grouped: [String: [ClientNotificationDTO]] = [:]
        for item in model.items {
            if grouped[item.clientId] == nil {
                order.append(item.clientId)
            }
            grouped[item.clientId, default: []].append(item)
        }
        // Keep deep-linked client visible even when they have zero rows.
        if let initialClientId, !initialClientId.isEmpty, grouped[initialClientId] == nil {
            order.insert(initialClientId, at: 0)
            grouped[initialClientId] = []
        }
        return order.map { clientId in
            CoachNotificationClientGroup(
                clientId: clientId,
                displayName: names[clientId] ?? "Member",
                items: grouped[clientId] ?? []
            )
        }
    }

    var body: some View {
        Group {
            if model.isLoading, model.items.isEmpty, selectedClientId == nil {
                ZStack {
                    Club360ScreenBackground()
                    ProgressView()
                        .tint(Club360Theme.tealDark)
                }
            } else if model.items.isEmpty, selectedClientId == nil {
                ZStack {
                    Club360ScreenBackground()
                    ContentUnavailableView(
                        "No updates yet",
                        systemImage: "bell",
                        description: Text("Alerts from your clients and system messages will show here.")
                    )
                }
            } else {
                listBody
            }
        }
        .navigationTitle("Updates")
        .navigationBarTitleDisplayMode(.inline)
        .toolbarBackground(.ultraThinMaterial, for: .navigationBar)
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button(selectedClientId == nil ? "Mark all read" : "Mark read") {
                    Task { await markAllReadInScope() }
                }
                .tint(Club360Theme.tealDark)
            }
        }
        .task {
            await reloadInbox()
        }
        .refreshable {
            await reloadInbox(showLoading: false)
        }
        .navigationDestination(item: $openClientHub) { ref in
            AdminClientHubView(clientId: ref.id, displayTitle: ref.displayTitle)
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
            if let target = actionTarget, canReply(to: target) {
                Button("Reply") {
                    replyTarget = target
                    replyText = ""
                    actionTarget = nil
                }
            }
            if let target = actionTarget {
                Button("Open client") {
                    Task { await openClient(from: target) }
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

            List {
                if let group = selectedGroup {
                    Section {
                        Button {
                            selectedClientId = nil
                        } label: {
                            Label("All clients", systemImage: "chevron.left")
                                .font(.subheadline.weight(.semibold))
                                .foregroundStyle(Club360Theme.burgundy)
                        }
                        .buttonStyle(.plain)
                        .listRowInsets(EdgeInsets(top: 6, leading: 16, bottom: 6, trailing: 16))
                        .listRowSeparator(.hidden)
                        .listRowBackground(Color.clear)

                        if group.items.isEmpty {
                            Text("No updates for this member yet.")
                                .font(.subheadline)
                                .foregroundStyle(Club360Theme.captionOnGlass)
                                .listRowInsets(EdgeInsets(top: 12, leading: 16, bottom: 12, trailing: 16))
                                .listRowSeparator(.hidden)
                                .listRowBackground(Color.clear)
                        } else {
                            ForEach(group.items, id: \.id) { n in
                                notificationCard(n)
                                    .listRowInsets(EdgeInsets(top: 6, leading: 16, bottom: 6, trailing: 16))
                                    .listRowSeparator(.hidden)
                                    .listRowBackground(Color.clear)
                                    .swipeActions(edge: .trailing, allowsFullSwipe: true) {
                                        notificationSwipeActions(for: n)
                                    }
                            }
                        }
                    } header: {
                        Text(group.displayName)
                            .font(.caption.weight(.bold))
                            .foregroundStyle(Club360Theme.tealDark)
                            .textCase(.uppercase)
                            .padding(.leading, 4)
                    }
                } else {
                    ForEach(notificationGroups) { group in
                        Button {
                            selectedClientId = group.clientId
                        } label: {
                            clientTile(group)
                        }
                        .buttonStyle(.plain)
                        .listRowInsets(EdgeInsets(top: 6, leading: 16, bottom: 6, trailing: 16))
                        .listRowSeparator(.hidden)
                        .listRowBackground(Color.clear)
                    }
                }
            }
            .listStyle(.plain)
            .scrollContentBackground(.hidden)
        }
    }

    @ViewBuilder
    private func notificationSwipeActions(for n: ClientNotificationDTO) -> some View {
        if canReply(to: n) {
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

    private var selectedGroup: CoachNotificationClientGroup? {
        guard let selectedClientId else { return nil }
        if let existing = notificationGroups.first(where: { $0.clientId == selectedClientId }) {
            return existing
        }
        return CoachNotificationClientGroup(
            clientId: selectedClientId,
            displayName: names[selectedClientId] ?? "Member",
            items: []
        )
    }

    private func reloadInbox(showLoading: Bool = true) async {
        let loadedNames = await model.load(showLoading: showLoading)
        resolvedNames.merge(loadedNames) { _, new in new }
        resolvedNames.merge(clientNameById) { _, new in new }
    }

    private func markAllReadInScope() async {
        if let cid = selectedClientId {
            try? await ClientDataService.markAllCoachNotificationsReadForClient(clientId: cid)
        } else {
            try? await ClientDataService.markAllNotificationsReadForCoach()
        }
        await reloadInbox(showLoading: false)
        onUnreadChanged()
    }

    private func openClient(from n: ClientNotificationDTO) async {
        let name = names[n.clientId] ?? "Member"
        await markNotificationRead(n)
        // Already on this member’s filter from client hub — stay put after mark-read.
        if selectedClientId == n.clientId, initialClientId == n.clientId {
            return
        }
        openClientHub = CoachUpdatesClientRef(id: n.clientId, displayTitle: name)
    }

    private func clientTile(_ group: CoachNotificationClientGroup) -> some View {
        let unread = group.items.filter { $0.coachReadAt == nil }.count
        let latest = group.items.first
        return HStack(alignment: .top, spacing: 12) {
            Image(systemName: unread > 0 ? "bell.badge.fill" : "bell.fill")
                .font(.title3.weight(.semibold))
                .foregroundStyle(unread > 0 ? Club360Theme.peachDeep : Club360Theme.tealDark)
                .frame(width: 28)

            VStack(alignment: .leading, spacing: 6) {
                HStack(alignment: .firstTextBaseline) {
                    Text(group.displayName)
                        .font(.headline.weight(.semibold))
                        .foregroundStyle(Club360Theme.cardTitle)
                    Spacer(minLength: 8)
                    Text("\(group.items.count)")
                        .font(.caption.weight(.bold))
                        .foregroundStyle(Club360Theme.burgundy)
                        .padding(.horizontal, 9)
                        .padding(.vertical, 4)
                        .background(.white.opacity(0.55), in: Capsule())
                }
                Text(unread == 0 ? "No new notifications" : "\(unread) new notification\(unread == 1 ? "" : "s")")
                    .font(.subheadline.weight(.medium))
                    .foregroundStyle(unread > 0 ? Club360Theme.burgundy : Club360Theme.captionOnGlass)
                if let latest {
                    Text("Latest: \(latest.title)")
                        .font(.caption)
                        .foregroundStyle(Club360Theme.captionOnGlass)
                        .lineLimit(1)
                }
            }
        }
        .padding(16)
        .club360Glass()
    }

    private func deleteNotification(_ n: ClientNotificationDTO) async {
        guard let id = n.rowId else { return }
        try? await ClientDataService.deleteClientNotificationForCoach(notificationId: id)
        await reloadInbox(showLoading: false)
        onUnreadChanged()
    }

    private func notificationCard(_ n: ClientNotificationDTO) -> some View {
        Button {
            actionTarget = n
        } label: {
            HStack(alignment: .top, spacing: 12) {
                if n.coachReadAt == nil {
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
                            .foregroundStyle(Club360Theme.cardSubtitle)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
            .padding(16)
            .club360Glass()
        }
        .buttonStyle(.plain)
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
            await reloadInbox(showLoading: false)
            onUnreadChanged()
        } catch {
            replyError = error.localizedDescription
            showReplyError = true
        }
    }

    private func markNotificationRead(_ n: ClientNotificationDTO) async {
        guard let id = n.rowId else { return }
        try? await ClientDataService.markNotificationReadAsCoach(notificationId: id)
        actionTarget = nil
        await reloadInbox(showLoading: false)
        onUnreadChanged()
    }

    private func deleteNotificationFromPopup(_ n: ClientNotificationDTO) async {
        guard let id = n.rowId else { return }
        try? await ClientDataService.deleteClientNotificationForCoach(notificationId: id)
        actionTarget = nil
        await reloadInbox(showLoading: false)
        onUnreadChanged()
    }
}

private struct CoachNotificationClientGroup: Identifiable {
    let clientId: String
    let displayName: String
    let items: [ClientNotificationDTO]

    var id: String { clientId }
}

private struct CoachUpdatesClientRef: Identifiable, Hashable {
    let id: String
    let displayTitle: String
}

@Observable
@MainActor
private final class CoachHubNotificationsModel {
    var isLoading = true
    var items: [ClientNotificationDTO] = []

    /// Loads notifications and returns client id → display name for the inbox.
    @discardableResult
    func load(showLoading: Bool = true) async -> [String: String] {
        if showLoading { isLoading = true }
        defer { if showLoading { isLoading = false } }
        let clients = ((try? await ClientDataService.fetchClientsForCoach()) ?? [])
            .filter { !($0.coachId?.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ?? true) }
        let ids = Set(clients.compactMap { $0.id })
        items = (try? await ClientDataService.fetchNotificationsForCoach(clientIds: ids, limit: 80)) ?? []
        return Dictionary(uniqueKeysWithValues: clients.compactMap { c -> (String, String)? in
            guard let id = c.id, !id.isEmpty else { return nil }
            return (id, AdminViewModel.listTitle(for: c))
        })
    }
}
