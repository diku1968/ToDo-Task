package com.example.backup

import android.content.Context
import android.content.Intent
import com.example.data.local.AppDatabase
import com.example.data.local.entity.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class BackupManager(
    private val database: AppDatabase
) {

    suspend fun exportToJson(): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("appName", "TaskFlow")
        root.put("exportedAt", System.currentTimeMillis())

        // Categories
        val categories = database.categoryDao().getAllCategoriesSync()
        val catArray = JSONArray()
        for (cat in categories) {
            val obj = JSONObject().apply {
                put("id", cat.id)
                put("name", cat.name)
                put("colorHex", cat.colorHex)
                put("iconName", cat.iconName)
            }
            catArray.put(obj)
        }
        root.put("categories", catArray)

        // Tags
        val tags = database.tagDao().getAllTagsSync()
        val tagArray = JSONArray()
        for (tag in tags) {
            val obj = JSONObject().apply {
                put("id", tag.id)
                put("name", tag.name)
            }
            tagArray.put(obj)
        }
        root.put("tags", tagArray)

        // Tasks & Subtasks
        val allTasks = database.taskDao().getAllTasksSync()
        val taskArray = JSONArray()
        for (item in allTasks) {
            val t = item.task
            val taskObj = JSONObject().apply {
                put("id", t.id)
                put("title", t.title)
                put("description", t.description ?: "")
                put("isCompleted", t.isCompleted)
                put("createdAt", t.createdAt)
                put("updatedAt", t.updatedAt)
                put("dueDate", t.dueDate ?: -1L)
                put("dueTime", t.dueTime ?: "")
                put("priority", t.priority.name)
                put("categoryId", t.categoryId ?: -1L)
                put("notes", t.notes ?: "")
                put("reminderEnabled", t.reminderEnabled)
                put("reminderTime", t.reminderTime ?: -1L)
                put("repeatType", t.repeatType.name)
                put("repeatInterval", t.repeatInterval ?: 1)
                put("archived", t.archived)

                // Subtasks
                val subArray = JSONArray()
                for (sub in item.subtasks) {
                    val subObj = JSONObject().apply {
                        put("id", sub.id)
                        put("title", sub.title)
                        put("isCompleted", sub.isCompleted)
                    }
                    subArray.put(subObj)
                }
                put("subtasks", subArray)

                // Tag Names
                val taskTagArray = JSONArray()
                for (tag in item.tags) {
                    taskTagArray.put(tag.name)
                }
                put("tags", taskTagArray)
            }
            taskArray.put(taskObj)
        }
        root.put("tasks", taskArray)

        return root.toString(2)
    }

    suspend fun importFromJson(jsonString: String): Result<Int> {
        return try {
            val root = JSONObject(jsonString)
            if (!root.has("tasks")) {
                return Result.failure(IllegalArgumentException("Invalid TaskFlow backup file: missing tasks"))
            }

            var importedCount = 0

            // Import categories if available
            if (root.has("categories")) {
                val catArray = root.getJSONArray("categories")
                for (i in 0 until catArray.length()) {
                    val catObj = catArray.getJSONObject(i)
                    val cat = CategoryEntity(
                        id = catObj.optLong("id", 0),
                        name = catObj.getString("name"),
                        colorHex = catObj.optString("colorHex", "#4F46E5"),
                        iconName = catObj.optString("iconName", "Folder")
                    )
                    database.categoryDao().insertCategory(cat)
                }
            }

            // Import tasks
            val taskArray = root.getJSONArray("tasks")
            for (i in 0 until taskArray.length()) {
                val tObj = taskArray.getJSONObject(i)
                val dueDateVal = tObj.optLong("dueDate", -1L)
                val reminderTimeVal = tObj.optLong("reminderTime", -1L)
                val catIdVal = tObj.optLong("categoryId", -1L)

                val task = TaskEntity(
                    id = 0, // Generate new auto ID
                    title = tObj.getString("title"),
                    description = tObj.optString("description").takeIf { it.isNotBlank() },
                    isCompleted = tObj.optBoolean("isCompleted", false),
                    createdAt = tObj.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = System.currentTimeMillis(),
                    dueDate = if (dueDateVal > 0) dueDateVal else null,
                    dueTime = tObj.optString("dueTime").takeIf { it.isNotBlank() },
                    priority = Priority.fromString(tObj.optString("priority")),
                    categoryId = if (catIdVal > 0) catIdVal else null,
                    notes = tObj.optString("notes").takeIf { it.isNotBlank() },
                    reminderEnabled = tObj.optBoolean("reminderEnabled", false),
                    reminderTime = if (reminderTimeVal > 0) reminderTimeVal else null,
                    repeatType = RepeatType.fromString(tObj.optString("repeatType")),
                    repeatInterval = tObj.optInt("repeatInterval", 1),
                    archived = tObj.optBoolean("archived", false)
                )

                val newTaskId = database.taskDao().insertTask(task)

                // Subtasks
                if (tObj.has("subtasks")) {
                    val subArray = tObj.getJSONArray("subtasks")
                    for (j in 0 until subArray.length()) {
                        val sObj = subArray.getJSONObject(j)
                        val sub = SubtaskEntity(
                            id = 0,
                            taskId = newTaskId,
                            title = sObj.getString("title"),
                            isCompleted = sObj.optBoolean("isCompleted", false),
                            sortOrder = j
                        )
                        database.subtaskDao().insertSubtask(sub)
                    }
                }

                // Tags
                if (tObj.has("tags")) {
                    val tagArray = tObj.getJSONArray("tags")
                    for (k in 0 until tagArray.length()) {
                        val tagName = tagArray.getString(k)
                        val tagId = database.tagDao().insertTag(TagEntity(name = tagName))
                        database.tagDao().insertTaskTagCrossRef(TaskTagCrossRef(taskId = newTaskId, tagId = tagId))
                    }
                }

                importedCount++
            }

            Result.success(importedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun exportToCsv(): String {
        val tasks = database.taskDao().getAllTasksSync()
        val sb = StringBuilder()
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

        sb.append("ID,Title,Description,Status,Priority,Category,Due Date,Notes,Created Date\n")
        for (item in tasks) {
            val t = item.task
            val status = if (t.isCompleted) "Completed" else if (t.archived) "Archived" else "Active"
            val category = item.category?.name ?: "Uncategorized"
            val due = t.dueDate?.let { sdf.format(Date(it)) } ?: ""
            val created = sdf.format(Date(t.createdAt))

            fun escapeCsv(s: String): String {
                return "\"" + s.replace("\"", "\"\"").replace("\n", " ") + "\""
            }

            sb.append("${t.id},")
            sb.append(escapeCsv(t.title)).append(",")
            sb.append(escapeCsv(t.description ?: "")).append(",")
            sb.append(status).append(",")
            sb.append(t.priority.title).append(",")
            sb.append(escapeCsv(category)).append(",")
            sb.append(due).append(",")
            sb.append(escapeCsv(t.notes ?: "")).append(",")
            sb.append(created).append("\n")
        }
        return sb.toString()
    }

    companion object {
        fun shareTask(context: Context, task: TaskWithDetails) {
            val t = task.task
            val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            val sb = StringBuilder()
            sb.append("📋 Task: ").append(t.title).append("\n")
            if (!t.description.isNullOrBlank()) {
                sb.append("Details: ").append(t.description).append("\n")
            }
            if (t.dueDate != null) {
                sb.append("Due: ").append(sdf.format(Date(t.dueDate)))
                if (!t.dueTime.isNullOrBlank()) sb.append(" at ").append(t.dueTime)
                sb.append("\n")
            }
            sb.append("Priority: ").append(t.priority.title).append("\n")
            if (task.category != null) {
                sb.append("Category: ").append(task.category.name).append("\n")
            }
            if (task.subtasks.isNotEmpty()) {
                sb.append("\nChecklist:\n")
                task.subtasks.forEach {
                    val mark = if (it.isCompleted) "[x]" else "[ ]"
                    sb.append("$mark ${it.title}\n")
                }
            }
            sb.append("\nShared via TaskFlow")

            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Task: ${t.title}")
                putExtra(Intent.EXTRA_TEXT, sb.toString())
            }
            val shareIntent = Intent.createChooser(sendIntent, "Share Task")
            context.startActivity(shareIntent)
        }

        fun shareTextContent(context: Context, subject: String, content: String, mimeType: String = "text/plain") {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, content)
            }
            val shareIntent = Intent.createChooser(sendIntent, subject)
            context.startActivity(shareIntent)
        }
    }
}
