package com.club360fit.app.ui.screens.client

/**
 * Short, user-facing copy for upload/delete failures (avoids dumping raw HTTP bodies).
 */
internal fun userMessageForMealPhotoError(e: Throwable): String {
    val msg = e.message ?: return "Something went wrong. Try again."
    return when {
        msg.contains("row-level security", ignoreCase = true) ||
            msg.contains("violates row-level security", ignoreCase = true) ->
            "Couldn’t save your photo (account permissions). If this keeps happening, contact your coach."
        msg.contains("JWT", ignoreCase = true) || msg.contains("expired", ignoreCase = true) ->
            "Session issue. Try signing out and back in."
        msg.length > 220 -> msg.take(217) + "…"
        else -> msg
    }
}
