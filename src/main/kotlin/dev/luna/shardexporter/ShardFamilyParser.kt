package dev.luna.shardexporter

object ShardFamilyParser {
    private const val SELECTED_PREFIX = "▶ "
    private const val ATTRIBUTE_PREFIX = "attribute:"

    fun activeFamilyFromLore(lines: List<String>): String? {
        val family = lines.firstOrNull { it.startsWith(SELECTED_PREFIX) }
            ?.removePrefix(SELECTED_PREFIX)
            ?.trim()
            ?: return null

        return family.takeUnless { it == "None" }
    }

    fun attributeIdFromSkyBlockId(skyBlockId: String): String? {
        val rootId = skyBlockId.removePrefix("!")
        if (!rootId.startsWith(ATTRIBUTE_PREFIX)) return null
        return rootId.substringAfterLast(':').takeIf(String::isNotEmpty)
    }
}
