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

class VowCommand(private val plugin: MythosSMP) : CommandExecutor, TabCompleter {

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
            "abilities" -> plugin.abilityExecutor.sendAbilityInfo(sender)
            else -> sendUsage(sender)
        }

        return true
    }

    private fun handleInfo(player: Player) {
        val profile = plugin.profileService.getProfile(player.uniqueId)
        if (profile == null) {
            player.sendMessage(Component.text("Profile not loaded yet.", NamedTextColor.RED))
            return
        }

        val activeKey = profile.activeVow
        if (activeKey != null) {
            val vow = plugin.vowRegistry.getVowByActiveKey(activeKey)
            if (vow != null) {
                player.sendMessage(miniMessage.deserialize(
                    "<dark_aqua>=== MYTHOS VOW ===</dark_aqua>\n" +
                    "<green>Active Vow: <gold>${vow.displayName}</gold> — ${vow.description}</green>"
                ))
            } else {
                player.sendMessage(Component.text()
                    .color(NamedTextColor.DARK_AQUA)
                    .append(Component.text("=== MYTHOS VOW ==="))
                    .append(Component.newline())
                    .append(Component.text("Active Vow: ", NamedTextColor.GREEN))
                    .append(Component.text(activeKey, NamedTextColor.GOLD))
                    .append(Component.text(" — (unknown vow)", NamedTextColor.GRAY))
                    .build()
                )
            }
        } else {
            player.sendMessage(miniMessage.deserialize(
                "<dark_aqua>=== MYTHOS VOW ===</dark_aqua>\n" +
                "<gray>No active Vow. Use <yellow>/vow list</yellow> to see available vows.</gray>"
            ))
        }
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

        val vowKey = args[1].lowercase()
        val vow = plugin.vowRegistry.getVow(vowKey)
        if (vow == null) {
            player.sendMessage(Component.text("Unknown vow: $vowKey", NamedTextColor.RED))
            return
        }

        if (!plugin.trialService.hasCompletedTrial(player.uniqueId, vowKey)) {
            player.sendMessage(miniMessage.deserialize(
                "<red>You must complete the <gold>${vow.displayName}</gold> trial first! " +
                "Use <yellow>/trial give $vowKey</yellow> to obtain a trial scroll.</red>"
            ))
            return
        }

        val profile = plugin.profileService.getProfile(player.uniqueId) ?: run {
            player.sendMessage(Component.text("Profile not loaded.", NamedTextColor.RED))
            return
        }

        val normalizeKey = plugin.vowRegistry.normalizeActiveKey(vowKey)
        if (profile.activeVow == normalizeKey) {
            player.sendMessage(Component.text("You already have this vow active.", NamedTextColor.YELLOW))
            return
        }

        val oldActiveKey = profile.activeVow
        if (oldActiveKey != null) {
            val oldVow = plugin.vowRegistry.getVowByActiveKey(oldActiveKey)
            oldVow?.passiveEffects?.forEach { it.remove(player) }
            oldVow?.curse?.remove(player)
        }

        plugin.profileService.updateProfile(player.uniqueId) {
            it.copy(activeVow = normalizeKey, dirty = true)
        }

        vow.passiveEffects.forEach { it.apply(player) }
        vow.curse.apply(player)

        player.sendMessage(miniMessage.deserialize(
            "<green>You have accepted the <gold>${vow.displayName}</gold> vow! " +
            "May the gods favor you.</green>"
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

        val profile = plugin.profileService.getProfile(player.uniqueId) ?: run {
            player.sendMessage(Component.text("Profile not loaded.", NamedTextColor.RED))
            return
        }

        val normalizeKey = plugin.vowRegistry.normalizeActiveKey(vowKey)
        if (profile.activeVow == normalizeKey) {
            player.sendMessage(Component.text("You already have this vow active.", NamedTextColor.YELLOW))
            return
        }

        val oldActiveKey = profile.activeVow
        if (oldActiveKey != null) {
            val oldVow = plugin.vowRegistry.getVowByActiveKey(oldActiveKey)
            oldVow?.passiveEffects?.forEach { it.remove(player) }
            oldVow?.curse?.remove(player)
        }

        plugin.profileService.updateProfile(player.uniqueId) {
            it.copy(activeVow = normalizeKey, dirty = true)
        }

        vow.passiveEffects.forEach { it.apply(player) }
        vow.curse.apply(player)

        player.sendMessage(miniMessage.deserialize(
            "<green>Test mode: <gold>${vow.displayName}</gold> vow activated. Passives applied.</green>"
        ))
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String>? {
        if (args.size == 1) {
            return listOf("info", "list", "accept", "abilities", "test")
                .filter { it.startsWith(args[0].lowercase()) }
        }
        if (args.size == 2 && args[0].lowercase() in listOf("accept", "test")) {
            return plugin.vowRegistry.getAllVows().map { it.key }
                .filter { it.startsWith(args[1].lowercase()) }
        }
        return emptyList()
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
