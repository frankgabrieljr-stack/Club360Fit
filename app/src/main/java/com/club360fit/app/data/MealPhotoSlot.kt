package com.club360fit.app.data

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Matches iOS `MealPhotoSlot` / DB `meal_photo_logs.meal_slot`. */
enum class MealPhotoSlot(val raw: String, val label: String, val sortRank: Int) {
    BREAKFAST("breakfast", "Breakfast", 0),
    LUNCH("lunch", "Lunch", 1),
    DINNER("dinner", "Dinner", 2),
    SNACK("snack", "Snack", 3),
    OTHER("other", "Other", 4);

    companion object {
        fun parse(raw: String?): MealPhotoSlot {
            val key = raw?.trim()?.lowercase(Locale.US) ?: return OTHER
            return entries.firstOrNull { it.raw == key } ?: OTHER
        }

        fun suggestedForNow(): MealPhotoSlot {
            val hour = java.time.LocalTime.now().hour
            return when (hour) {
                in 0 until 10 -> BREAKFAST
                in 10 until 14 -> LUNCH
                in 14 until 17 -> SNACK
                in 17 until 22 -> DINNER
                else -> SNACK
            }
        }
    }
}

data class MealPhotoDayGroup(
    val logDate: LocalDate,
    val logs: List<MealPhotoLogDto>
) {
    val displayTitle: String
        get() = when {
            logDate == LocalDate.now() -> "Today"
            logDate == LocalDate.now().minusDays(1) -> "Yesterday"
            else -> logDate.format(DateTimeFormatter.ofPattern("EEE MMM d", Locale.getDefault()))
        }

    val mealCountLabel: String
        get() = if (logs.size == 1) "1 photo" else "${logs.size} photos"

    val slotsPresent: List<MealPhotoSlot>
        get() = logs.map { it.resolvedSlot }.distinct().sortedBy { it.sortRank }

    val pendingReviewCount: Int
        get() = logs.count { it.needsCoachFeedback }

    companion object {
        fun grouped(from: List<MealPhotoLogDto>): List<MealPhotoDayGroup> {
            return from
                .groupBy { it.logDate }
                .entries
                .sortedByDescending { it.key }
                .map { (day, dayLogs) ->
                    MealPhotoDayGroup(
                        logDate = day,
                        logs = dayLogs.sortedWith(
                            compareBy<MealPhotoLogDto> { it.resolvedSlot.sortRank }
                                .thenBy { it.createdAt.orEmpty() }
                        )
                    )
                }
        }
    }
}

val MealPhotoLogDto.resolvedSlot: MealPhotoSlot
    get() = MealPhotoSlot.parse(mealSlot)

val MealPhotoLogDto.needsCoachFeedback: Boolean
    get() = coachFeedback.isNullOrBlank()
