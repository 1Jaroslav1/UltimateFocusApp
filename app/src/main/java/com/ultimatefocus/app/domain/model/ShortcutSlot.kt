/**
 * Domain model for bottom-bar quick-access shortcuts.
 * ShortcutSlot identifies a fixed position on the home screen (LEFT/RIGHT).
 * ShortcutTarget represents the user's explicit override for a slot; a null
 * override means "fall back to the system default" (Phone for LEFT, Camera for RIGHT).
 * Covers: ULT-13, ULT-14
 */
package com.ultimatefocus.app.domain.model

enum class ShortcutSlot { LEFT, RIGHT }

data class ShortcutTarget(
    val packageName: String,
    val componentName: String, // flattened via ComponentName.flattenToString()
    val label: String,
)
