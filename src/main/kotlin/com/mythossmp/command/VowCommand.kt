package com.mythossmp.command

import com.mythossmp.MythosSMP
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class VowCommand(private val plugin: MythosSMP) : CommandExecutor {

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
            "info" -> handleInfo(sender)
            "list" -> handleList(sender)
            "accept" -> handleAccept(sender, args)
            else -> sendUsage(sender)
        }

        return true
    }

    private fun handleInfo(player: Player) {
        val profile = plugin.playerDataManager.getProfile(player.uniqueId)
        if (profile == null) {
            player.sendMessage(Component.text("Profile not loaded yet.", NamedTextColor.RED))
            return
        }

        val vowStatus = if (profile.activeVow != null) {
            "<green>Active Vow: <gold>${profile.activeVow}</gold></green>"
        } else {
            "<gray>No active Vow. Use <yellow>/vow list</yellow> to see available vows.</gray>"
        }

        player.sendMessage(miniMessage.deserialize(
            "<dark_aqua>=== MYTHOS VOW ===</dark_aqua>\n$vowStatus"
        ))
    }

    private fun handleList(player: Player) {
        player.sendMessage(miniMessage.deserialize(
            """
            <dark_aqua>=== Available Vows ===</dark_aqua>
            <gray>Trials are not yet implemented in this foundation phase.</gray>
            <gray>Available Vows:</gray>
            <gold>Ares, Plutus, Zeus, Hades, Hercules, Boreas</gold>
            <gold>Hephaestus, Erebus, Loki, Poseidon, Chronos, Hermes</gold>
            <gold>Dionysus, Apollo</gold>
            """.trimIndent()
        ))
    }

    private fun handleAccept(player: Player, args: Array<out String>) {
        if (args.size < 2) {
            player.sendMessage(Component.text("Usage: /vow accept <vow_name>", NamedTextColor.RED))
            return
        }

        player.sendMessage(Component.text(
            "Vow acceptance requires completing a Trial. This feature will be implemented in Phase 3.",
            NamedTextColor.YELLOW
        ))
    }

    private fun sendUsage(player: Player) {
        player.sendMessage(miniMessage.deserialize(
            """
            <dark_aqua>=== Vow Commands ===</dark_aqua>
            <gray>/vow info</gray> - <white>View your current Vow status</white>
            <gray>/vow list</gray> - <white>List all available Vows</white>
            <gray>/vow accept <vow></gray> - <white>Accept a Vow trial</white>
            """.trimIndent()
        ))
    }
}
