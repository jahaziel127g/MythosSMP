package com.mythossmp.vow

import com.mythossmp.MythosSMP
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.title.Title
import org.bukkit.entity.Player
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class CooldownManager(private val plugin: MythosSMP) {

    private val cooldowns: ConcurrentHashMap<UUID, ConcurrentHashMap<String, Instant>> = ConcurrentHashMap()

    fun isOnCooldown(player: Player, abilityKey: String): Boolean {
        val playerCooldowns = cooldowns[player.uniqueId] ?: return false
        val expiry = playerCooldowns[abilityKey.lowercase()] ?: return false
        return Instant.now().isBefore(expiry)
    }

    fun getRemainingSeconds(player: Player, abilityKey: String): Long {
        val playerCooldowns = cooldowns[player.uniqueId] ?: return 0
        val expiry = playerCooldowns[abilityKey.lowercase()] ?: return 0
        val remaining = Duration.between(Instant.now(), expiry).seconds
        return maxOf(0, remaining)
    }

    fun setCooldown(player: Player, abilityKey: String, seconds: Int) {
        val adjustedSeconds = applyGlobalMultiplier(player, seconds)
        val expiry = Instant.now().plusSeconds(adjustedSeconds.toLong())
        cooldowns.computeIfAbsent(player.uniqueId) { ConcurrentHashMap() }
            .put(abilityKey.lowercase(), expiry)
    }

    private fun applyGlobalMultiplier(player: Player, baseSeconds: Int): Int {
        val profile = plugin.playerDataManager.getProfile(player.uniqueId)
        val multiplier = profile?.cooldownMultiplier ?: 1.0
        return (baseSeconds * multiplier).toInt().coerceAtLeast(1)
    }

    fun sendCooldownActionBar(player: Player, abilityKey: String, remaining: Long) {
        val msg = Component.text()
            .append(Component.text("\u23F1 ", NamedTextColor.GRAY))
            .append(Component.text(abilityKey.replaceFirstChar { it.uppercase() }, NamedTextColor.RED))
            .append(Component.text(" cooldown: ", NamedTextColor.GRAY))
            .append(Component.text("${remaining}s", NamedTextColor.YELLOW))
            .build()
        player.sendActionBar(msg)
    }

    fun sendOnCooldownMessage(player: Player, abilityKey: String) {
        val remaining = getRemainingSeconds(player, abilityKey)
        player.sendMessage(Component.text()
            .append(Component.text("\u26A0 ", NamedTextColor.RED))
            .append(Component.text(abilityKey, NamedTextColor.GOLD))
            .append(Component.text(" is on cooldown for ", NamedTextColor.RED))
            .append(Component.text("${remaining}s", NamedTextColor.YELLOW))
            .append(Component.text(".", NamedTextColor.RED))
            .build()
        )
    }

    fun removePlayer(player: Player) {
        cooldowns.remove(player.uniqueId)
    }
}
