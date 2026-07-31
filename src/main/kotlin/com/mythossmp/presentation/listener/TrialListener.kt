package com.mythossmp.presentation.listener

import com.mythossmp.MythosSMP
import com.mythossmp.application.trial.TrialService
import com.mythossmp.infrastructure.trial.ScrollItemRegistry
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityCombustEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.EquipmentSlot

class TrialListener(
    private val plugin: MythosSMP,
    private val trialService: TrialService,
    private val scrollItemRegistry: ScrollItemRegistry
) : Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    fun onMobCombust(event: EntityCombustEvent) {
        if (trialService.isTrialMob(event.entity.uniqueId)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onMobDeath(event: EntityDeathEvent) {
        val entity = event.entity
        if (!trialService.isTrialMob(entity.uniqueId)) return

        event.drops.clear()
        event.droppedExp = 0

        trialService.onMobDeath(entity.uniqueId)
    }

    @EventHandler
    fun onScrollUse(event: PlayerInteractEvent) {
        if (event.hand != EquipmentSlot.HAND) return
        val item = event.item ?: return
        val trialKey = scrollItemRegistry.getTrialKey(item) ?: return

        event.isCancelled = true

        val player = event.player
        val session = trialService.startTrial(player, trialKey)
        if (session != null) {
            item.amount--
        }
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val uuid = event.player.uniqueId
        if (trialService.isParticipating(uuid)) {
            plugin.logger.info("Player $uuid left during a trial — trial will fail on next tick.")
        }
    }
}
