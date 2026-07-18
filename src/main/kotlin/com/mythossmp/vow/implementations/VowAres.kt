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
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
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

    override val passiveEffects: List<PassiveEffect> = listOf(AresPassive())
    override val ability1: ActiveAbility = Bloodthorns()
    override val ability2: ActiveAbility = RageAura()
    override val curse: Curse = WarHunger()
}

private val rageMap = ConcurrentHashMap<UUID, Int>()
private val MAX_RAGE = 100

data class RageData(
    var rage: Int = 0,
    var ability2Available: Boolean = false
)

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
        if (!rageMap.containsKey(damager.uniqueId)) return
        val current = rageMap[damager.uniqueId] ?: return
        val damage = event.finalDamage.toInt()
        val newRage = (current + damage).coerceAtMost(MAX_RAGE)
        rageMap[damager.uniqueId] = newRage

        if (newRage >= MAX_RAGE) {
            damager.sendMessage(Component.text("\u26A1 Rage Meter FULL! Use /ability2 to unleash Rage Aura.", NamedTextColor.RED))
        }
    }

    @EventHandler
    fun onDamageReceived(event: EntityDamageEvent) {
        val entity = event.entity as? Player ?: return
        if (!rageMap.containsKey(entity.uniqueId)) return
        val current = rageMap[entity.uniqueId] ?: return
        val damage = event.finalDamage.toInt()
        val newRage = (current + damage * 2).coerceAtMost(MAX_RAGE)
        rageMap[entity.uniqueId] = newRage

        if (newRage >= MAX_RAGE) {
            entity.sendMessage(Component.text("\u26A1 Rage Meter FULL! Use /ability2 to unleash Rage Aura.", NamedTextColor.RED))
        }
    }

    private fun getDamageBonus(rage: Int): Double {
        return (rage.toDouble() / MAX_RAGE) * 0.5
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

        player.world.getNearbyEntities(player.location, radius, radius, radius)
            .filterIsInstance<LivingEntity>()
            .filter { it != player }
            .forEach { entity ->
                if (entity is Player && isTrusted(player, entity, mythosPlugin)) return@forEach

                entity.damage(12.0, player)
                if (entity is Player) {
                    entity.sendMessage(Component.text("\uD83D\uDCA5 You were hit by ${player.name}'s Rage Aura!", NamedTextColor.RED))
                }
            }

        player.sendMessage(Component.text("\uD83D\uDCA5 Rage Aura unleashed!", NamedTextColor.RED))
    }

    private fun isTrusted(player: Player, target: Player, plugin: MythosSMP): Boolean {
        return try {
            plugin.databaseManager.getConnection().use { conn ->
                conn.prepareStatement(
                    "SELECT 1 FROM mythos_trusts WHERE player_uuid = ? AND trusted_uuid = ?"
                ).use { stmt ->
                    stmt.setString(1, player.uniqueId.toString())
                    stmt.setString(2, target.uniqueId.toString())
                    stmt.executeQuery().use { rs -> rs.next() }
                }
            }
        } catch (_: Exception) {
            false
        }
    }
}

class WarHunger : Curse {
    override val key = "ares-war-hunger"
    override val description = "Food saturation reduced by 25%. Hunger depletes 15% faster during combat."

    override fun apply(player: Player) {}

    override fun remove(player: Player) {}
}
