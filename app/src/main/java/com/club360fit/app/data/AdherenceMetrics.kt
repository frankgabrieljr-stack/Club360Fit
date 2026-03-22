package com.club360fit.app.data

import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.math.min
import kotlin.math.roundToInt

data class AdherenceSnapshot(
    val weekStart: LocalDate,
    val workoutCompletionPercent: Int,
    val weeklyComplianceScore: Int,
    val currentStreakDays: Int,
    val longestStreakDays: Int,
    val expectedSessions: Int,
    val sessionsLoggedThisWeek: Int
)

object AdherenceMetricsCalculator {

    fun weekStartSunday(d: LocalDate): LocalDate = d.with(DayOfWeek.SUNDAY)

    fun weekEndSaturday(weekStart: LocalDate): LocalDate = weekStart.plusDays(6)

    fun workoutCompletionPercent(sessionsLogged: Int, expectedSessions: Int): Int {
        if (expectedSessions <= 0) return 0
        return min(100, (sessionsLogged * 100) / expectedSessions)
    }

    /** Days in [weekStart..weekEnd] with any habit signal (water, steps, or sleep). */
    fun habitDaysInWeek(logs: List<DailyHabitLogDto>, weekStart: LocalDate, weekEnd: LocalDate): Int {
        return logs.count { log ->
            !log.logDate.isBefore(weekStart) && !log.logDate.isAfter(weekEnd) &&
                (log.waterDone ||
                    (log.steps != null && log.steps > 0) ||
                    (log.sleepHours != null && log.sleepHours > 0))
        }
    }

    fun habitScore01(daysWithHabits: Int): Double = (daysWithHabits.coerceIn(0, 7)) / 7.0

    fun mealScore01(checkIns: List<ProgressCheckInDto>, weekStart: LocalDate, weekEnd: LocalDate): Double {
        val inWeek = checkIns.filter {
            !it.checkInDate.isBefore(weekStart) && !it.checkInDate.isAfter(weekEnd)
        }
        if (inWeek.isEmpty()) return 0.0
        val followed = inWeek.count { it.mealsFollowed }
        return followed.toDouble() / inWeek.size
    }

    fun checkInScore01(checkIns: List<ProgressCheckInDto>, weekStart: LocalDate, weekEnd: LocalDate): Double {
        val inWeek = checkIns.count {
            !it.checkInDate.isBefore(weekStart) && !it.checkInDate.isAfter(weekEnd)
        }
        return min(1.0, inWeek / 7.0)
    }

    /**
     * Weighted weekly score 0–100.
     * Weights: workouts 50%, habits 20%, meals 15%, check-ins 15%.
     */
    fun weeklyComplianceScore(
        workout01: Double,
        habit01: Double,
        meal01: Double,
        checkIn01: Double
    ): Int {
        val raw = workout01 * 0.50 + habit01 * 0.20 + meal01 * 0.15 + checkIn01 * 0.15
        return (raw * 100).roundToInt().coerceIn(0, 100)
    }

    fun activityDates(
        habitLogs: List<DailyHabitLogDto>,
        sessionLogs: List<WorkoutSessionLogDto>,
        checkIns: List<ProgressCheckInDto>,
        lookbackDays: Long = 120
    ): Set<LocalDate> {
        val from = LocalDate.now().minusDays(lookbackDays)
        val s = mutableSetOf<LocalDate>()
        habitLogs.forEach { h ->
            if (!h.logDate.isBefore(from) &&
                (h.waterDone || (h.steps != null && h.steps > 0) || (h.sleepHours != null && h.sleepHours > 0))
            ) {
                s.add(h.logDate)
            }
        }
        sessionLogs.forEach { if (!it.sessionDate.isBefore(from)) s.add(it.sessionDate) }
        checkIns.forEach { if (!it.checkInDate.isBefore(from)) s.add(it.checkInDate) }
        return s
    }

    /**
     * Current streak: consecutive days ending today or yesterday with at least one activity.
     */
    fun currentStreak(activityDates: Set<LocalDate>, today: LocalDate): Int {
        var d = today
        var streak = 0
        while (activityDates.contains(d)) {
            streak++
            d = d.minusDays(1)
        }
        if (streak == 0) {
            d = today.minusDays(1)
            while (activityDates.contains(d)) {
                streak++
                d = d.minusDays(1)
            }
        }
        return streak
    }

    fun longestStreak(activityDates: Set<LocalDate>): Int {
        if (activityDates.isEmpty()) return 0
        val sorted = activityDates.sorted()
        var best = 1
        var run = 1
        for (i in 1 until sorted.size) {
            if (sorted[i] == sorted[i - 1].plusDays(1)) {
                run++
                best = maxOf(best, run)
            } else {
                run = 1
            }
        }
        return best
    }
}

object AdherenceMetricsRepository {

