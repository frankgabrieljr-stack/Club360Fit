package com.club360fit.app.ui.navigation

import com.club360fit.app.data.ClientNotificationDto

object NotificationDeepLink {
    const val EXTRA_DEEP_LINK = "club360_deep_link"
    const val PAYMENTS = "payments"
    const val COMMUNITY = "community"
    const val MEAL_PHOTOS = "meal_photos"
    const val SCHEDULE = "schedule"
    const val WORKOUTS = "workouts"
    const val MEALS = "meals"
    const val PROGRESS = "progress"
    const val HABITS = "habits"
}

/** Route a member notification to the matching deep-link key (null = stay in inbox). */
fun ClientNotificationDto.memberDeepLink(): String? {
    val k = kind.lowercase()
    val ref = refType?.lowercase().orEmpty()
    return when {
        k in setOf("payment", "payment_reminder", "payment_confirmation") || ref == "payment" ->
            NotificationDeepLink.PAYMENTS
        k in setOf("community", "community_post", "community_reply", "peer_tip") || ref == "community_post" ->
            NotificationDeepLink.COMMUNITY
        k in setOf("meal_feedback", "meal_photo", "meal_photo_upload") || ref == "meal_photo" ->
            NotificationDeepLink.MEAL_PHOTOS
        k in setOf("schedule", "session") || ref == "schedule" ->
            NotificationDeepLink.SCHEDULE
        k in setOf("workout_plan", "workout", "workout_session_reply") ->
            NotificationDeepLink.WORKOUTS
        k in setOf("meal_plan", "nutrition") ->
            NotificationDeepLink.MEALS
        k in setOf("progress", "progress_checkin", "check_in") ->
            NotificationDeepLink.PROGRESS
        k in setOf("habit", "daily_habit") ->
            NotificationDeepLink.HABITS
        else -> null
    }
}

fun ClientNotificationDto.shouldOpenPayments(): Boolean =
    memberDeepLink() == NotificationDeepLink.PAYMENTS

fun paymentKindFromPushData(data: Map<String, String>): Boolean {
    val kind = data["kind"].orEmpty().lowercase()
    val ref = data["ref_type"].orEmpty().lowercase()
    val deep = data["deep_link"].orEmpty().lowercase()
    return deep == NotificationDeepLink.PAYMENTS ||
        kind == "payment" ||
        kind == "payment_reminder" ||
        kind == "payment_confirmation" ||
        ref == "payment"
}

fun deepLinkFromPushData(data: Map<String, String>): String? {
    val deep = data["deep_link"].orEmpty().lowercase()
    if (deep.isNotBlank()) return deep
    val dto = ClientNotificationDto(
        clientId = data["client_id"].orEmpty(),
        kind = data["kind"].orEmpty().ifBlank { "info" },
        title = data["title"].orEmpty(),
        body = data["body"].orEmpty(),
        refType = data["ref_type"]
    )
    return dto.memberDeepLink()
}
