package com.example.utils

import com.example.data.local.entity.RepeatType
import java.text.SimpleDateFormat
import java.util.*

object DateTimeUtils {
    private val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    private val shortDateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

    fun formatDate(timestamp: Long?): String {
        if (timestamp == null) return ""
        return dateFormat.format(Date(timestamp))
    }

    fun formatShortDate(timestamp: Long?): String {
        if (timestamp == null) return ""
        return shortDateFormat.format(Date(timestamp))
    }

    fun formatTime(hour: Int, minute: Int): String {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }
        return timeFormat.format(calendar.time)
    }

    fun getStartOfDay(timestamp: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun getEndOfDay(timestamp: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return cal.timeInMillis
    }

    fun isToday(timestamp: Long?): Boolean {
        if (timestamp == null) return false
        val todayStart = getStartOfDay()
        val todayEnd = getEndOfDay()
        return timestamp in todayStart..todayEnd
    }

    fun isTomorrow(timestamp: Long?): Boolean {
        if (timestamp == null) return false
        val tomorrowStart = getStartOfDay() + 24 * 60 * 60 * 1000L
        val tomorrowEnd = getEndOfDay() + 24 * 60 * 60 * 1000L
        return timestamp in tomorrowStart..tomorrowEnd
    }

    fun isOverdue(dueDate: Long?, isCompleted: Boolean): Boolean {
        if (dueDate == null || isCompleted) return false
        return dueDate < getStartOfDay()
    }

    fun formatRelativeDate(dueDate: Long?): String {
        if (dueDate == null) return ""
        return when {
            isToday(dueDate) -> "Today"
            isTomorrow(dueDate) -> "Tomorrow"
            dueDate < getStartOfDay() -> "Overdue (${formatShortDate(dueDate)})"
            else -> formatShortDate(dueDate)
        }
    }
}

object RecurrenceCalculator {

    fun calculateNextDueDate(
        currentDueDate: Long,
        repeatType: RepeatType,
        repeatInterval: Int = 1
    ): Long? {
        if (repeatType == RepeatType.NONE) return null

        val calendar = Calendar.getInstance().apply {
            timeInMillis = currentDueDate
        }

        when (repeatType) {
            RepeatType.NONE -> return null
            RepeatType.DAILY -> {
                calendar.add(Calendar.DAY_OF_YEAR, repeatInterval.coerceAtLeast(1))
            }
            RepeatType.WEEKDAYS -> {
                do {
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                } while (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY ||
                    calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)
            }
            RepeatType.WEEKLY -> {
                calendar.add(Calendar.WEEK_OF_YEAR, repeatInterval.coerceAtLeast(1))
            }
            RepeatType.BIWEEKLY -> {
                calendar.add(Calendar.WEEK_OF_YEAR, 2 * repeatInterval.coerceAtLeast(1))
            }
            RepeatType.MONTHLY -> {
                calendar.add(Calendar.MONTH, repeatInterval.coerceAtLeast(1))
            }
            RepeatType.YEARLY -> {
                calendar.add(Calendar.YEAR, repeatInterval.coerceAtLeast(1))
            }
            RepeatType.CUSTOM -> {
                calendar.add(Calendar.DAY_OF_YEAR, repeatInterval.coerceAtLeast(1))
            }
        }

        return calendar.timeInMillis
    }
}