    suspend fun loadSnapshot(clientId: String, anchor: LocalDate = LocalDate.now()): AdherenceSnapshot {
        val weekStart = AdherenceMetricsCalculator.weekStartSunday(anchor)
        val weekEnd = AdherenceMetricsCalculator.weekEndSaturday(weekStart)

        val plan = WorkoutPlanRepository.getCurrentPlan(clientId)
        val expected = plan?.expectedSessions?.takeIf { it > 0 } ?: 4
        val sessionsThisWeek = WorkoutSessionLogRepository.countForWeek(clientId, weekStart)

        val workout01 = min(
            1.0,
            sessionsThisWeek.toDouble() / expected.toDouble()
        )

        val habitLogs = DailyHabitRepository.listRange(clientId, weekStart, weekEnd)
        val habitDays = AdherenceMetricsCalculator.habitDaysInWeek(habitLogs, weekStart, weekEnd)
        val habit01 = AdherenceMetricsCalculator.habitScore01(habitDays)

        val checkIns = ProgressRepository.getOwnCheckIns(clientId)
        val meal01 = AdherenceMetricsCalculator.mealScore01(checkIns, weekStart, weekEnd)
        val checkIn01 = AdherenceMetricsCalculator.checkInScore01(checkIns, weekStart, weekEnd)

        val score = AdherenceMetricsCalculator.weeklyComplianceScore(workout01, habit01, meal01, checkIn01)
        val wPct = AdherenceMetricsCalculator.workoutCompletionPercent(sessionsThisWeek, expected)

        val allHabits = DailyHabitRepository.listRange(clientId, LocalDate.now().minusDays(180), LocalDate.now())
        val sessionForStreak = loadAllSessionLogsForStreak(clientId)
        val dates = AdherenceMetricsCalculator.activityDates(allHabits, sessionForStreak, checkIns)
        val cur = AdherenceMetricsCalculator.currentStreak(dates, LocalDate.now())
        val longest = AdherenceMetricsCalculator.longestStreak(dates)

        return AdherenceSnapshot(
            weekStart = weekStart,
            workoutCompletionPercent = wPct,
            weeklyComplianceScore = score,
            currentStreakDays = cur,
            longestStreakDays = longest,
            expectedSessions = expected,
            sessionsLoggedThisWeek = sessionsThisWeek
        )
    }

    /** Coach path: same metrics using getForClient check-ins. */
    suspend fun loadSnapshotForCoachView(clientId: String, anchor: LocalDate = LocalDate.now()): AdherenceSnapshot {
        val weekStart = AdherenceMetricsCalculator.weekStartSunday(anchor)
        val weekEnd = AdherenceMetricsCalculator.weekEndSaturday(weekStart)
        val plan = WorkoutPlanRepository.getCurrentPlan(clientId)
        val expected = plan?.expectedSessions?.takeIf { it > 0 } ?: 4
        val sessionsThisWeek = WorkoutSessionLogRepository.countForWeek(clientId, weekStart)
        val workout01 = min(1.0, sessionsThisWeek.toDouble() / expected.toDouble())
        val habitLogs = DailyHabitRepository.listRange(clientId, weekStart, weekEnd)
        val habitDays = AdherenceMetricsCalculator.habitDaysInWeek(habitLogs, weekStart, weekEnd)
        val habit01 = AdherenceMetricsCalculator.habitScore01(habitDays)
        val checkIns = ProgressRepository.getForClient(clientId)
        val meal01 = AdherenceMetricsCalculator.mealScore01(checkIns, weekStart, weekEnd)
        val checkIn01 = AdherenceMetricsCalculator.checkInScore01(checkIns, weekStart, weekEnd)
        val score = AdherenceMetricsCalculator.weeklyComplianceScore(workout01, habit01, meal01, checkIn01)
        val wPct = AdherenceMetricsCalculator.workoutCompletionPercent(sessionsThisWeek, expected)
        val allHabits = DailyHabitRepository.listRange(clientId, LocalDate.now().minusDays(180), LocalDate.now())
        val sessionForStreak = loadAllSessionLogsForStreak(clientId)
        val dates = AdherenceMetricsCalculator.activityDates(allHabits, sessionForStreak, checkIns)
        return AdherenceSnapshot(
            weekStart = weekStart,
            workoutCompletionPercent = wPct,
            weeklyComplianceScore = score,
            currentStreakDays = AdherenceMetricsCalculator.currentStreak(dates, LocalDate.now()),
            longestStreakDays = AdherenceMetricsCalculator.longestStreak(dates),
            expectedSessions = expected,
            sessionsLoggedThisWeek = sessionsThisWeek
        )
    }

    private suspend fun loadAllSessionLogsForStreak(clientId: String): List<WorkoutSessionLogDto> {
        val out = mutableListOf<WorkoutSessionLogDto>()
        var ws = AdherenceMetricsCalculator.weekStartSunday(LocalDate.now())
        repeat(26) {
            out.addAll(WorkoutSessionLogRepository.listForWeek(clientId, ws))
            ws = ws.minusWeeks(1)
        }
        return out.distinctBy { it.sessionDate }
    }
}
