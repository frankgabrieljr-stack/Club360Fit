import Observation
import SwiftUI

/// Meal plans + entry to meal photos — mirrors Android `MyMealsScreen`.
struct MyMealsView: View {
    var isCoachReviewing = false

    @Environment(ClientHomeViewModel.self) private var home: ClientHomeViewModel
    @State private var model = MyMealsViewModel()
    @State private var showIntroHelp = false
    @State private var showMealPhotosHelp = false
    @State private var expandedMealPlanHelp: Set<String> = []

    var body: some View {
        Group {
            if !home.canViewNutrition {
                ContentUnavailableView(
                    "Meals unavailable",
                    systemImage: "lock.fill",
                    description: Text("Your coach has disabled nutrition access for your account.")
                )
            } else if home.clientId == nil {
                ContentUnavailableView(
                    "No profile",
                    systemImage: "person.crop.circle.badge.xmark",
                    description: Text("We couldn’t load your client profile.")
                )
            } else {
                mealsContent
            }
        }
        .navigationTitle("Meals")
        .navigationBarTitleDisplayMode(.large)
        .toolbarBackground(.ultraThinMaterial, for: .navigationBar)
        .task(id: home.clientId) {
            guard let cid = home.clientId else { return }
            await model.load(clientId: cid)
        }
        .refreshable {
            guard let cid = home.clientId else { return }
            await model.load(clientId: cid)
        }
    }

    @ViewBuilder
    private var mealsContent: some View {
        ZStack {
            Club360ScreenBackground()

            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    if model.isLoading {
                        ProgressView("Loading meal plans…")
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

                    Club360InfoSectionHeader(
                        title: "How this screen works",
                        helpTitle: nil,
                        helpBody: isCoachReviewing
                            ? "This is a read-only coach view of assigned meal plans. Open meal photos to review what the client uploaded and leave feedback."
                            : "Your coach posts weekly meal plans below. Use Meal photos to send pictures of what you eat so they can review and give feedback.",
                        isExpanded: $showIntroHelp
                    )
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(16)
                    .club360Glass(cornerRadius: 22)

                    VStack(alignment: .leading, spacing: 12) {
                        Club360InfoSectionHeader(
                            title: "Meal photos",
                            helpTitle: "Photo log",
                            helpBody: isCoachReviewing
                                ? "Open the photo log to review this client’s meals day by day (breakfast, lunch, dinner, snacks). Coaches leave feedback but cannot upload for clients."
                                : "Log breakfast, lunch, dinner, and snacks with your camera or library. "
                                    + "Photos are grouped by day so you and your coach can review the whole day at a glance.",
                            isExpanded: $showMealPhotosHelp
                        )
                        NavigationLink {
                            MyMealPhotosView()
                                .environment(home)
                        } label: {
                            HStack(alignment: .center, spacing: 14) {
                                ZStack {
                                    RoundedRectangle(cornerRadius: 18, style: .continuous)
                                        .fill(Club360Theme.teal.opacity(0.4))
                                        .overlay(
                                            RoundedRectangle(cornerRadius: 18, style: .continuous)
                                                .stroke(Color.black.opacity(0.08), lineWidth: 1)
                                        )
                                        .frame(width: 52, height: 52)
                                    Image(systemName: "camera.fill")
                                        .font(.title2)
                                        .foregroundStyle(Club360Theme.tealDark)
                                }
                                Text(isCoachReviewing ? "Review meal photos" : "Open meal photos")
                                    .font(.headline.weight(.semibold))
                                    .foregroundStyle(Club360Theme.cardTitle)
                                Spacer(minLength: 0)
                                Image(systemName: "chevron.right")
                                    .font(.caption.weight(.semibold))
                                    .foregroundStyle(Club360Theme.captionOnGlass)
                            }
                            .frame(maxWidth: .infinity, alignment: .leading)
                        }
                        .buttonStyle(.plain)
                    }
                    .padding(16)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .club360Glass(cornerRadius: 28)

                    if model.plans.isEmpty, !model.isLoading {
                        Text("No meal plans assigned yet.")
                            .font(.body)
                            .foregroundStyle(Club360Theme.captionOnGlass)
                    } else {
                        ForEach(model.plans, id: \.rowIdentity) { plan in
                            let planExpanded = Binding<Bool>(
                                get: { expandedMealPlanHelp.contains(plan.rowIdentity) },
                                set: { newValue in
                                    if newValue {
                                        expandedMealPlanHelp.insert(plan.rowIdentity)
                                    } else {
                                        expandedMealPlanHelp.remove(plan.rowIdentity)
                                    }
                                }
                            )
                            VStack(alignment: .leading, spacing: 10) {
                                HStack(alignment: .top, spacing: 10) {
                                    Text("Week of \(Club360DateFormats.displayDay(fromPostgresDay: plan.weekStart)) – \(plan.title)")
                                        .font(.headline.weight(.semibold))
                                        .foregroundStyle(Club360Theme.cardTitle)
                                        .multilineTextAlignment(.leading)
                                    Spacer(minLength: 8)
                                    Club360InfoTrailingButton(isExpanded: planExpanded)
                                }
                                if expandedMealPlanHelp.contains(plan.rowIdentity) {
                                    Club360InfoHelpBlock(
                                        helpTitle: "Meal plan",
                                        helpBody:
                                            "This is what your coach assigned for nutrition that week. "
                                            + "Follow it alongside your meal photos and coach notes."
                                    )
                                }
                                Text(plan.planText)
                                    .font(.body)
                                    .foregroundStyle(Club360Theme.cardTitle)
                            }
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(16)
                            .club360Glass(cornerRadius: 28)
                        }
                    }
                }
                .padding()
            }
        }
    }
}

@Observable
@MainActor
private final class MyMealsViewModel {
    var isLoading = true
    var errorMessage: String?
    var plans: [MealPlanDTO] = []

    func load(clientId: String) async {
        isLoading = true
        errorMessage = nil
        defer { isLoading = false }
        do {
            plans = try await ClientDataService.fetchMealPlans(clientId: clientId)
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}
