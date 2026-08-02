package com.mythossmp.domain.vow

import com.mythossmp.MythosSMP
import com.mythossmp.domain.event.VowVFXEvent
import com.mythossmp.domain.model.ActiveAbility
import com.mythossmp.domain.model.Curse
import com.mythossmp.domain.model.PassiveEffect
import com.mythossmp.domain.model.Vow
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.block.data.Waterlogged
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.time.Duration
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class VowPoseidon : Vow {
    override val key = "poseidon"
    override val displayName = "Poseidon"
    override val enabled = true
    override val description = "God of the Sea — Aquatic Gladiator"

    override val passiveEffects: List<PassiveEffect> = listOf(PoseidonPassive())
    override val ability1: ActiveAbility = WaveCrash()
    override val ability2: ActiveAbility = AbyssalBlessing()
    override val curse: Curse = curseInstance

    companion object {
        val curseInstance = Dehydration()
    }
}

class PoseidonPassive : PassiveEffect {
    override val key = "poseidon-passive"
    override val description = "Dolphin's Grace + water breathing in water, underwater mining haste"

    override fun apply(player: Player) {}
    override fun remove(player: Player) {}
}

private fun hasPoseidon(playerUuid: UUID): Boolean {
    val plugin = MythosSMP.instance
    val profile = plugin.profileService.getProfile(playerUuid) ?: return false
    if (profile.activeVow?.removePrefix("vow-") != "poseidon") return false
    return plugin.configManager.getPoseidonConfig().enabled
}

private fun isWet(player: Player): Boolean {
    if (player.isInWaterOrRain) return true
    val blockData = player.location.block.blockData
    return (blockData as? Waterlogged)?.isWaterlogged == true
}

class PoseidonListener : Listener {

    private val inWater = ConcurrentHashMap.newKeySet<UUID>()

    @EventHandler
    fun onMove(event: PlayerMoveEvent) {
        val player = event.player
        if (!hasPoseidon(player.uniqueId)) return

        val wet = isWet(player)
        val wasWet = inWater.contains(player.uniqueId)

        if (wet && !wasWet) {
            inWater.add(player.uniqueId)
            applyAquaticEffects(player)
            if (AbyssalBlessing.blessed.contains(player.uniqueId)) {
                applyBlessingEffects(player)
            }
        } else if (!wet && wasWet) {
            inWater.remove(player.uniqueId)
            stripAquaticEffects(player)
        }
    }

    private fun applyAquaticEffects(player: Player) {
        player.addPotionEffect(PotionEffect(PotionEffectType.DOLPHINS_GRACE, 600, 0, false, false))
        player.addPotionEffect(PotionEffect(PotionEffectType.WATER_BREATHING, 600, 0, false, false))
        player.addPotionEffect(PotionEffect(PotionEffectType.HASTE, 600, 4, false, false))
    }

    private fun stripAquaticEffects(player: Player) {
        player.removePotionEffect(PotionEffectType.DOLPHINS_GRACE)
        player.removePotionEffect(PotionEffectType.WATER_BREATHING)
        player.removePotionEffect(PotionEffectType.HASTE)

        if (AbyssalBlessing.blessed.contains(player.uniqueId)) {
            MythosSMP.instance.scheduler.later(Duration.ofSeconds(3)) {
                if (!player.isOnline) return@later
                if (!isWet(player)) {
                    stripBlessingEffects(player)
                }
            }
        }
    }
}

class WaveCrash : ActiveAbility {
    override val key = "poseidon-wave-crash"
    override val displayName = "Wave Crash"
    override val cooldownSeconds = 35
    override val description = "Push untrusted enemies back with a crashing wave."

    override fun execute(player: Player, plugin: Any): Boolean {
        val mythosPlugin = plugin as MythosSMP
        val cfg = mythosPlugin.configManager.getPoseidonConfig()

        val radius = cfg.waveCrashRadius
        val trusted = mythosPlugin.trustService.getTrusted(player.uniqueId).toSet()

        mythosPlugin.vfxBroadcastService.publish(
            VowVFXEvent.PoseidonWaveCrash(player.location.clone(), radius)
        )

        player.world.getNearbyEntities(player.location, radius, radius, radius)
            .filterIsInstance<LivingEntity>()
            .filter { it != player && it.isValid && !it.isDead }
            .forEach { entity ->
                if (entity is Player && trusted.contains(entity.uniqueId)) return@forEach
                val push = entity.location.toVector().subtract(player.location.toVector())
                    .normalize().multiply(cfg.pushVelocityFactor).setY(0.4)
                entity.setVelocity(push)
            }

        player.sendMessage(Component.text("\uD83C\uDF0A Wave Crash unleashed!", NamedTextColor.AQUA))
        return true
    }
}

