package com.mythossmp.presentation.command

import com.mythossmp.MythosSMP
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class TrustCommand(private val plugin: MythosSMP) : CommandExecutor, TabCompleter {

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
            "add" -> handleAdd(sender, args)
            "remove" -> handleRemove(sender, args)
            "list" -> handleList(sender)
            else -> sendUsage(sender)
        }

        return true
    }

    private fun handleAdd(player: Player, args: Array<out String>) {
        if (args.size < 2) {
            player.sendMessage(Component.text("Usage: /trust add <player>", NamedTextColor.RED))
            return
        }

        val target = Bukkit.getPlayerExact(args[1])
        if (target == null) {
            player.sendMessage(Component.text("Player not found.", NamedTextColor.RED))
            return
        }

        if (target.uniqueId == player.uniqueId) {
            player.sendMessage(Component.text("You cannot trust yourself.", NamedTextColor.RED))
            return
        }

        val added = plugin.trustService.addTrust(player.uniqueId, target.uniqueId)
        if (added) {
            player.sendMessage(miniMessage.deserialize(
                "<green>Trusted <gold>${target.name}</gold>.</green>"
            ))
        } else {
            player.sendMessage(Component.text("Already trusted: ${target.name}", NamedTextColor.YELLOW))
        }
    }

    private fun resolveTarget(name: String): OfflinePlayer? {
        val online = Bukkit.getPlayerExact(name)
        if (online != null) return online
        val offline = Bukkit.getOfflinePlayer(name)
        return if (offline.hasPlayedBefore()) offline else null
    }

    private fun handleRemove(player: Player, args: Array<out String>) {
        if (args.size < 2) {
            player.sendMessage(Component.text("Usage: /trust remove <player>", NamedTextColor.RED))
            return
        }

        val target = resolveTarget(args[1])
        if (target == null) {
            player.sendMessage(Component.text("Player not found.", NamedTextColor.RED))
            return
        }

        val targetUuid = target.uniqueId
        val targetName = target.name ?: args[1]

        val removed = plugin.trustService.removeTrust(player.uniqueId, targetUuid)
        if (removed) {
            player.sendMessage(miniMessage.deserialize(
                "<green>Removed <gold>${targetName}</gold> from your trust list.</green>"
            ))
        } else {
            player.sendMessage(Component.text(
                "${targetName} was not in your trust list.",
                NamedTextColor.YELLOW
            ))
        }
    }

    private fun handleList(player: Player) {
        val trusted = plugin.trustService.getTrusted(player.uniqueId)

        if (trusted.isEmpty()) {
            player.sendMessage(Component.text("You have no trusted players.", NamedTextColor.GRAY))
        } else {
            player.sendMessage(miniMessage.deserialize(
                "<dark_aqua>=== Trusted Players ===</dark_aqua>"
            ))
            trusted.forEach { uuid ->
                val name = Bukkit.getOfflinePlayer(uuid).name ?: uuid.toString().take(8)
                player.sendMessage(miniMessage.deserialize(
                    "<gray>- <white>$name</white></gray>"
                ))
            }
        }
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String>? {
        if (args.size == 1) {
            return listOf("add", "remove", "list")
                .filter { it.startsWith(args[0].lowercase()) }
        }
        if (args.size == 2 && args[0].lowercase() in listOf("add", "remove")) {
            return null
        }
        return emptyList()
    }

    private fun sendUsage(player: Player) {
        player.sendMessage(miniMessage.deserialize(
            """
            <dark_aqua>=== Trust Commands ===</dark_aqua>
            <gray>/trust add <player></gray> - <white>Add a player to your trust list</white>
            <gray>/trust remove <player></gray> - <white>Remove a player from your trust list</white>
            <gray>/trust list</gray> - <white>List trusted players</white>
            """.trimIndent()
        ))
    }
}
