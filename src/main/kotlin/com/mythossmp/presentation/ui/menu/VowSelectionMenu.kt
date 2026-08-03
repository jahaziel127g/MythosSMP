package com.mythossmp.presentation.ui.menu

import com.mythossmp.MythosSMP
import com.mythossmp.domain.model.Vow
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

class VowSelectionMenu(private val plugin: MythosSMP) {

    private val menuTitle = Component.text()
        .append(Component.text("\u2728 ", NamedTextColor.DARK_AQUA))
        .append(Component.text("Choose Your Vow", NamedTextColor.GOLD, TextDecoration.BOLD))
        .append(Component.text(" \u2728", NamedTextColor.DARK_AQUA))
        .build()

    val deitySlots: Map<Int, String> = mapOf(
        10 to "ares",
        13 to "loki",
        16 to "zeus",
        19 to "hades",
        22 to "poseidon",
        25 to "hermes",
        28 to "hercules",
        31 to "plutus",
        34 to "chronos",
        37 to "hephaestus",
        40 to "boreas",
        43 to "erebus",
        46 to "apollo",
        49 to "dionysus"
    )

    fun open(player: Player) {
        val holder = VowMenuHolder(this)
        val inventory = Bukkit.createInventory(holder, 54, menuTitle)
        holder.inventory = inventory

        fillInventory(inventory, player)

        player.openInventory(inventory)
    }

    private fun fillInventory(inventory: Inventory, player: Player) {
        val activeVowKey = plugin.profileService.getProfile(player.uniqueId)
            ?.activeVow?.lowercase()?.removePrefix("vow-")

        val allVows = plugin.vowRegistry.getAllVows()

        for (vow in allVows) {
            val slot = deitySlots.entries.firstOrNull { it.value == vow.key }?.key ?: continue
            inventory.setItem(slot, createVowItem(vow, vow.key == activeVowKey, player))
        }

        for (slot in 0 until 54) {
            if (inventory.getItem(slot) != null) continue
            val col = slot % 9
            val row = slot / 9
            val isIconRow = deitySlots.keys.any { it / 9 == row }
            if (col == 0 || col == 8 || !isIconRow) {
                inventory.setItem(slot, createFillerItem())
            }
        }
    }

    private fun createVowItem(vow: Vow, isActive: Boolean, player: Player): ItemStack {
        val item = ItemStack(deityIcon(vow.key))
        val meta = item.itemMeta ?: return item

        meta.displayName(Component.text()
            .append(if (isActive) Component.text("\u2714 ", NamedTextColor.GREEN) else Component.empty())
            .append(Component.text(vow.displayName, if (isActive) NamedTextColor.GREEN else NamedTextColor.GOLD, TextDecoration.BOLD))
            .build()
        )

        val lore = mutableListOf<Component>()

        if (isActive) {
            lore.add(Component.text("\u279C Currently Active", NamedTextColor.GREEN, TextDecoration.ITALIC))
            lore.add(Component.empty())
        }

        lore.add(Component.text(vow.description, NamedTextColor.GRAY, TextDecoration.ITALIC))
        lore.add(Component.empty())
        lore.add(Component.text("\u2694 Abilities:", NamedTextColor.DARK_AQUA))
        lore.add(Component.text("\u25B6 ${vow.ability1.displayName}", NamedTextColor.AQUA))
        lore.add(Component.text("   ${vow.ability1.description}", NamedTextColor.DARK_GRAY, TextDecoration.ITALIC))
        lore.add(Component.text("\u25B6 ${vow.ability2.displayName}", NamedTextColor.AQUA))
        lore.add(Component.text("   ${vow.ability2.description}", NamedTextColor.DARK_GRAY, TextDecoration.ITALIC))

        val cd1 = plugin.cooldownService.getRemainingSeconds(player, vow.ability1.key)
        val cd2 = plugin.cooldownService.getRemainingSeconds(player, vow.ability2.key)
        val cdText = buildString {
            if (cd1 > 0) append("CD1: ${cd1}s")
            if (cd1 > 0 && cd2 > 0) append(" | ")
            if (cd2 > 0) append("CD2: ${cd2}s")
        }
        if (cdText.isNotEmpty()) {
            lore.add(Component.text("   $cdText", NamedTextColor.YELLOW, TextDecoration.ITALIC))
        }

        lore.add(Component.empty())
        lore.add(Component.text("\uD83D\uDD12 Curse: ${vow.curse.description}", NamedTextColor.DARK_RED, TextDecoration.ITALIC))

        if (!isActive) {
            lore.add(Component.empty())
            lore.add(Component.text("\uD83D\uDC49 Click to select", NamedTextColor.YELLOW))
        }

        meta.lore(lore)
        item.itemMeta = meta

        return item
    }

    private fun createFillerItem(): ItemStack {
        val item = ItemStack(Material.GRAY_STAINED_GLASS_PANE)
        val meta = item.itemMeta ?: return item
        meta.displayName(Component.text(" "))
        item.itemMeta = meta
        return item
    }

    private fun deityIcon(key: String): Material {
        return when (key) {
            "ares" -> Material.NETHERITE_SWORD
            "loki" -> Material.ENDER_EYE
            "zeus" -> Material.LIGHTNING_ROD
            "hades" -> Material.WITHER_SKELETON_SKULL
            "poseidon" -> Material.TRIDENT
            "hermes" -> Material.FEATHER
            "hercules" -> Material.IRON_AXE
            "plutus" -> Material.GOLD_INGOT
            "chronos" -> Material.CLOCK
            "hephaestus" -> Material.BLAZE_POWDER
            "boreas" -> Material.BLUE_ICE
            "erebus" -> Material.BLACK_DYE
            "apollo" -> Material.SUNFLOWER
            "dionysus" -> Material.APPLE
            else -> Material.BARRIER
        }
    }

    fun getSlotVowKey(slot: Int): String? = deitySlots[slot]
}
