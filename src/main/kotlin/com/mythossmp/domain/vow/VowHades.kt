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
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.EntityRegainHealthEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

private val soulChargesMap = ConcurrentHashMap<UUID, Int>()
private val deathDefianceArmed = ConcurrentHashMap.newKeySet<UUID>()
private val combatTimestamps = ConcurrentHashMap<UUID, Long>()
private const val COMBAT_TIMEOUT_MS = 10_000L

private fun isInCombat(playerUuid: UUID): Boolean {
    val last = combatTimestamps[playerUuid] ?: return false
    return System.currentTimeMillis() - last < COMBAT_TIMEOUT_MS
}

class VowHades : Vow {
    override val key = "hades"
    override val displayName = "Hades"
    override val enabled = true
    override val description = "God of the Underworld — Soul Reaper"

    override val passiveEffects: List<PassiveEffect> = listOf(passive)
    override val ability1: ActiveAbility = DeathDefiance()
    override val ability2: ActiveAbility = SoulBurst()
    override val curse: Curse = curseInstance

    companion object {
        val passive = SoulHarvest()
        val curseInstance = SoulTether()

        fun getCharges(playerUuid: UUID): Int = soulChargesMap[playerUuid] ?: 0
        fun getMaxCharges(): Int {
            val cfg = MythosSMP.instance.configManager.getVowConfig("hades")
            return cfg.maxCharges
        }
    }
}

class SoulHarvest : PassiveEffect, Listener {
    override val key = "hades-soul-harvest"
    override val description = "Kills grant Soul Charges. Each charge increases outgoing damage."

    override fun apply(player: Player) {
        soulChargesMap[player.uniqueId] = 0
    }

    override fun remove(player: Player) {
        soulChargesMap.remove(player.uniqueId)
        deathDefianceArmed.remove(player.uniqueId)
    }

    @EventHandler
    fun onEntityDeath(event: EntityDeathEvent) {
        val killer = event.entity.killer ?: return
        val plugin = MythosSMP.instance
        val profile = plugin.profileService.getProfile(killer.uniqueId) ?: return
        if (profile.activeVow?.removePrefix("vow-") != "hades") return

        val cfg = plugin.configManager.getVowConfig("hades")
        if (!cfg.enabled) return

        soulChargesMap.compute(killer.uniqueId) { _, current ->
            val cur = current ?: 0
            if (cur >= cfg.maxCharges) return@compute cur
            val newCount = cur + 1
            killer.sendMessage(Component.text("\uD83D\uDC7B Soul harvested! Charges: $newCount/${cfg.maxCharges}", NamedTextColor.DARK_GREEN))
            plugin.vfxBroadcastService.publish(VowVFXEvent.HadesSoulHarvest(killer.location.clone()))
            newCount
        }
    }

    @EventHandler
    fun onDamageDealt(event: EntityDamageByEntityEvent) {
        if (event.isCancelled) return
        val damager = event.damager as? Player ?: return
        val plugin = MythosSMP.instance
        val profile = plugin.profileService.getProfile(damager.uniqueId) ?: return
        if (profile.activeVow?.removePrefix("vow-") != "hades") return

        val cfg = plugin.configManager.getVowConfig("hades")
        if (!cfg.enabled) return

        combatTimestamps[damager.uniqueId] = System.currentTimeMillis()

        val charges = soulChargesMap[damager.uniqueId] ?: 0
        if (charges > 0) {
            val multiplier = 1.0 + (charges * cfg.damageBonusPerCharge)
            event.damage = event.damage * multiplier
            plugin.vfxBroadcastService.publish(VowVFXEvent.HadesSoulDamage(event.entity.location.clone()))
        }
    }

    @EventHandler
    fun onDamageReceived(event: EntityDamageEvent) {
        val entity = event.entity as? Player ?: return
        val plugin = MythosSMP.instance
        val profile = plugin.profileService.getProfile(entity.uniqueId) ?: return
        if (profile.activeVow?.removePrefix("vow-") != "hades") return
        combatTimestamps[entity.uniqueId] = System.currentTimeMillis()
    }
}

class DeathDefiance : ActiveAbility {
    override val key = "hades-death-defiance"
    override val displayName = "Death Defiance"
    override val cooldownSeconds = 90
    override val description = "Arm yourself against death. If you have max Soul Charges when taking fatal damage, consume all charges to survive with 1 HP."