class AbyssalBlessing : ActiveAbility {
    override val key = "poseidon-abyssal-blessing"
    override val displayName = "Abyssal Blessing"
    override val cooldownSeconds = 10
    override val description = "Gain Strength I + Regeneration I while in water (stripped 3s after leaving)."

    companion object {
        val blessed = ConcurrentHashMap.newKeySet<UUID>()
    }

    override fun execute(player: Player, plugin: Any): Boolean {
        if (!isWet(player)) {
            player.sendMessage(Component.text("You must be in water to receive the Abyssal Blessing!", NamedTextColor.RED))
            return false
        }

        val mythosPlugin = plugin as MythosSMP
        blessed.add(player.uniqueId)
        applyBlessingEffects(player)
        mythosPlugin.vfxBroadcastService.publish(
            VowVFXEvent.PoseidonAbyssalBlessing(player.location.clone())
        )
        player.sendMessage(Component.text("\uD83D\uDC2C Abyssal Blessing active while you remain in water.", NamedTextColor.AQUA))
        return true
    }
}

private fun applyBlessingEffects(player: Player) {
    val cfg = MythosSMP.instance.configManager.getPoseidonConfig()
    player.addPotionEffect(PotionEffect(PotionEffectType.STRENGTH, 600, cfg.strengthAmplifier, false, false))
    player.addPotionEffect(PotionEffect(PotionEffectType.REGENERATION, 600, cfg.regenAmplifier, false, false))
}

private fun stripBlessingEffects(player: Player) {
    player.removePotionEffect(PotionEffectType.STRENGTH)
    player.removePotionEffect(PotionEffectType.REGENERATION)
}

class Dehydration : Curse, Listener {
    override val key = "poseidon-dehydration"
    override val description = "Bound to the sea: no water contact for 5 minutes deals 1 damage every 3s."

    private val afflicted = ConcurrentHashMap.newKeySet<UUID>()
    private val lastWaterTouch = ConcurrentHashMap<UUID, Long>()
    private val lastDamage = ConcurrentHashMap<UUID, Long>()
    private var tickTask: com.mythossmp.application.task.Cancellable? = null

    override fun apply(player: Player) {
        afflicted.add(player.uniqueId)
        lastWaterTouch[player.uniqueId] = System.currentTimeMillis()
        if (tickTask == null) {
            tickTask = MythosSMP.instance.scheduler.repeat(Duration.ofSeconds(1)) { tick() }
        }
    }

    override fun remove(player: Player) {
        afflicted.remove(player.uniqueId)
        lastWaterTouch.remove(player.uniqueId)
        lastDamage.remove(player.uniqueId)
        if (afflicted.isEmpty()) {
            tickTask?.cancel()
            tickTask = null
        }
    }

    @EventHandler
    fun onMove(event: PlayerMoveEvent) {
        val player = event.player
        if (!afflicted.contains(player.uniqueId)) return
        if (isWet(player)) {
            lastWaterTouch[player.uniqueId] = System.currentTimeMillis()
        }
    }

    private fun tick() {
        val cfg = MythosSMP.instance.configManager.getPoseidonConfig()
        val limitMs = (cfg.dehydrationLimitMinutes * 60_000).toLong()
        val intervalMs = (cfg.dehydrationIntervalSeconds * 1000).toLong()
        val now = System.currentTimeMillis()

        for (uuid in afflicted) {
            val player = MythosSMP.instance.server.getPlayer(uuid) ?: continue
            val lastTouch = lastWaterTouch[uuid] ?: now
            if (now - lastTouch < limitMs) continue
            if (now - (lastDamage[uuid] ?: 0) < intervalMs) continue

            lastDamage[uuid] = now
            player.damage(cfg.dehydrationDamage)
            player.sendMessage(Component.text("\uD83D\uDCA7 Dehydration! Get to water!", NamedTextColor.RED))
        }
    }
}
