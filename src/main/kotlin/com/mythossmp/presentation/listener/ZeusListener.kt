package com.mythossmp.presentation.listener

import com.mythossmp.application.vfx.VFXBroadcastService
import com.mythossmp.domain.event.VowVFXEvent
import com.mythossmp.domain.vow.VowZeus
import com.mythossmp.infrastructure.config.ConfigManager
import kotlin.math.sqrt
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerMoveEvent

class ZeusListener(
    private val vowZeus: VowZeus,
    private val vfxService: VFXBroadcastService,
    private val config: ConfigManager.ZeusConfig
) : Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    fun onMove(event: PlayerMoveEvent) {
        val to = event.to ?: return

        val dx = to.x - event.from.x
        val dz = to.z - event.from.z
        val horizontalDistance = sqrt(dx * dx + dz * dz)

        if (horizontalDistance > 0.001) {
            vowZeus.addCharge(event.player.uniqueId, horizontalDistance * config.staticChargePerBlock)
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onAttack(event: EntityDamageByEntityEvent) {
        val attacker = event.damager as? Player ?: return

        if (vowZeus.consumeCharge(attacker.uniqueId)) {
            event.damage += config.staticStrikeDamage
            vfxService.publish(VowVFXEvent.ZeusStaticOverchargeVFXEvent(event.entity.location))
        }
    }

    @EventHandler
    fun onDamage(event: EntityDamageEvent) {
        val player = event.entity as? Player ?: return
        val cause = event.cause

        if (cause != EntityDamageEvent.DamageCause.MAGIC &&
            cause != EntityDamageEvent.DamageCause.LIGHTNING &&
            cause != EntityDamageEvent.DamageCause.FIRE &&
            cause != EntityDamageEvent.DamageCause.FIRE_TICK
        ) return

        val location = player.location
        val multiplier = if (location.block.isLiquid || location.world.hasStorm()) {
            config.conductiveMultiplier * 2.0
        } else {
            config.conductiveMultiplier
        }
        event.damage *= multiplier
    }
}
