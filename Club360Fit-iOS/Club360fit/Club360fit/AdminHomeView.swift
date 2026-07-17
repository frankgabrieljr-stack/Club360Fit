import Auth
import SwiftUI

/// Coach / admin shell — client list, gallery, and account (mirrors Android `AdminHomeScreen` tabs).
struct AdminHomeView: View {
    @State private var badges = AdminTabBadgesModel()

    var body: some View {
        TabView {
            NavigationStack {
                CoachMainHubView()
            }
            .tabItem { Label("Hub", systemImage: "square.grid.2x2.fill") }

            AdminScheduleTab()
                .tabItem { Label("Schedule", systemImage: "calendar") }

            AdminNewClientsTab()
                .tabItem { Label("New clients", systemImage: "person.badge.plus") }
                .badge(badges.newClientCount)

            AdminClientsTab()
                .tabItem { Label("My clients", systemImage: "person.3.fill") }

            NavigationStack {
                CoachMealPhotoInboxView()
            }
            .tabItem { Label("Meal inbox", systemImage: "tray.full") }
            .badge(badges.mealNeedsReviewCount)

            NavigationStack {
                TransformationGalleryView()
            }
            .tabItem { Label("Gallery", systemImage: "photo.on.rectangle.angled") }

            NavigationStack {
                UserProfileView()
            }
            .tabItem { Label("Profile", systemImage: "person.crop.circle") }
        }
        .tint(Club360Theme.burgundy)
        .preferredColorScheme(.light)
        .task {
            await badges.refresh()
        }
        .onAppear {
            Task { await badges.refresh() }
        }
    }
}

@Observable
@MainActor
private final class AdminTabBadgesModel {
    var newClientCount = 0
    var mealNeedsReviewCount = 0

    func refresh() async {
        async let clientsTask = ClientDataService.fetchClientsForCoach()
        async let mealsTask = ClientDataService.listMealPhotoLogsForCoachInbox()
        let clients = (try? await clientsTask) ?? []
        let meals = (try? await mealsTask) ?? []
        newClientCount = clients.filter {
            $0.coachId?.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ?? true
        }.count
        mealNeedsReviewCount = meals.filter {
            ($0.coachFeedback ?? "").trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
        }.count
    }
}

// MARK: - Schedule tab

private struct AdminScheduleTab: View {
    @Environment(Club360AuthSession.self) private var auth: Club360AuthSession
    @State private var model = AdminScheduleModel()

    private var todaysEvents: [ScheduleEventDTO] {
        let todayKey = Club360DateFormats.dayString(Date())
        return model.events(on: todayKey)
    }

    private var weekEvents: [ScheduleEventDTO] {
        let cal = Calendar.current
        let weekStart = Calendar.weekStartSunday(containing: Date())
        let weekEnd = cal.date(byAdding: .day, value: 6, to: weekStart) ?? weekStart
        return model.events(from: weekStart, through: weekEnd)
    }

    var body: some View {
        NavigationStack {
            ZStack {
                Club360ScreenBackground()

                ScrollView {
                    VStack(alignment: .leading, spacing: 22) {
                        HStack(alignment: .center, spacing: 14) {
                            Image(systemName: "calendar")
                                .font(.title.weight(.semibold))
                                .foregroundStyle(Club360Theme.tealDark)
                                .frame(width: 56, height: 56)
                                .background(.white.opacity(0.5), in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                            VStack(alignment: .leading, spacing: 4) {
                                Text("Schedule")
                                    .font(.largeTitle.weight(.bold))
                                    .foregroundStyle(Club360Theme.burgundy)
                                Text("Day and week views")
                                    .font(.title3.weight(.semibold))
                                    .foregroundStyle(Club360Theme.burgundy)
                            }
                        }
                        .padding(.top, 4)

                        if model.isLoading {
                            ProgressView("Loading schedule...")
                                .tint(Club360Theme.tealDark)
                                .frame(maxWidth: .infinity)
                                .padding()
                        }

                        if let err = model.errorMessage {
                            Text(err)
                                .font(.footnote)
                                .foregroundStyle(.red)
                                .padding()
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .club360Glass(cornerRadius: 22)
                        }

                        VStack(spacing: 12) {
                            NavigationLink {
                                AdminScheduleListView(
                                    title: "Schedule for the day",
                                    subtitle: Club360DateFormats.displayDay(from: Date()),
                                    emptyTitle: "No sessions today",
                                    events: todaysEvents,
                                    clientNameById: model.clientNameById,
                                    onEventsChanged: {
                                        await model.load(coachUserId: auth.session?.user.id.uuidString)
                                    }
                                )
                            } label: {
                                AdminScheduleOptionRow(
                                    title: "Schedule for the day",
                                    subtitle: daySubtitle,
                                    systemImage: "sun.max.fill"
                                )
                            }
                            .buttonStyle(.plain)

                            NavigationLink {
                                AdminScheduleListView(
                                    title: "Schedule for the Week",
                                    subtitle: weekSubtitle,
                                    emptyTitle: "No sessions this week",
                                    events: weekEvents,
                                    clientNameById: model.clientNameById,
                                    onEventsChanged: {
                                        await model.load(coachUserId: auth.session?.user.id.uuidString)
                                    }
                                )
                            } label: {
                                AdminScheduleOptionRow(
                                    title: "Schedule for the Week",
                                    subtitle: "\(weekEvents.count) session\(weekEvents.count == 1 ? "" : "s") this week",
                                    systemImage: "calendar.badge.clock"
                                )
                            }
                            .buttonStyle(.plain)
                        }
                    }
                    .padding(.horizontal, 18)
                    .padding(.bottom, 28)
                }
            }
            .navigationTitle("Schedule")
            .navigationBarTitleDisplayMode(.inline)
            .toolbarBackground(.ultraThinMaterial, for: .navigationBar)
            .task {
                await model.load(coachUserId: auth.session?.user.id.uuidString)
            }
            .refreshable {
                await model.load(coachUserId: auth.session?.user.id.uuidString)
            }
        }
    }

    private var daySubtitle: String {
        todaysEvents.isEmpty
        ? "No sessions today"
        : "\(todaysEvents.count) session\(todaysEvents.count == 1 ? "" : "s") today"
    }

    private var weekSubtitle: String {
        let weekStart = Calendar.weekStartSunday(containing: Date())
        let weekEnd = Calendar.current.date(byAdding: .day, value: 6, to: weekStart) ?? weekStart
        return "\(Club360DateFormats.displayDay(from: weekStart)) - \(Club360DateFormats.displayDay(from: weekEnd))"
    }
}

private struct AdminScheduleOptionRow: View {
    let title: String
    let subtitle: String
    let systemImage: String

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: systemImage)
                .font(.title3.weight(.semibold))
                .foregroundStyle(Club360Theme.burgundy)
                .frame(width: 36)

            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(.headline.weight(.semibold))
                    .foregroundStyle(Club360Theme.cardTitle)
                Text(subtitle)
                    .font(.caption)
                    .foregroundStyle(Club360Theme.captionOnGlass)
            }

            Spacer()

            Image(systemName: "chevron.right")
                .font(.caption.weight(.semibold))
                .foregroundStyle(Club360Theme.captionOnGlass.opacity(0.8))
        }
        .padding(18)
        .frame(maxWidth: .infinity, alignment: .leading)
        .club360Glass(cornerRadius: 26)
    }
}

