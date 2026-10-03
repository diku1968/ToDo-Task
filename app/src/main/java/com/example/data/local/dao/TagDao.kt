package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.TagEntity
import com.example.data.local.entity.TaskTagCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {

    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun getAllTags(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags ORDER BY name ASC")
    suspend fun getAllTagsSync(): List<TagEntity>

    @Query("SELECT * FROM tags WHERE name = :name LIMIT 1")
    suspend fun getTagByName(name: String): TagEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: TagEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaskTagCrossRef(crossRef: TaskTagCrossRef)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaskTagCrossRefs(crossRefs: List<TaskTagCrossRef>)

    @Query("DELETE FROM task_tag_cross_ref WHERE taskId = :taskId")
    suspend fun deleteTagsForTask(taskId: Long)

    @Query("DELETE FROM task_tag_cross_ref WHERE taskId = :taskId AND tagId = :tagId")
    suspend fun deleteTaskTagCrossRef(taskId: Long, tagId: Long)

    @Query("DELETE FROM tags WHERE id = :id")
    suspend fun deleteTag(id: Long)
}
