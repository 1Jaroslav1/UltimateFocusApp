/**
 * Confirmation dialog for the "Sync with system categories" action on
 * Manage Tags. Shows every detected default category (Social Media,
 * Games) with its color dot and a checkbox; underneath each category, a
 * scrollable nested list of detected app candidates each with their own
 * checkbox.
 *
 * Default state: every category and every detected app is checked. The
 * user can deselect any category (which collapses + skips its apps) or
 * any individual app (which keeps the category but assigns fewer apps).
 *
 * Categories that already exist in the user's tag set are labeled with
 * "(already exists)" and remain togglable — applying them just augments
 * the existing tag with the selected apps rather than creating it.
 *
 * Categories that detect zero matching apps render an empty-state hint
 * and disable the category checkbox.
 *
 * Renders inside DefaultAppModal so it shares the launcher's dark,
 * text-only modal aesthetic.
 *
 * Covers: ULT-85
 */
package com.ultimatefocus.app.ui.tags

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ultimatefocus.app.domain.service.SyncAppCandidate
import com.ultimatefocus.app.domain.service.SyncCategoryCandidate
import com.ultimatefocus.app.domain.service.SyncSelection
import com.ultimatefocus.app.ui.components.DefaultAppModal
import com.ultimatefocus.app.ui.components.DefaultModalAction

private const val DIALOG_MAX_HEIGHT_DP = 600

@Composable
fun SyncCategoriesDialog(
    candidates: List<SyncCategoryCandidate>,
    onConfirm: (SyncSelection) -> Unit,
    onDismiss: () -> Unit,
) {
    // Mutable selection state; initialized so every category + every
    // detected app is checked by default per the AC.
    var selectedCategories by remember(candidates) {
        mutableStateOf(candidates.filter { it.packages.isNotEmpty() }.map { it.name }.toSet())
    }
    val initialAppsByCategory: Map<String, Set<String>> = remember(candidates) {
        candidates.associate { c ->
            c.name to c.packages.map { it.packageName }.toSet()
        }
    }
    var selectedAppsByCategory by remember(candidates) {
        mutableStateOf(initialAppsByCategory)
    }

    DefaultAppModal(
        header = "Sync with system categories",
        onDismiss = onDismiss,
        modifier = Modifier.testTag("sync_categories_dialog"),
    ) {
        // Bound the dialog height inside the modal so weight(1f) on the
        // LazyColumn has something to size against. Without this bound
        // the column took its intrinsic height and overflowed the screen,
        // pushing the Cancel/Sync footer below the visible area.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = DIALOG_MAX_HEIGHT_DP.dp),
        ) {
            Text(
                text = "Pick the categories and apps to sync. Default selections " +
                    "match Android's system categories plus a small list of " +
                    "common apps.",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .testTag("sync_dialog_intro"),
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.4f), thickness = 1.dp)

            // weight(1f) so the list takes whatever space remains after
            // the intro + footer are measured. fill = true so it
            // definitively occupies that share — the footer is then
            // always visible at the bottom regardless of list length.
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("sync_dialog_list"),
            ) {
                items(items = candidates, key = { it.name }) { candidate ->
                    CategoryBlock(
                        candidate = candidate,
                        categoryChecked = candidate.name in selectedCategories,
                        appsChecked = selectedAppsByCategory[candidate.name].orEmpty(),
                        onCategoryToggle = { checked ->
                            selectedCategories = if (checked) {
                                selectedCategories + candidate.name
                            } else {
                                selectedCategories - candidate.name
                            }
                            // Toggling a category also toggles every app
                            // beneath it so the visual + the apply
                            // semantics stay aligned.
                            selectedAppsByCategory = selectedAppsByCategory + (
                                candidate.name to if (checked) {
                                    candidate.packages.map { it.packageName }.toSet()
                                } else {
                                    emptySet()
                                }
                                )
                        },
                        onAppToggle = { pkg, checked ->
                            val current = selectedAppsByCategory[candidate.name].orEmpty()
                            val next = if (checked) current + pkg else current - pkg
                            selectedAppsByCategory =
                                selectedAppsByCategory + (candidate.name to next)
                            // Auto-tick the category if the user just
                            // selected an app under a previously-cleared
                            // category — avoids the trap where checked
                            // apps belong to an unchecked category and
                            // the apply silently drops them.
                            if (checked && candidate.name !in selectedCategories) {
                                selectedCategories = selectedCategories + candidate.name
                            }
                        },
                    )
                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.15f),
                        thickness = 1.dp,
                    )
                }
            }

            HorizontalDivider(color = Color.White, thickness = 1.dp)

            // Sticky footer: Cancel + Sync. Pinned outside the LazyColumn
            // so it stays visible as the list scrolls.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sync_dialog_footer"),
            ) {
                DefaultModalAction(
                    text = "Cancel",
                    onClick = onDismiss,
                    fillWidth = false,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("sync_dialog_cancel"),
                )
                DefaultModalAction(
                    text = "Sync",
                    enabled = selectedCategories.any { name ->
                        selectedAppsByCategory[name].orEmpty().isNotEmpty()
                    },
                    onClick = {
                        onConfirm(
                            SyncSelection(
                                selectedCategories = selectedCategories,
                                selectedAppsByCategory = selectedAppsByCategory
                                    .filterKeys { it in selectedCategories },
                            ),
                        )
                    },
                    fillWidth = false,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("sync_dialog_confirm"),
                )
            }
        }
    }
}

