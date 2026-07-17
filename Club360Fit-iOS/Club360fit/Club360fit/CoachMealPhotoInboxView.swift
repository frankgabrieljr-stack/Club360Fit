import Observation
import SwiftUI

/// Coach-wide feed of meal photos across all visible clients, grouped by member then by day.
struct CoachMealPhotoInboxView: View {
    @State private var model = CoachMealPhotoInboxViewModel()

    var body: some View {
        ZStack {
            Club360ScreenBackground()

            ScrollView {
                LazyVStack(alignment: .leading, spacing: 20) {
                    inboxHeader

                    if model.isLoading {
                        ProgressView("Loading meal inbox…")
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

                    if !model.isLoading, model.errorMessage == nil, model.groups.isEmpty {
                        ContentUnavailableView(
                            "No meal photos yet",
                            systemImage: "camera",
                            description: Text("When clients log meals, they appear here day by day.")
                        )
                        .padding(.top, 24)
                    }

                    ForEach(model.groups) { group in
                        VStack(alignment: .leading, spacing: 14) {
                            HStack(alignment: .firstTextBaseline) {
                                Image(systemName: "person.circle.fill")
                                    .font(.title3)
                                    .foregroundStyle(Club360Theme.burgundy)
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(group.displayName)
                                        .font(.title3.weight(.bold))
                                        .foregroundStyle(Club360Theme.burgundy)
                                    Text(group.summaryLine)
                                        .font(.caption.weight(.semibold))
                                        .foregroundStyle(Club360Theme.captionOnGlass)
                                }
                                Spacer(minLength: 0)
                                NavigationLink {
                                    AdminClientHubView(clientId: group.clientId, displayTitle: group.displayName)
                                } label: {
                                    Text("Client hub")
                                        .font(.subheadline.weight(.semibold))
                                }
                                .buttonStyle(.plain)
                                .tint(Club360Theme.tealDark)
                            }

                            ForEach(group.dayGroups) { day in
                                MealPhotoDaySection(
                                    day: day,
                                    clientId: group.clientId,
                                    isCoachReviewing: true,
                                    onDataChanged: {
                                        Task { await model.load() }
                                    }
                                )
                            }
                        }
                    }
                }
                .padding()
            }
        }
        .navigationTitle("Meal inbox")
        .navigationBarTitleDisplayMode(.inline)
        .toolbarBackground(.ultraThinMaterial, for: .navigationBar)
        .task {
            await model.load()
        }
        .refreshable {
            await model.load()
        }
    }

    private var inboxHeader: some View {
        HStack(alignment: .center, spacing: 14) {
            Image("LogoBurgundy")
                .resizable()
                .scaledToFit()
                .frame(width: 48, height: 48)
                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
            VStack(alignment: .leading, spacing: 4) {
                Text("Your clients")
                    .font(.title2.weight(.bold))
                    .foregroundStyle(Club360Theme.burgundy)
                Text("Grouped by member, then by day — breakfast through snacks. Save feedback on each card.")
                    .font(.caption)
                    .foregroundStyle(Club360Theme.cardSubtitle)
            }
        }
        .padding(.bottom, 4)
    }
}

private struct InboxClientGroup: Identifiable {
    let clientId: String
    let displayName: String
    let dayGroups: [MealPhotoDayGroup]

    var id: String { clientId }

    var summaryLine: String {
        let photos = dayGroups.reduce(0) { $0 + $1.logs.count }
        let days = dayGroups.count
        let pending = dayGroups.flatMap(\.logs).filter(\.needsCoachFeedback).count
        var parts = ["\(photos) photo\(photos == 1 ? "" : "s")", "\(days) day\(days == 1 ? "" : "s")"]
        if pending > 0 {
            parts.append("\(pending) to review")
        }
        return parts.joined(separator: " · ")
    }
}

@Observable
@MainActor
private final class CoachMealPhotoInboxViewModel {
    var isLoading = true
    var errorMessage: String?
    var groups: [InboxClientGroup] = []

    func displayName(forClientId id: String, titleByClientId: [String: String]) -> String {
        if let t = titleByClientId[id], !t.isEmpty { return t }
        return "Client \(id.prefix(8))…"
    }

    func load() async {
        isLoading = true
        errorMessage = nil
        defer { isLoading = false }
        do {
            async let logsTask = ClientDataService.listMealPhotoLogsForCoachInbox()
            async let clientsTask = ClientDataService.fetchClientsForCoach()
            let (logs, clients) = try await (logsTask, clientsTask)
            let assignedClients = clients.filter {
                !($0.coachId?.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ?? true)
            }
            let assignedIds = Set(assignedClients.compactMap(\.id))
            let scopedLogs = logs.filter { assignedIds.contains($0.clientId) }
            let titleByClientId = Dictionary(uniqueKeysWithValues: assignedClients.compactMap { c -> (String, String)? in
                guard let id = c.id, !id.isEmpty else { return nil }
                return (id, AdminViewModel.listTitle(for: c))
            })

            var order: [String] = []
            var seen = Set<String>()
            for log in scopedLogs {
                if !seen.contains(log.clientId) {
                    seen.insert(log.clientId)
                    order.append(log.clientId)
                }
            }
            let byClient = Dictionary(grouping: scopedLogs) { $0.clientId }
            groups = order.map { cid in
                let name = displayName(forClientId: cid, titleByClientId: titleByClientId)
                let clientLogs = byClient[cid] ?? []
                return InboxClientGroup(
                    clientId: cid,
                    displayName: name,
                    dayGroups: MealPhotoDayGroup.grouped(from: clientLogs)
                )
            }
        } catch {
            errorMessage = error.localizedDescription
            groups = []
        }
    }
}
