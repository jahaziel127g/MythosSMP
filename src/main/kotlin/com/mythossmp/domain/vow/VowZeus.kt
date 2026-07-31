package com.mythossmp.domain.vow

import com.mythossmp.MythosSMP
import com.mythossmp.domain.event.VowVFXEvent
import com.mythossmp.domain.model.ActiveAbility
import com.mythossmp.domain.model.Curse
import com.mythossmp.domain.model.PassiveEffect
import com.mythossmp.domain.model.Vow
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class VowZeus : Vow {

    override val key = "zeus"
    override val displayName = "Zeus"
    override val enabled = true
    override val description = "God of the Sky — Stormcaller"

    override val passiveEffects: List<PassiveEffect> = listOf(ZeusStaticCharge())
    override val ability1: ActiveAbility = StaticStrike()
    override val ability2: ActiveAbility = Overcharge()
    override val curse: Curse = Conductive()

    private val chargeRegistry = ConcurrentHashMap<UUID, Double>()

    fun getCharge(uuid: UUID): Double = chargeRegistry.getOrDefault(uuid, 0.0)

    fun addCharge(uuid: UUID, amount: Double) {
        chargeRegistry.compute(uuid) { _, current ->
            ((current ?: 0.0) + amount.coerceAtLeast(0.0)).coerceAtMost(1.0)
        }
    }

    fun consumeCharge(uuid: UUID): Boolean {
        return if (getCharge(uuid) >= 1.0) {
            chargeRegistry[uuid] = 0.0
            true
        } else {
            false
        }
    }
}

class ZeusStaticCharge : PassiveEffect {
    override val key = "zeus-passive"
    override val description = "Movement builds static charge; a full charge unleashes a static strike"
    override fun apply(player: Player) {}
    override fun remove(player: Player) {}
}

class Conductive : Curse {
    override val key = "zeus-curse"
    override val description = "Fire and lightning damage amplified; doubled in water or storms"
    override fun apply(player: Player) {}
    override fun remove(player: Player) {}
}

class StaticStrike : ActiveAbility {
    override val key = "zeus-static-strike"
    override val displayName = "Static Strike"
    override val cooldownSeconds = 30
    override val description = "Strike the nearest enemy in front of you with lightning"

    override fun execute(player: Player, plugin: Any): Boolean {
        val mythos = plugin as MythosSMP
        val config = mythos.configManager.getZeusConfig()

        val target = player.location.getNearbyEntities(8.0, 8.0, 8.0)
            .filterIsInstance<LivingEntity>()
            .filter { it !== player && it.isValid }
            .filter { isInFrontOf(player, it) }
            .minByOrNull { it.location.distanceSquared(player.location) }
            ?: run {
                player.sendMessage(Component.text("No enemy in front of you.", NamedTextColor.RED))
                return false
            }

        target.damage(config.staticStrikeDamage, player)
        mythos.vfxBroadcastService.publish(VowVFXEvent.ZeusStaticOverchargeVFXEvent(target.location))
        player.sendMessage(Component.text()
            .append(Component.text("\u26A1 ", NamedTextColor.YELLOW))
            .append(Component.text("Static strike hits ", NamedTextColor.GRAY))
            .append(target.customName() ?: Component.text(target.type.name))
            .build()
        )
        return true
    }

    private fun isInFrontOf(player: Player, target: LivingEntity): Boolean {
        val facing = player.location.direction.normalize()
        val toTarget = target.location.toVector().subtract(player.location.toVector()).normalize()
        return facing.dot(toTarget) > 0.5
    }
}

class Overcharge : ActiveAbility {
    override val key = "zeus-overcharge"
    override val displayName = "Overcharge"
    override val cooldownSeconds = 60
    override val description = "Instantly fill your static meter to 100%"

    override fun execute(player: Player, plugin: Any): Boolean {
        val mythos = plugin as MythosSMP
        val zeus = mythos.vowZeus

        if (zeus.getCharge(player.uniqueId) >= 1.0) {
            player.sendMessage(Component.text("Your static meter is already full.", NamedTextColor.YELLOW))
            return false
        }

        zeus.addCharge(player.uniqueId, 1.0)
        mythos.vfxBroadcastService.publish(VowVFXEvent.ZeusStaticOverchargeVFXEvent(player.location))
        player.sendMessage(Component.text()
            .append(Component.text("\u26A1 ", NamedTextColor.YELLOW))
            .append(Component.text("Static meter overcharged to 100%!", NamedTextColor.GRAY))
            .build()
        )
        return true
    }
}
