package com.club360fit.app.ui.navigation

import com.club360fit.app.data.ClientNotificationDto

object NotificationDeepLink {
    const val EXTRA_DEEP_LINK = "club360_deep_link"
    const val PAYMENTS = "payments"
}

fun ClientNotificationDto.shouldOpenPayments(): Boolean {
    val k = kind.lowercase()
    return k == "payment" ||
        k == "payment_reminder" ||
        k == "payment_confirmation" ||
        refType?.lowercase() == "payment"
}

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
