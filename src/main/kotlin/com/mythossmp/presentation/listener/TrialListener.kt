package com.mythossmp.presentation.listener

import com.mythossmp.MythosSMP
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.EquipmentSlot

class TrialListener(private val plugin: MythosSMP) : Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    fun onMobDeath(event: EntityDeathEvent) {
        val entity = event.entity
        if (!plugin.trialService.isTrialMob(entity.uniqueId)) return

        event.drops.clear()
        event.droppedExp = 0

        plugin.trialService.onMobDeath(entity.uniqueId)
    }

    @EventHandler
    fun onScrollUse(event: PlayerInteractEvent) {
        if (event.hand != EquipmentSlot.HAND) return
        val item = event.item ?: return
        val trialKey = plugin.scrollItemRegistry.getTrialKey(item) ?: return

        event.isCancelled = true

        val player = event.player
        val session = plugin.trialService.startTrial(player, trialKey)
        if (session != null) {
            item.amount--
        }
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val uuid = event.player.uniqueId
        if (plugin.trialService.isParticipating(uuid)) {
            plugin.logger.info("Player $uuid left during a trial — trial will fail on next tick.")
        }
    }
}
