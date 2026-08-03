package com.mythossmp.domain.vow

import com.mythossmp.MythosSMP
import com.mythossmp.application.task.Cancellable
import com.mythossmp.domain.event.VowVFXEvent
import com.mythossmp.domain.model.ActiveAbility
import com.mythossmp.domain.model.Curse
import com.mythossmp.domain.model.PassiveEffect
import com.mythossmp.domain.model.Vow
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityKnockbackEvent
import org.bukkit.event.entity.EntityPotionEffectEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.time.Duration
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class VowHercules : Vow {
    override val key = "hercules"
    override val displayName = "Hercules"
    override val enabled = true
    override val description = "God of Strength — Immovable Juggernaut"

    override val passiveEffects: List<PassiveEffect> = listOf(HerculesPassive())
    override val ability1: ActiveAbility = TitansSlam()
    override val ability2: ActiveAbility = UnbreakableWill()
    override val curse: Curse = curseInstance

    companion object {
        val curseInstance = ColossalBurden()
        val slamPlayers = ConcurrentHashMap.newKeySet<UUID>()
        val willPlayers = ConcurrentHashMap.newKeySet<UUID>()
        val airborne = ConcurrentHashMap.newKeySet<UUID>()
    }
}

private fun hasHercules(playerUuid: UUID): Boolean {
    val plugin = MythosSMP.instance
    val profile = plugin.profileService.getProfile(playerUuid) ?: return false
    if (profile.activeVow?.removePrefix("vow-") != "hercules") return false
    return plugin.configManager.getHerculesConfig().enabled
}

class HerculesPassive : PassiveEffect {
    override val key = "hercules-passive"
    override val description = "Permanent Resistance I + 20% flat knockback resistance"

    private val refreshTasks = ConcurrentHashMap<UUID, Cancellable>()

    override fun apply(player: Player) {
        val cfg = MythosSMP.instance.configManager.getHerculesConfig()
        player.addPotionEffect(PotionEffect(PotionEffectType.RESISTANCE, 20 * 30, cfg.passiveResistanceAmplifier, false, false))
        refreshTasks[player.uniqueId] = MythosSMP.instance.scheduler.repeat(Duration.ofSeconds(25)) {
            if (!player.isOnline) return@repeat
            if (!hasHercules(player.uniqueId)) return@repeat
            player.addPotionEffect(PotionEffect(PotionEffectType.RESISTANCE, 20 * 30, cfg.passiveResistanceAmplifier, false, false))
        }
    }

    override fun remove(player: Player) {
        refreshTasks.remove(player.uniqueId)?.cancel()
        player.removePotionEffect(PotionEffectType.RESISTANCE)
    }
}

class HerculesListener : Listener {

    @EventHandler
    fun onMove(event: PlayerMoveEvent) {
        val player = event.player
        val uuid = player.uniqueId
        if (!hasHercules(uuid)) {
            VowHercules.airborne.remove(uuid)
            return
        }

        val wasAirborne = VowHercules.airborne.contains(uuid)
        if (player.isOnGround) {
            VowHercules.airborne.remove(uuid)
        } else {
            VowHercules.airborne.add(uuid)
        }

        if (wasAirborne && player.isOnGround && VowHercules.slamPlayers.remove(uuid)) {
            triggerShockwave(player)
        }
    }

    @EventHandler
    fun onKnockback(event: EntityKnockbackEvent) {
        val entity = event.entity
        if (entity !is Player) return
        val uuid = entity.uniqueId

        if (VowHercules.willPlayers.contains(uuid)) {
            event.isCancelled = true
            return
        }

        if (hasHercules(uuid)) {
            val cfg = MythosSMP.instance.configManager.getHerculesConfig()
            event.setFinalKnockback(event.knockback.multiply(1.0 - cfg.passiveKbResistance))
        }
    }

    @EventHandler
    fun onPotionEffect(event: EntityPotionEffectEvent) {
        val entity = event.entity
        if (entity !is Player) return
        if (!VowHercules.willPlayers.contains(entity.uniqueId)) return

        val action = event.action
        if (action != EntityPotionEffectEvent.Action.ADDED && action != EntityPotionEffectEvent.Action.CHANGED) return

        val type = event.newEffect?.type ?: return
        if (type == PotionEffectType.SLOWNESS || type == PotionEffectType.POISON || type == PotionEffectType.WITHER) {
            event.isCancelled = true
        }
    }

