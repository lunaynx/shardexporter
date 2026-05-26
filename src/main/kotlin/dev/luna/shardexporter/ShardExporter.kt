package dev.luna.shardexporter

import com.google.gson.GsonBuilder
import net.fabricmc.api.ClientModInitializer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import tech.thatgravyboat.skyblockapi.api.SkyBlockAPI
import tech.thatgravyboat.skyblockapi.api.events.screen.ContainerCloseEvent
import tech.thatgravyboat.skyblockapi.api.events.screen.ContainerInitializedEvent
import tech.thatgravyboat.skyblockapi.api.events.screen.InventoryChangeEvent
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId.Companion.getSkyBlockId
import tech.thatgravyboat.skyblockapi.helpers.McClient
import tech.thatgravyboat.skyblockapi.utils.extentions.filterContainerItems
import tech.thatgravyboat.skyblockapi.utils.extentions.getRawLore
import tech.thatgravyboat.skyblockapi.utils.text.Text

object ShardExporter : ClientModInitializer {
    var activeFamily: String? = null
    val families = mutableMapOf<String, MutableSet<String>>()

    private val gson = GsonBuilder().setPrettyPrinting().create()
    private var trackingShardsInventory = false

    override fun onInitializeClient() {
        SkyBlockAPI.eventBus.register<ContainerInitializedEvent> { event ->
            processInventory(event.title, event.containerItems)
        }
        SkyBlockAPI.eventBus.register<InventoryChangeEvent> { event ->
            // SkyblockAPI filters out the player inventory here, so upper slot 46 stays index 46.
            processInventory(event.title, event.inventory.filterContainerItems())
        }
        SkyBlockAPI.eventBus.register<ContainerCloseEvent> {
            exportAndReset()
        }
    }

    private fun processInventory(title: String, items: List<ItemStack>) {
        if (!title.endsWith("Oddities ➜ Shards")) return

        val familySelector = items.getOrNull(FAMILY_SELECTOR_SLOT) ?: return
        if (familySelector.item != Items.ENDER_EYE) return

        trackingShardsInventory = true
        activeFamily = ShardFamilyParser.activeFamilyFromLore(familySelector.getRawLore())

        val family = activeFamily ?: return
        for (item in items) {
            val skyBlockId = item.getSkyBlockId()?.id ?: continue
            val attributeId = ShardFamilyParser.attributeIdFromSkyBlockId(skyBlockId) ?: continue
            families.getOrPut(attributeId) { mutableSetOf() }.add(family)
        }
    }

    private fun exportAndReset() {
        if (!trackingShardsInventory && families.isEmpty()) return

        McClient.clipboard = gson.toJson(families.toSortedMap().mapValues { (_, familySet) -> familySet.sorted() })
        McClient.chat.addMessage(Text.of("Exported shard families to clipboard."))

        activeFamily = null
        trackingShardsInventory = false
        families.clear()
    }

    private const val FAMILY_SELECTOR_SLOT = 46
}