    override fun execute(player: Player, plugin: Any): Boolean {
        val mythosPlugin = plugin as MythosSMP
        val cfg = mythosPlugin.configManager.getVowConfig("hades")
        val charges = soulChargesMap[player.uniqueId] ?: 0

        if (charges < cfg.maxCharges) {
            player.sendMessage(Component.text(
                "Death Defiance requires max Soul Charges ($charges/${cfg.maxCharges}).",
                NamedTextColor.RED
            ))
            return false
        }

        deathDefianceArmed.add(player.uniqueId)
        player.sendMessage(Component.text(
            "\u2620 Death Defiance armed! Fatal damage will consume all charges to spare your life.",
            NamedTextColor.DARK_GRAY
        ))
        mythosPlugin.vfxBroadcastService.publish(VowVFXEvent.HadesDeathDefianceArm(player.location.clone()))
        return true
    }
}

class SoulBurst : ActiveAbility {
    override val key = "hades-soul-burst"
    override val displayName = "Soul Burst"
    override val cooldownSeconds = 30
    override val description = "Consume all Soul Charges to deal AoE damage to nearby enemies."

    override fun execute(player: Player, plugin: Any): Boolean {
        val mythosPlugin = plugin as MythosSMP
        val cfg = mythosPlugin.configManager.getVowConfig("hades")
        val charges = soulChargesMap[player.uniqueId] ?: 0

        if (charges <= 0) {
            player.sendMessage(Component.text("No Soul Charges to burst!", NamedTextColor.RED))
            return false
        }

        soulChargesMap[player.uniqueId] = 0

        val dmg = cfg.soulBurstBaseDamage * charges
        val radius = cfg.soulBurstRange
        val trusted = mythosPlugin.trustService.getTrusted(player.uniqueId).toSet()

        mythosPlugin.vfxBroadcastService.publish(VowVFXEvent.HadesSoulBurst(player.location.clone(), radius))

        player.world.getNearbyEntities(player.location, radius, radius, radius)
            .filterIsInstance<LivingEntity>()
            .filter { it != player }
            .forEach { entity ->
                if (entity is Player && trusted.contains(entity.uniqueId)) return@forEach
                entity.damage(dmg, player)
                if (entity is Player) {
                    entity.sendMessage(Component.text(
                        "\uD83D\uDCA5 You were hit by ${player.name}'s Soul Burst!",
                        NamedTextColor.DARK_GRAY
                    ))
                }
            }

        player.sendMessage(Component.text(
            "\uD83D\uDCA5 Soul Burst unleashed! $charges charges consumed for $dmg damage.",
            NamedTextColor.DARK_GREEN
        ))
        return true
    }
}

class SoulTether : Curse, Listener {
    override val key = "hades-soul-tether"
    override val description = "Natural health regeneration is disabled while in combat."

    private val afflicted = ConcurrentHashMap.newKeySet<UUID>()

    override fun apply(player: Player) {
        afflicted.add(player.uniqueId)
    }

    override fun remove(player: Player) {
        afflicted.remove(player.uniqueId)
    }

    @EventHandler
    fun onRegainHealth(event: EntityRegainHealthEvent) {
        val player = event.entity as? Player ?: return
        if (!afflicted.contains(player.uniqueId)) return
        if (event.regainReason == EntityRegainHealthEvent.RegainReason.SATIATED && isInCombat(player.uniqueId)) {
            event.isCancelled = true
        }
    }
}

class HadesListener : Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    fun onDeathDefianceSave(event: EntityDamageEvent) {
        val player = event.entity as? Player ?: return
        if (!deathDefianceArmed.contains(player.uniqueId)) return

        val plugin = MythosSMP.instance
        val cfg = plugin.configManager.getVowConfig("hades")

        val charges = soulChargesMap[player.uniqueId] ?: 0
        if (charges < cfg.maxCharges) {
            deathDefianceArmed.remove(player.uniqueId)
            return
        }

        val newHealth = player.health - event.finalDamage
        if (newHealth <= 0.0) {
            event.isCancelled = true
            deathDefianceArmed.remove(player.uniqueId)
            soulChargesMap[player.uniqueId] = 0

            player.health = 1.0
            player.setNoDamageTicks(60)
            player.addPotionEffect(PotionEffect(PotionEffectType.SPEED, 60, 1, false, false, true))
            player.addPotionEffect(PotionEffect(PotionEffectType.RESISTANCE, 60, 0, false, false, true))

            plugin.vfxBroadcastService.publish(VowVFXEvent.HadesDeathDefianceSave(player.location.clone()))

            player.sendMessage(Component.text(
                "\u2620 Death Defiance saved you! Your soul charges have been consumed.",
                NamedTextColor.DARK_GRAY
            ))
        }
    }
}
