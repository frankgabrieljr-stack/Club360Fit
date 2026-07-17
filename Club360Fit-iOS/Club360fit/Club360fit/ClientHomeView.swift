import Auth
import SwiftUI

/// Client shell: Today home + gated tabs + More (Community, Profile, extras).
struct ClientHomeView: View {
    @Environment(Club360AuthSession.self) private var auth: Club360AuthSession
    @State private var homeModel = ClientHomeViewModel()
    @State private var tabRouter = ClientTabRouter()

    var body: some View {
        @Bindable var tabRouter = tabRouter
        TabView(selection: $tabRouter.selectedTab) {
            ClientHomeTab(tabRouter: tabRouter)
                .tabItem { Label("Today", systemImage: "sun.max.fill") }
                .tag(ClientTab.home)

            if homeModel.canViewWorkouts {
                ClientWorkoutsTab()
                    .tabItem { Label("Workouts", systemImage: "figure.strengthtraining.traditional") }
                    .tag(ClientTab.workouts)
            }

            if homeModel.canViewNutrition {
                ClientMealsTab(tabRouter: tabRouter)
                    .tabItem { Label("Meals", systemImage: "fork.knife") }
                    .tag(ClientTab.meals)
            }

            ClientProgressTab()
                .tabItem { Label("Progress", systemImage: "chart.line.uptrend.xyaxis") }
                .tag(ClientTab.progress)

            ClientMoreTab(tabRouter: tabRouter)
                .tabItem { Label("More", systemImage: "ellipsis.circle.fill") }
                .tag(ClientTab.profile)
        }
        .tint(Club360Theme.burgundy)
        .preferredColorScheme(.light)
        .environment(homeModel)
        .environment(\.clientTabRouter, tabRouter)
        .task(id: auth.session?.user.id) {
            guard let session = auth.session else { return }
            await homeModel.load(session: session)
        }
        .onChange(of: homeModel.canViewWorkouts) { _, enabled in
            if !enabled, tabRouter.selectedTab == .workouts {
                tabRouter.selectedTab = .home
            }
        }
        .onChange(of: homeModel.canViewNutrition) { _, enabled in
            if !enabled, tabRouter.selectedTab == .meals {
                tabRouter.selectedTab = .home
            }
        }
    }
}

// MARK: - Today (Home)

private struct ClientHomeTab: View {
    @Bindable var tabRouter: ClientTabRouter
    @Environment(Club360AuthSession.self) private var auth: Club360AuthSession
    @Environment(ClientHomeViewModel.self) private var home: ClientHomeViewModel

