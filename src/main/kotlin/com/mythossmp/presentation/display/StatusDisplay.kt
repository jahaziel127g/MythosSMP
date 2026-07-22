package com.mythossmp.presentation.display

import com.mythossmp.MythosSMP
import com.mythossmp.domain.vow.VowAres
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.scoreboard.RenderType
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class StatusDisplay(private val plugin: MythosSMP) {

    private var taskId: Int? = null
    private val boards = ConcurrentHashMap<UUID, org.bukkit.scoreboard.Scoreboard>()
    private val prevEntries = ConcurrentHashMap<UUID, List<String>>()

    fun start() {
        taskId = plugin.server.scheduler.runTaskTimer(plugin, Runnable { tick() }, 20L, 40L).taskId
        plugin.logger.info("StatusDisplay started")
    }

    fun refreshPlayer(player: Player) {
        val profile = plugin.profileService.getProfile(player.uniqueId) ?: return
        val activeKey = profile.activeVow ?: return
        when (activeKey) {
            "ares" -> updateAresActionBar(player)
        }
        if (player.isOp) {
            updateDevSidebar(player, activeKey)
        }
    }

    fun stop() {
        taskId?.let { plugin.server.scheduler.cancelTask(it) }
        boards.clear()
        prevEntries.clear()
    }

    private fun tick() {
        for (player in plugin.server.onlinePlayers) {
            val profile = plugin.profileService.getProfile(player.uniqueId) ?: continue
            val activeKey = profile.activeVow ?: continue

            when (activeKey) {
                "ares" -> updateAresActionBar(player)
            }

            if (player.isOp) {
                updateDevSidebar(player, activeKey)
            } else {
                clearDevSidebar(player)
            }
        }
    }

    private fun updateAresActionBar(player: Player) {
        val rage = VowAres.getRage(player.uniqueId)
        val bar = buildRageBar(rage)

        val cd1 = plugin.abilityExecutor.getCooldownSeconds(player, "ares-bloodthorns")
        val cd2 = plugin.abilityExecutor.getCooldownSeconds(player, "ares-rage-aura")

        val bt = if (cd1 <= 0) Component.text("READY", NamedTextColor.GREEN)
                 else Component.text(formatTime(cd1), NamedTextColor.RED)

        val ra = if (cd2 <= 0) Component.text("READY", NamedTextColor.GREEN)
                 else Component.text(formatTime(cd2), NamedTextColor.RED)

        player.sendActionBar(Component.text()
            .append(Component.text("Rage ", NamedTextColor.GRAY))
            .append(bar)
            .append(Component.text(" $rage%", NamedTextColor.YELLOW))
            .append(Component.text("  \u23F1 ", NamedTextColor.DARK_GRAY))
            .append(Component.text("BT:", NamedTextColor.GRAY))
            .append(bt)
            .append(Component.text("  ", NamedTextColor.DARK_GRAY))
            .append(Component.text("RA:", NamedTextColor.GRAY))
            .append(ra)
            .build()
        )
    }

    private fun formatTime(seconds: Long): String {
        val mins = seconds / 60
        val secs = seconds % 60
        return if (mins > 0) "%d:%02d".format(mins, secs) else "${secs}s"
    }

    private fun buildRageBar(rage: Int): Component {
        val filled = (rage / 10).coerceIn(0, 10)
        val empty = 10 - filled
        val bar = "\u2588".repeat(filled) + "\u2591".repeat(empty)
        val color = when {
            rage >= 100 -> NamedTextColor.RED
            rage >= 75 -> NamedTextColor.GOLD
            rage >= 50 -> NamedTextColor.YELLOW
            else -> NamedTextColor.GREEN
        }
        return Component.text(bar, color)
    }

    private fun updateDevSidebar(player: Player, activeKey: String) {
        val manager = Bukkit.getScoreboardManager()
        var board = boards[player.uniqueId]
        if (board == null) {
            board = manager.newScoreboard
            val obj = board.registerNewObjective(
                "mythos", "dummy",
                Component.text("Mythos SMP", NamedTextColor.DARK_AQUA),
                RenderType.INTEGER
            )
            obj.displaySlot = DisplaySlot.SIDEBAR
            boards[player.uniqueId] = board
        }
        player.scoreboard = board

        val obj = board.getObjective("mythos")!!

        val entries = buildSidebarEntries(player, activeKey)

        val old = prevEntries.put(player.uniqueId, entries.map { it.first })
        old?.forEach { entry -> board.resetScores(entry) }

        entries.forEach { (text, score) ->
            obj.getScore(text).score = score
        }
    }

    private fun buildSidebarEntries(player: Player, activeKey: String): List<Pair<String, Int>> {
        val list = mutableListOf<Pair<String, Int>>()
        list.add("\u00A77Active Vow:" to 9)
        list.add("\u00A76${activeKey.uppercase()}" to 8)
        list.add("\u00A77\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500" to 7)

        when (activeKey) {
            "ares" -> {
                val rage = VowAres.getRage(player.uniqueId)
                val maxRage = VowAres.getMaxRage()
                list.add("\u00A77Rage:" to 6)
                list.add("\u00A7e$rage\u00A77/\u00A7e$maxRage" to 5)

                val cd1 = plugin.abilityExecutor.getCooldownSeconds(player, "ares-bloodthorns")
                val cd2 = plugin.abilityExecutor.getCooldownSeconds(player, "ares-rage-aura")
                list.add("\u00A77\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500" to 4)
                list.add("\u00A77Bloodthorns:" to 3)
                list.add(if (cd1 <= 0) Pair("\u00A7aREADY", 2) else Pair("\u00A7c${cd1}s", 2))
                list.add("\u00A77Rage Aura:" to 1)
                list.add(if (cd2 <= 0) Pair("\u00A7aREADY", 0) else Pair("\u00A7c${cd2}s", 0))
            }
        }

        return list
    }

    private fun clearDevSidebar(player: Player) {
        val board = boards.remove(player.uniqueId)
        if (board != null) {
            val old = prevEntries.remove(player.uniqueId)
            old?.forEach { entry -> board.resetScores(entry) }
        }
        val main = Bukkit.getScoreboardManager().mainScoreboard
        if (player.scoreboard != main) {
            player.scoreboard = main
        }
    }
}
