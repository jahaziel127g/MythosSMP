package com.mythossmp.presentation.command

import com.mythossmp.MythosSMP
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class TrialCommand(private val plugin: MythosSMP) : CommandExecutor, TabCompleter {

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
            "start" -> handleStart(sender, args)
            "info" -> handleInfo(sender)
            "leave" -> handleLeave(sender)
            "give" -> handleGive(sender, args)
            else -> sendUsage(sender)
        }

        return true
    }

    private fun handleStart(player: Player, args: Array<out String>) {
        if (args.size < 2) {
            player.sendMessage(Component.text("Usage: /trial start <trial_key>", NamedTextColor.RED))
            return
        }

        val trialKey = args[1].lowercase()
        plugin.trialService.startTrial(player, trialKey)
    }

    private fun handleInfo(player: Player) {
        val session = plugin.trialService.getSessionForPlayer(player.uniqueId)
        if (session == null) {
            player.sendMessage(Component.text("You are not in a trial.", NamedTextColor.GRAY))
            return
        }

        player.sendMessage(miniMessage.deserialize(
            """
            <dark_aqua>=== Trial: <gold>${session.trialConfig.displayName}</gold> ===</dark_aqua>
            <gray>State: <white>${session.state.name}</white></gray>
            <gray>Wave: <white>${session.currentWave}/${session.trialConfig.totalWaves}</white></gray>
            <gray>Party: <white>${session.allPlayers().size}/${session.trialConfig.maxPartySize}</white></gray>
            <gray>Elapsed: <white>${session.elapsedSeconds}s / ${session.trialConfig.timeLimitSeconds}s</white></gray>
            """.trimIndent()
        ))
    }

    private fun handleLeave(player: Player) {
        val session = plugin.trialService.getSessionForPlayer(player.uniqueId)
        if (session == null) {
            player.sendMessage(Component.text("You are not in a trial.", NamedTextColor.GRAY))
            return
        }

        player.sendMessage(Component.text("You abandoned the trial.", NamedTextColor.RED))
    }

    private fun handleGive(player: Player, args: Array<out String>) {
        if (!player.isOp) {
            player.sendMessage(Component.text("Only operators can use this command.", NamedTextColor.RED))
            return
        }
        if (args.size < 2) {
            player.sendMessage(Component.text("Usage: /trial give <trial_key>", NamedTextColor.RED))
            return
        }

        val trialKey = args[1].lowercase()
        val config = plugin.trialService.getDefaultConfig(trialKey)
        if (config == null) {
            player.sendMessage(Component.text("Unknown trial: $trialKey", NamedTextColor.RED))
            return
        }

        val scroll = plugin.scrollItemRegistry.createScroll(config)
        player.inventory.addItem(scroll)
        player.sendMessage(miniMessage.deserialize(
            "<green>Given you a <gold>${config.displayName}</gold> trial scroll.</green>"
        ))
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String>? {
        if (args.size == 1) {
            return listOf("start", "info", "leave", "give")
                .filter { it.startsWith(args[0].lowercase()) }
        }
        if (args.size == 2 && args[0].lowercase() in listOf("start", "give")) {
            return plugin.trialService.getAllDefaultConfigs().map { it.key }
                .filter { it.startsWith(args[1].lowercase()) }
        }
        return emptyList()
    }

    private fun sendUsage(player: Player) {
        player.sendMessage(miniMessage.deserialize(
            """
            <dark_aqua>=== Trial Commands ===</dark_aqua>
            <gray>/trial start <trial></gray> - <white>Start a trial</white>
            <gray>/trial info</gray> - <white>View trial status</white>
            <gray>/trial leave</gray> - <white>Abandon current trial</white>
            <gray>/trial give <trial></gray> - <white>[OP] Spawn a trial scroll</white>
            """.trimIndent()
        ))
    }
}
