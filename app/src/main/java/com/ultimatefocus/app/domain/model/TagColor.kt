/**
 * Fixed neutral color palette used for tags. The 10 entries are exactly the
 * colors specified in the AC of ULT-72; black and white are excluded by spec.
 * Each color is unique — the repository layer enforces "no two tags share a
 * color" on top of this enum so the picker can disable already-used swatches.
 * Covers: ULT-72
 */
package com.ultimatefocus.app.domain.model

enum class TagColor(val hex: String, val displayName: String) {
    STEEL_BLUE("#5B8DB8", "Steel blue"),
    SAGE_GREEN("#6AAB72", "Sage green"),
    DUSTY_ROSE("#C47E8A", "Dusty rose"),
    SOFT_LAVENDER("#9B8EC4", "Soft lavender"),
    AMBER_GOLD("#D4A74E", "Amber gold"),
    TERRACOTTA("#CC7A5A", "Terracotta"),
    TEAL("#4BA8A8", "Teal"),
    WARM_TAUPE("#A0887A", "Warm taupe"),
    DEEP_PLUM("#8B6BAE", "Deep plum"),
    BERRY("#B85B6E", "Berry");

    companion object {
        fun fromHex(hex: String): TagColor? =
            values().firstOrNull { it.hex.equals(hex, ignoreCase = true) }
    }
}
