/**
 * Room entity representing a user-pinned favorite app stored in the local database.
 * Covers: ULT-6
 */
package com.minimalist.launcher.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey
    val packageName: String,
    val componentName: String,
    val displayOrder: Int = 0,
)
