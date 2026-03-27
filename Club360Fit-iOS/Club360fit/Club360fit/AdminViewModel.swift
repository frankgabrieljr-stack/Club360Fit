import Foundation
import Observation

/// Coach dashboard: loads all clients the signed-in admin can see (same as Android `AdminHomeViewModel`).
@Observable
@MainActor
final class AdminViewModel {
    var isLoading = true
    var errorMessage: String?
    var clients: [ClientDTO] = []
    /// `clients.user_id` → `public.profiles.role` (`admin` / `client`).
    var profileRolesByUserId: [String: String] = [:]

    func load() async {
        isLoading = true
        errorMessage = nil
        defer { isLoading = false }
        do {
            clients = try await ClientDataService.fetchClientsForCoach()
            var roles: [String: String] = [:]
            for c in clients {
                let uid = c.userId.trimmingCharacters(in: .whitespacesAndNewlines)
                guard !uid.isEmpty else { continue }
                if let r = try? await ClientDataService.fetchProfileRoleForUser(userId: uid) {
                    roles[uid] = r
                }
            }
            profileRolesByUserId = roles
        } catch {
            errorMessage = error.localizedDescription
            clients = []
            profileRolesByUserId = [:]
        }
    }

    /// Stable list label for a row.
    static func listTitle(for client: ClientDTO) -> String {
        if let name = client.fullName?.trimmingCharacters(in: .whitespacesAndNewlines), !name.isEmpty {
            return name
        }
        if let id = client.id {
            return "Client \(id.prefix(8))…"
        }
        return "Client"
    }
}
