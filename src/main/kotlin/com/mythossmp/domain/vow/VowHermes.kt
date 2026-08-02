package com.mythossmp.domain.vow

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent
import com.mythossmp.MythosSMP
import com.mythossmp.domain.event.VowVFXEvent
import com.mythossmp.domain.model.ActiveAbility
import com.mythossmp.domain.model.Curse
import com.mythossmp.domain.model.PassiveEffect
import com.mythossmp.domain.model.Vow
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerToggleFlightEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.time.Duration
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class VowHermes : Vow {
    override val key = "hermes"
    override val displayName = "Hermes"
    override val enabled = true
    override val description = "God of Travel — Hyper-Mobility Scout"

    override val passiveEffects: List<PassiveEffect> = listOf(HermesPassive())
    override val ability1: ActiveAbility = Dash()
    override val ability2: ActiveAbility = ZephyrWing()
    override val curse: Curse = curseInstance

    companion object {
        val curseInstance = LightConstitution()
        val djLastUsed = ConcurrentHashMap<UUID, Long>()
        val zephyrPlayers = ConcurrentHashMap.newKeySet<UUID>()

        fun getDoubleJumpRemainingSeconds(playerUuid: UUID): Long {
            val last = djLastUsed[playerUuid] ?: return 0
            val cooldownMs = MythosSMP.instance.configManager.getHermesConfig().doubleJumpCooldownTicks * 50L
            val remaining = cooldownMs - (System.currentTimeMillis() - last)
            return if (remaining > 0) (remaining + 999) / 1000 else 0
        }
    }
}

private fun hasHermes(playerUuid: UUID): Boolean {
    val plugin = MythosSMP.instance
    val profile = plugin.profileService.getProfile(playerUuid) ?: return false
    if (profile.activeVow?.removePrefix("vow-") != "hermes") return false
    return plugin.configManager.getHermesConfig().enabled
}

class HermesPassive : PassiveEffect {
    override val key = "hermes-passive"
    override val description = "Double jump (15s cooldown) + immunity to fall damage"

    override fun apply(player: Player) {
        player.setAllowFlight(true)
    }

    override fun remove(player: Player) {
        player.setAllowFlight(false)
        player.setFlying(false)
        VowHermes.djLastUsed.remove(player.uniqueId)
        if (VowHermes.zephyrPlayers.remove(player.uniqueId)) {
            player.setFlying(false)
            player.setAllowFlight(false)
        }
    }
}

class HermesListener : Listener {

    @EventHandler
    fun onToggleFlight(event: PlayerToggleFlightEvent) {
        val player = event.player
        if (!hasHermes(player.uniqueId)) return

        event.isCancelled = true

        if (player.isFlying) {
            if (VowHermes.zephyrPlayers.contains(player.uniqueId)) return
            player.setFlying(false)
            return
        }

        if (VowHermes.zephyrPlayers.contains(player.uniqueId)) {
            player.setFlying(true)
            return
        }

        val cfg = MythosSMP.instance.configManager.getHermesConfig()
        val cooldownMs = cfg.doubleJumpCooldownTicks * 50L
        val now = System.currentTimeMillis()
        val last = VowHermes.djLastUsed[player.uniqueId] ?: 0
        if (now - last < cooldownMs) {
            player.sendMessage(Component.text("Double jump recharging...", NamedTextColor.GRAY))
            return
        }

        VowHermes.djLastUsed[player.uniqueId] = now
        player.setVelocity(player.location.direction.multiply(0.7).setY(0.55))
        MythosSMP.instance.vfxBroadcastService.publish(
            VowVFXEvent.HermesDoubleJump(player.location.clone())
        )
    }

    @EventHandler
    fun onFallDamage(event: EntityDamageEvent) {
        if (event.cause != EntityDamageEvent.DamageCause.FALL) return
        val player = event.entity as? Player ?: return
        if (hasHermes(player.uniqueId)) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onZephyrDamage(event: EntityDamageEvent) {
        val player = event.entity as? Player ?: return
        if (!VowHermes.zephyrPlayers.remove(player.uniqueId)) return

        player.setFlying(false)
        player.setAllowFlight(false)
        player.addPotionEffect(PotionEffect(PotionEffectType.SLOW_FALLING, 20, 0, false, false))
        player.sendMessage(Component.text("\uD83C\uDF43 Zephyr Wing interrupted! Slow falling...", NamedTextColor.GRAY))
    }
}

class Dash : ActiveAbility {
    override val key = "hermes-dash"
    override val displayName = "Dash"
    override val cooldownSeconds = 6
    override val description = "Instantly launch forward 12 blocks."

