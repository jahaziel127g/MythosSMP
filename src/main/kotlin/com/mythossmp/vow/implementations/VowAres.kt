package com.mythossmp.vow.implementations

import com.mythossmp.MythosSMP
import com.mythossmp.model.ActiveAbility
import com.mythossmp.model.Curse
import com.mythossmp.model.PassiveEffect
import com.mythossmp.model.Vow
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Color
import org.bukkit.Particle
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.FoodLevelChangeEvent
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
    override val curse: Curse = WarHunger()

    companion object {
        val passive = AresPassive()
    }
}

private val rageMap = ConcurrentHashMap<UUID, Int>()
private const val MAX_RAGE = 100

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
        val newRage = rageMap.compute(damager.uniqueId) { _, current ->
            if (current == null) return@compute null
            (current + event.finalDamage.toInt()).coerceAtMost(MAX_RAGE)
        } ?: return

        if (newRage >= MAX_RAGE) {
            damager.sendMessage(Component.text("\u26A1 Rage Meter FULL! Use /ability2 to unleash Rage Aura.", NamedTextColor.RED))
        }
    }

    @EventHandler
    fun onDamageReceived(event: EntityDamageEvent) {
        val entity = event.entity as? Player ?: return
        val newRage = rageMap.compute(entity.uniqueId) { _, current ->
            if (current == null) return@compute null
            (current + event.finalDamage.toInt() * 2).coerceAtMost(MAX_RAGE)
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

    override fun execute(player: Player, plugin: Any) {
        activePlayers[player.uniqueId] = BloodthornsData()

        player.sendMessage(Component.text("\u2694 Bloodthorns activated! Damage scales with your missing health.", NamedTextColor.RED))
        player.world.spawnParticle(
            Particle.DUST,
            player.location.add(0.0, 1.0, 0.0),
            50, 0.5, 0.5, 0.5, 0.1,
            Particle.DustOptions(Color.fromRGB(0xFF0000), 1.5f)
        )
    }
}

class RageAura : ActiveAbility {
    override val key = "ares-rage-aura"
    override val displayName = "Rage Aura"
    override val cooldownSeconds = 30
    override val description = "Consumes 100% Rage to deal 12 damage to all untrusted entities in an 8-block radius."

    override fun execute(player: Player, plugin: Any) {
        val mythosPlugin = plugin as MythosSMP
        val currentRage = rageMap[player.uniqueId] ?: 0

        if (currentRage < MAX_RAGE) {
            player.sendMessage(Component.text("Rage Aura requires 100% Rage! Current: $currentRage%", NamedTextColor.RED))
            return
        }

        rageMap[player.uniqueId] = 0
        val radius = 8.0

        player.world.spawnParticle(
            Particle.DUST,
            player.location,
            200, radius, 0.5, radius, 0.2,
            Particle.DustOptions(Color.fromRGB(0xFF0000), 2.0f)
        )

        val trusted = mythosPlugin.databaseManager.getConnection().use { conn ->
            conn.prepareStatement(
                "SELECT trusted_uuid FROM mythos_trusts WHERE player_uuid = ?"
            ).use { stmt ->
                stmt.setString(1, player.uniqueId.toString())
                stmt.executeQuery().use { rs ->
                    val set = mutableSetOf<UUID>()
                    while (rs.next()) set.add(UUID.fromString(rs.getString("trusted_uuid")))
                    set
                }
            }
        }

        player.world.getNearbyEntities(player.location, radius, radius, radius)
            .filterIsInstance<LivingEntity>()
            .filter { it != player }
            .forEach { entity ->
                if (entity is Player && trusted.contains(entity.uniqueId)) return@forEach

                entity.damage(12.0, player)
                if (entity is Player) {
                    entity.sendMessage(Component.text("\uD83D\uDCA5 You were hit by ${player.name}'s Rage Aura!", NamedTextColor.RED))
                }
            }

        player.sendMessage(Component.text("\uD83D\uDCA5 Rage Aura unleashed!", NamedTextColor.RED))
    }
}

class WarHunger : Curse {
    override val key = "ares-war-hunger"
    override val description = "Food saturation reduced by 25%. Hunger depletes 15% faster during combat."

    private val afflicted = ConcurrentHashMap.newKeySet<UUID>()

    override fun apply(player: Player) {
        afflicted.add(player.uniqueId)
    }

    override fun remove(player: Player) {
        afflicted.remove(player.uniqueId)
    }
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
            player.sendMessage(Component.text("\uD83D\uDEE1 Bloodthorns saved you! You have 3 seconds of immunity.", NamedTextColor.RED))
            org.bukkit.Bukkit.getScheduler().runTaskLater(
                MythosSMP.instance,
                Runnable {
                    val profile = MythosSMP.instance.playerDataManager.getProfile(player.uniqueId)
                    if (profile?.activeVow != null) {
                        player.damage(100.0)
                    }
                },
                60L
            )
        }
    }
}
