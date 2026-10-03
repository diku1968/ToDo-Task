package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.CategoryDao
import com.example.data.local.dao.SubtaskDao
import com.example.data.local.dao.TagDao
import com.example.data.local.dao.TaskDao
import com.example.data.local.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        TaskEntity::class,
        CategoryEntity::class,
        TagEntity::class,
        TaskTagCrossRef::class,
        SubtaskEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao
    abstract fun categoryDao(): CategoryDao
    abstract fun tagDao(): TagDao
    abstract fun subtaskDao(): SubtaskDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "taskflow_database"
                )
                    .fallbackToDestructiveMigration(false)
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateDefaultCategories(database.categoryDao())
                    }
                }
            }

            private suspend fun populateDefaultCategories(categoryDao: CategoryDao) {
                val defaultCategories = listOf(
                    CategoryEntity(id = 1, name = "Personal", colorHex = "#4F46E5", iconName = "Person"),
                    CategoryEntity(id = 2, name = "Work", colorHex = "#0284C7", iconName = "Work"),
                    CategoryEntity(id = 3, name = "Shopping", colorHex = "#10B981", iconName = "ShoppingCart"),
                    CategoryEntity(id = 4, name = "Health", colorHex = "#EF4444", iconName = "FitnessCenter"),
                    CategoryEntity(id = 5, name = "Finance", colorHex = "#F59E0B", iconName = "AttachMoney"),
                    CategoryEntity(id = 6, name = "Home", colorHex = "#8B5CF6", iconName = "Home"),
                    CategoryEntity(id = 7, name = "Study", colorHex = "#EC4899", iconName = "School"),
                    CategoryEntity(id = 8, name = "Other", colorHex = "#64748B", iconName = "Folder")
                )
                categoryDao.insertCategories(defaultCategories)
            }
        }
    }
}
