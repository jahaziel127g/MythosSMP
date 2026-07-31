package com.mythossmp.domain.vow

import com.mythossmp.MythosSMP
import com.mythossmp.domain.event.VowVFXEvent
import com.mythossmp.domain.model.ActiveAbility
import com.mythossmp.domain.model.Curse
import com.mythossmp.domain.model.PassiveEffect
import com.mythossmp.domain.model.Vow
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.attribute.Attribute
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class VowLoki : Vow {
    override val key = "loki"
    override val displayName = "Loki"
    override val enabled = true
    override val description = "God of Mischief — Trickster Rogue"

    override val passiveEffects: List<PassiveEffect> = listOf(LokiPassive())
    override val ability1: ActiveAbility = ShadowStep()
    override val ability2: ActiveAbility = VeilOfLies()
    override val curse: Curse = curseInstance

    companion object {
        val curseInstance = FragilityOfDeceit()
    }
}

class LokiPassive : PassiveEffect {
    override val key = "loki-passive"
    override val description = "Strike from behind for 2.5x damage"

    override fun apply(player: Player) {}
    override fun remove(player: Player) {}
}

class LokiBackstabListener : Listener {

    @EventHandler(priority = EventPriority.NORMAL)
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        if (event.isCancelled) return
        val attacker = event.damager as? Player ?: return
        val target = event.entity as? LivingEntity ?: return

        val plugin = MythosSMP.instance
        val profile = plugin.profileService.getProfile(attacker.uniqueId) ?: return
        if (profile.activeVow?.removePrefix("vow-") != "loki") return

        val cfg = plugin.configManager.getVowConfig("loki")
        if (!cfg.enabled) return

        val toAttacker = attacker.location.toVector().subtract(target.location.toVector())
            .setY(0.0).normalize()
        val targetFacing = target.location.direction.clone().setY(0.0).normalize()
        val dot = targetFacing.dot(toAttacker)
        val isBehind = dot < -cfg.backstabDotThreshold

        if (isBehind) {
            event.damage *= cfg.backstabMultiplier
            plugin.vfxBroadcastService.publish(VowVFXEvent.LokiBackstab(target.location.clone()))
        }
    }
}

class FragilityOfDeceit : Curse, Listener {
    override val key = "loki-fragility"
    override val description = "Max health reduced by 4 HP (2 hearts)."

    private val afflicted = ConcurrentHashMap.newKeySet<UUID>()
    private val originalHealth = ConcurrentHashMap<UUID, Double>()

    override fun apply(player: Player) {
        afflicted.add(player.uniqueId)
        val attr = player.getAttribute(Attribute.MAX_HEALTH) ?: return
        if (!originalHealth.containsKey(player.uniqueId)) {
            originalHealth[player.uniqueId] = attr.baseValue
        }
        attr.baseValue = (originalHealth[player.uniqueId] ?: 20.0) - 4.0
    }

    override fun remove(player: Player) {
        afflicted.remove(player.uniqueId)
        val original = originalHealth.remove(player.uniqueId) ?: return
        val attr = player.getAttribute(Attribute.MAX_HEALTH) ?: return
        attr.baseValue = original
    }
}

class ShadowStep : ActiveAbility {
    override val key = "loki-shadow-step"
    override val displayName = "Shadow Step"
    override val cooldownSeconds = 25
    override val description = "Teleport behind the nearest enemy within 15 blocks."

    override fun execute(player: Player, plugin: Any): Boolean {
        val mythosPlugin = plugin as MythosSMP
        val cfg = mythosPlugin.configManager.getVowConfig("loki")
        val range = cfg.shadowStepRange

        val target = player.world.getNearbyEntities(player.location, range, range, range)
            .filterIsInstance<LivingEntity>()
            .filter { it != player && it.isValid && !it.isDead }
            .minByOrNull { it.location.distanceSquared(player.location) }
            ?: run {
                player.sendMessage(Component.text("No enemy nearby to shadow step to.", NamedTextColor.GRAY))
                return false
            }

        val behind = target.location.clone().let { loc ->
            val dir = target.location.direction.normalize()
            loc.subtract(dir.multiply(2.0))
        }
        behind.y = behind.world?.getHighestBlockYAt(behind.blockX, behind.blockZ)?.toDouble() ?: behind.y
        player.teleport(behind)

        mythosPlugin.vfxBroadcastService.publish(VowVFXEvent.LokiShadowStep(player.location.clone()))

        player.sendMessage(Component.text("\uD83D\uDCAB Shadow Step behind ${target.name}!", NamedTextColor.LIGHT_PURPLE))
        return true
    }
}

class VeilOfLies : ActiveAbility {
    override val key = "loki-veil-of-lies"
    override val displayName = "Veil of Lies"
    override val cooldownSeconds = 45
    override val description = "Become invisible for 10s. First attack after cloak deals 3x damage."

    companion object {
        val cloaked = ConcurrentHashMap.newKeySet<UUID>()
        val firstHitPending = ConcurrentHashMap.newKeySet<UUID>()
    }

    override fun execute(player: Player, plugin: Any): Boolean {
        val mythosPlugin = plugin as MythosSMP
        val cfg = mythosPlugin.configManager.getVowConfig("loki")
        val duration = (cfg.veilDuration * 20).toInt()

        player.addPotionEffect(PotionEffect(PotionEffectType.INVISIBILITY, duration, 0, false, false))
        cloaked.add(player.uniqueId)
        firstHitPending.add(player.uniqueId)

        mythosPlugin.vfxBroadcastService.publish(VowVFXEvent.LokiVeilActivate(player.location.clone()))

        player.sendMessage(Component.text("\uD83C\uDF2A Veil of Lies activated! You are invisible. Your next attack deals 3x damage.", NamedTextColor.LIGHT_PURPLE))

        MythosSMP.instance.scheduler.later(java.time.Duration.ofMillis(duration * 50L)) {
            cloaked.remove(player.uniqueId)
            firstHitPending.remove(player.uniqueId)
        }
        return true
    }
}

class VeilFirstHitListener : Listener {

    @EventHandler(priority = EventPriority.HIGH)
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        if (event.isCancelled) return
        val attacker = event.damager as? Player ?: return
        if (!VeilOfLies.firstHitPending.contains(attacker.uniqueId)) return

        VeilOfLies.firstHitPending.remove(attacker.uniqueId)
        VeilOfLies.cloaked.remove(attacker.uniqueId)

        attacker.removePotionEffect(PotionEffectType.INVISIBILITY)

            val cfg = MythosSMP.instance.configManager.getVowConfig("loki")
            event.damage *= cfg.veilFirstHitMultiplier

            MythosSMP.instance.vfxBroadcastService.publish(VowVFXEvent.LokiVeilFirstHit(event.entity.location.clone()))

            attacker.sendMessage(Component.text("\uD83D\uDCA5 Veil strike! 3x damage dealt.", NamedTextColor.LIGHT_PURPLE))
    }
}
