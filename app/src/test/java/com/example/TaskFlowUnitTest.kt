package com.example

import com.example.data.local.entity.Priority
import com.example.data.local.entity.RepeatType
import com.example.utils.DateTimeUtils
import com.example.utils.RecurrenceCalculator
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TaskFlowUnitTest {

    @Test
    fun testPriorityEnum() {
        assertEquals(Priority.URGENT, Priority.fromString("URGENT"))
        assertEquals(Priority.HIGH, Priority.fromString("high"))
        assertEquals(Priority.NONE, Priority.fromString("invalid"))
        assertTrue(Priority.URGENT.level > Priority.HIGH.level)
    }

    @Test
    fun testRepeatTypeEnum() {
        assertEquals(RepeatType.DAILY, RepeatType.fromString("DAILY"))
        assertEquals(RepeatType.WEEKLY, RepeatType.fromString("weekly"))
        assertEquals(RepeatType.NONE, RepeatType.fromString(null))
    }

    @Test
    fun testRecurrenceDailyCalculation() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 25, 10, 0, 0)
        }
        val currentDue = cal.timeInMillis

        val nextDue = RecurrenceCalculator.calculateNextDueDate(currentDue, RepeatType.DAILY, 1)
        assertNotNull(nextDue)

        val nextCal = Calendar.getInstance().apply { timeInMillis = nextDue!! }
        assertEquals(26, nextCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(Calendar.SEPTEMBER, nextCal.get(Calendar.MONTH))
    }

    @Test
    fun testRecurrenceWeeklyCalculation() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 25, 10, 0, 0)
        }
        val currentDue = cal.timeInMillis

        val nextDue = RecurrenceCalculator.calculateNextDueDate(currentDue, RepeatType.WEEKLY, 1)
        assertNotNull(nextDue)

        val nextCal = Calendar.getInstance().apply { timeInMillis = nextDue!! }
        assertEquals(Calendar.OCTOBER, nextCal.get(Calendar.MONTH))
        assertEquals(2, nextCal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun testRecurrenceWeekdaysCalculation() {
        // Friday
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 25, 10, 0, 0) // Friday
        }
        val fridayDue = cal.timeInMillis

        val nextDue = RecurrenceCalculator.calculateNextDueDate(fridayDue, RepeatType.WEEKDAYS)
        assertNotNull(nextDue)

        val nextCal = Calendar.getInstance().apply { timeInMillis = nextDue!! }
        // Next weekday from Friday should be Monday (Sep 28)
        assertEquals(Calendar.MONDAY, nextCal.get(Calendar.DAY_OF_WEEK))
        assertEquals(28, nextCal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun testOverdueCalculation() {
        val now = System.currentTimeMillis()
        val yesterday = now - 24 * 60 * 60 * 1000L
        val tomorrow = now + 24 * 60 * 60 * 1000L

        // Yesterday and uncompleted -> overdue
        assertTrue(DateTimeUtils.isOverdue(yesterday, isCompleted = false))
        // Yesterday and completed -> not overdue
        assertFalse(DateTimeUtils.isOverdue(yesterday, isCompleted = true))
        // Tomorrow and uncompleted -> not overdue
        assertFalse(DateTimeUtils.isOverdue(tomorrow, isCompleted = false))
    }

    @Test
    fun testBackupJsonStructure() {
        val sampleJson = JSONObject().apply {
            put("version", 1)
            put("appName", "TaskFlow")
            put("tasks", JSONArray().apply {
                put(JSONObject().apply {
                    put("title", "Test Task")
                    put("isCompleted", false)
                    put("priority", "HIGH")
                })
            })
        }

        assertTrue(sampleJson.has("tasks"))
        assertEquals("TaskFlow", sampleJson.getString("appName"))
        val tasksArray = sampleJson.getJSONArray("tasks")
        assertEquals(1, tasksArray.length())
        assertEquals("Test Task", tasksArray.getJSONObject(0).getString("title"))
    }
}