    override fun execute(player: Player, plugin: Any): Boolean {
        val mythosPlugin = plugin as MythosSMP
        val cfg = mythosPlugin.configManager.getHermesConfig()
        val velocity = player.location.direction.multiply(cfg.dashDistance / 6.0).setY(0.2)

        player.setVelocity(velocity)
        mythosPlugin.vfxBroadcastService.publish(
            VowVFXEvent.HermesDash(player.location.clone())
        )
        player.sendMessage(Component.text("\u26A1 Dash!", NamedTextColor.YELLOW))
        return true
    }
}

class ZephyrWing : ActiveAbility {
    override val key = "hermes-zephyr-wing"
    override val displayName = "Zephyr Wing"
    override val cooldownSeconds = 150
    override val description = "Gain flight for 8 seconds. Damaged while flying cancels it."

    override fun execute(player: Player, plugin: Any): Boolean {
        val mythosPlugin = plugin as MythosSMP
        val cfg = mythosPlugin.configManager.getHermesConfig()

        VowHermes.zephyrPlayers.add(player.uniqueId)
        player.setAllowFlight(true)
        player.setFlying(true)
        mythosPlugin.vfxBroadcastService.publish(
            VowVFXEvent.HermesZephyrActivate(player.location.clone())
        )
        player.sendMessage(Component.text("\uD83C\uDF43 Zephyr Wing grants you flight!", NamedTextColor.YELLOW))

        MythosSMP.instance.scheduler.later(Duration.ofSeconds(cfg.zephyrDuration.toLong())) {
            if (!player.isOnline) return@later
            if (VowHermes.zephyrPlayers.remove(player.uniqueId)) {
                player.setFlying(false)
                player.setAllowFlight(false)
                player.sendMessage(Component.text("Zephyr Wing fades...", NamedTextColor.GRAY))
            }
        }
        return true
    }
}

class LightConstitution : Curse, Listener {
    override val key = "hermes-light-constitution"
    override val description = "Cannot wear armor heavier than chainmail — heavy armor is rejected (4 damage)."

    private val afflicted = ConcurrentHashMap.newKeySet<UUID>()

    override fun apply(player: Player) {
        afflicted.add(player.uniqueId)
        val inv = player.inventory
        val heavy = listOf(
            inv.helmet, inv.chestplate, inv.leggings, inv.boots
        ).filterNotNull().filter { isHeavy(it.type) }

        inv.helmet = null
        inv.chestplate = null
        inv.leggings = null
        inv.boots = null

        heavy.forEach { rejectArmor(player, it) }
    }

    override fun remove(player: Player) {
        afflicted.remove(player.uniqueId)
    }

    @EventHandler
    fun onArmorChange(event: PlayerArmorChangeEvent) {
        val player = event.player
        if (!afflicted.contains(player.uniqueId)) return
        val newItem = event.newItem ?: return
        if (!isHeavy(newItem.type)) return

        when (event.slotType) {
            PlayerArmorChangeEvent.SlotType.HEAD -> player.inventory.helmet = null
            PlayerArmorChangeEvent.SlotType.CHEST -> player.inventory.chestplate = null
            PlayerArmorChangeEvent.SlotType.LEGS -> player.inventory.leggings = null
            PlayerArmorChangeEvent.SlotType.FEET -> player.inventory.boots = null
            else -> return
        }
        rejectArmor(player, newItem)
    }

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        val player = event.player
        if (!afflicted.contains(player.uniqueId)) return
        val item = event.item ?: return
        if (!isHeavy(item.type)) return

        event.isCancelled = true
        player.damage(MythosSMP.instance.configManager.getHermesConfig().rejectionDamage)
        player.sendMessage(Component.text("\uD83D\uDEE1 Too heavy! Your vow forbids this armor.", NamedTextColor.RED))
    }

    private fun rejectArmor(player: Player, item: org.bukkit.inventory.ItemStack) {
        val cfg = MythosSMP.instance.configManager.getHermesConfig()
        player.world.dropItem(player.location, item)
        player.damage(cfg.rejectionDamage)
        player.sendMessage(Component.text("\uD83D\uDEE1 Your vow rejects the heavy armor!", NamedTextColor.RED))
    }

    private fun isHeavy(type: Material): Boolean {
        val name = type.name
        return (name.startsWith("IRON_") || name.startsWith("DIAMOND_") || name.startsWith("NETHERITE_")) &&
            (name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE") || name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS"))
    }
}
