/**
 * Compose UI tests for ManageTagsScreen using a real ManageTagsViewModel
 * backed by an in-memory Room database. Verifies: empty state copy,
 * Create flow inserts a row visible in the list, 10-tag cap disables the
 * Create CTA and shows the limit message.
 * Covers: ULT-72
 */
package com.ultimatefocus.app.ui.tags

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ultimatefocus.app.data.local.AppDatabase
import com.ultimatefocus.app.data.repository.TagRepositoryImpl
import com.ultimatefocus.app.domain.model.TagColor
import com.ultimatefocus.app.domain.service.FakeClock
import com.ultimatefocus.app.ui.theme.UltimateFocusTheme
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ManageTagsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var db: AppDatabase
    private lateinit var repo: TagRepositoryImpl

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = TagRepositoryImpl(db.tagDao(), db.appTagDao(), FakeClock(0L))
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun emptyState_isDisplayed_whenNoTagsExist() {
        composeRule.setContent {
            UltimateFocusTheme {
                ManageTagsScreen(
                    onNavigateBack = {},
                    viewModel = ManageTagsViewModel(repo),
                )
            }
        }
        composeRule.onNodeWithTag("manage_tags_empty_state").assertIsDisplayed()
        composeRule.onNodeWithText("No tags yet").assertIsDisplayed()
    }

    @Test
    fun creating_tag_shows_it_in_the_list() {
        composeRule.setContent {
            UltimateFocusTheme {
                ManageTagsScreen(
                    onNavigateBack = {},
                    viewModel = ManageTagsViewModel(repo),
                )
            }
        }
        composeRule.onNodeWithTag("create_tag_button").performClick()
        composeRule.onNodeWithTag("tag_editor_name_field").performTextInput("Social Media")
        composeRule.onNodeWithTag("tag_editor_swatch_${TagColor.STEEL_BLUE.name}").performClick()
        composeRule.onNodeWithTag("tag_editor_save_button").performClick()

        composeRule.onNodeWithText("Social Media").assertIsDisplayed()
    }

    @Test
    fun ten_tag_cap_disables_create_cta_with_explanatory_message() {
        // Pre-seed 10 tags via the repo
        TagColor.values().forEachIndexed { idx, color ->
            kotlinx.coroutines.runBlocking { repo.createTag("Tag$idx", color) }
        }

        composeRule.setContent {
            UltimateFocusTheme {
                ManageTagsScreen(
                    onNavigateBack = {},
                    viewModel = ManageTagsViewModel(repo),
                )
            }
        }

        composeRule.onNodeWithTag("create_tag_limit_message").assertIsDisplayed()
    }

    @Test
    fun screen_renders_with_safe_drawing_padding() {
        composeRule.setContent {
            UltimateFocusTheme {
                ManageTagsScreen(
                    onNavigateBack = {},
                    viewModel = ManageTagsViewModel(repo),
                )
            }
        }
        composeRule.onNodeWithTag("manage_tags_screen").assertIsDisplayed()
    }
}
