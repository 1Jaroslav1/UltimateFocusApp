/**
 * Unit tests for TagColor — fromHex round-trip and uniqueness of hex values.
 * Covers: ULT-72
 */
package com.ultimatefocus.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class TagColorTest {

    @Test
    fun `fromHex resolves every defined color (case insensitive)`() {
        TagColor.values().forEach { color ->
            assertEquals(color, TagColor.fromHex(color.hex))
            assertEquals(color, TagColor.fromHex(color.hex.lowercase()))
        }
    }

    @Test
    fun `fromHex returns null for unknown hex`() {
        assertNull(TagColor.fromHex("#000000"))
        assertNull(TagColor.fromHex("notahex"))
    }

    @Test
    fun `every defined color has a unique hex`() {
        val hexes = TagColor.values().map { it.hex.lowercase() }
        assertEquals(hexes.size, hexes.toSet().size)
    }

    @Test
    fun `palette has exactly 10 entries`() {
        assertEquals(10, TagColor.values().size)
    }
}
