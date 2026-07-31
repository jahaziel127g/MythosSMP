package com.mythossmp.presentation.ui.menu

import com.mythossmp.MythosSMP
import com.mythossmp.domain.event.VowVFXEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

class VowConfirmationMenu(private val plugin: MythosSMP) {

    private val menuTitle = Component.text()
        .append(Component.text("\u2728 ", NamedTextColor.GOLD))
        .append(Component.text("Enable Vow?", NamedTextColor.WHITE, TextDecoration.BOLD))
        .build()

    fun open(player: Player, vowKey: String) {
        val holder = VowConfirmationHolder(vowKey)
        val inventory = Bukkit.createInventory(holder, 9, menuTitle)
        holder.inventory = inventory

        inventory.setItem(2, createEnableItem())
        inventory.setItem(4, createInfoItem(vowKey))
        inventory.setItem(6, createLaterItem())

        for (i in 0 until 9) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, createFiller())
            }
        }

        player.openInventory(inventory)
    }

    private fun createEnableItem(): ItemStack {
        val item = ItemStack(Material.LIME_WOOL)
        val meta = item.itemMeta ?: return item
        meta.displayName(Component.text("\u2714 Enable Now", NamedTextColor.GREEN, TextDecoration.BOLD))
        meta.lore(listOf(
            Component.text("Equip this vow immediately.", NamedTextColor.GRAY)
        ))
        item.itemMeta = meta
        return item
    }

    private fun createLaterItem(): ItemStack {
        val item = ItemStack(Material.RED_WOOL)
        val meta = item.itemMeta ?: return item
        meta.displayName(Component.text("\uD83D\uDD52 Save for Later", NamedTextColor.RED, TextDecoration.BOLD))
        meta.lore(listOf(
            Component.text("Use /vow accept to equip later.", NamedTextColor.GRAY)
        ))
        item.itemMeta = meta
        return item
    }

    private fun createInfoItem(vowKey: String): ItemStack {
        val vow = plugin.vowRegistry.getVow(vowKey)
        val item = ItemStack(Material.BOOK)
        val meta = item.itemMeta ?: return item
        meta.displayName(Component.text()
            .append(Component.text(vow?.displayName ?: "Unknown Vow", NamedTextColor.GOLD, TextDecoration.BOLD))
            .build()
        )
        meta.lore(listOf(
            Component.text("You earned this vow!", NamedTextColor.GREEN),
            Component.empty(),
            Component.text(vow?.description ?: "", NamedTextColor.GRAY)
        ))
        item.itemMeta = meta
        return item
    }

    private fun createFiller(): ItemStack {
        val item = ItemStack(Material.GRAY_STAINED_GLASS_PANE)
        val meta = item.itemMeta ?: return item
        meta.displayName(Component.text(" "))
        item.itemMeta = meta
        return item
    }

    class VowConfirmationHolder(val vowKey: String) : org.bukkit.inventory.InventoryHolder {
        private var inv: Inventory? = null

        override fun getInventory(): Inventory = inv!!

        internal fun setInventory(inventory: Inventory) {
            inv = inventory
        }
    }
}