private struct AdminScheduleListView: View {
    let title: String
    let subtitle: String
    let emptyTitle: String
    let clientNameById: [String: String]
    var onEventsChanged: (() async -> Void)? = nil

    @State private var events: [ScheduleEventDTO]
    @State private var completionOverride: [String: Bool] = [:]
    @State private var sessionActions: ScheduleEventDTO?
    @State private var noteSession: ScheduleEventDTO?
    @State private var deleteSessionId: String?
    @State private var actionBusy = false
    @State private var actionError: String?
    @State private var openPlansClient: AdminQuickAssignRef?

    init(
        title: String,
        subtitle: String,
        emptyTitle: String,
        events: [ScheduleEventDTO],
        clientNameById: [String: String],
        onEventsChanged: (() async -> Void)? = nil
    ) {
        self.title = title
        self.subtitle = subtitle
        self.emptyTitle = emptyTitle
        self.clientNameById = clientNameById
        self.onEventsChanged = onEventsChanged
        _events = State(initialValue: events)
    }

    var body: some View {
        ZStack {
            Club360ScreenBackground()

            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(title)
                            .font(.largeTitle.weight(.bold))
                            .foregroundStyle(Club360Theme.burgundy)
                        Text(subtitle)
                            .font(.subheadline.weight(.semibold))
                            .foregroundStyle(Club360Theme.captionOnGlass)
                    }
                    .padding(.top, 4)

                    if events.isEmpty {
                        ContentUnavailableView(
                            emptyTitle,
                            systemImage: "calendar",
                            description: Text("Scheduled sessions appear here once they are assigned.")
                        )
                        .padding(.top, 24)
                    } else {
                        Text("Tap a session to complete, add a note, delete, or open Plans.")
                            .font(.footnote)
                            .foregroundStyle(Club360Theme.captionOnGlass)
                        ForEach(events, id: \.id) { event in
                            Button {
                                sessionActions = event
                            } label: {
                                AdminScheduleEventRow(
                                    event: event,
                                    clientName: event.clientId.flatMap { clientNameById[$0] },
                                    isCompleted: completionOverride[event.id] ?? event.isCompleted
                                )
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }
                .padding(.horizontal, 18)
                .padding(.bottom, 28)
            }
        }
        .navigationTitle(title)
        .navigationBarTitleDisplayMode(.inline)
        .toolbarBackground(.ultraThinMaterial, for: .navigationBar)
        .sheet(item: $sessionActions) { ev in
            HubSessionActionsSheet(
                event: ev,
                clientName: ev.clientId.flatMap { clientNameById[$0] },
                isBusy: actionBusy,
                isCompletedOverride: completionOverride[ev.id],
                onMarkComplete: { Task { await markComplete(ev, completed: true) } },
                onMarkIncomplete: { Task { await markComplete(ev, completed: false) } },
                onAddNote: {
                    noteSession = ev
                    sessionActions = nil
                },
                onDelete: {
                    deleteSessionId = ev.rowId
                    sessionActions = nil
                },
                onDismiss: { sessionActions = nil }
            )
            .safeAreaInset(edge: .bottom) {
                if let cid = ev.clientId, !cid.isEmpty {
                    Button {
                        sessionActions = nil
                        openPlansClient = AdminQuickAssignRef(
                            clientId: cid,
                            name: clientNameById[cid] ?? "Client",
                            initialTab: .schedule
                        )
                    } label: {
                        Text("Open Plans & schedule")
                            .font(.subheadline.weight(.semibold))
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 12)
                    }
                    .tint(Club360Theme.tealDark)
                    .padding(.horizontal, 18)
                    .padding(.bottom, 8)
                }
            }
        }
        .sheet(item: $noteSession) { ev in
            HubSessionNoteSheet(
                event: ev,
                isBusy: actionBusy,
                onSave: { notes in Task { await saveNote(ev, notes: notes) } },
                onDismiss: { noteSession = nil }
            )
        }
        .fullScreenCover(item: $openPlansClient) { ref in
            NavigationStack {
                CoachPlansHubView(
                    clientId: ref.clientId,
                    displayTitle: ref.name,
                    initialTab: ref.initialTab
                )
                .toolbar {
                    ToolbarItem(placement: .topBarTrailing) {
                        Button("Done") { openPlansClient = nil }
                    }
                }
            }
        }
        .alert("Delete session?", isPresented: Binding(
            get: { deleteSessionId != nil },
            set: { if !$0 { deleteSessionId = nil } }
        )) {
            Button("Cancel", role: .cancel) { deleteSessionId = nil }
            Button("Delete", role: .destructive) {
                guard let id = deleteSessionId else { return }
                Task { await deleteSession(id: id) }
            }
        } message: {
            Text("Remove this session from the calendar? This cannot be undone.")
        }
        .alert("Could not update session", isPresented: Binding(
            get: { actionError != nil },
            set: { if !$0 { actionError = nil } }
        )) {
            Button("OK", role: .cancel) { actionError = nil }
        } message: {
            Text(actionError ?? "")
        }
    }

    private func markComplete(_ ev: ScheduleEventDTO, completed: Bool) async {
        guard let rid = ev.rowId, !rid.isEmpty, let cid = ev.clientId, !cid.isEmpty else {
            actionError = "Missing session or client id."
            return
        }
        actionBusy = true
        defer { actionBusy = false }
        do {
            let day = Club360DateFormats.postgresDay.date(from: ev.date) ?? Date()
            try await ClientDataService.coachUpdateScheduleEvent(
                id: rid,
                clientId: cid,
                title: ev.title,
                date: day,
                time: ev.time,
                notes: ev.notes ?? "",
                isCompleted: completed
            )
            sessionActions = nil
            completionOverride[ev.id] = completed
            await onEventsChanged?()
        } catch {
            actionError = error.localizedDescription
        }
    }

    private func saveNote(_ ev: ScheduleEventDTO, notes: String) async {
        guard let rid = ev.rowId, !rid.isEmpty, let cid = ev.clientId, !cid.isEmpty else {
            actionError = "Missing session or client id."
            return
        }
        actionBusy = true
        defer { actionBusy = false }
        do {
            let day = Club360DateFormats.postgresDay.date(from: ev.date) ?? Date()
            try await ClientDataService.coachUpdateScheduleEvent(
                id: rid,
                clientId: cid,
                title: ev.title,
                date: day,
                time: ev.time,
                notes: notes.trimmingCharacters(in: .whitespacesAndNewlines),
                isCompleted: ev.isCompleted
            )
            noteSession = nil
            await onEventsChanged?()
        } catch {
            actionError = error.localizedDescription
        }
    }

    private func deleteSession(id: String) async {
        actionBusy = true
        defer {
            actionBusy = false
            deleteSessionId = nil
        }
        do {
            try await ClientDataService.coachDeleteScheduleEvent(id: id)
            events.removeAll { $0.rowId == id }
            await onEventsChanged?()
        } catch {
            actionError = error.localizedDescription
        }
    }
}

