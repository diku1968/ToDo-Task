package com.example.data.repository

import com.example.data.local.dao.TagDao
import com.example.data.local.entity.TagEntity
import kotlinx.coroutines.flow.Flow

class TagRepository(
    private val tagDao: TagDao
) {
    val allTags: Flow<List<TagEntity>> = tagDao.getAllTags()

    suspend fun getOrCreateTag(name: String): Long {
        val trimmed = name.trim().removePrefix("#")
        val existing = tagDao.getTagByName(trimmed)
        if (existing != null) {
            return existing.id
        }
        return tagDao.insertTag(TagEntity(name = trimmed))
    }

    suspend fun deleteTag(id: Long) {
        tagDao.deleteTag(id)
    }
}
