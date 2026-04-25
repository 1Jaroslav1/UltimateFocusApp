/**
 * Compose tests for BlockDurationPickerDialog.
 * Verifies all 8 durations appear, confirmation step interpolates the app
 * name, and Confirm/Cancel wire to the expected callbacks — a deliberate
 * two-tap barrier so the user cannot accidentally commit a binding block.
 * Covers: ULT-20
 */
package com.ultimatefocus.app.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ultimatefocus.app.domain.model.BlockDuration
import com.ultimatefocus.app.ui.theme.UltimateFocusTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BlockDurationPickerDialogTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun picker_showsAllEightDurations() {
        composeRule.setContent {
            UltimateFocusTheme {
                BlockDurationPickerDialog(
                    appLabel = "Instagram",
                    onConfirm = {},
                    onDismiss = {},
                )
            }
        }
        BlockDuration.entries.forEach { duration ->
            composeRule.onNodeWithTag("block_duration_${duration.label}").assertIsDisplayed()
        }
    }

    @Test
    fun tappingDuration_revealsConfirmationWithAppName() {
        composeRule.setContent {
            UltimateFocusTheme {
                BlockDurationPickerDialog(
                    appLabel = "Instagram",
                    onConfirm = {},
                    onDismiss = {},
                )
            }
        }
        composeRule.onNodeWithTag("block_duration_1h").performClick()

        composeRule.onNodeWithTag("block_duration_confirm_message").assertIsDisplayed()
        composeRule.onNodeWithText(
            "You won't be able to open Instagram for 1h. Blocks cannot be undone.",
        ).assertIsDisplayed()
    }

    @Test
    fun confirm_firesOnConfirmWithSelectedDurationAndDismisses() {
        var confirmed: BlockDuration? = null
        var dismissed = false
        composeRule.setContent {
            UltimateFocusTheme {
                BlockDurationPickerDialog(
                    appLabel = "Calendar",
                    onConfirm = { confirmed = it },
                    onDismiss = { dismissed = true },
                )
            }
        }
        composeRule.onNodeWithTag("block_duration_7d").performClick()
        composeRule.onNodeWithTag("block_duration_confirm").performClick()

        assertEquals(BlockDuration.DAY_7, confirmed)
        assertEquals(true, dismissed)
    }

    @Test
    fun cancel_returnsToPickerWithoutFiringOnConfirm() {
        var confirmed: BlockDuration? = null
        composeRule.setContent {
            UltimateFocusTheme {
                BlockDurationPickerDialog(
                    appLabel = "Calendar",
                    onConfirm = { confirmed = it },
                    onDismiss = {},
                )
            }
        }
        composeRule.onNodeWithTag("block_duration_4h").performClick()
        composeRule.onNodeWithTag("block_duration_cancel").performClick()

        assertNull(confirmed)
        // Back at stage 1: all duration rows visible again.
        BlockDuration.entries.forEach { duration ->
            composeRule.onNodeWithTag("block_duration_${duration.label}").assertIsDisplayed()
        }
    }
}