private struct AdminQuickAssignRef: Identifiable {
    let clientId: String
    let name: String
    let initialTab: CoachPlansHubTab
    var id: String { "\(clientId)-\(initialTab.rawValue)" }
}

private struct AdminScheduleEventRow: View {
    let event: ScheduleEventDTO
    let clientName: String?
    var isCompleted: Bool? = nil

    private var showDone: Bool { isCompleted ?? event.isCompleted }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(alignment: .firstTextBaseline) {
                Text(event.time)
                    .font(.caption.weight(.bold))
                    .foregroundStyle(Club360Theme.tealDark)
                Text(Club360DateFormats.displayDay(fromPostgresDay: event.date))
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(Club360Theme.captionOnGlass)
                Spacer()
                if showDone {
                    Text("Done")
                        .font(.caption2.weight(.bold))
                        .foregroundStyle(Club360Theme.mintDeep)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 4)
                        .background(.white.opacity(0.55), in: Capsule())
                }
            }

            Text(event.title)
                .font(.headline.weight(.semibold))
                .foregroundStyle(Club360Theme.cardTitle)

            if let clientName, !clientName.isEmpty {
                Text(clientName)
                    .font(.subheadline.weight(.medium))
                    .foregroundStyle(Club360Theme.burgundy)
            }

            if let notes = event.notes?.trimmingCharacters(in: .whitespacesAndNewlines), !notes.isEmpty {
                Text(notes)
                    .font(.caption)
                    .foregroundStyle(Club360Theme.captionOnGlass)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .club360Glass(cornerRadius: 24)
    }
}

@Observable
@MainActor
private final class AdminScheduleModel {
    var isLoading = false
    var errorMessage: String?
    var events: [ScheduleEventDTO] = []
    var clientNameById: [String: String] = [:]

