package com.ourlist.app.data.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit

object DDayCalculator {

    /**
     * Calculates days passed since startDateStr (format: YYYY-MM-DD).
     * In Korean couple culture, the start date is counted as Day 1.
     * e.g., if today is 2026-11-05 and start is 2026-11-05 -> 1.
     */
    fun calculateDays(startDateStr: String, targetDate: LocalDate = LocalDate.now()): Long? {
        if (startDateStr.isBlank()) return null
        return try {
            val start = LocalDate.parse(startDateStr.trim())
            ChronoUnit.DAYS.between(start, targetDate) + 1
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Formats human-readable D-Day string.
     * e.g. "함께한 지 365일째 💕" or "오늘부터 1일 💕"
     */
    fun formatDDayText(startDateStr: String): String? {
        val days = calculateDays(startDateStr) ?: return null
        return when {
            days > 1 -> "함께한 지 ${days}일째 💕"
            days == 1L -> "오늘부터 1일 💕"
            days == 0L -> "D-Day 💕"
            else -> "D${days}"
        }
    }
}