@Composable
private fun CategoryBlock(
    candidate: SyncCategoryCandidate,
    categoryChecked: Boolean,
    appsChecked: Set<String>,
    onCategoryToggle: (Boolean) -> Unit,
    onAppToggle: (String, Boolean) -> Unit,
) {
    val noApps = candidate.packages.isEmpty()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag("sync_dialog_category_${candidate.name}"),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Checkbox(
                checked = categoryChecked,
                enabled = !noApps,
                onCheckedChange = { onCategoryToggle(it) },
                modifier = Modifier.testTag("sync_dialog_category_checkbox_${candidate.name}"),
            )
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(color = candidate.color.toComposeColor(), shape = CircleShape),
            )
            Spacer(modifier = Modifier.size(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = candidate.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                )
                Text(
                    text = when {
                        noApps -> "No matching apps installed"
                        candidate.alreadyExists -> "Already exists — apps will be added"
                        else -> "${candidate.packages.size} apps detected"
                    },
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.testTag("sync_dialog_category_subtitle_${candidate.name}"),
                )
            }
        }

        if (!noApps) {
            Column(
                verticalArrangement = Arrangement.spacedBy(0.dp),
                modifier = Modifier.padding(start = 16.dp),
            ) {
                candidate.packages.forEach { app ->
                    AppRow(
                        app = app,
                        checked = app.packageName in appsChecked,
                        onToggle = { onAppToggle(app.packageName, it) },
                    )
                }
            }
        }
    }
}

@Composable
private fun AppRow(
    app: SyncAppCandidate,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("sync_dialog_app_${app.packageName}"),
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onToggle,
            modifier = Modifier.testTag("sync_dialog_app_checkbox_${app.packageName}"),
        )
        Text(
            text = app.label,
            fontSize = 13.sp,
            color = Color.White.copy(alpha = 0.9f),
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp),
        )
    }
}

/**
 * Renders the "no installed apps fall into any default category" empty
 * state. Surfaced when the user opens the sync dialog on a freshly-wiped
 * device or one whose installed apps do not declare a CATEGORY_* and are
 * not in the curated allowlist.
 */
@Composable
fun SyncCategoriesEmptyDialog(onDismiss: () -> Unit) {
    DefaultAppModal(
        header = "Sync with system categories",
        onDismiss = onDismiss,
        modifier = Modifier.testTag("sync_categories_empty_dialog"),
    ) {
        Box(
            contentAlignment = Alignment.CenterStart,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 16.dp),
        ) {
            Text(
                text = "No installed apps fall into any of the default categories " +
                    "(Social Media, Games, Video & Streaming, Music & Audio, News). " +
                    "Nothing to sync right now.",
                fontSize = 14.sp,
                color = Color.White,
            )
        }
        HorizontalDivider(color = Color.White, thickness = 1.dp)
        DefaultModalAction(
            text = "OK",
            onClick = onDismiss,
            modifier = Modifier.testTag("sync_dialog_empty_dismiss"),
        )
    }
}
