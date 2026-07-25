package com.mythossmp.presentation.listener

import com.mythossmp.application.union.UnionService
import com.mythossmp.domain.event.DoubleLossEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

class UnionListener(
    private val unionService: UnionService
) : Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val player = event.player
        val result = unionService.handlePlayerDeath(player.uniqueId)
        if (result != null) {
            broadcastDoubleLoss(result, player)
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        if (unionService.isInUnion(player.uniqueId)) {
            unionService.markDisconnected(player.uniqueId)
            player.sendMessage(
                Component.text("You disconnected while in a Seal of Union.", NamedTextColor.RED)
            )
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        if (unionService.isDisconnectedFromUnion(event.player.uniqueId)) {
            unionService.clearDisconnected(event.player.uniqueId)
        }
    }

    private fun broadcastDoubleLoss(event: DoubleLossEvent, deathPlayer: Player) {
        val partnerName = deathPlayer.server.getOfflinePlayer(event.partnerUuid).name ?: event.partnerUuid.toString().take(8)
        val message = Component.text()
            .color(NamedTextColor.DARK_RED)
            .append(Component.text("\u2620 DOUBLE LOSS: "))
            .append(Component.text(deathPlayer.name, NamedTextColor.RED))
            .append(Component.text(" died while in a Seal of Union with "))
            .append(Component.text(partnerName, NamedTextColor.RED))
            .append(Component.text(". Both players have lost their Vows!"))
            .build()

        deathPlayer.server.onlinePlayers.forEach { p ->
            p.sendMessage(message)
        }
    }
}