    private fun triggerShockwave(player: Player) {
        val plugin = MythosSMP.instance
        val cfg = plugin.configManager.getHerculesConfig()
        val trusted = plugin.trustService.getTrusted(player.uniqueId).toSet()

        plugin.vfxBroadcastService.publish(
            VowVFXEvent.HerculesTitanSlam(player.location.clone(), cfg.slamRadius)
        )

        player.world.getNearbyEntities(player.location, cfg.slamRadius, cfg.slamRadius, cfg.slamRadius)
            .filterIsInstance<LivingEntity>()
            .filter { it != player && it.isValid && !it.isDead }
            .forEach { entity ->
                if (entity is Player && trusted.contains(entity.uniqueId)) return@forEach
                entity.damage(cfg.slamDamage, player)
                val knock = entity.location.toVector().subtract(player.location.toVector())
                    .normalize().multiply(1.2).setY(0.8)
                entity.setVelocity(knock)
            }

        player.sendMessage(Component.text("\uD83D\uDCAA Titan's Slam! The earth shakes!", NamedTextColor.GOLD))
    }
}

class TitansSlam : ActiveAbility {
    override val key = "hercules-titan-slam"
    override val displayName = "Titan's Slam"
    override val cooldownSeconds = 45
    override val description = "Launch skyward; on landing, crush untrusted foes with a shockwave."

    override fun execute(player: Player, plugin: Any): Boolean {
        val mythosPlugin = plugin as MythosSMP
        val launch = player.location.direction.multiply(0.7).setY(1.15)
        player.setVelocity(launch)

        VowHercules.slamPlayers.add(player.uniqueId)
        VowHercules.airborne.add(player.uniqueId)
        mythosPlugin.scheduler.later(Duration.ofSeconds(10)) {
            VowHercules.slamPlayers.remove(player.uniqueId)
        }

        player.sendMessage(Component.text("\uD83D\uDCAA Titan's Slam! Land to crush your enemies!", NamedTextColor.GOLD))
        return true
    }
}

class UnbreakableWill : ActiveAbility {
    override val key = "hercules-unbreakable-will"
    override val displayName = "Unbreakable Will"
    override val cooldownSeconds = 180
    override val description = "10s: immune to knockback, slowness, poison and wither; Resistance II."

    override fun execute(player: Player, plugin: Any): Boolean {
        val mythosPlugin = plugin as MythosSMP
        val cfg = mythosPlugin.configManager.getHerculesConfig()
        val uuid = player.uniqueId

        VowHercules.willPlayers.add(uuid)
        player.addPotionEffect(PotionEffect(PotionEffectType.RESISTANCE, 20 * cfg.willDurationSeconds.toInt(), cfg.willResistanceAmplifier, false, false))
        mythosPlugin.vfxBroadcastService.publish(
            VowVFXEvent.HerculesUnbreakableWill(player.location.clone())
        )
        player.sendMessage(Component.text("\uD83D\uDD27 Unbreakable Will! Nothing can move you now.", NamedTextColor.GOLD))

        mythosPlugin.scheduler.later(Duration.ofSeconds(cfg.willDurationSeconds.toLong())) {
            VowHercules.willPlayers.remove(uuid)
            if (player.isOnline) {
                player.sendMessage(Component.text("Unbreakable Will fades...", NamedTextColor.GRAY))
            }
        }
        return true
    }
}

class ColossalBurden : Curse {
    override val key = "hercules-colossal-burden"
    override val description = "Heavy constitution: permanent Slowness I."

    private val refreshTasks = ConcurrentHashMap<UUID, Cancellable>()

    override fun apply(player: Player) {
        val cfg = MythosSMP.instance.configManager.getHerculesConfig()
        player.addPotionEffect(PotionEffect(PotionEffectType.SLOWNESS, 20 * 30, cfg.curseSlownessAmplifier, false, false))
        refreshTasks[player.uniqueId] = MythosSMP.instance.scheduler.repeat(Duration.ofSeconds(25)) {
            if (!player.isOnline) return@repeat
            if (!hasHercules(player.uniqueId)) return@repeat
            player.addPotionEffect(PotionEffect(PotionEffectType.SLOWNESS, 20 * 30, cfg.curseSlownessAmplifier, false, false))
        }
    }

    override fun remove(player: Player) {
        refreshTasks.remove(player.uniqueId)?.cancel()
        player.removePotionEffect(PotionEffectType.SLOWNESS)
    }
}
