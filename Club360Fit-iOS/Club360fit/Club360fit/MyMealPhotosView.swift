import Auth
import Observation
import PhotosUI
import SwiftUI
import UIKit

/// Meal photo log — **client**: upload & delete own rows; **coach/admin**: review & leave feedback (Android `MyMealPhotosScreen` vs `ClientMealPhotosScreen`).
struct MyMealPhotosView: View {
    @Environment(ClientHomeViewModel.self) private var home: ClientHomeViewModel
    @Environment(Club360AuthSession.self) private var auth: Club360AuthSession
    @State private var model = MyMealPhotosViewModel()
    @State private var showAdd = false

    private var isCoachReviewing: Bool {
        auth.session?.user.isAdminRole == true
    }

    var body: some View {
        Group {
            if let cid = home.clientId {
                mealPhotosContent(clientId: cid)
            } else {
                ContentUnavailableView("No profile", systemImage: "person.crop.circle.badge.xmark")
            }
        }
        .navigationTitle(isCoachReviewing ? "Client meal photos" : "Meal photos")
        .navigationBarTitleDisplayMode(.inline)
        .toolbarBackground(.ultraThinMaterial, for: .navigationBar)
        .toolbar {
            if !isCoachReviewing {
                ToolbarItem(placement: .primaryAction) {
                    Button {
                        showAdd = true
                    } label: {
                        Image(systemName: "plus.circle.fill")
                    }
                    .tint(Club360Theme.tealDark)
                }
            }
        }
        .sheet(isPresented: $showAdd) {
            if let cid = home.clientId {
                AddMealPhotoSheet(clientId: cid, onSaved: {
                    showAdd = false
                    Task { await model.load(clientId: cid) }
                })
            }
        }
    }

    @ViewBuilder
    private func mealPhotosContent(clientId: String) -> some View {
        ZStack {
            Club360ScreenBackground()

            ScrollView {
                LazyVStack(alignment: .leading, spacing: 18) {
                    if model.isLoading {
                        ProgressView("Loading photos…")
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

                    if !model.isLoading {
                        mealPhotosIntro
                    }

                    if model.dayGroups.isEmpty, !model.isLoading {
                        emptyState
                    }

                    ForEach(model.dayGroups) { day in
                        MealPhotoDaySection(
                            day: day,
                            clientId: clientId,
                            isCoachReviewing: isCoachReviewing,
                            onDataChanged: {
                                Task { await model.load(clientId: clientId) }
                            }
                        )
                    }
                }
                .padding()
            }
        }
        .task(id: clientId) {
            await model.load(clientId: clientId)
        }
        .refreshable {
            await model.load(clientId: clientId)
        }
    }

    private var mealPhotosIntro: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(isCoachReviewing ? "Daily meal log" : "Your daily meals")
                .font(.title3.weight(.bold))
                .foregroundStyle(Club360Theme.burgundy)
            Text(
                isCoachReviewing
                    ? "Photos are grouped by day — breakfast through snacks. Leave feedback on any meal that needs a tweak."
                    : "Log breakfast, lunch, dinner, and snacks each day so your coach can review portions and keep your plan on track."
            )
            .font(.subheadline)
            .foregroundStyle(Club360Theme.captionOnGlass)
            .fixedSize(horizontal: false, vertical: true)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .club360Glass(cornerRadius: 22)
    }

    private var emptyState: some View {
        VStack(alignment: .leading, spacing: 12) {
            Image(systemName: "camera.viewfinder")
                .font(.system(size: 36, weight: .semibold))
                .foregroundStyle(Club360Theme.burgundy.opacity(0.85))
            Text(
                isCoachReviewing
                    ? "No meal photos from this client yet."
                    : "No meal photos yet"
            )
            .font(.headline.weight(.semibold))
            .foregroundStyle(Club360Theme.cardTitle)
            Text(
                isCoachReviewing
                    ? "When they upload, you’ll see each day as a neat meal log."
                    : "Tap + to add breakfast, lunch, dinner, or a snack. Your coach reviews them day by day."
            )
            .font(.subheadline)
            .foregroundStyle(Club360Theme.captionOnGlass)
            .fixedSize(horizontal: false, vertical: true)
        }
        .padding(20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .club360Glass(cornerRadius: 24)
    }
}

// MARK: - Day section

struct MealPhotoDaySection: View {
    let day: MealPhotoDayGroup
    let clientId: String
    var clientNameHeader: String? = nil
    let isCoachReviewing: Bool
    var onDataChanged: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            dayHeader