    func load(coachUserId: String?) async {
        guard let coachUserId else {
            errorMessage = "Not signed in."
            return
        }
        isLoading = true
        errorMessage = nil
        defer { isLoading = false }

        do {
            async let clientsTask = ClientDataService.fetchClientsForCoach()
            async let eventsTask = ClientDataService.fetchScheduleEventsForCoach(coachUserId: coachUserId)
            let (clients, fetchedEvents) = try await (clientsTask, eventsTask)
            let assignedClients = clients.filter {
                !($0.coachId?.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ?? true)
            }
            let assignedIds = Set(assignedClients.compactMap(\.id))

            clientNameById = Dictionary(uniqueKeysWithValues: assignedClients.compactMap { client in
                guard let id = client.id, !id.isEmpty else { return nil }
                return (id, AdminViewModel.listTitle(for: client))
            })
            events = fetchedEvents
                .filter { event in
                    guard let clientId = event.clientId else { return false }
                    return assignedIds.contains(clientId)
                }
                .sorted(by: Self.eventSort)
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    func events(on day: String) -> [ScheduleEventDTO] {
        events.filter { $0.date == day }
    }

    func events(from start: Date, through end: Date) -> [ScheduleEventDTO] {
        let cal = Calendar.current
        let startDay = cal.startOfDay(for: start)
        let endDay = cal.startOfDay(for: end)
        return events.filter { event in
            guard let eventDate = Club360DateFormats.postgresDay.date(from: event.date) else { return false }
            let day = cal.startOfDay(for: eventDate)
            return day >= startDay && day <= endDay
        }
    }

    private static func eventSort(_ lhs: ScheduleEventDTO, _ rhs: ScheduleEventDTO) -> Bool {
        if lhs.date != rhs.date { return lhs.date < rhs.date }
        return lhs.time < rhs.time
    }
}

// MARK: - Clients tab

private struct AdminClientsTab: View {
    @State private var model = AdminViewModel()
    @State private var quickAssign: AdminQuickAssignRef?

    var body: some View {
        NavigationStack {
            ZStack {
                Club360ScreenBackground()

                ScrollView {
                    VStack(alignment: .leading, spacing: 22) {
                        adminListHeader

                        if model.isLoading {
                            ProgressView("Loading clients…")
                                .tint(Club360Theme.tealDark)
                                .frame(maxWidth: .infinity)
                                .padding()
                        }

                        if let err = model.errorMessage {
                            Text(err)
                                .font(.footnote)
                                .foregroundStyle(.red)
                                .padding()
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .club360Glass(cornerRadius: 22)
                        }

                        if !model.isLoading, model.errorMessage == nil, model.assignedClients.isEmpty {
                            ContentUnavailableView(
                                "No assigned clients yet",
                                systemImage: "person.3",
                                description: Text("New signups appear in New clients until a coach claims them.")
                            )
                            .padding(.top, 24)
                        }

                        ForEach(model.assignedClients, id: \.stableId) { client in
                            if let cid = client.id, !cid.isEmpty {
                                let title = AdminViewModel.listTitle(for: client)
                                NavigationLink {
                                    AdminClientHubView(clientId: cid, displayTitle: title)
                                } label: {
                                    AdminClientRow(
                                        title: title,
                                        subtitle: "Plans, meals, progress",
                                        memberSummary: client.memberSummaryLine,
                                        platformRole: model.profileRolesByUserId[client.userId]
                                    )
                                }
                                .buttonStyle(.plain)
                                .contextMenu {
                                    Button("Add session") {
                                        quickAssign = AdminQuickAssignRef(clientId: cid, name: title, initialTab: .schedule)
                                    }
                                    Button("Workout plan") {
                                        quickAssign = AdminQuickAssignRef(clientId: cid, name: title, initialTab: .workouts)
                                    }
                                    Button("Meal plan") {
                                        quickAssign = AdminQuickAssignRef(clientId: cid, name: title, initialTab: .meals)
                                    }
                                }
                            } else {
                                AdminClientRow(title: AdminViewModel.listTitle(for: client), subtitle: "Missing client id — check Supabase")
                                    .opacity(0.55)
                            }
                        }
                    }
                    .padding(.horizontal, 18)
                    .padding(.bottom, 28)
                }
            }
            .navigationTitle("Club360Fit")
            .navigationBarTitleDisplayMode(.inline)
            .toolbarBackground(.ultraThinMaterial, for: .navigationBar)
            .task {
                await model.load()
            }
            .refreshable {
                await model.load()
            }
            .fullScreenCover(item: $quickAssign) { ref in
                NavigationStack {
                    CoachPlansHubView(
                        clientId: ref.clientId,
                        displayTitle: ref.name,
                        initialTab: ref.initialTab
                    )
                    .toolbar {
                        ToolbarItem(placement: .topBarTrailing) {
                            Button("Done") { quickAssign = nil }
                        }
                    }
                }
            }
        }
    }

    private var adminListHeader: some View {
        HStack(alignment: .center, spacing: 14) {
            Image("LogoBurgundy")
                .resizable()
                .scaledToFit()
                .frame(width: 56, height: 56)
                .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                .shadow(color: Color.black.opacity(0.08), radius: 8, y: 4)
            VStack(alignment: .leading, spacing: 4) {
                Text("Coach")
                    .font(.largeTitle.weight(.bold))
                    .foregroundStyle(Club360Theme.burgundy)
                Text("Clients")
                    .font(.title3.weight(.semibold))
                    .foregroundStyle(Club360Theme.burgundy)
            }
        }
        .padding(.top, 4)
    }
}

private struct ClaimedClientNav: Identifiable, Hashable {
    let id: String
    let displayTitle: String
    var showOnboardingChecklist: Bool = true
}

private struct AdminNewClientsTab: View {
    @State private var model = AdminViewModel()
    @State private var claimingClientId: String?
    @State private var claimedNav: ClaimedClientNav?

    var body: some View {
        NavigationStack {
            ZStack {
                Club360ScreenBackground()

                ScrollView {
                    VStack(alignment: .leading, spacing: 22) {
                        HStack(alignment: .center, spacing: 14) {
                            Image(systemName: "person.badge.plus")
                                .font(.title.weight(.semibold))
                                .foregroundStyle(Club360Theme.tealDark)
                                .frame(width: 56, height: 56)
                                .background(.white.opacity(0.5), in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                            VStack(alignment: .leading, spacing: 4) {
                                Text("New clients")
                                    .font(.largeTitle.weight(.bold))
                                    .foregroundStyle(Club360Theme.burgundy)
                                Text("Review intake and claim members")
                                    .font(.title3.weight(.semibold))
                                    .foregroundStyle(Club360Theme.burgundy)
                            }
                            Spacer()
                            if !model.newClients.isEmpty {
                                Text("\(model.newClients.count)")
                                    .font(.caption.weight(.bold))
                                    .foregroundStyle(.white)
                                    .padding(.horizontal, 10)
                                    .padding(.vertical, 6)
                                    .background(Club360Theme.peachDeep, in: Capsule())
                            }
                        }
                        .padding(.top, 4)

                        if model.isLoading {
                            ProgressView("Loading new clients…")
                                .tint(Club360Theme.tealDark)
                                .frame(maxWidth: .infinity)
                                .padding()
                        }

                        if let err = model.errorMessage {
                            Text(err)
                                .font(.footnote)
                                .foregroundStyle(.red)
                                .padding()
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .club360Glass(cornerRadius: 22)
                        }

                        if !model.isLoading, model.errorMessage == nil, model.newClients.isEmpty {
                            ContentUnavailableView(
                                "No new clients",
                                systemImage: "person.badge.plus",
                                description: Text("New member signups appear here until a coach claims them.")
                            )
                            .padding(.top, 24)
                        }

                        ForEach(model.newClients, id: \.stableId) { client in
                            NewClientClaimRow(
                                client: client,
                                platformRole: model.profileRolesByUserId[client.userId],
                                isClaiming: claimingClientId == client.id,
                                onClaim: {
                                    guard let cid = client.id, !cid.isEmpty else { return }
                                    claimingClientId = cid
                                    let title = AdminViewModel.listTitle(for: client)
                                    Task {
                                        await model.claimClient(cid)
                                        claimingClientId = nil
                                        claimedNav = ClaimedClientNav(id: cid, displayTitle: title)
                                    }
                                }
                            )
                        }
                    }
                    .padding(.horizontal, 18)
                    .padding(.bottom, 28)
                }
            }
            .navigationTitle("New clients")
            .navigationBarTitleDisplayMode(.inline)
            .toolbarBackground(.ultraThinMaterial, for: .navigationBar)
            .navigationDestination(item: $claimedNav) { nav in
                AdminClientHubView(
                    clientId: nav.id,
                    displayTitle: nav.displayTitle,
                    showOnboardingChecklist: nav.showOnboardingChecklist
                )
            }
            .task {
                await model.load()
            }
            .refreshable {
                await model.load()
            }
        }
    }
}

private struct AdminClientRow: View {
    let title: String
    let subtitle: String
    var memberSummary: String = ""
    var platformRole: String? = nil

    var body: some View {
        HStack(alignment: .center, spacing: 12) {
            VStack(alignment: .leading, spacing: 6) {
                Text(title)
                    .font(.headline.weight(.semibold))
                    .foregroundStyle(Club360Theme.cardTitle)
                if let platformRole {
                    Text(platformRole == "admin" ? "App login: Admin" : "App login: Client")
                        .font(.caption2.weight(.semibold))
                        .foregroundStyle(platformRole == "admin" ? Club360Theme.tealDark : Club360Theme.captionOnGlass)
                }
                Text(subtitle)
                    .font(.caption)
                    .foregroundStyle(Club360Theme.captionOnGlass)
                if !memberSummary.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                    Text(memberSummary)
                        .font(.caption.weight(.medium))
                        .foregroundStyle(Club360Theme.cardTitle)
                        .fixedSize(horizontal: false, vertical: true)
                }
            }
            Spacer(minLength: 8)
            Image(systemName: "chevron.right")
                .font(.caption.weight(.semibold))
                .foregroundStyle(Club360Theme.captionOnGlass.opacity(0.85))
        }
        .padding(18)
        .frame(maxWidth: .infinity, alignment: .leading)
        .club360Glass(cornerRadius: 28)
    }
}

private struct NewClientClaimRow: View {
    let client: ClientDTO
    var platformRole: String? = nil
    let isClaiming: Bool
    let onClaim: () -> Void

    private var summary: String {
        client.memberSummaryLine.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            VStack(alignment: .leading, spacing: 6) {
                Text(AdminViewModel.listTitle(for: client))
                    .font(.headline.weight(.semibold))
                    .foregroundStyle(Club360Theme.cardTitle)
                if let platformRole {
                    Text(platformRole == "admin" ? "App login: Admin" : "App login: Client")
                        .font(.caption2.weight(.semibold))
                        .foregroundStyle(platformRole == "admin" ? Club360Theme.tealDark : Club360Theme.captionOnGlass)
                }
                Text(summary.isEmpty ? "No age, height, weight, or goal on file yet." : summary)
                    .font(.caption.weight(.medium))
                    .foregroundStyle(summary.isEmpty ? Club360Theme.captionOnGlass : Club360Theme.cardTitle)
                    .fixedSize(horizontal: false, vertical: true)
            }

            Button {
                onClaim()
            } label: {
                HStack {
                    if isClaiming {
                        ProgressView()
                            .tint(.white)
                    }
                    Text(isClaiming ? "Claiming…" : "Claim client")
                        .font(.subheadline.weight(.semibold))
                }
                .frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
            .tint(Club360Theme.burgundy)
            .disabled(isClaiming || client.id?.isEmpty != false)
        }
        .padding(18)
        .frame(maxWidth: .infinity, alignment: .leading)
        .club360Glass(cornerRadius: 28)
    }
}

// MARK: - Client hub (coach view of member tools)

struct AdminClientHubView: View {
    let clientId: String
    let displayTitle: String
    var showOnboardingChecklist: Bool = false

    @Environment(\.dismiss) private var dismiss
    @Environment(Club360AuthSession.self) private var auth

    @State private var homeModel = ClientHomeViewModel()
    @State private var roleBusy = false
    @State private var roleError: String?
    @State private var roleSuccess: String?
    @State private var desiredCoachAccess = false
    @State private var showApplyConfirm = false
    @State private var accessWorkouts = true
    @State private var accessNutrition = true
    @State private var accessEvents = false
    @State private var accessPayments = false
    @State private var accessBusy = false
    @State private var accessError: String?
    @State private var accessSuccess: String?
    @State private var transferTargetCoachId = ""
    @State private var transferBusy = false
    @State private var transferError: String?
    @State private var showTransferConfirm = false
    @State private var showCoachDirectorySheet = false
    @State private var showTransferSuccess = false
    @State private var showClaimChecklist = false
    @State private var pendingOpenPlans = false
    @State private var navigateToPlans = false

    var body: some View {
        Group {
            if homeModel.isLoading {
                ZStack {
                    Club360ScreenBackground()
                    ProgressView("Loading client…")
                        .tint(Club360Theme.tealDark)
                }
            } else if let err = homeModel.errorMessage {
                ZStack {
                    Club360ScreenBackground()
                    Text(err)
                        .font(.footnote)
                        .foregroundStyle(.red)
                        .multilineTextAlignment(.center)
                        .padding()
                        .club360Glass(cornerRadius: 22)
                        .padding()
                }
            } else {
                clientHubScroll
            }
        }
        .environment(homeModel)
        .navigationTitle(displayTitle)
        .navigationBarTitleDisplayMode(.inline)
        .toolbarBackground(.ultraThinMaterial, for: .navigationBar)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                NavigationLink {
                    CoachHubNotificationsView(
                        clientNameById: [clientId: displayTitle],
                        initialClientId: clientId,
                        onUnreadChanged: {
                            Task { await homeModel.reloadNotificationsCount() }
                        }
                    )
                } label: {
                    ZStack(alignment: .topTrailing) {
                        Image(systemName: "bell.fill")
                            .font(.body.weight(.semibold))
                            .foregroundStyle(Club360Theme.tealDark)
                        if homeModel.unreadNotifications > 0 {
                            Text("\(min(homeModel.unreadNotifications, 99))")
                                .font(.caption2.weight(.bold))
                                .foregroundStyle(.white)
                                .padding(4)
                                .background(Circle().fill(Club360Theme.peachDeep))
                                .offset(x: 10, y: -10)
                        }
                    }
                }
            }
        }
        .task(id: clientId) {
            await homeModel.loadForClient(clientId: clientId)
        }
        .refreshable {
            await homeModel.loadForClient(clientId: clientId)
        }
        .onAppear {
            Task { await homeModel.reloadNotificationsCount() }
            if showOnboardingChecklist {
                showClaimChecklist = true
            }
        }
        .sheet(isPresented: $showClaimChecklist, onDismiss: {
            guard pendingOpenPlans else { return }
            pendingOpenPlans = false
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.35) {
                navigateToPlans = true
            }
        }) {
            ClaimOnboardingChecklistSheet(
                clientName: displayTitle,
                onOpenPlans: {
                    pendingOpenPlans = true
                    showClaimChecklist = false
                },
                onDismiss: {
                    pendingOpenPlans = false
                    showClaimChecklist = false
                }
            )
        }
        .navigationDestination(isPresented: $navigateToPlans) {
            CoachPlansHubView(clientId: clientId, displayTitle: displayTitle)
        }
    }

    private var clientHubScroll: some View {
        ZStack {
            Club360ScreenBackground()

            ScrollView {
                VStack(alignment: .leading, spacing: 22) {
                    HStack(alignment: .center, spacing: 14) {
                        memberAvatar
                            .frame(width: 56, height: 56)
                            .clipShape(Circle())
                            .overlay(
                                Circle()
                                    .stroke(Color.black.opacity(0.1), lineWidth: 1)
                            )
                        VStack(alignment: .leading, spacing: 4) {
                            Text(homeModel.platformAccessCaption)
                                .font(.caption.weight(.bold))
                                .foregroundStyle(Club360Theme.captionOnGlass)
                                .textCase(.uppercase)
                            Text(homeModel.welcomeName)
                                .font(.title2.weight(.bold))
                                .foregroundStyle(Club360Theme.burgundy)
                        }
                    }
                    .padding(.top, 4)

                    NavigationLink {
                        CoachPlansHubView(clientId: clientId, displayTitle: displayTitle)
                    } label: {
                        HStack(spacing: 14) {
                            Image(systemName: "calendar.badge.plus")
                                .font(.title2.weight(.semibold))
                                .foregroundStyle(.white)
                                .frame(width: 44, height: 44)
                                .background(Club360Theme.primaryButtonGradient, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                            VStack(alignment: .leading, spacing: 4) {
                                Text("Plans & schedule")
                                    .font(.headline.weight(.semibold))
                                    .foregroundStyle(Club360Theme.cardTitle)
                                Text("Assign workouts, meals, and sessions")
                                    .font(.caption)
                                    .foregroundStyle(Club360Theme.captionOnGlass)
                            }
                            Spacer()
                            Image(systemName: "chevron.right")
                                .font(.caption.weight(.semibold))
                                .foregroundStyle(Club360Theme.captionOnGlass)
                        }
                        .padding(16)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .club360Glass(cornerRadius: 24)
                    }
                    .buttonStyle(.plain)

                    VStack(alignment: .leading, spacing: 14) {
                        Text("Client details")
                            .font(.caption.weight(.bold))
                            .foregroundStyle(Club360Theme.captionOnGlass)
                            .textCase(.uppercase)

                        HStack(alignment: .top, spacing: 14) {
                            memberAvatar
                                .frame(width: 92, height: 92)
                                .clipShape(Circle())
                                .overlay(
                                    Circle()
                                        .stroke(Color.black.opacity(0.1), lineWidth: 1)
                                )

                            VStack(alignment: .leading, spacing: 8) {
                                detailRow("Email", homeModel.memberEmail ?? "Not available")
                                detailRow("Phone", homeModel.memberPhone ?? "Not provided")
                                detailRow("Birthday", birthdayLabel)
                                detailRow("Member since", memberSinceLabel)
                            }
                        }

                        if homeModel.memberProfileSummaryLine.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                            Text("No age, height, weight, or goal on file yet. They appear here when set on the client record.")
                                .font(.footnote)
                                .foregroundStyle(Club360Theme.captionOnGlass)
                                .fixedSize(horizontal: false, vertical: true)
                        } else {
                            Text(homeModel.memberProfileSummaryLine)
                                .font(.subheadline.weight(.medium))
                                .foregroundStyle(Club360Theme.cardTitle)
                                .fixedSize(horizontal: false, vertical: true)
                        }

                        Divider().opacity(0.35)

                        detailRow("Medical notes", homeModel.memberMedicalConditions ?? "Not provided")
                        detailRow("Food restrictions", homeModel.memberFoodRestrictions ?? "Not provided")
                        detailRow("Meals per day", homeModel.memberMealsPerDay ?? "Not provided")
                        detailRow("Workout frequency", homeModel.memberWorkoutFrequency ?? "Not provided")
                    }
                    .padding(18)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .club360Glass(cornerRadius: 22)

                    Text("Review")
                        .font(.subheadline.weight(.bold))
                        .foregroundStyle(Club360Theme.cardTitle)
                        .textCase(.uppercase)
                        .tracking(0.8)

                    adminNextSessionCard

                    LazyVGrid(
                        columns: [GridItem(.flexible(), spacing: 12), GridItem(.flexible(), spacing: 12)],
                        spacing: 12
                    ) {
                        NavigationLink {
                            MyWorkoutsView(isCoachReviewing: true)
                                .environment(homeModel)
                        } label: {
                            Club360HomeTile(
                                title: "Workouts",
                                subtitle: workoutSubtitle,
                                systemImage: "figure.strengthtraining.traditional",
                                accent: Club360Theme.burgundyLight
                            )
                        }

                        NavigationLink {
                            MyMealsView(isCoachReviewing: true)
                                .environment(homeModel)
                        } label: {
                            Club360HomeTile(
                                title: "Meals",
                                subtitle: mealSubtitle,
                                systemImage: "takeoutbag.and.cup.and.straw.fill",
                                accent: Club360Theme.teal
                            )
                        }

                        NavigationLink {
                            MyProgressView(isCoachReviewing: true)
                                .environment(homeModel)
                        } label: {
                            Club360HomeTile(
                                title: "Progress",
                                subtitle: progressSubtitle,
                                systemImage: "chart.line.uptrend.xyaxis",
                                accent: Club360Theme.tealDark
                            )
                        }

                        NavigationLink {
                            MyDailyHabitsView(isCoachReviewing: true)
                                .environment(homeModel)
                        } label: {
                            Club360HomeTile(
                                title: "Habits",
                                subtitle: "Water · steps · sleep",
                                systemImage: "checkmark.circle.fill",
                                accent: Club360Theme.teal
                            )
                        }

                        NavigationLink {
                            MyScheduleView()
                                .environment(homeModel)
                        } label: {
                            Club360HomeTile(
                                title: "Schedule",
                                subtitle: scheduleSubtitle,
                                systemImage: "calendar.badge.clock",
                                accent: Club360Theme.burgundy
                            )
                        }

                        NavigationLink {
                            CoachPaymentSettingsView(clientId: clientId, clientDisplayName: displayTitle)
                        } label: {
                            Club360HomeTile(
                                title: "Payment setup",
                                subtitle: "Venmo, Zelle, due date",
                                systemImage: "square.and.pencil",
                                accent: Club360Theme.burgundy
                            )
                        }

                        NavigationLink {
                            MyPaymentsView(isCoachReviewing: true)
                                .environment(homeModel)
                        } label: {
                            Club360HomeTile(
                                title: "Payments",
                                subtitle: homeModel.canViewPayments ? "Approve · history" : "Hidden from member",
                                systemImage: "banknote.fill",
                                accent: Club360Theme.burgundy
                            )
                        }

                        NavigationLink {
                            MyMealPhotosView()
                                .environment(homeModel)
                        } label: {
                            Club360HomeTile(
                                title: "Meal photos",
                                subtitle: "Review uploads",
                                systemImage: "camera.fill",
                                accent: Club360Theme.purpleLight
                            )
                        }

                        if homeModel.memberAuthUserId != nil {
                            NavigationLink {
                                clientSettingsView
                            } label: {
                                Club360HomeTile(
                                    title: "Member settings",
                                    subtitle: "Access and transfer",
                                    systemImage: "slider.horizontal.3",
                                    accent: Club360Theme.burgundy
                                )
                            }
                        }
                    }
                }
                .padding(.horizontal, 18)
                .padding(.bottom, 24)
            }
        }
    }

    private var memberAvatar: some View {
        Group {
            if let rawURL = homeModel.memberAvatarURLString,
               let url = URL(string: rawURL) {
                AsyncImage(url: url) { phase in
                    switch phase {
                    case .empty:
                        ZStack {
                            Club360Theme.creamWarm
                            ProgressView()
                                .tint(Club360Theme.burgundy)
                        }
                    case .success(let image):
                        image
                            .resizable()
                            .scaledToFill()
                    default:
                        Image("LogoBurgundy")
                            .resizable()
                            .scaledToFit()
                            .padding(6)
                    }
                }
            } else if let uid = homeModel.memberAuthUserId,
               let url = ClientDataService.publicAvatarURLForAuthUserId(uid) {
                AsyncImage(url: url) { phase in
                    switch phase {
                    case .empty:
                        ZStack {
                            Club360Theme.creamWarm
                            ProgressView()
                                .tint(Club360Theme.burgundy)
                        }
                    case .success(let image):
                        image
                            .resizable()
                            .scaledToFill()
                    default:
                        Image("LogoBurgundy")
                            .resizable()
                            .scaledToFit()
                            .padding(6)
                    }
                }
            } else {
                Image("LogoBurgundy")
                    .resizable()
                    .scaledToFit()
                    .padding(4)
            }
        }
    }

    private var birthdayLabel: String {
        guard let raw = homeModel.memberBirthDate else { return "Not provided" }
        return Club360DateFormats.displayDay(fromPostgresDay: raw)
    }

    private var memberSinceLabel: String {
        guard let date = homeModel.memberSinceStartOfDay else { return "Not available" }
        return Club360DateFormats.displayDay(from: date)
    }

    private func detailRow(_ label: String, _ value: String) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(label)
                .font(.caption.weight(.bold))
                .foregroundStyle(Club360Theme.captionOnGlass)
                .textCase(.uppercase)
            Text(value)
                .font(.subheadline.weight(.medium))
                .foregroundStyle(Club360Theme.cardTitle)
                .fixedSize(horizontal: false, vertical: true)
        }
    }

    private var clientSettingsView: some View {
        ZStack {
            Club360ScreenBackground()
            ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                    VStack(alignment: .leading, spacing: 12) {
                        Text("Account access")
                            .font(.headline.weight(.semibold))
                            .foregroundStyle(Club360Theme.cardTitle)
                        Text(
                            "Use the toggle, then tap Apply. Requires the set-user-role Edge Function deployed to your Supabase project. The member must sign out and sign in again."
                        )
                        .font(.footnote)
                        .foregroundStyle(Club360Theme.captionOnGlass)
                        .fixedSize(horizontal: false, vertical: true)

                        Toggle(isOn: $desiredCoachAccess) {
                            VStack(alignment: .leading, spacing: 2) {
                                Text("Coach / admin access")
                                    .foregroundStyle(Club360Theme.cardTitle)
                                Text("Off = member-only access")
                                    .font(.caption)
                                    .foregroundStyle(Club360Theme.captionOnGlass)
                            }
                        }
                        .tint(Club360Theme.burgundy)
                        .disabled(roleBusy)

                        if let roleSuccess {
                            Text(roleSuccess)
                                .font(.footnote.weight(.medium))
                                .foregroundStyle(Club360Theme.burgundy)
                        }
                        if let roleError {
                            Text(roleError)
                                .font(.footnote)
                                .foregroundStyle(.red)
                        }

                        Button {
                            showApplyConfirm = true
                        } label: {
                            Text(roleBusy ? "Updating…" : "Apply")
                        }
                        .buttonStyle(Club360PrimaryGradientButtonStyle())
                        .disabled(roleBusy)
                        .confirmationDialog(
                            desiredCoachAccess ? "Grant coach/admin access?" : "Set member-only access?",
                            isPresented: $showApplyConfirm,
                            titleVisibility: .visible
                        ) {
                            Button("Apply", role: .destructive) {
                                Task { await applyMemberRole(desiredCoachAccess ? "admin" : "client") }
                            }
                            Button("Cancel", role: .cancel) {}
                        } message: {
                            Text(
                                desiredCoachAccess
                                    ? "This will let the member access the Hub after signing in again."
                                    : "This will remove Hub access after signing in again."
                            )
                        }
                    }
                    .padding(16)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .club360Glass(cornerRadius: 22)

                    VStack(alignment: .leading, spacing: 12) {
                        Text("Member app access")
                            .font(.headline.weight(.semibold))
                            .foregroundStyle(Club360Theme.cardTitle)
                        Text("Enable or disable tiles in the member app.")
                            .font(.footnote)
                            .foregroundStyle(Club360Theme.captionOnGlass)

                        Toggle("Workouts", isOn: $accessWorkouts)
                            .tint(Club360Theme.burgundy)
                            .disabled(accessBusy)
                        Toggle("Meals / nutrition", isOn: $accessNutrition)
                            .tint(Club360Theme.burgundy)
                            .disabled(accessBusy)
                        Toggle("Schedule", isOn: $accessEvents)
                            .tint(Club360Theme.burgundy)
                            .disabled(accessBusy)
                        Toggle("Payments", isOn: $accessPayments)
                            .tint(Club360Theme.burgundy)
                            .disabled(accessBusy)

                        if let accessSuccess {
                            Text(accessSuccess)
                                .font(.footnote.weight(.medium))
                                .foregroundStyle(Club360Theme.burgundy)
                        }
                        if let accessError {
                            Text(accessError)
                                .font(.footnote)
                                .foregroundStyle(.red)
                        }

                        Button {
                            Task { await applyMemberAccess() }
                        } label: {
                            Text(accessBusy ? "Saving…" : "Apply access")
                        }
                        .buttonStyle(Club360PrimaryGradientButtonStyle())
                        .disabled(accessBusy)
                    }
                    .padding(16)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .club360Glass(cornerRadius: 22)

                    VStack(alignment: .leading, spacing: 12) {
                        Text("Transfer to another coach")
                            .font(.headline.weight(.semibold))
                            .foregroundStyle(Club360Theme.cardTitle)
                        Text(
                            "Paste another coach’s user ID, or open the directory below. You must be this member’s current coach. After transfer, you will no longer see them in your list."
                        )
                        .font(.footnote)
                        .foregroundStyle(Club360Theme.captionOnGlass)
                        .fixedSize(horizontal: false, vertical: true)
                        Button {
                            showCoachDirectorySheet = true
                        } label: {
                            HStack {
                                Image(systemName: "person.2.fill")
                                Text("Browse coaches & copy IDs")
                                    .font(.subheadline.weight(.semibold))
                            }
                            .frame(maxWidth: .infinity)
                        }
                        .buttonStyle(.bordered)
                        .tint(Club360Theme.burgundy)
                        TextField("Target coach user UUID", text: $transferTargetCoachId)
                            .textContentType(.none)
                            .autocorrectionDisabled()
                            .foregroundStyle(Club360Theme.cardTitle)
                        if let transferError {
                            Text(transferError)
                                .font(.footnote)
                                .foregroundStyle(.red)
                        }
                        Button {
                            showTransferConfirm = true
                        } label: {
                            Text(transferBusy ? "Transferring…" : "Transfer client")
                        }
                        .buttonStyle(Club360PrimaryGradientButtonStyle())
                        .disabled(
                            transferBusy
                                || transferTargetCoachId.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
                        )
                        .confirmationDialog(
                            "Transfer this member to another coach?",
                            isPresented: $showTransferConfirm,
                            titleVisibility: .visible
                        ) {
                            Button("Transfer", role: .destructive) {
                                Task { await runTransferClient() }
                            }
                            Button("Cancel", role: .cancel) {}
                        } message: {
                            Text("You will lose access to this client’s hub after transfer.")
                        }
                    }
                    .padding(16)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .club360Glass(cornerRadius: 22)
                }
                .padding(.horizontal, 18)
                .padding(.vertical, 14)
            }
        }
        .navigationTitle("Member settings")
        .navigationBarTitleDisplayMode(.inline)
        .toolbarBackground(.ultraThinMaterial, for: .navigationBar)
        .onAppear {
            syncAccessTogglesFromHomeModel()
        }
        .sheet(isPresented: $showCoachDirectorySheet) {
            NavigationStack {
                CoachDirectoryView(
                    currentUserId: auth.session?.user.id.uuidString,
                    onSelectForTransfer: { id in
                        transferTargetCoachId = id
                        showCoachDirectorySheet = false
                    }
                )
            }
        }
        .alert("Transfer complete", isPresented: $showTransferSuccess) {
            Button("OK") { dismiss() }
        } message: {
            Text(
                "This member is now assigned to the other coach. They will no longer appear in your client list."
            )
        }
    }

    private func applyMemberRole(_ role: String) async {
        guard let uid = homeModel.memberAuthUserId else { return }
        roleBusy = true
        roleError = nil
        roleSuccess = nil
        defer { roleBusy = false }
        do {
            try await AdminRoleService.setUserRole(targetAuthUserId: uid, role: role)
            roleSuccess =
                role == "admin"
                ? "Coach access updated. They will see the Hub after signing in again."
                : "Member access set. They will see the client app after signing in again."
        } catch {
            roleError = error.localizedDescription
        }
    }

    private func runTransferClient() async {
        transferBusy = true
        transferError = nil
        defer { transferBusy = false }
        do {
            try await ClientTransferService.transferClient(
                clientId: clientId,
                targetCoachUserId: transferTargetCoachId
            )
            showTransferSuccess = true
        } catch {
            transferError = error.localizedDescription
        }
    }

    private func syncAccessTogglesFromHomeModel() {
        accessWorkouts = homeModel.canViewWorkouts
        accessNutrition = homeModel.canViewNutrition
        accessEvents = homeModel.canViewEvents
        accessPayments = homeModel.canViewPayments
    }

    private func applyMemberAccess() async {
        accessBusy = true
        accessError = nil
        accessSuccess = nil
        defer { accessBusy = false }
        do {
            try await ClientDataService.updateClientAccessFlags(
                clientId: clientId,
                canViewWorkouts: accessWorkouts,
                canViewNutrition: accessNutrition,
                canViewEvents: accessEvents,
                canViewPayments: accessPayments
            )
            accessSuccess = "Member app access updated."
            await homeModel.loadForClient(clientId: clientId)
            syncAccessTogglesFromHomeModel()
        } catch {
            accessError = error.localizedDescription
        }
    }

    private var adminNextSessionCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text("Next session")
                    .font(.caption.weight(.bold))
                    .foregroundStyle(Club360Theme.captionOnTintedCard)
                    .textCase(.uppercase)
                Spacer()
                Image(systemName: "calendar.badge.clock")
                    .foregroundStyle(Club360Theme.burgundy)
            }
            Text(homeModel.nextSessionLine ?? "No upcoming sessions scheduled.")
                .font(.body.weight(.semibold))
                .foregroundStyle(Club360Theme.cardTitle)
                .fixedSize(horizontal: false, vertical: true)
            Text("\(homeModel.upcomingSessionCount) upcoming")
                .font(.caption.weight(.medium))
                .foregroundStyle(Club360Theme.captionOnTintedCard)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(18)
        .background(Club360Theme.sessionCardGradient, in: RoundedRectangle(cornerRadius: 28, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 28, style: .continuous)
                .stroke(
                    LinearGradient(
                        colors: [Color.white.opacity(0.9), Color.black.opacity(0.12)],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    ),
                    lineWidth: 1.25
                )
        )
        .shadow(color: Color.black.opacity(0.12), radius: 20, x: 0, y: 12)
    }

    private var workoutSubtitle: String {
        if let t = homeModel.currentWorkoutTitle {
            return "Current: \(t) · \(homeModel.workoutPlanCount) plan\(homeModel.workoutPlanCount == 1 ? "" : "s")"
        }
        let accessNote = homeModel.canViewWorkouts ? "" : " · hidden from member"
        return (homeModel.isLoading ? "…" : "Plans & sessions") + accessNote
    }

    private var mealSubtitle: String {
        if let t = homeModel.currentMealTitle {
            return "Current: \(t) · \(homeModel.mealPlanCount) plan\(homeModel.mealPlanCount == 1 ? "" : "s")"
        }
        let accessNote = homeModel.canViewNutrition ? "" : " · hidden from member"
        return (homeModel.isLoading ? "…" : "Nutrition") + accessNote
    }

    private var progressSubtitle: String {
        let n = homeModel.progressCheckInCount
        return n == 0 ? "Metrics" : "\(n) check-in\(n == 1 ? "" : "s") logged"
    }

    private var scheduleSubtitle: String {
        if let line = homeModel.nextSessionLine {
            return "Next: \(line)"
        }
        return homeModel.upcomingSessionCount == 0 ? "No upcoming" : "\(homeModel.upcomingSessionCount) upcoming"
    }
}

