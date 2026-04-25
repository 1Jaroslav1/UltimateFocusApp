/**
 * Hilt EntryPoint exposing the TagRepository singleton to Composables that
 * need it without going through hiltViewModel — used by the TagPickerDialog
 * which can be hosted from multiple screens (drawer, app-detail, settings)
 * with different scoping. Pulling a singleton this way keeps each call site
 * lightweight without re-injecting through every ViewModel.
 * Covers: ULT-73
 */
package com.ultimatefocus.app.ui.tags

import com.ultimatefocus.app.domain.repository.TagRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface TagsEntryPoint {
    fun tagRepository(): TagRepository
}
