import Auth
import Observation
import SwiftUI

/// Main coach dashboard: overview (overdue / current / upcoming), calendar, quick assign, per-client editors.
struct CoachMainHubView: View {
    @Environment(Club360AuthSession.self) private var auth: Club360AuthSession

    @State private var admin = AdminViewModel()
    @State private var overview = CoachOverviewModel()

    @State private var visibleMonth: Date = Date()
    @State private var selectedDay: Date = Calendar.current.startOfDay(for: Date())

    @State private var clientPicker: ClientPickerMode?
    @State private var fullScreenPlans: ClientRef?
    @State private var coachUnreadNotifications = 0

    @State private var hubSessionActions: ScheduleEventDTO?
    @State private var hubNoteSession: ScheduleEventDTO?
    @State private var hubDeleteSessionId: String?
    @State private var hubActionBusy = false
    @State private var hubActionError: String?

    private let upcomingHorizonDays = 7

    var body: some View {
        ZStack {
            Club360ScreenBackground()

            ScrollViewReader { proxy in
                ScrollView {
                    VStack(alignment: .leading, spacing: 22) {
                        hubHeader

                        if overview.isLoading, overview.scheduleEvents.isEmpty, overview.workoutPlans.isEmpty {
                            ProgressView("Loading overview…")
                                .tint(Club360Theme.tealDark)
                                .frame(maxWidth: .infinity)
                        }

                        if let err = overview.errorMessage {
                            Text(err)
                                .font(.footnote)
                                .foregroundStyle(.red)
                                .padding()
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .club360Glass(cornerRadius: 22)
                        }

                        overviewCards(proxy: proxy)

                        Text("Quick assign")
                            .font(.subheadline.weight(.bold))
                            .foregroundStyle(Club360Theme.cardTitle)
                            .textCase(.uppercase)

                        VStack(spacing: 10) {
                            assignButton("Schedule session", systemImage: "calendar.badge.plus", mode: .schedule)
                            assignButton("Workout plan", systemImage: "figure.run", mode: .workout)
                            assignButton("Meal plan", systemImage: "takeoutbag.and.cup.and.straw.fill", mode: .meal)
                        }

                        VStack(alignment: .leading, spacing: 12) {
                            Text("Calendar")
                                .font(.subheadline.weight(.bold))
                                .foregroundStyle(Club360Theme.cardTitle)
                                .textCase(.uppercase)

                            CoachHubCalendarStrip(
                                visibleMonth: $visibleMonth,
                                selectedDay: $selectedDay,
                                eventDates: Set(overview.scheduleEvents.map(\.date))
                            )

                            eventsOnSelectedDay
                        }
                        .id("calendarSection")

                        Text("Clients")
                            .font(.subheadline.weight(.bold))
                            .foregroundStyle(Club360Theme.cardTitle)
                            .textCase(.uppercase)

                        Text("Open a member to edit their assigned plans and sessions.")
                            .font(.footnote)
                            .foregroundStyle(Club360Theme.captionOnGlass)

                        ForEach(admin.clients, id: \.stableId) { client in
                            if let cid = client.id, !cid.isEmpty {
                                NavigationLink {
                                    CoachPlansHubView(clientId: cid, displayTitle: AdminViewModel.listTitle(for: client))
                                } label: {
                                    HStack {
                                        VStack(alignment: .leading, spacing: 4) {
                                            Text(AdminViewModel.listTitle(for: client))
                                                .font(.headline.weight(.semibold))
                                                .foregroundStyle(Club360Theme.cardTitle)
                                            Text("Plans & schedule editor")
                                                .font(.caption)
                                                .foregroundStyle(Club360Theme.captionOnGlass)
                                        }
                                        Spacer()
                                        Image(systemName: "chevron.right")
                                            .font(.caption.weight(.semibold))
                                            .foregroundStyle(Club360Theme.captionOnGlass.opacity(0.8))
                                    }
                                    .padding(16)
                                    .frame(maxWidth: .infinity, alignment: .leading)
                                    .club360Glass(cornerRadius: 24)
                                }
                                .buttonStyle(.plain)
                            }
                        }
                    }
                .padding(.horizontal, 18)
                .padding(.bottom, 28)
                }
            }
        }
        .navigationTitle("Hub")
        .navigationBarTitleDisplayMode(.large)
        .toolbarBackground(.ultraThinMaterial, for: .navigationBar)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                NavigationLink {
                    CoachHubNotificationsView(
                        clientNameById: clientNameMap,
                        onUnreadChanged: {
                            Task { await loadCoachUnreadCount() }
                        }
                    )
                } label: {
                    ZStack(alignment: .topTrailing) {
                        Image(systemName: "bell.fill")
                            .font(.body.weight(.semibold))
                            .foregroundStyle(Club360Theme.tealDark)
                        if coachUnreadNotifications > 0 {
                            Text("\(min(coachUnreadNotifications, 99))")
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
        .task {
            await admin.load()
            await reloadOverview()
            await loadCoachUnreadCount()
        }
        .onAppear {
            Task { await loadCoachUnreadCount() }
        }
        .refreshable {
            await admin.load()
            await reloadOverview()
            await loadCoachUnreadCount()
        }
        .sheet(item: $clientPicker) { mode in
            ClientPickerSheet(
                clients: admin.clients,
                mode: mode,
                onSelect: { ref in
                    clientPicker = nil
                    fullScreenPlans = ref
                },
                onCancel: { clientPicker = nil }
            )
        }
        .fullScreenCover(item: $fullScreenPlans) { ref in
            NavigationStack {
                CoachPlansHubView(
                    clientId: ref.clientId,
                    displayTitle: ref.name,
                    initialTab: ref.initialTab
                )
                .toolbar {
                    ToolbarItem(placement: .topBarTrailing) {
                        Button("Done") { fullScreenPlans = nil }
                    }
                }
            }
        }
        .sheet(item: $hubSessionActions) { ev in
            HubSessionActionsSheet(
                event: ev,
                clientName: ev.clientId.flatMap { clientNameMap[$0] },
                isBusy: hubActionBusy,
                onMarkComplete: { Task { await hubMarkComplete(ev, completed: true) } },
                onMarkIncomplete: { Task { await hubMarkComplete(ev, completed: false) } },
                onAddNote: {
                    hubNoteSession = ev
                    hubSessionActions = nil
                },
                onDelete: {
                    hubDeleteSessionId = ev.rowId
                    hubSessionActions = nil
                },
                onDismiss: { hubSessionActions = nil }
            )
        }
        .sheet(item: $hubNoteSession) { ev in
            HubSessionNoteSheet(
                event: ev,
                isBusy: hubActionBusy,
                onSave: { notes in
                    Task { await hubSaveNote(ev, notes: notes) }
                },
                onDismiss: { hubNoteSession = nil }
            )
        }
        .alert("Delete session?", isPresented: Binding(
            get: { hubDeleteSessionId != nil },
            set: { if !$0 { hubDeleteSessionId = nil } }
        )) {
            Button("Cancel", role: .cancel) { hubDeleteSessionId = nil }
            Button("Delete", role: .destructive) {
                guard let id = hubDeleteSessionId else { return }
                Task { await hubDeleteSession(id: id) }
            }
        } message: {
            Text("Remove this session from the calendar? This cannot be undone.")
        }
        .alert("Could not update session", isPresented: Binding(
            get: { hubActionError != nil },
            set: { if !$0 { hubActionError = nil } }
        )) {
            Button("OK", role: .cancel) { hubActionError = nil }
        } message: {
            Text(hubActionError ?? "")
        }
    }

    private var hubHeader: some View {
        HStack(alignment: .center, spacing: 14) {
            Image("LogoBurgundy")
                .resizable()
                .scaledToFit()
                .frame(width: 52, height: 52)
                .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
            VStack(alignment: .leading, spacing: 4) {
                Text("Coach hub")
                    .font(.largeTitle.weight(.bold))
                    .foregroundStyle(Club360Theme.burgundy)
                Text("Assignments & schedule")
                    .font(.title3.weight(.semibold))
                    .foregroundStyle(Club360Theme.burgundy)
            }
        }
        .padding(.top, 4)
    }

    private var clientNameMap: [String: String] {
        var m: [String: String] = [:]
        for c in admin.clients {
            guard let id = c.id, !id.isEmpty else { continue }
            m[id] = AdminViewModel.listTitle(for: c)
        }
        return m
    }

    private func overviewCards(proxy: ScrollViewProxy) -> some View {
        let cal = Calendar.current
        let today = cal.startOfDay(for: Date())
        let weekStart = Club360DateFormats.dayString(Calendar.weekStartSunday(containing: Date()))
        let horizonEndDay = cal.date(byAdding: .day, value: upcomingHorizonDays, to: today) ?? today

        let overdue = overview.scheduleEvents.filter { ev in
            guard !ev.isCompleted, let d = Club360DateFormats.postgresDay.date(from: ev.date) else { return false }
            return cal.startOfDay(for: d) < today
        }

        let upcoming = overview.scheduleEvents.filter { ev in
            guard !ev.isCompleted, let d = Club360DateFormats.postgresDay.date(from: ev.date) else { return false }
            let ds = cal.startOfDay(for: d)
            return ds >= today && ds <= horizonEndDay
        }

        let overdueSorted = overdue.sorted { a, b in
            let da = Club360DateFormats.postgresDay.date(from: a.date) ?? .distantPast
            let db = Club360DateFormats.postgresDay.date(from: b.date) ?? .distantPast
            return da < db
        }
        let upcomingSorted = upcoming.sorted { a, b in
            let da = Club360DateFormats.postgresDay.date(from: a.date) ?? .distantPast
            let db = Club360DateFormats.postgresDay.date(from: b.date) ?? .distantPast
            return da < db
        }

        let workoutThisWeek = overview.workoutPlans.filter { $0.weekStart == weekStart }.count
        let mealThisWeek = overview.mealPlans.filter { $0.weekStart == weekStart }.count

        func scrollCalendarIntoView() {
            withAnimation(.easeInOut(duration: 0.35)) {
                proxy.scrollTo("calendarSection", anchor: .center)
            }
        }

        func jumpToOverdueOrCalendar() {
            if let ev = overdueSorted.first, let d = Club360DateFormats.postgresDay.date(from: ev.date) {
                let day = cal.startOfDay(for: d)
                selectedDay = day
                visibleMonth = day
            } else {
                selectedDay = today
                visibleMonth = today
            }
            scrollCalendarIntoView()
        }

        func jumpToUpcomingOrCalendar() {
            if let ev = upcomingSorted.first, let d = Club360DateFormats.postgresDay.date(from: ev.date) {
                let day = cal.startOfDay(for: d)
                selectedDay = day
                visibleMonth = day
            } else {
                selectedDay = today
                visibleMonth = today
            }
            scrollCalendarIntoView()
        }

        return VStack(alignment: .leading, spacing: 12) {
            Text("At a glance")
                .font(.subheadline.weight(.bold))
                .foregroundStyle(Club360Theme.cardTitle)
                .textCase(.uppercase)

            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                CoachStatButton(
                    title: "Overdue",
                    value: "\(overdue.count)",
                    subtitle: "Sessions not done",
                    tint: Club360Theme.peachDeep,
                    action: jumpToOverdueOrCalendar
                )
                CoachStatButton(
                    title: "Next \(upcomingHorizonDays) days",
                    value: "\(upcoming.count)",
                    subtitle: "Upcoming sessions",
                    tint: Club360Theme.tealDark,
                    action: jumpToUpcomingOrCalendar
                )
                CoachStatButton(
                    title: "This week",
                    value: "\(workoutThisWeek)",
                    subtitle: "Workout plans",
                    tint: Club360Theme.mintDeep,
                    disabled: admin.clients.isEmpty,
                    action: { clientPicker = .workout }
                )
                CoachStatButton(
                    title: "This week",
                    value: "\(mealThisWeek)",
                    subtitle: "Meal plans",
                    tint: Club360Theme.teal,
                    disabled: admin.clients.isEmpty,
                    action: { clientPicker = .meal }
                )
            }
        }
    }

    private func loadCoachUnreadCount() async {
        coachUnreadNotifications = (try? await ClientDataService.unreadNotificationCountForCoach()) ?? 0
    }

    private func hubMarkComplete(_ ev: ScheduleEventDTO, completed: Bool) async {
        guard let rid = ev.rowId, !rid.isEmpty, let cid = ev.clientId, !cid.isEmpty else {
            hubActionError = "Missing session or client id."
            return
        }
        hubActionBusy = true
        defer { hubActionBusy = false }
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
            hubSessionActions = nil
            await reloadOverview()
        } catch {
            hubActionError = error.localizedDescription
        }
    }

    private func hubSaveNote(_ ev: ScheduleEventDTO, notes: String) async {
        guard let rid = ev.rowId, !rid.isEmpty, let cid = ev.clientId, !cid.isEmpty else {
            hubActionError = "Missing session or client id."
            return
        }
        hubActionBusy = true
        defer { hubActionBusy = false }
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
            hubNoteSession = nil
            await reloadOverview()
        } catch {
            hubActionError = error.localizedDescription
        }
    }

