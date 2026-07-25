package com.mythossmp.presentation.listener

import com.mythossmp.application.combat.CombatService
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.AreaEffectCloudApplyEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.PotionSplashEvent

class CombatListener(
    private val combatService: CombatService
) : Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        if (event.isCancelled) return
        if (combatService.shouldCancelDamage(event.damager, event.entity)) {
            event.isCancelled = true
            event.damager.sendMessageIfPlayer(
                Component.text("Your attack was negated by the Seal of Trust.", NamedTextColor.GRAY)
            )
            event.entity.sendMessageIfPlayer(
                Component.text("Damage negated — you are protected by the Seal of Trust.", NamedTextColor.GREEN)
            )
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPotionSplash(event: PotionSplashEvent) {
        val shooter = event.entity.shooter
        if (shooter !is Player) return

        for (entity in event.affectedEntities) {
            if (entity !is Player) continue
            if (combatService.shouldCancelAreaEffect(event.entity, entity)) {
                event.setIntensity(entity, 0.0)
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onAreaEffectCloudApply(event: AreaEffectCloudApplyEvent) {
        val source = event.entity.source
        if (source !is Player) return

        val iterator = event.affectedEntities.iterator()
        while (iterator.hasNext()) {
            val entity = iterator.next()
            if (entity !is Player) continue
            if (combatService.shouldCancelAreaEffect(event.entity, entity)) {
                iterator.remove()
                entity.sendMessage(
                    Component.text("Lingering effects negated by the Seal of Trust.", NamedTextColor.GREEN)
                )
            }
        }
    }

    private fun Entity.sendMessageIfPlayer(message: Component) {
        if (this is Player) {
            this.sendMessage(message)
        }
    }
}