    var body: some View {
        NavigationStack(path: $tabRouter.homePath) {
            ZStack {
                Club360ScreenBackground()

                ScrollView {
                    VStack(alignment: .leading, spacing: 18) {
                        if home.isLoading {
                            ProgressView("Loading your day…")
                                .tint(Club360Theme.tealDark)
                                .frame(maxWidth: .infinity)
                                .padding()
                        }

                        if let err = home.errorMessage {
                            Text(err)
                                .font(.footnote)
                                .foregroundStyle(.red)
                                .padding()
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .club360Glass(cornerRadius: 22)
                        }

                        HStack(alignment: .center, spacing: 14) {
                            Image("LogoBurgundy")
                                .resizable()
                                .scaledToFit()
                                .frame(width: 56, height: 56)
                                .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                                .shadow(color: Color.black.opacity(0.08), radius: 8, y: 4)
                            VStack(alignment: .leading, spacing: 4) {
                                Text("Today")
                                    .font(.largeTitle.weight(.bold))
                                    .foregroundStyle(Club360Theme.burgundy)
                                Text(home.welcomeName)
                                    .font(.title3.weight(.semibold))
                                    .foregroundStyle(Club360Theme.burgundy)
                            }
                        }
                        .padding(.top, 4)

                        if !home.isLoading, home.clientId != nil, !home.hasAssignedCoach {
                            waitingForCoachCard
                        }

                        if home.canViewEvents {
                            Button {
                                tabRouter.homePath.append(HomeDeepLink.schedule)
                            } label: {
                                nextSessionCard
                            }
                            .buttonStyle(.plain)
                        }

                        Text("Quick actions")
                            .font(.subheadline.weight(.bold))
                            .foregroundStyle(Club360Theme.cardTitle)
                            .textCase(.uppercase)
                            .tracking(0.8)

                        VStack(spacing: 10) {
                            if home.canViewWorkouts {
                                todayActionRow(
                                    title: "Log today’s workout",
                                    subtitle: workoutSubtitle,
                                    systemImage: "figure.strengthtraining.traditional"
                                ) {
                                    tabRouter.selectedTab = .workouts
                                }
                            }
                            if home.canViewNutrition {
                                todayActionRow(
                                    title: "Log a meal photo",
                                    subtitle: mealSubtitle,
                                    systemImage: "camera.fill"
                                ) {
                                    tabRouter.selectedTab = .meals
                                    tabRouter.mealsPath = NavigationPath()
                                    tabRouter.mealsPath.append(MealsDeepLink.mealPhotos)
                                }
                            }
                            todayActionRow(
                                title: "Daily habits",
                                subtitle: "Water · steps · sleep",
                                systemImage: "checkmark.circle.fill"
                            ) {
                                tabRouter.homePath.append(HomeDeepLink.habits)
                            }
                            if home.canViewPayments {
                                todayActionRow(
                                    title: "Payments",
                                    subtitle: "Due date · Venmo / Zelle",
                                    systemImage: "banknote.fill"
                                ) {
                                    tabRouter.homePath.append(HomeDeepLink.payments)
                                }
                            }
                        }
                    }
                    .padding(.horizontal, 18)
                    .padding(.bottom, 24)
                }
            }
            .navigationTitle("Today")
            .navigationBarTitleDisplayMode(.inline)
            .toolbarBackground(.ultraThinMaterial, for: .navigationBar)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        tabRouter.homePath.append(HomeDeepLink.notifications)
                    } label: {
                        ZStack(alignment: .topTrailing) {
                            Image(systemName: "bell.fill")
                                .font(.body.weight(.semibold))
                                .foregroundStyle(Club360Theme.tealDark)
                            if home.unreadNotifications > 0 {
                                Text("\(min(home.unreadNotifications, 99))")
                                    .font(.caption2.weight(.bold))
                                    .foregroundStyle(.white)
                                    .padding(4)
                                    .background(Circle().fill(Club360Theme.peachDeep))
                                    .offset(x: 10, y: -10)
                            }
                        }
                    }
                    .accessibilityLabel("Updates")
                }
            }
            .onAppear {
                Task { await home.reloadNotificationsCount() }
            }
            .refreshable {
                if let s = auth.session {
                    await home.load(session: s)
                }
            }
            .navigationDestination(for: HomeDeepLink.self) { link in
                switch link {
                case .notifications:
                    MyNotificationsView()
                        .environment(home)
                case .schedule:
                    MyScheduleView()
                        .environment(home)
                case .payments:
                    MyPaymentsView()
                        .environment(home)
                case .habits:
                    MyDailyHabitsView()
                        .environment(home)
                case .gallery:
                    TransformationGalleryView()
                }
            }
        }
    }

    private var waitingForCoachCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("Waiting for your coach")
                .font(.headline.weight(.semibold))
                .foregroundStyle(Club360Theme.cardTitle)
            Text("A coach still needs to claim your account. Meanwhile you can set your profile photo and browse Community from More. Workouts, meals, and posting unlock after you’re assigned.")
                .font(.footnote)
                .foregroundStyle(Club360Theme.captionOnGlass)
                .fixedSize(horizontal: false, vertical: true)
            Button {
                tabRouter.selectedTab = .profile
            } label: {
                Text("Open More")
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(Club360Theme.burgundy)
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .club360Glass(cornerRadius: 22)
    }

    private func todayActionRow(
        title: String,
        subtitle: String,
        systemImage: String,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            HStack(spacing: 14) {
                Image(systemName: systemImage)
                    .font(.title3.weight(.semibold))
                    .foregroundStyle(Club360Theme.burgundy)
                    .frame(width: 36)
                VStack(alignment: .leading, spacing: 4) {
                    Text(title)
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(Club360Theme.cardTitle)
                    Text(subtitle)
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
            .club360Glass(cornerRadius: 22)
        }
        .buttonStyle(.plain)
    }

    private var nextSessionCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text("Next session")
                    .font(.caption.weight(.bold))
                    .foregroundStyle(Club360Theme.captionOnTintedCard)
                    .textCase(.uppercase)
                Spacer()
                Text("View schedule")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(Club360Theme.burgundy)
                Image(systemName: "chevron.right")
                    .font(.caption2.weight(.bold))
                    .foregroundStyle(Club360Theme.burgundy)
            }
            Text(home.nextSessionLine ?? "No upcoming sessions scheduled.")
                .font(.body.weight(.semibold))
                .foregroundStyle(Club360Theme.cardTitle)
                .fixedSize(horizontal: false, vertical: true)
            Text("\(home.upcomingSessionCount) upcoming")
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
        if let t = home.currentWorkoutTitle {
            return "Current: \(t)"
        }
        return home.isLoading ? "…" : "Open workouts"
    }

    private var mealSubtitle: String {
        if let t = home.currentMealTitle {
            return "Current: \(t)"
        }
        return home.isLoading ? "…" : "Open meal photos"
    }
}