    private func hubDeleteSession(id: String) async {
        hubActionBusy = true
        defer {
            hubActionBusy = false
            hubDeleteSessionId = nil
        }
        do {
            try await ClientDataService.coachDeleteScheduleEvent(id: id)
            await reloadOverview()
        } catch {
            hubActionError = error.localizedDescription
        }
    }

    private var eventsOnSelectedDay: some View {
        let key = Club360DateFormats.dayString(selectedDay)
        let dayEvents = overview.scheduleEvents.filter { $0.date == key }
            .sorted { $0.time < $1.time }

        return VStack(alignment: .leading, spacing: 10) {
            Text("Sessions on \(Club360DateFormats.displayDay(fromPostgresDay: key))")
                .font(.headline.weight(.semibold))
                .foregroundStyle(Club360Theme.cardTitle)

            if dayEvents.isEmpty {
                Text("No sessions.")
                    .font(.subheadline)
                    .foregroundStyle(Club360Theme.captionOnGlass)
            } else {
                ForEach(dayEvents, id: \.id) { ev in
                    Button {
                        hubSessionActions = ev
                    } label: {
                        HubSessionCard(
                            event: ev,
                            clientName: ev.clientId.flatMap { overview.clientNameById[$0] }
                        )
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    private func assignButton(_ title: String, systemImage: String, mode: ClientPickerMode) -> some View {
        Button {
            clientPicker = mode
        } label: {
            HStack(spacing: 12) {
                Image(systemName: systemImage)
                    .font(.title3)
                    .foregroundStyle(Club360Theme.burgundy)
                    .frame(width: 36)
                Text(title)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(Club360Theme.cardTitle)
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(Club360Theme.captionOnGlass)
            }
            .padding(14)
            .frame(maxWidth: .infinity, alignment: .leading)
            .club360Glass(cornerRadius: 22)
        }
        .buttonStyle(.plain)
        .disabled(admin.clients.isEmpty)
        .opacity(admin.clients.isEmpty ? 0.45 : 1)
    }

    private func reloadOverview() async {
        guard let uid = auth.session?.user.id.uuidString else {
            overview.errorMessage = "Not signed in."
            return
        }
        await overview.load(coachUserId: uid, clients: admin.clients)
    }
}

// MARK: - Overview model

@Observable
@MainActor
private final class CoachOverviewModel {
    var isLoading = false
    var errorMessage: String?
    var workoutPlans: [WorkoutPlanDTO] = []
    var mealPlans: [MealPlanDTO] = []
    var scheduleEvents: [ScheduleEventDTO] = []
    var clientNameById: [String: String] = [:]

    func load(coachUserId: String, clients: [ClientDTO]) async {
        isLoading = true
        errorMessage = nil
        defer { isLoading = false }
        var nameMap: [String: String] = [:]
        for c in clients {
            guard let id = c.id, !id.isEmpty else { continue }
            nameMap[id] = AdminViewModel.listTitle(for: c)
        }
        clientNameById = nameMap
        do {
            async let w = ClientDataService.fetchWorkoutPlansForCoach()
            async let m = ClientDataService.fetchMealPlansForCoach()
            async let s = ClientDataService.fetchScheduleEventsForCoach(coachUserId: coachUserId)
            workoutPlans = try await w
            mealPlans = try await m
            scheduleEvents = try await s
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}

// MARK: - Client picker & full-screen editor

private struct ClientRef: Identifiable {
    let clientId: String
    let name: String
    let initialTab: CoachPlansHubTab

    var id: String { "\(clientId)-\(initialTab.rawValue)" }
}

private enum ClientPickerMode: String, Identifiable {
    case schedule
    case workout
    case meal

    var id: String { rawValue }

    var title: String {
        switch self {
        case .schedule: return "Choose client — session"
        case .workout: return "Choose client — workout plan"
        case .meal: return "Choose client — meal plan"
        }
    }

    var tab: CoachPlansHubTab {
        switch self {
        case .schedule: return .schedule
        case .workout: return .workouts
        case .meal: return .meals
        }
    }
}

private struct ClientPickerSheet: View {
    let clients: [ClientDTO]
    let mode: ClientPickerMode
    let onSelect: (ClientRef) -> Void
    let onCancel: () -> Void

    var body: some View {
        NavigationStack {
            List {
                ForEach(clients, id: \.stableId) { c in
                    if let cid = c.id, !cid.isEmpty {
                        Button {
                            let name = AdminViewModel.listTitle(for: c)
                            onSelect(ClientRef(clientId: cid, name: name, initialTab: mode.tab))
                        } label: {
                            Text(AdminViewModel.listTitle(for: c))
                        }
                    }
                }
            }
            .navigationTitle(mode.title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel", action: onCancel)
                }
            }
        }
    }
}

// MARK: - Hub session card & actions

private struct HubSessionCard: View {
    let event: ScheduleEventDTO
    let clientName: String?

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack(alignment: .firstTextBaseline) {
                HStack(spacing: 8) {
                    if !event.time.isEmpty {
                        Text(event.time)
                            .font(.caption.weight(.bold))
                            .foregroundStyle(Club360Theme.burgundy)
                    }
                    if let clientName, !clientName.isEmpty {
                        Text(clientName)
                            .font(.caption.weight(.semibold))
                            .foregroundStyle(Club360Theme.captionOnGlass)
                    }
                }
                Spacer()
                Image(systemName: "ellipsis")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(Club360Theme.captionOnGlass)
            }
            Text(event.title)
                .font(.body.weight(.medium))
                .foregroundStyle(Club360Theme.cardTitle)
                .multilineTextAlignment(.leading)
            if let notes = event.notes?.trimmingCharacters(in: .whitespacesAndNewlines), !notes.isEmpty {
                Text(notes)
                    .font(.caption)
                    .foregroundStyle(Club360Theme.captionOnGlass)
                    .lineLimit(2)
            }
            if event.isCompleted {
                Text("Done")
                    .font(.caption2.weight(.bold))
                    .foregroundStyle(Club360Theme.teal)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(12)
        .club360Glass(cornerRadius: 18)
    }
}

private struct HubSessionActionsSheet: View {
    let event: ScheduleEventDTO
    let clientName: String?
    var isBusy: Bool = false
    let onMarkComplete: () -> Void
    let onMarkIncomplete: () -> Void
    let onAddNote: () -> Void
    let onDelete: () -> Void
    let onDismiss: () -> Void

    private var noteLabel: String {
        let n = (event.notes ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
        return n.isEmpty ? "Add note" : "Edit note"
    }

    var body: some View {
        NavigationStack {
            VStack(alignment: .leading, spacing: 16) {
                VStack(alignment: .leading, spacing: 6) {
                    Text("Session")
                        .font(.caption.weight(.bold))
                        .foregroundStyle(Club360Theme.captionOnGlass)
                    Text(event.title)
                        .font(.title3.weight(.semibold))
                        .foregroundStyle(Club360Theme.cardTitle)
                    Text(sessionMeta)
                        .font(.footnote)
                        .foregroundStyle(Club360Theme.captionOnGlass)
                    if let notes = event.notes?.trimmingCharacters(in: .whitespacesAndNewlines), !notes.isEmpty {
                        Text(notes)
                            .font(.footnote)
                            .foregroundStyle(Club360Theme.cardTitle)
                    }
                }
                .padding(.horizontal, 20)
                .padding(.top, 8)

                if let rid = event.rowId, !rid.isEmpty {
                    VStack(spacing: 10) {
                        if !event.isCompleted {
                            hubActionButton(
                                title: "Mark complete",
                                systemImage: "checkmark.circle.fill",
                                role: nil,
                                action: onMarkComplete
                            )
                        } else {
                            hubActionButton(
                                title: "Mark incomplete",
                                systemImage: "arrow.uturn.backward.circle",
                                role: nil,
                                action: onMarkIncomplete
                            )
                        }
                        hubActionButton(
                            title: noteLabel,
                            systemImage: "note.text",
                            role: nil,
                            action: onAddNote
                        )
                        hubActionButton(
                            title: "Delete session",
                            systemImage: "trash",
                            role: .destructive,
                            action: onDelete
                        )
                    }
                    .padding(.horizontal, 18)
                } else {
                    Text("This session cannot be updated in the app (missing id).")
                        .font(.footnote)
                        .foregroundStyle(.red)
                        .padding(.horizontal, 20)
                }

                Spacer(minLength: 0)
            }
            .navigationTitle("Session")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel", action: onDismiss)
                        .disabled(isBusy)
                }
            }
            .overlay {
                if isBusy {
                    ProgressView()
                        .tint(Club360Theme.burgundy)
                }
            }
        }
        .presentationDetents([.medium])
        .presentationDragIndicator(.visible)
    }

    private var sessionMeta: String {
        var parts: [String] = []
        if !event.time.isEmpty { parts.append(event.time) }
        if let clientName, !clientName.isEmpty { parts.append(clientName) }
        parts.append(Club360DateFormats.displayDay(fromPostgresDay: event.date))
        return parts.joined(separator: " · ")
    }

    private func hubActionButton(
        title: String,
        systemImage: String,
        role: ButtonRole?,
        action: @escaping () -> Void
    ) -> some View {
        Button(role: role, action: action) {
            HStack(spacing: 12) {
                Image(systemName: systemImage)
                    .font(.body.weight(.semibold))
                    .frame(width: 24)
                Text(title)
                    .font(.subheadline.weight(.semibold))
                Spacer()
            }
            .foregroundStyle(role == .destructive ? Color.red : Club360Theme.burgundy)
            .padding(.horizontal, 16)
            .padding(.vertical, 14)
            .frame(maxWidth: .infinity, alignment: .leading)
            .club360Glass(cornerRadius: 16)
        }
        .buttonStyle(.plain)
        .disabled(isBusy)
    }
}

private struct HubSessionNoteSheet: View {
    let event: ScheduleEventDTO
    var isBusy: Bool = false
    let onSave: (String) -> Void
    let onDismiss: () -> Void

    @State private var notes: String = ""

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    TextField("Coach note", text: $notes, axis: .vertical)
                        .lineLimit(3 ... 8)
                } footer: {
                    Text("Visible on this session in your calendar and hub.")
                }
            }
            .navigationTitle((event.notes ?? "").trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? "Add note" : "Edit note")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel", action: onDismiss)
                        .disabled(isBusy)
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(isBusy ? "Saving…" : "Save") {
                        onSave(notes)
                    }
                    .disabled(isBusy || event.rowId == nil)
                }
            }
            .onAppear {
                notes = event.notes ?? ""
            }
            .overlay {
                if isBusy {
                    ProgressView()
                }
            }
        }
        .presentationDetents([.medium])
    }
}

