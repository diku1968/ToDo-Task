package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

enum class Priority(val title: String, val level: Int) {
    NONE("No Priority", 0),
    LOW("Low", 1),
    MEDIUM("Medium", 2),
    HIGH("High", 3),
    URGENT("Urgent", 4);

    companion object {
        fun fromString(value: String?): Priority {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: NONE
        }
    }
}

enum class RepeatType(val title: String) {
    NONE("Does not repeat"),
    DAILY("Every day"),
    WEEKDAYS("Every weekday (Mon-Fri)"),
    WEEKLY("Every week"),
    BIWEEKLY("Every 2 weeks"),
    MONTHLY("Every month"),
    YEARLY("Every year"),
    CUSTOM("Custom interval");

    companion object {
        fun fromString(value: String?): RepeatType {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: NONE
        }
    }
}

class Converters {
    @TypeConverter
    fun fromPriority(priority: Priority): String = priority.name

    @TypeConverter
    fun toPriority(value: String): Priority = Priority.fromString(value)

    @TypeConverter
    fun fromRepeatType(repeatType: RepeatType): String = repeatType.name

    @TypeConverter
    fun toRepeatType(value: String): RepeatType = RepeatType.fromString(value)
}

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String? = null,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val dueDate: Long? = null, // timestamp in ms (midnight UTC or local date)
    val startDate: Long? = null,
    val dueTime: String? = null, // e.g. "14:30"
    val priority: Priority = Priority.NONE,
    val categoryId: Long? = null,
    val color: String? = null,
    val notes: String? = null,
    val estimatedMinutes: Int? = null,
    val actualMinutes: Int? = null,
    val reminderEnabled: Boolean = false,
    val reminderTime: Long? = null,
    val repeatType: RepeatType = RepeatType.NONE,
    val repeatInterval: Int? = 1,
    val repeatEndDate: Long? = null,
    val parentTaskId: Long? = null,
    val sortOrder: Int = 0,
    val completedAt: Long? = null,
    val archived: Boolean = false,
    val deleted: Boolean = false
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val colorHex: String,
    val iconName: String = "Folder"
)

@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String
)

@Entity(
    tableName = "task_tag_cross_ref",
    primaryKeys = ["taskId", "tagId"],
    indices = [Index(value = ["tagId"])]
)
data class TaskTagCrossRef(
    val taskId: Long,
    val tagId: Long
)

@Entity(tableName = "subtasks")
data class SubtaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val taskId: Long,
    val title: String,
    val isCompleted: Boolean = false,
    val sortOrder: Int = 0
)
