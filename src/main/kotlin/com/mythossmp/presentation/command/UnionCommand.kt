package com.mythossmp.presentation.command

import com.mythossmp.MythosSMP
import com.mythossmp.domain.model.SealOfUnion
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import java.time.Duration
import java.time.Instant

class UnionCommand(private val plugin: MythosSMP) : CommandExecutor, TabCompleter {

    private val miniMessage = MiniMessage.miniMessage()

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED))
            return true
        }

        if (args.isEmpty()) {
            sendUsage(sender)
            return true
        }

        when (args[0].lowercase()) {
            "create" -> handleCreate(sender, args)
            "break" -> handleBreak(sender)
            "info" -> handleInfo(sender)
            else -> sendUsage(sender)
        }

        return true
    }

    private fun handleCreate(player: Player, args: Array<out String>) {
        if (args.size < 2) {
            player.sendMessage(Component.text("Usage: /union create <player>", NamedTextColor.RED))
            return
        }

        if (plugin.unionService.isInUnion(player.uniqueId)) {
            player.sendMessage(Component.text("You are already in a Seal of Union.", NamedTextColor.RED))
            return
        }

        val target = Bukkit.getPlayerExact(args[1])
        if (target == null) {
            player.sendMessage(Component.text("Player not found.", NamedTextColor.RED))
            return
        }

        if (target.uniqueId == player.uniqueId) {
            player.sendMessage(Component.text("You cannot create a union with yourself.", NamedTextColor.RED))
            return
        }

        if (plugin.unionService.isInUnion(target.uniqueId)) {
            player.sendMessage(Component.text("That player is already in a Seal of Union.", NamedTextColor.RED))
            return
        }

        val union = plugin.unionService.createUnion(player.uniqueId, target.uniqueId)
        if (union != null) {
            player.sendMessage(miniMessage.deserialize(
                "<gold>Seal of Union</gold> <green>created with </green><gold>${target.name}</gold><green>.</green>"
            ))
            target.sendMessage(miniMessage.deserialize(
                "<gold>Seal of Union</gold> <green>created with </green><gold>${player.name}</gold><green>.</green>"
            ))
        } else {
            player.sendMessage(Component.text("Failed to create Seal of Union.", NamedTextColor.RED))
        }
    }

    private fun handleBreak(player: Player) {
        val union = plugin.unionService.getUnion(player.uniqueId)
        if (union == null) {
            player.sendMessage(Component.text("You are not in a Seal of Union.", NamedTextColor.RED))
            return
        }

        val partnerName = Bukkit.getOfflinePlayer(union.partnerOf(player.uniqueId)!!).name ?: "Unknown"
        plugin.unionService.breakUnion(player.uniqueId, "player_command")

        player.sendMessage(miniMessage.deserialize(
            "<red>Seal of Union with </red><gold>$partnerName</gold> <red>has been broken.</red>"
        ))
    }

    private fun handleInfo(player: Player) {
        val union = plugin.unionService.getUnion(player.uniqueId)
        if (union == null) {
            player.sendMessage(Component.text("You are not in a Seal of Union.", NamedTextColor.GRAY))
            return
        }

        val partnerUuid = union.partnerOf(player.uniqueId)!!
        val partnerName = Bukkit.getOfflinePlayer(partnerUuid).name ?: partnerUuid.toString().take(8)
        val age = Duration.between(union.createdAt, Instant.now())

        player.sendMessage(miniMessage.deserialize(
            """
            <dark_aqua>=== Seal of Union ===</dark_aqua>
            <gray>Partner:</gray> <white>$partnerName</white>
            <gray>Formed:</gray> <white>${age.seconds / 60}m ${age.seconds % 60}s ago</white>
            """.trimIndent()
        ))
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String>? {
        if (args.size == 1) {
            return listOf("create", "break", "info")
                .filter { it.startsWith(args[0].lowercase()) }
        }
        if (args.size == 2 && args[0].lowercase() == "create") {
            return null
        }
        return emptyList()
    }

    private fun sendUsage(player: Player) {
        player.sendMessage(miniMessage.deserialize(
            """
            <dark_aqua>=== Union Commands ===</dark_aqua>
            <gray>/union create <player></gray> - <white>Form a Seal of Union</white>
            <gray>/union break</gray> - <white>Break your Seal of Union</white>
            <gray>/union info</gray> - <white>View your union status</white>
            """.trimIndent()
        ))
    }
}
