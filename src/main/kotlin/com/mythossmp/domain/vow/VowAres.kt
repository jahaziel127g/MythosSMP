package com.mythossmp.domain.vow

import com.mythossmp.MythosSMP
import com.mythossmp.domain.event.VowVFXEvent
import com.mythossmp.domain.model.ActiveAbility
import com.mythossmp.domain.model.Curse
import com.mythossmp.domain.model.PassiveEffect
import com.mythossmp.domain.model.Vow
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class VowAres : Vow {
    override val key = "ares"
    override val displayName = "Ares"
    override val enabled = true
    override val description = "God of War — Vanguard Berserker"

    override val passiveEffects: List<PassiveEffect> = listOf(passive)
    override val ability1: ActiveAbility = Bloodthorns()
    override val ability2: ActiveAbility = RageAura()
    override val curse: Curse = curseInstance

    companion object {
        val passive = AresPassive()
        val curseInstance = WarHunger()

        fun getRage(playerUuid: UUID): Int = rageMap[playerUuid] ?: 0
        fun getMaxRage(): Int = MAX_RAGE
        fun restoreRage(playerUuid: UUID, rage: Int) {
            rageMap[playerUuid] = rage.coerceIn(0, MAX_RAGE)
        }
    }
}

private val rageMap = ConcurrentHashMap<UUID, Int>()
private const val MAX_RAGE = 100
private val lastCombatTime = ConcurrentHashMap<UUID, Long>()
private const val COMBAT_TIMEOUT_MS = 10_000L

private fun isInCombat(playerUuid: UUID): Boolean {
    val last = lastCombatTime[playerUuid] ?: return false
    return System.currentTimeMillis() - last < COMBAT_TIMEOUT_MS
}

class AresPassive : PassiveEffect, Listener {
    override val key = "ares-passive"
    override val description = "Infinite Strength I + Rage Meter"

    override fun apply(player: Player) {
        player.addPotionEffect(PotionEffect(PotionEffectType.STRENGTH, PotionEffect.INFINITE_DURATION, 0, false, false))
        rageMap[player.uniqueId] = 0
    }

    override fun remove(player: Player) {
        player.removePotionEffect(PotionEffectType.STRENGTH)
        rageMap.remove(player.uniqueId)
    }

    @EventHandler
    fun onDamageDealt(event: EntityDamageByEntityEvent) {
        val damager = event.damager as? Player ?: return
        lastCombatTime[damager.uniqueId] = System.currentTimeMillis()

        val currentRage = rageMap[damager.uniqueId]
        if (currentRage != null) {
            val rageMultiplier = 1.0 + (currentRage / 200.0)

            val bloodthornsMultiplier = run {
                val data = Bloodthorns.activePlayers[damager.uniqueId] ?: return@run 1.0
                val elapsed = System.currentTimeMillis() - data.startTime
                if (elapsed > data.durationMs) {
                    Bloodthorns.activePlayers.remove(damager.uniqueId)
                    return@run 1.0
                }
                val maxHealth = damager.maxHealth
                val healthLost = maxHealth - damager.health
                val chunks = (healthLost / (maxHealth * 0.1)).toInt()
                1.0 + (chunks * 0.05)
            }

            val total = rageMultiplier * bloodthornsMultiplier
            if (total > 1.0) {
                event.damage = event.damage * total
            }
        }

        val newRage = rageMap.compute(damager.uniqueId) { _, current ->
            if (current == null) return@compute null
            (current + event.finalDamage.toInt().coerceAtLeast(0)).coerceIn(0, MAX_RAGE)
        } ?: return

        if (newRage >= MAX_RAGE) {
            damager.sendMessage(Component.text("\u26A1 Rage Meter FULL! Use /ability2 to unleash Rage Aura.", NamedTextColor.RED))
        }
    }

    @EventHandler
    fun onDamageReceived(event: EntityDamageEvent) {
        val entity = event.entity as? Player ?: return
        lastCombatTime[entity.uniqueId] = System.currentTimeMillis()
        val newRage = rageMap.compute(entity.uniqueId) { _, current ->
            if (current == null) return@compute null
            (current + event.finalDamage.toInt().coerceAtLeast(0) * 2).coerceIn(0, MAX_RAGE)
        } ?: return

        if (newRage >= MAX_RAGE) {
            entity.sendMessage(Component.text("\u26A1 Rage Meter FULL! Use /ability2 to unleash Rage Aura.", NamedTextColor.RED))
        }
    }
}

class Bloodthorns : ActiveAbility {
    override val key = "ares-bloodthorns"
    override val displayName = "Bloodthorns"
    override val cooldownSeconds = 120
    override val description = "For 15s, damage scales with health lost. Grants 1-time lethal damage mitigation."

    companion object {
        val activePlayers = ConcurrentHashMap<UUID, BloodthornsData>()
    }

