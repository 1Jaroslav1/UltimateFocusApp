/**
 * Hilt EntryPoint exposing the TagRepository + BlockedTagRepository singletons
 * to Composables that need them without going through hiltViewModel — used by
 * the TagPickerDialog which can be hosted from multiple screens (drawer,
 * app-detail, settings) with different scoping. Pulling singletons this way
 * keeps each call site lightweight without re-injecting through every
 * ViewModel. BlockedTagRepository surfaces the active-tag-block set so the
 * picker can mark blocked tags' rows as disabled (ULT-75 follow-up).
 * Covers: ULT-73, ULT-75
 */
package com.ultimatefocus.app.ui.tags

import com.ultimatefocus.app.domain.repository.BlockedTagRepository
import com.ultimatefocus.app.domain.repository.TagRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface TagsEntryPoint {
    fun tagRepository(): TagRepository
    fun blockedTagRepository(): BlockedTagRepository
}