private struct ClaimOnboardingChecklistSheet: View {
    let clientName: String
    let onOpenPlans: () -> Void
    let onDismiss: () -> Void

    var body: some View {
        NavigationStack {
            VStack(alignment: .leading, spacing: 18) {
                Text("\(clientName) is on your roster")
                    .font(.title2.weight(.bold))
                    .foregroundStyle(Club360Theme.cardTitle)
                Text("Suggested next steps:")
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(Club360Theme.captionOnGlass)

                checklistRow(1, "Member settings — turn on Workouts, Meals, Schedule, Payments as needed")
                checklistRow(2, "Plans & schedule — assign a workout or meal plan and first session")
                checklistRow(3, "Payment setup — Venmo/Zelle and next due date")

                Spacer()

                Button("Open Plans & schedule", action: onOpenPlans)
                    .buttonStyle(Club360PrimaryGradientButtonStyle())
                Button("I’ll do this later", action: onDismiss)
                    .font(.subheadline.weight(.semibold))
                    .frame(maxWidth: .infinity)
                    .foregroundStyle(Club360Theme.captionOnGlass)
            }
            .padding(22)
            .navigationTitle("Client claimed")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close", action: onDismiss)
                }
            }
        }
        .presentationDetents([.medium])
    }

    private func checklistRow(_ n: Int, _ text: String) -> some View {
        HStack(alignment: .top, spacing: 12) {
            Text("\(n)")
                .font(.caption.weight(.bold))
                .foregroundStyle(.white)
                .frame(width: 24, height: 24)
                .background(Club360Theme.burgundy, in: Circle())
            Text(text)
                .font(.subheadline)
                .foregroundStyle(Club360Theme.cardTitle)
                .fixedSize(horizontal: false, vertical: true)
        }
    }
}

#Preview {
    AdminHomeView()
        .environment(Club360AuthSession())
}
