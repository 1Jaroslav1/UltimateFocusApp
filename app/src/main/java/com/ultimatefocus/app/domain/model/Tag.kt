/**
 * Domain model representing a single user-created tag. Wraps an immutable
 * id + name + TagColor. Cap of 10 tags per user (MAX_TAGS) and 5 tags per
 * app (MAX_TAGS_PER_APP) is enforced by TagRepository.
 * Covers: ULT-72, ULT-73
 */
package com.ultimatefocus.app.domain.model

const val MAX_TAGS: Int = 10
const val MAX_TAGS_PER_APP: Int = 5

data class Tag(
    val id: Long,
    val name: String,
    val color: TagColor,
)
