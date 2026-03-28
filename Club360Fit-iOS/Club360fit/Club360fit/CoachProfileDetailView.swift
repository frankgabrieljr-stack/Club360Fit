import SwiftUI

/// Read-only coach profile from `public.profiles` (same source as the coach directory).
struct CoachProfileDetailView: View {
    let row: ClientDataService.CoachDirectoryProfileRow
    var currentUserId: String?

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                HStack(alignment: .center, spacing: 16) {
                    avatar
                        .frame(width: 100, height: 100)
                        .clipShape(Circle())
                        .overlay(Circle().stroke(Color.black.opacity(0.08), lineWidth: 1))
                    VStack(alignment: .leading, spacing: 6) {
                        Text(displayName(for: row))
                            .font(.title2.weight(.bold))
                            .foregroundStyle(Club360Theme.burgundy)
                        if let email = row.email?.trimmingCharacters(in: .whitespacesAndNewlines), !email.isEmpty {
                            Text(email)
                                .font(.subheadline)
                                .foregroundStyle(Club360Theme.cardTitle)
                        }
                        if isSelf {
                            Text("(you)")
                                .font(.caption.weight(.semibold))
                                .foregroundStyle(Club360Theme.tealDark)
                        }
                    }
                    Spacer(minLength: 0)
                }

                VStack(alignment: .leading, spacing: 10) {
                    Text("Supabase Auth user ID")
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(Club360Theme.captionOnGlass)
                    Text(row.id.trimmingCharacters(in: .whitespacesAndNewlines).lowercased())
                        .font(.caption.monospaced())
                        .foregroundStyle(Club360Theme.cardTitle)
                        .textSelection(.enabled)
                }
                .padding(16)
                .frame(maxWidth: .infinity, alignment: .leading)
                .club360Glass(cornerRadius: 22)

                Text("Directory profiles are read-only. Edit your own account from Profile.")
                    .font(.footnote)
                    .foregroundStyle(Club360Theme.captionOnGlass)
            }
            .padding()
        }
        .background(Club360ScreenBackground())
        .navigationTitle("Coach profile")
        .navigationBarTitleDisplayMode(.inline)
        .toolbarBackground(.ultraThinMaterial, for: .navigationBar)
    }

    private var isSelf: Bool {
        let selfLower = currentUserId?.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        let idLower = row.id.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        return selfLower != nil && selfLower == idLower
    }

    @ViewBuilder
    private var avatar: some View {
        let trimmedAvatar = row.avatar_url?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        if !trimmedAvatar.isEmpty, let direct = URL(string: trimmedAvatar) {
            AsyncImage(url: direct) { phase in
                switch phase {
                case .empty:
                    ZStack {
                        Club360Theme.creamWarm
                        ProgressView().tint(Club360Theme.burgundy)
                    }
                case .success(let image):
                    image.resizable().scaledToFill()
                default:
                    fallbackAvatar
                }
            }
        } else if let url = ClientDataService.publicAvatarURLForAuthUserId(row.id) {
            AsyncImage(url: url) { phase in
                switch phase {
                case .empty:
                    ZStack {
                        Club360Theme.creamWarm
                        ProgressView().tint(Club360Theme.burgundy)
                    }
                case .success(let image):
                    image.resizable().scaledToFill()
                default:
                    fallbackAvatar
                }
            }
        } else {
            fallbackAvatar
        }
    }

    private var fallbackAvatar: some View {
        Image("LogoBurgundy")
            .resizable()
            .scaledToFit()
            .padding(12)
    }

    private func displayName(for row: ClientDataService.CoachDirectoryProfileRow) -> String {
        if let n = row.full_name?.trimmingCharacters(in: .whitespacesAndNewlines), !n.isEmpty {
            return n
        }
        if let email = row.email?.trimmingCharacters(in: .whitespacesAndNewlines),
           !email.isEmpty,
           let local = email.split(separator: "@").first
        {
            return String(local)
        }
        return "Coach \(row.id.prefix(8))…"
    }
}
