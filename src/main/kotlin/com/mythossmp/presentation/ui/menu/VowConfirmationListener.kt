package com.mythossmp.presentation.ui.menu

import com.mythossmp.MythosSMP
import com.mythossmp.application.task.Scheduler
import com.mythossmp.domain.event.TrialCompletedEvent
import com.mythossmp.domain.event.VowVFXEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import java.time.Duration

class VowConfirmationListener(
    private val plugin: MythosSMP,
    private val scheduler: Scheduler
) : Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    fun onInventoryClick(event: InventoryClickEvent) {
        val holder = event.view.topInventory.holder as? VowConfirmationMenu.VowConfirmationHolder ?: return
        event.isCancelled = true

        val slot = event.rawSlot
        if (slot < 0 || slot >= event.view.topInventory.size) return

        val player = event.whoClicked as? Player ?: return

        when (slot) {
            2 -> enableVow(player, holder.vowKey)
            6 -> player.closeInventory()
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onInventoryDrag(event: InventoryDragEvent) {
        if (event.view.topInventory.holder is VowConfirmationMenu.VowConfirmationHolder) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onTrialCompleted(event: TrialCompletedEvent) {
        val player = plugin.server.getPlayer(event.playerUuid) ?: return
        scheduler.later(Duration.ofMillis(500)) {
            VowConfirmationMenu(plugin).open(player, event.trialKey)
        }
    }

    private fun enableVow(player: Player, vowKey: String) {
        val profile = plugin.profileService.getProfile(player.uniqueId) ?: return
        val newVow = plugin.vowRegistry.getVow(vowKey) ?: return

        val currentActive = profile.activeVow?.lowercase()?.removePrefix("vow-")
        if (currentActive != null) {
            val oldVow = plugin.vowRegistry.getVow(currentActive)
            if (oldVow != null) {
                oldVow.passiveEffects.forEach { it.remove(player) }
                oldVow.curse.remove(player)
            }
        }

        plugin.profileService.updateProfile(player.uniqueId) {
            it.copy(activeVow = vowKey, dirty = true)
        }

        newVow.passiveEffects.forEach { it.apply(player) }
        newVow.curse.apply(player)

        plugin.vfxBroadcastService.publish(VowVFXEvent.VowSelectionActivate(
            player.location.clone(),
            vowKey
        ))

        player.sendMessage(Component.text()
            .append(Component.text("\u2728 ", NamedTextColor.GOLD))
            .append(Component.text("You pledge yourself to ", NamedTextColor.GRAY))
            .append(Component.text(newVow.displayName, NamedTextColor.GOLD, TextDecoration.BOLD))
            .append(Component.text("!", NamedTextColor.GRAY))
            .build()
        )

        plugin.cooldownService.removePlayer(player)
        plugin.statusDisplay.refreshPlayer(player)

        player.closeInventory()
    }
}
