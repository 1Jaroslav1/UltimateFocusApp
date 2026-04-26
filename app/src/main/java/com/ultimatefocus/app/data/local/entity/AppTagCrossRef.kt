/**
 * Many-to-many cross-reference between an installed app (by packageName) and
 * a TagEntity (by id). Composite primary key prevents duplicate assignments;
 * ON DELETE CASCADE on tagId means deleting a tag automatically removes the
 * app↔tag rows so the AC "deleting a tag removes it from all apps" holds
 * without bespoke cleanup logic.
 * Covers: ULT-72, ULT-73
 */
package com.ultimatefocus.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "app_tags",
    primaryKeys = ["packageName", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("tagId")],
)
data class AppTagCrossRef(
    val packageName: String,
    val tagId: Long,
)
