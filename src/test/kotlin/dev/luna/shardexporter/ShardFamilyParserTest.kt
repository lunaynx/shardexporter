package dev.luna.shardexporter

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ShardFamilyParserTest {
    @Test
    fun `activeFamilyFromLore returns selected family`() {
        val lore = listOf("Families:", "  Arctic", "▶ Amphibian", "  Aquatic")

        assertEquals("Amphibian", ShardFamilyParser.activeFamilyFromLore(lore))
    }

    @Test
    fun `activeFamilyFromLore returns null for none`() {
        val lore = listOf("Families:", "▶ None", "  Arctic")

        assertNull(ShardFamilyParser.activeFamilyFromLore(lore))
    }

    @Test
    fun `activeFamilyFromLore returns null without selected family`() {
        val lore = listOf("Families:", "  Arctic", "  Amphibian")

        assertNull(ShardFamilyParser.activeFamilyFromLore(lore))
    }

    @Test
    fun `attributeIdFromSkyBlockId returns attribute id`() {
        assertEquals("mana_pool", ShardFamilyParser.attributeIdFromSkyBlockId("attribute:mana_pool"))
    }

    @Test
    fun `attributeIdFromSkyBlockId returns last component`() {
        assertEquals("3", ShardFamilyParser.attributeIdFromSkyBlockId("attribute:mana_pool:3"))
    }

    @Test
    fun `attributeIdFromSkyBlockId handles derived ids`() {
        assertEquals("mana_pool", ShardFamilyParser.attributeIdFromSkyBlockId("!attribute:mana_pool"))
    }

    @Test
    fun `attributeIdFromSkyBlockId ignores non attributes`() {
        assertNull(ShardFamilyParser.attributeIdFromSkyBlockId("item:ender_pearl"))
    }
}
