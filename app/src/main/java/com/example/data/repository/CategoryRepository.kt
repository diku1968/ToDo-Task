package com.example.data.repository

import com.example.data.local.dao.CategoryDao
import com.example.data.local.dao.TaskDao
import com.example.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

class CategoryRepository(
    private val categoryDao: CategoryDao,
    private val taskDao: TaskDao
) {
    val allCategories: Flow<List<CategoryEntity>> = categoryDao.getAllCategories()

    suspend fun getCategoryById(id: Long): CategoryEntity? = categoryDao.getCategoryById(id)

    suspend fun createCategory(name: String, colorHex: String, iconName: String): Long {
        return categoryDao.insertCategory(
            CategoryEntity(
                name = name.trim(),
                colorHex = colorHex,
                iconName = iconName
            )
        )
    }

    suspend fun updateCategory(category: CategoryEntity) {
        categoryDao.updateCategory(category)
    }

    suspend fun deleteCategory(categoryId: Long) {
        // Reassign tasks to Uncategorized (null categoryId)
        taskDao.reassignCategoryToNull(categoryId)
        categoryDao.deleteCategoryById(categoryId)
    }
}
