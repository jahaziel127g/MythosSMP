package com.mythossmp.presentation.ui.menu

import com.mythossmp.MythosSMP
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent

class MenuListener(private val plugin: MythosSMP) : Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    fun onInventoryClick(event: InventoryClickEvent) {
        val top = event.view.topInventory
        val holder = top.holder as? VowMenuHolder ?: return
        event.isCancelled = true

        val slot = event.rawSlot
        if (slot < 0 || slot >= top.size) return

        val vowKey = holder.menu.getSlotVowKey(slot) ?: return
        handleVowSelection(event.whoClicked as Player, vowKey)
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onInventoryDrag(event: InventoryDragEvent) {
        if (event.view.topInventory.holder !is VowMenuHolder) return
        event.isCancelled = true
    }

    private fun handleVowSelection(player: Player, vowKey: String) {
        val profile = plugin.profileService.getProfile(player.uniqueId) ?: return
        val newVow = plugin.vowRegistry.getVow(vowKey) ?: return

        val currentActive = profile.activeVow?.lowercase()?.removePrefix("vow-")
        if (currentActive == vowKey) {
            player.sendMessage(Component.text()
                .append(Component.text("\u2728 ", NamedTextColor.GOLD))
                .append(Component.text("You already serve ", NamedTextColor.GRAY))
                .append(Component.text(newVow.displayName, NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text(".", NamedTextColor.GRAY))
                .build()
            )
            player.closeInventory()
            return
        }

        val trialConfig = plugin.trialService.getDefaultConfig(vowKey)
        if (trialConfig == null) {
            player.sendMessage(Component.text("No trial configured for this vow.", NamedTextColor.RED))
            player.closeInventory()
            return
        }

        if (plugin.trialService.hasCompletedTrial(player.uniqueId, vowKey)) {
            VowConfirmationMenu(plugin).open(player, vowKey)
        } else {
            val scroll = plugin.scrollItemRegistry.createScroll(trialConfig)
            player.inventory.addItem(scroll)
            player.sendMessage(Component.text()
                .append(Component.text("\uD83D\uDCDC ", NamedTextColor.GOLD))
                .append(Component.text("Complete the ", NamedTextColor.GRAY))
                .append(Component.text(trialConfig.displayName, NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text(" trial to earn the ", NamedTextColor.GRAY))
                .append(Component.text(newVow.displayName, NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text(" vow!", NamedTextColor.GRAY))
                .build()
            )
        }

        player.closeInventory()
    }
}
