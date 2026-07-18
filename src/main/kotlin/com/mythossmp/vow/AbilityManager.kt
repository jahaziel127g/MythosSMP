package com.mythossmp.vow

import com.mythossmp.MythosSMP
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player

class AbilityManager(
    private val plugin: MythosSMP,
    private val vowRegistry: VowRegistry,
    private val cooldownManager: CooldownManager
) {

    fun executeAbility(player: Player, abilityNumber: Int) {
        if (!player.hasPermission("mythossmp.ability")) {
            player.sendMessage(Component.text("You do not have permission to use abilities.", NamedTextColor.RED))
            return
        }

        val vow = vowRegistry.getActiveVowForPlayer(player.uniqueId)
        if (vow == null) {
            player.sendMessage(Component.text("You do not have an active Vow.", NamedTextColor.RED))
            return
        }

        val ability = when (abilityNumber) {
            1 -> vow.ability1
            2 -> vow.ability2
            else -> {
                player.sendMessage(Component.text("Invalid ability number. Use 1 or 2.", NamedTextColor.RED))
                return
            }
        }

        if (cooldownManager.isOnCooldown(player, ability.key)) {
            cooldownManager.sendOnCooldownMessage(player, ability.displayName)
            return
        }

        ability.execute(player, plugin)

        cooldownManager.setCooldown(player, ability.key, ability.cooldownSeconds)
    }

    fun sendAbilityInfo(player: Player) {
        val vow = vowRegistry.getActiveVowForPlayer(player.uniqueId)
        if (vow == null) {
            player.sendMessage(Component.text("You do not have an active Vow.", NamedTextColor.RED))
            return
        }

        player.sendMessage(Component.text()
            .append(Component.text("=== ", NamedTextColor.DARK_AQUA))
            .append(Component.text(vow.displayName, NamedTextColor.GOLD))
            .append(Component.text(" Abilities ===", NamedTextColor.DARK_AQUA))
            .build()
        )

        val cd1 = cooldownManager.getRemainingSeconds(player, vow.ability1.key)
        val cd2 = cooldownManager.getRemainingSeconds(player, vow.ability2.key)

        player.sendMessage(Component.text()
            .append(Component.text("1. ", NamedTextColor.GRAY))
            .append(Component.text(vow.ability1.displayName, NamedTextColor.AQUA))
            .append(Component.text(" (${vow.ability1.cooldownSeconds}s cd", NamedTextColor.DARK_GRAY))
            .append(if (cd1 > 0) Component.text(" - ${cd1}s remaining", NamedTextColor.RED) else Component.empty())
            .append(Component.text(")", NamedTextColor.DARK_GRAY))
            .build()
        )

        player.sendMessage(Component.text()
            .append(Component.text("2. ", NamedTextColor.GRAY))
            .append(Component.text(vow.ability2.displayName, NamedTextColor.AQUA))
            .append(Component.text(" (${vow.ability2.cooldownSeconds}s cd", NamedTextColor.DARK_GRAY))
            .append(if (cd2 > 0) Component.text(" - ${cd2}s remaining", NamedTextColor.RED) else Component.empty())
            .append(Component.text(")", NamedTextColor.DARK_GRAY))
            .build()
        )
    }
}
