package com.example.data.local.entity

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class TaskWithDetails(
    @Embedded
    val task: TaskEntity,

    @Relation(
        parentColumn = "categoryId",
        entityColumn = "id"
    )
    val category: CategoryEntity? = null,

    @Relation(
        parentColumn = "id",
        entityColumn = "taskId"
    )
    val subtasks: List<SubtaskEntity> = emptyList(),

    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = TaskTagCrossRef::class,
            parentColumn = "taskId",
            entityColumn = "tagId"
        )
    )
    val tags: List<TagEntity> = emptyList()
)