            if !day.slotsPresent.isEmpty {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(day.slotsPresent) { slot in
                            Label(slot.label, systemImage: slot.systemImage)
                                .font(.caption.weight(.semibold))
                                .foregroundStyle(Club360Theme.cardTitle)
                                .padding(.horizontal, 10)
                                .padding(.vertical, 6)
                                .background(Color.white.opacity(0.55), in: Capsule())
                        }
                    }
                }
            }

            ForEach(day.logs, id: \.rowIdentity) { log in
                MealPhotoLogCard(
                    log: log,
                    clientId: clientId,
                    clientNameHeader: clientNameHeader,
                    showsDateHeadline: false,
                    isCoachReviewing: isCoachReviewing,
                    onDataChanged: onDataChanged
                )
            }
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: 28, style: .continuous)
                .fill(Color.white.opacity(0.28))
        )
        .overlay(
            RoundedRectangle(cornerRadius: 28, style: .continuous)
                .stroke(Color.white.opacity(0.45), lineWidth: 1)
        )
    }

    private var dayHeader: some View {
        HStack(alignment: .firstTextBaseline, spacing: 10) {
            VStack(alignment: .leading, spacing: 2) {
                Text(day.displayTitle)
                    .font(.title3.weight(.bold))
                    .foregroundStyle(Club360Theme.burgundy)
                Text(Club360DateFormats.displayDay(fromPostgresDay: day.logDate))
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(Club360Theme.captionOnGlass)
            }
            Spacer(minLength: 0)
            Text(day.mealCountLabel)
                .font(.caption.weight(.bold))
                .foregroundStyle(.white)
                .padding(.horizontal, 10)
                .padding(.vertical, 6)
                .background(Club360Theme.tealDark, in: Capsule())
            if isCoachReviewing {
                let pending = day.logs.filter(\.needsCoachFeedback).count
                if pending > 0 {
                    Text("\(pending) to review")
                        .font(.caption.weight(.bold))
                        .foregroundStyle(.white)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 6)
                        .background(Club360Theme.peachDeep, in: Capsule())
                }
            }
        }
    }
}

@Observable
@MainActor
private final class MyMealPhotosViewModel {
    var isLoading = true
    var errorMessage: String?
    var dayGroups: [MealPhotoDayGroup] = []

    func load(clientId: String) async {
        isLoading = true
        errorMessage = nil
        defer { isLoading = false }
        do {
            let logs = try await ClientDataService.listMealPhotoLogs(clientId: clientId)
            dayGroups = MealPhotoDayGroup.grouped(from: logs)
        } catch {
            errorMessage = error.localizedDescription
            dayGroups = []
        }
    }
}

// MARK: - Add sheet (client)

private struct AddMealPhotoSheet: View {
    let clientId: String
    var onSaved: () -> Void

    @Environment(\.dismiss) private var dismiss

    @State private var mealDate = Date()
    @State private var mealSlot: MealPhotoSlot = .lunch
    @State private var notes = ""
    @State private var selectedItem: PhotosPickerItem?
    @State private var pickedData: Data?
    @State private var pickedName = "photo.jpg"
    @State private var isUploading = false
    @State private var errorMessage: String?
    @State private var showCamera = false

    private var cameraAvailable: Bool {
        UIImagePickerController.isSourceTypeAvailable(.camera)
    }

    private var previewUIImage: UIImage? {
        pickedData.flatMap { UIImage(data: $0) }
    }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    Button {
                        showCamera = true
                    } label: {
                        Label("Take picture", systemImage: "camera.fill")
                    }
                    .disabled(!cameraAvailable)
                    .foregroundStyle(Club360Theme.burgundy)

                    PhotosPicker(selection: $selectedItem, matching: .images) {
                        Label("Choose from library", systemImage: "photo.on.rectangle")
                    }
                    .tint(Club360Theme.burgundy)
                    .onChange(of: selectedItem) { _, new in
                        Task { await loadPhoto(from: new) }
                    }

                    if !cameraAvailable {
                        Text("Camera isn’t available here (e.g. Simulator). Use “Choose from library” or run on a device.")
                            .font(.caption)
                            .foregroundStyle(Club360Theme.captionOnGlass)
                    }

