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
import org.bukkit.entity.Player
import java.sql.SQLException
import java.util.UUID

class TrustCommand(private val plugin: MythosSMP) : CommandExecutor {

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

        try {
            plugin.databaseManager.getConnection().use { conn ->
                conn.prepareStatement(
                    "INSERT OR IGNORE INTO mythos_trusts (player_uuid, trusted_uuid) VALUES (?, ?)"
                ).use { stmt ->
                    stmt.setString(1, player.uniqueId.toString())
                    stmt.setString(2, target.uniqueId.toString())
                    val rows = stmt.executeUpdate()
                    if (rows > 0) {
                        player.sendMessage(miniMessage.deserialize(
                            "<green>Trusted <gold>${target.name}</gold>.</green>"
                        ))
                    } else {
                        player.sendMessage(Component.text(
                            "Already trusted: ${target.name}",
                            NamedTextColor.YELLOW
                        ))
                    }
                }
            }
        } catch (e: SQLException) {
            player.sendMessage(Component.text("Database error while adding trust.", NamedTextColor.RED))
            plugin.logger.severe("Trust add error: ${e.message}")
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

        try {
            plugin.databaseManager.getConnection().use { conn ->
                conn.prepareStatement(
                    "DELETE FROM mythos_trusts WHERE player_uuid = ? AND trusted_uuid = ?"
                ).use { stmt ->
                    stmt.setString(1, player.uniqueId.toString())
                    stmt.setString(2, targetUuid.toString())
                    val rows = stmt.executeUpdate()
                    if (rows > 0) {
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
            }
        } catch (e: SQLException) {
            player.sendMessage(Component.text("Database error while removing trust.", NamedTextColor.RED))
            plugin.logger.severe("Trust remove error: ${e.message}")
        }
    }

    private fun handleList(player: Player) {
        try {
            plugin.databaseManager.getConnection().use { conn ->
                conn.prepareStatement(
                    "SELECT trusted_uuid FROM mythos_trusts WHERE player_uuid = ?"
                ).use { stmt ->
                    stmt.setString(1, player.uniqueId.toString())
                    stmt.executeQuery().use { rs ->
                        val trusted = mutableListOf<String>()
                        while (rs.next()) {
                            val uuid = UUID.fromString(rs.getString("trusted_uuid"))
                            val name = Bukkit.getOfflinePlayer(uuid).name ?: uuid.toString().take(8)
                            trusted.add(name)
                        }

                        if (trusted.isEmpty()) {
                            player.sendMessage(Component.text("You have no trusted players.", NamedTextColor.GRAY))
                        } else {
                            player.sendMessage(miniMessage.deserialize(
                                "<dark_aqua>=== Trusted Players ===</dark_aqua>"
                            ))
                            trusted.forEach { name ->
                                player.sendMessage(miniMessage.deserialize(
                                    "<gray>- <white>$name</white></gray>"
                                ))
                            }
                        }
                    }
                }
            }
        } catch (e: SQLException) {
            player.sendMessage(Component.text("Database error while listing trusts.", NamedTextColor.RED))
            plugin.logger.severe("Trust list error: ${e.message}")
        }
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