// MARK: - More tab

private struct ClientMoreTab: View {
    @Bindable var tabRouter: ClientTabRouter
    @Environment(ClientHomeViewModel.self) private var home: ClientHomeViewModel

    var body: some View {
        NavigationStack {
            ZStack {
                Club360ScreenBackground()
                ScrollView {
                    VStack(alignment: .leading, spacing: 14) {
                        Text("More")
                            .font(.largeTitle.weight(.bold))
                            .foregroundStyle(Club360Theme.burgundy)
                            .padding(.top, 4)

                        Text("Community, account, and extras")
                            .font(.subheadline)
                            .foregroundStyle(Club360Theme.captionOnGlass)

                        NavigationLink {
                            CommunityView()
                                .environment(home)
                        } label: {
                            moreRow(title: "Community", subtitle: "Members, tips & encouragement", systemImage: "person.2.fill")
                        }
                        .buttonStyle(.plain)

                        NavigationLink {
                            UserProfileView()
                        } label: {
                            moreRow(title: "Profile", subtitle: "Photo & account", systemImage: "person.crop.circle.fill")
                        }
                        .buttonStyle(.plain)

                        if home.canViewEvents {
                            NavigationLink {
                                MyScheduleView()
                                    .environment(home)
                            } label: {
                                moreRow(title: "Schedule", subtitle: "Upcoming sessions", systemImage: "calendar.badge.clock")
                            }
                            .buttonStyle(.plain)
                        }

                        if home.canViewPayments {
                            NavigationLink {
                                MyPaymentsView()
                                    .environment(home)
                            } label: {
                                moreRow(title: "Payments", subtitle: "Venmo or Zelle", systemImage: "banknote.fill")
                            }
                            .buttonStyle(.plain)
                        }

                        NavigationLink {
                            MyDailyHabitsView()
                                .environment(home)
                        } label: {
                            moreRow(title: "Habits", subtitle: "Water · steps · sleep", systemImage: "checkmark.circle.fill")
                        }
                        .buttonStyle(.plain)

                        NavigationLink {
                            TransformationGalleryView()
                        } label: {
                            moreRow(title: "Gallery", subtitle: "Transformation photos", systemImage: "photo.on.rectangle.angled")
                        }
                        .buttonStyle(.plain)
                    }
                    .padding(.horizontal, 18)
                    .padding(.bottom, 28)
                }
            }
            .navigationTitle("More")
            .navigationBarTitleDisplayMode(.inline)
            .toolbarBackground(.ultraThinMaterial, for: .navigationBar)
        }
    }

    private func moreRow(title: String, subtitle: String, systemImage: String) -> some View {
        HStack(spacing: 14) {
            Image(systemName: systemImage)
                .font(.title3.weight(.semibold))
                .foregroundStyle(Club360Theme.burgundy)
                .frame(width: 36)
            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(Club360Theme.cardTitle)
                Text(subtitle)
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
        .club360Glass(cornerRadius: 22)
    }
}

// MARK: - Other tabs

private struct ClientWorkoutsTab: View {
    @Environment(ClientHomeViewModel.self) private var home: ClientHomeViewModel

    var body: some View {
        NavigationStack {
            MyWorkoutsView()
                .environment(home)
        }
    }
}

private struct ClientMealsTab: View {
    @Bindable var tabRouter: ClientTabRouter
    @Environment(ClientHomeViewModel.self) private var home: ClientHomeViewModel

    var body: some View {
        NavigationStack(path: $tabRouter.mealsPath) {
            MyMealsView()
                .environment(home)
                .navigationDestination(for: MealsDeepLink.self) { link in
                    switch link {
                    case .mealPhotos:
                        MyMealPhotosView()
                            .environment(home)
                    }
                }
        }
    }
}

private struct ClientProgressTab: View {
    @Environment(ClientHomeViewModel.self) private var home: ClientHomeViewModel

    var body: some View {
        NavigationStack {
            MyProgressView()
                .environment(home)
        }
    }
}