                    if let img = previewUIImage {
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Preview")
                                .font(.caption.weight(.semibold))
                                .foregroundStyle(Club360Theme.cardTitle)
                            Image(uiImage: img)
                                .resizable()
                                .scaledToFit()
                                .frame(maxHeight: 260)
                                .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                                .overlay(
                                    RoundedRectangle(cornerRadius: 14, style: .continuous)
                                        .stroke(Color.black.opacity(0.12), lineWidth: 1)
                                )
                            Text("Review the photo, then tap Upload.")
                                .font(.caption)
                                .foregroundStyle(Club360Theme.captionOnGlass)
                        }
                        .listRowInsets(EdgeInsets(top: 8, leading: 0, bottom: 8, trailing: 0))
                    }
                } header: {
                    Text("Photo")
                        .foregroundStyle(Club360Theme.cardTitle)
                }

                Section {
                    DatePicker("Meal date", selection: $mealDate, displayedComponents: .date)

                    VStack(alignment: .leading, spacing: 10) {
                        Text("Which meal?")
                            .font(.subheadline.weight(.semibold))
                            .foregroundStyle(Club360Theme.cardTitle)
                        LazyVGrid(
                            columns: [GridItem(.flexible()), GridItem(.flexible())],
                            spacing: 8
                        ) {
                            ForEach(MealPhotoSlot.allCases.filter { $0 != .other }) { slot in
                                mealSlotButton(slot)
                            }
                        }
                        mealSlotButton(.other)
                    }

                    TextField("Notes (optional)", text: $notes, axis: .vertical)
                        .lineLimit(2...4)
                } header: {
                    Text("Details")
                        .foregroundStyle(Club360Theme.cardTitle)
                }
                if let errorMessage {
                    Section {
                        Text(errorMessage)
                            .font(.footnote)
                            .foregroundStyle(.red)
                    }
                }
            }
            .tint(Club360Theme.burgundy)
            .club360FormScreen()
            .preferredColorScheme(.light)
            .navigationTitle("Add meal photo")
            .navigationBarTitleDisplayMode(.inline)
            .toolbarBackground(.ultraThinMaterial, for: .navigationBar)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") {
                        dismiss()
                    }
                    .disabled(isUploading)
                }
                ToolbarItem(placement: .confirmationAction) {
                    if isUploading {
                        ProgressView()
                    } else {
                        Button("Upload") { Task { await upload() } }
                            .foregroundStyle(Club360Theme.burgundy)
                            .disabled(pickedData == nil)
                    }
                }
            }
            .fullScreenCover(isPresented: $showCamera) {
                CameraImagePicker(
                    onCapture: { data in
                        pickedData = data
                        pickedName = "camera.jpg"
                        selectedItem = nil
                    },
                    onDismiss: { showCamera = false }
                )
                .ignoresSafeArea()
            }
            .onAppear {
                mealSlot = Self.suggestedSlot(for: Date())
            }
        }
    }

    private func mealSlotButton(_ slot: MealPhotoSlot) -> some View {
        let selected = mealSlot == slot
        return Button {
            mealSlot = slot
        } label: {
            HStack(spacing: 8) {
                Image(systemName: slot.systemImage)
                Text(slot.label)
                    .font(.subheadline.weight(.semibold))
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 12)
            .foregroundStyle(selected ? Color.white : Club360Theme.cardTitle)
            .background(
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .fill(selected ? Club360Theme.burgundy : Color.white.opacity(0.7))
            )
            .overlay(
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .stroke(selected ? Color.clear : Color.black.opacity(0.08), lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
    }

    private static func suggestedSlot(for date: Date) -> MealPhotoSlot {
        let hour = Calendar.current.component(.hour, from: date)
        switch hour {
        case 0..<10: return .breakfast
        case 10..<14: return .lunch
        case 14..<17: return .snack
        case 17..<22: return .dinner
        default: return .snack
        }
    }

    private func loadPhoto(from item: PhotosPickerItem?) async {
        guard let item else {
            pickedData = nil
            return
        }
        if let data = try? await item.loadTransferable(type: Data.self) {
            pickedData = data
            pickedName = "photo.jpg"
        }
    }

    private func upload() async {
        guard let data = pickedData else { return }
        isUploading = true
        errorMessage = nil
        defer { isUploading = false }
        do {
            _ = try await ClientDataService.uploadMealPhotoAndInsert(
                clientId: clientId,
                imageData: data,
                logDate: mealDate,
                notes: notes,
                originalFilename: pickedName,
                mealSlot: mealSlot
            )
            dismiss()
            onSaved()
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}
