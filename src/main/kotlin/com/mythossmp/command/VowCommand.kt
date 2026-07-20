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
            "test" -> handleTest(sender, args)
            "abilities" -> plugin.abilityManager.sendAbilityInfo(sender)
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
            val vow = plugin.vowRegistry.getVowByActiveKey(profile.activeVow!!)
            val display = vow?.displayName ?: profile.activeVow
            "<green>Active Vow: <gold>$display</gold> — ${vow?.description ?: ""}</green>"
        } else {
            "<gray>No active Vow. Use <yellow>/vow list</yellow> to see available vows.</gray>"
        }

        player.sendMessage(miniMessage.deserialize(
            "<dark_aqua>=== MYTHOS VOW ===</dark_aqua>\n$vowStatus"
        ))
    }

    private fun handleList(player: Player) {
        val vows = plugin.vowRegistry.getAllVows()
        val vowNames = vows.joinToString(", ") { "<gold>${it.displayName}</gold>" }
        player.sendMessage(miniMessage.deserialize(
            """
            <dark_aqua>=== Available Vows (${vows.size}) ===</dark_aqua>
            $vowNames
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

    private fun handleTest(player: Player, args: Array<out String>) {
        if (!player.isOp) {
            player.sendMessage(Component.text("Only operators can use this command.", NamedTextColor.RED))
            return
        }
        if (args.size < 2) {
            player.sendMessage(Component.text("Usage: /vow test <vow_name>", NamedTextColor.RED))
            return
        }

        val vowKey = args[1].lowercase()
        val vow = plugin.vowRegistry.getVow(vowKey)
        if (vow == null) {
            player.sendMessage(Component.text("Unknown vow: $vowKey", NamedTextColor.RED))
            return
        }

        val profile = plugin.playerDataManager.getProfile(player.uniqueId) ?: run {
            player.sendMessage(Component.text("Profile not loaded.", NamedTextColor.RED))
            return
        }

        if (profile.activeVow == vowKey) {
            player.sendMessage(Component.text("You already have this vow active.", NamedTextColor.YELLOW))
            return
        }

        if (profile.activeVow != null) {
            val oldVow = plugin.vowRegistry.getVowByActiveKey(profile.activeVow!!)
            oldVow?.passiveEffects?.forEach { it.remove(player) }
            oldVow?.curse?.remove(player)
        }

        profile.activeVow = vowKey
        profile.dirty = true

        vow.passiveEffects.forEach { it.apply(player) }
        vow.curse.apply(player)

        player.sendMessage(miniMessage.deserialize(
            "<green>Test mode: <gold>${vow.displayName}</gold> vow activated. Passives applied.</green>"
        ))
    }

    private fun sendUsage(player: Player) {
        player.sendMessage(miniMessage.deserialize(
            """
            <dark_aqua>=== Vow Commands ===</dark_aqua>
            <gray>/vow info</gray> - <white>View your current Vow status</white>
            <gray>/vow list</gray> - <white>List all available Vows</white>
            <gray>/vow abilities</gray> - <white>View ability cooldowns</white>
            <gray>/vow accept <vow></gray> - <white>Accept a Vow trial</white>
            <gray>/vow test <vow></gray> - <white>[OP] Activate a vow for testing</white>
            """.trimIndent()
        ))
    }
}