// MARK: - Stat tile (tap)

private struct CoachStatButton: View {
    let title: String
    let value: String
    let subtitle: String
    let tint: Color
    var disabled: Bool = false
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(alignment: .leading, spacing: 6) {
                Text(title)
                    .font(.caption.weight(.bold))
                    .foregroundStyle(Club360Theme.captionOnGlass)
                Text(value)
                    .font(.title.weight(.bold))
                    .foregroundStyle(Club360Theme.burgundy)
                Text(subtitle)
                    .font(.caption2)
                    .foregroundStyle(Club360Theme.captionOnGlass)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(14)
            .club360Glass(cornerRadius: 20)
        }
        .buttonStyle(.plain)
        .disabled(disabled)
        .opacity(disabled ? 0.45 : 1)
    }
}

// MARK: - Month calendar

private struct CoachHubCalendarStrip: View {
    @Binding var visibleMonth: Date
    @Binding var selectedDay: Date
    /// `log_date`-style keys (`yyyy-MM-dd`) with at least one session.
    var eventDates: Set<String> = []

    private let cal = Calendar.current

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Button {
                    visibleMonth = cal.date(byAdding: .month, value: -1, to: visibleMonth) ?? visibleMonth
                } label: {
                    Image(systemName: "chevron.left")
                }
                Spacer()
                Text(monthYearString(visibleMonth))
                    .font(.headline.weight(.semibold))
                    .foregroundStyle(Club360Theme.cardTitle)
                Spacer()
                Button {
                    visibleMonth = cal.date(byAdding: .month, value: 1, to: visibleMonth) ?? visibleMonth
                } label: {
                    Image(systemName: "chevron.right")
                }
            }
            .tint(Club360Theme.tealDark)

            LazyVGrid(columns: Array(repeating: GridItem(.flexible()), count: 7), spacing: 8) {
                ForEach(weekdayHeaders, id: \.self) { s in
                    Text(s)
                        .font(.caption2.weight(.bold))
                        .foregroundStyle(Club360Theme.captionOnGlass)
                        .frame(maxWidth: .infinity)
                }
                ForEach(Array(daysInMonthGrid().enumerated()), id: \.offset) { _, cell in
                    if let day = cell {
                        let isSel = cal.isDate(day, inSameDayAs: selectedDay)
                        Button {
                            selectedDay = cal.startOfDay(for: day)
                        } label: {
                            VStack(spacing: 2) {
                                Text("\(cal.component(.day, from: day))")
                                    .font(.subheadline.weight(isSel ? .bold : .regular))
                                    .foregroundStyle(isSel ? Club360Theme.burgundy : Club360Theme.cardTitle)
                                if eventDates.contains(Club360DateFormats.dayString(day)) {
                                    Circle()
                                        .fill(Club360Theme.tealDark)
                                        .frame(width: 5, height: 5)
                                } else {
                                    Spacer()
                                        .frame(height: 5)
                                }
                            }
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 5)
                            .padding(.horizontal, 2)
                            .background {
                                RoundedRectangle(cornerRadius: 11, style: .continuous)
                                    .fill(isSel ? Club360Theme.burgundy.opacity(0.14) : Color.clear)
                            }
                            .overlay {
                                if isSel {
                                    RoundedRectangle(cornerRadius: 11, style: .continuous)
                                        .strokeBorder(Club360Theme.burgundy, lineWidth: 2)
                                }
                            }
                            .shadow(
                                color: isSel ? Club360Theme.burgundy.opacity(0.22) : .clear,
                                radius: isSel ? 3 : 0,
                                y: 1
                            )
                        }
                        .buttonStyle(.plain)
                    } else {
                        Text(" ")
                            .frame(height: 36)
                    }
                }
            }
        }
        .padding(14)
        .club360Glass(cornerRadius: 22)
    }

    private var weekdayHeaders: [String] {
        let f = DateFormatter()
        return f.shortWeekdaySymbols
    }

    private func monthYearString(_ d: Date) -> String {
        let f = DateFormatter()
        f.locale = .current
        f.dateFormat = "MMM yyyy"
        return f.string(from: d)
    }

    private func daysInMonthGrid() -> [Date?] {
        var days: [Date?] = []
        let comps = cal.dateComponents([.year, .month], from: visibleMonth)
        guard let firstOfMonth = cal.date(from: comps),
              let range = cal.range(of: .day, in: .month, for: firstOfMonth)
        else { return [] }

        let firstWeekday = cal.component(.weekday, from: firstOfMonth)
        let leading = (firstWeekday - cal.firstWeekday + 7) % 7
        for _ in 0 ..< leading {
            days.append(nil)
        }
        for day in range {
            if let date = cal.date(byAdding: .day, value: day - 1, to: firstOfMonth) {
                days.append(date)
            }
        }
        while days.count % 7 != 0 {
            days.append(nil)
        }
        return days
    }
}