    data class BloodthornsData(
        val startTime: Long = System.currentTimeMillis(),
        val durationMs: Long = 15000L,
        var deathImmunityUsed: Boolean = false
    )

    override fun execute(player: Player, plugin: Any): Boolean {
        activePlayers[player.uniqueId] = BloodthornsData()

        player.sendMessage(Component.text("\u2694 Bloodthorns activated! Damage scales with your missing health.", NamedTextColor.RED))
        MythosSMP.instance.vfxBroadcastService.publish(VowVFXEvent.AresBloodthorns(player.location.clone()))
        return true
    }
}

class RageAura : ActiveAbility {
    override val key = "ares-rage-aura"
    override val displayName = "Rage Aura"
    override val cooldownSeconds = 30
    override val description = "Consumes 100% Rage to deal 12 damage to all untrusted entities in an 8-block radius."

    override fun execute(player: Player, plugin: Any): Boolean {
        val mythosPlugin = plugin as MythosSMP
        val currentRage = rageMap[player.uniqueId] ?: 0

        if (currentRage < MAX_RAGE) {
            player.sendMessage(Component.text("Rage Aura requires 100% Rage! Current: $currentRage%", NamedTextColor.RED))
            return false
        }

        rageMap[player.uniqueId] = 0
        val radius = 8.0

        mythosPlugin.vfxBroadcastService.publish(VowVFXEvent.AresRageAura(player.location.clone(), radius))

        val trusted = mythosPlugin.trustService.getTrusted(player.uniqueId).toSet()

        player.world.getNearbyEntities(player.location, radius, radius, radius)
            .filterIsInstance<LivingEntity>()
            .filter { it != player }
            .forEach { entity ->
                if (entity is Player && trusted.contains(entity.uniqueId)) return@forEach

                entity.damage(12.0, player)
                if (entity is Player) {
                    val p = entity as Player
                    if (p.isBlocking) {
                        p.setCooldown(Material.SHIELD, 100)
                    }
                    p.sendMessage(Component.text("\uD83D\uDCA5 You were hit by ${player.name}'s Rage Aura!", NamedTextColor.RED))
                }
            }

        player.sendMessage(Component.text("\uD83D\uDCA5 Rage Aura unleashed!", NamedTextColor.RED))
        return true
    }
}

class WarHunger : Curse, Listener {
    override val key = "ares-war-hunger"
    override val description = "Food saturation reduced by 25%. Hunger depletes 15% faster during combat."

    private val afflicted = ConcurrentHashMap.newKeySet<UUID>()

    override fun apply(player: Player) {
        afflicted.add(player.uniqueId)
    }

    override fun remove(player: Player) {
        afflicted.remove(player.uniqueId)
    }

    @EventHandler
    fun onFoodLevelChange(event: FoodLevelChangeEvent) {
        val player = event.entity as? Player ?: return
        if (!afflicted.contains(player.uniqueId)) return

        val oldLevel = player.foodLevel
        val newLevel = event.foodLevel
        if (newLevel < oldLevel && isInCombat(player.uniqueId)) {
            val diff = oldLevel - newLevel
            val increasedDiff = (diff * 1.15).toInt().coerceAtLeast(1)
            event.foodLevel = (oldLevel - increasedDiff).coerceAtLeast(0)
        }
    }

    @EventHandler
    fun onPlayerConsume(event: PlayerItemConsumeEvent) {
        val player = event.player
        if (!afflicted.contains(player.uniqueId)) return
        if (!event.item.type.isEdible) return

        MythosSMP.instance.scheduler.later(java.time.Duration.ofMillis(50)) {
            if (!player.isOnline) return@later
            val reduced = player.saturation * 0.75f
            player.saturation = reduced.coerceAtLeast(0.0f)
        }    }
}

class AresListeners : Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    fun onBloodthornsSave(event: EntityDamageEvent) {
        val player = event.entity as? Player ?: return
        val data = Bloodthorns.activePlayers[player.uniqueId] ?: return
        if (System.currentTimeMillis() - data.startTime > data.durationMs) {
            Bloodthorns.activePlayers.remove(player.uniqueId)
            return
        }
        if (data.deathImmunityUsed) return

        val newHealth = player.health - event.finalDamage
        if (newHealth <= 0.0) {
            event.isCancelled = true
            data.deathImmunityUsed = true
            player.health = 1.0
            player.setNoDamageTicks(60)
            player.sendMessage(Component.text("\uD83D\uDEE1 Bloodthorns saved you from lethal damage! You have 3 seconds before the vow exacts its price.", NamedTextColor.RED))
            MythosSMP.instance.scheduler.later(java.time.Duration.ofSeconds(3)) {
                if (!player.isOnline) return@later
                player.setNoDamageTicks(0)
                val profile = MythosSMP.instance.profileService.getProfile(player.uniqueId)
                if (profile?.activeVow != null) {
                    player.damage(100.0)
                }
            }
        }
    }
}
