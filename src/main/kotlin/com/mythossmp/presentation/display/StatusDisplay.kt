package com.mythossmp.presentation.display

import com.mythossmp.MythosSMP
import com.mythossmp.application.task.Cancellable
import com.mythossmp.application.task.Scheduler
import com.mythossmp.domain.vow.VowAres
import com.mythossmp.domain.vow.VowHades
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.scoreboard.RenderType
import java.time.Duration
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class StatusDisplay(
    private val plugin: MythosSMP,
    private val scheduler: Scheduler
) {

    private var tickTask: Cancellable? = null
    private val boards = ConcurrentHashMap<UUID, org.bukkit.scoreboard.Scoreboard>()
    private val prevEntries = ConcurrentHashMap<UUID, List<String>>()

    fun start() {
        tickTask = scheduler.repeat(Duration.ofSeconds(2)) { tick() }
        plugin.logger.info("StatusDisplay started")
    }

    fun refreshPlayer(player: Player) {
        val profile = plugin.profileService.getProfile(player.uniqueId) ?: return
        val activeKey = profile.activeVow ?: return
        when (activeKey) {
            "ares" -> updateAresActionBar(player)
            "loki" -> updateLokiActionBar(player)
            "zeus" -> updateZeusActionBar(player)
            "hades" -> updateHadesActionBar(player)
            "poseidon" -> updatePoseidonActionBar(player)
            "hermes" -> updateHermesActionBar(player)
        }
        if (player.isOp) {
            updateDevSidebar(player, activeKey)
        }
    }

    fun stop() {
        tickTask?.cancel()
        boards.clear()
        prevEntries.clear()
    }

    private fun tick() {
        for (player in plugin.server.onlinePlayers) {
            val profile = plugin.profileService.getProfile(player.uniqueId) ?: continue
            val activeKey = profile.activeVow ?: continue

            when (activeKey) {
                "ares" -> updateAresActionBar(player)
                "loki" -> updateLokiActionBar(player)
                "zeus" -> updateZeusActionBar(player)
                "hades" -> updateHadesActionBar(player)
                "poseidon" -> updatePoseidonActionBar(player)
                "hermes" -> updateHermesActionBar(player)
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

    private fun updateLokiActionBar(player: Player) {
        val cd1 = plugin.abilityExecutor.getCooldownSeconds(player, "loki-shadow-step")
        val cd2 = plugin.abilityExecutor.getCooldownSeconds(player, "loki-veil-of-lies")

        val ss = if (cd1 <= 0) Component.text("READY", NamedTextColor.GREEN)
                 else Component.text(formatTime(cd1), NamedTextColor.RED)
        val vl = if (cd2 <= 0) Component.text("READY", NamedTextColor.GREEN)
                 else Component.text(formatTime(cd2), NamedTextColor.RED)

        player.sendActionBar(Component.text()
            .append(Component.text("\uD83D\uDCAB ", NamedTextColor.LIGHT_PURPLE))
            .append(Component.text("SS:", NamedTextColor.GRAY))
            .append(ss)
            .append(Component.text("  ", NamedTextColor.DARK_GRAY))
            .append(Component.text("VL:", NamedTextColor.GRAY))
            .append(vl)
            .build()
        )
    }

    private fun updateHadesActionBar(player: Player) {
        val charges = VowHades.getCharges(player.uniqueId)
        val maxCharges = VowHades.getMaxCharges()
        val bar = buildSoulBar(charges, maxCharges)

        val cd1 = plugin.abilityExecutor.getCooldownSeconds(player, "hades-death-defiance")
        val cd2 = plugin.abilityExecutor.getCooldownSeconds(player, "hades-soul-burst")

        val dd = if (cd1 <= 0) Component.text("READY", NamedTextColor.GREEN)
                 else Component.text(formatTime(cd1), NamedTextColor.RED)
        val sb = if (cd2 <= 0) Component.text("READY", NamedTextColor.GREEN)
                 else Component.text(formatTime(cd2), NamedTextColor.RED)

        player.sendActionBar(Component.text()
            .append(Component.text("Soul ", NamedTextColor.GRAY))
            .append(bar)
            .append(Component.text(" $charges/$maxCharges", NamedTextColor.DARK_GREEN))
            .append(Component.text("  \u23F1 ", NamedTextColor.DARK_GRAY))
            .append(Component.text("DD:", NamedTextColor.GRAY))
            .append(dd)
            .append(Component.text("  ", NamedTextColor.DARK_GRAY))
            .append(Component.text("SB:", NamedTextColor.GRAY))
            .append(sb)
            .build()
        )
    }

    private fun updateZeusActionBar(player: Player) {
        val charge = plugin.vowZeus.getCharge(player.uniqueId)
        val bar = buildChargeBar(charge)

        val cd1 = plugin.abilityExecutor.getCooldownSeconds(player, "zeus-static-strike")
        val cd2 = plugin.abilityExecutor.getCooldownSeconds(player, "zeus-overcharge")

        val ss = if (cd1 <= 0) Component.text("READY", NamedTextColor.GREEN)
                 else Component.text(formatTime(cd1), NamedTextColor.RED)
        val oc = if (cd2 <= 0) Component.text("READY", NamedTextColor.GREEN)
                 else Component.text(formatTime(cd2), NamedTextColor.RED)

        player.sendActionBar(Component.text()
            .append(Component.text("\u26A1 ", NamedTextColor.YELLOW))
            .append(Component.text("Charge ", NamedTextColor.GRAY))
            .append(bar)
            .append(Component.text(" ${(charge * 100).toInt()}%", NamedTextColor.YELLOW))
            .append(Component.text("  \u23F1 ", NamedTextColor.DARK_GRAY))
            .append(Component.text("SS:", NamedTextColor.GRAY))
            .append(ss)
            .append(Component.text("  ", NamedTextColor.DARK_GRAY))
            .append(Component.text("OC:", NamedTextColor.GRAY))
            .append(oc)
            .build()
        )
    }

    private fun updatePoseidonActionBar(player: Player) {
        val cd1 = plugin.abilityExecutor.getCooldownSeconds(player, "poseidon-wave-crash")
        val cd2 = plugin.abilityExecutor.getCooldownSeconds(player, "poseidon-abyssal-blessing")

        val wc = if (cd1 <= 0) Component.text("READY", NamedTextColor.GREEN)
                 else Component.text(formatTime(cd1), NamedTextColor.RED)
        val ab = if (cd2 <= 0) Component.text("READY", NamedTextColor.GREEN)
                 else Component.text(formatTime(cd2), NamedTextColor.RED)

        player.sendActionBar(Component.text()
            .append(Component.text("\uD83C\uDF0A ", NamedTextColor.AQUA))
            .append(Component.text("WC:", NamedTextColor.GRAY))
            .append(wc)
            .append(Component.text("  ", NamedTextColor.DARK_GRAY))
            .append(Component.text("AB:", NamedTextColor.GRAY))
            .append(ab)
            .build()
        )
    }

    private fun updateHermesActionBar(player: Player) {
        val dj = com.mythossmp.domain.vow.VowHermes.getDoubleJumpRemainingSeconds(player.uniqueId)
        val cd1 = plugin.abilityExecutor.getCooldownSeconds(player, "hermes-dash")
        val cd2 = plugin.abilityExecutor.getCooldownSeconds(player, "hermes-zephyr-wing")

        val djC = if (dj <= 0) Component.text("READY", NamedTextColor.GREEN)
                  else Component.text("${dj}s", NamedTextColor.RED)
        val da = if (cd1 <= 0) Component.text("READY", NamedTextColor.GREEN)
                 else Component.text(formatTime(cd1), NamedTextColor.RED)
        val zw = if (cd2 <= 0) Component.text("READY", NamedTextColor.GREEN)
                 else Component.text(formatTime(cd2), NamedTextColor.RED)

        player.sendActionBar(Component.text()
            .append(Component.text("\uD83E\uDD86 ", NamedTextColor.YELLOW))
            .append(Component.text("DJ:", NamedTextColor.GRAY))
            .append(djC)
            .append(Component.text("  ", NamedTextColor.DARK_GRAY))
            .append(Component.text("D:", NamedTextColor.GRAY))
            .append(da)
            .append(Component.text("  ", NamedTextColor.DARK_GRAY))
            .append(Component.text("ZW:", NamedTextColor.GRAY))
            .append(zw)
            .build()
        )
    }

    private fun buildChargeBar(charge: Double): Component {
        val filled = (charge * 10).toInt().coerceIn(0, 10)
        val empty = 10 - filled
        val bar = "\u2588".repeat(filled) + "\u2591".repeat(empty)
        val color = when {
            charge >= 1.0 -> NamedTextColor.YELLOW
            charge >= 0.6 -> NamedTextColor.GOLD
            charge > 0.0 -> NamedTextColor.GREEN
            else -> NamedTextColor.GRAY
        }
        return Component.text(bar, color)
    }

    private fun buildSoulBar(charges: Int, max: Int): Component {
        val filled = charges.coerceIn(0, max)
        val empty = max - filled
        val bar = "\u2588".repeat(filled) + "\u2591".repeat(empty)
        val color = when {
            charges >= max -> NamedTextColor.DARK_PURPLE
            charges >= max * 0.6 -> NamedTextColor.DARK_GREEN
            charges > 0 -> NamedTextColor.GREEN
            else -> NamedTextColor.GRAY
        }
        return Component.text(bar, color)
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
            "loki" -> {
                val cd1 = plugin.abilityExecutor.getCooldownSeconds(player, "loki-shadow-step")
                val cd2 = plugin.abilityExecutor.getCooldownSeconds(player, "loki-veil-of-lies")
                list.add("\u00A77\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500" to 6)
                list.add("\u00A77Shadow Step:" to 5)
                list.add(if (cd1 <= 0) Pair("\u00A7aREADY", 4) else Pair("\u00A7c${cd1}s", 4))
                list.add("\u00A77Veil of Lies:" to 3)
                list.add(if (cd2 <= 0) Pair("\u00A7aREADY", 2) else Pair("\u00A7c${cd2}s", 2))
            }
            "hades" -> {
                val charges = VowHades.getCharges(player.uniqueId)
                val maxCharges = VowHades.getMaxCharges()
                list.add("\u00A77Soul Charges:" to 6)
                list.add("\u00A7a$charges\u00A77/\u00A7a$maxCharges" to 5)

                val cd1 = plugin.abilityExecutor.getCooldownSeconds(player, "hades-death-defiance")
                val cd2 = plugin.abilityExecutor.getCooldownSeconds(player, "hades-soul-burst")
                list.add("\u00A77\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500" to 4)
                list.add("\u00A77Death Defiance:" to 3)
                list.add(if (cd1 <= 0) Pair("\u00A7aREADY", 2) else Pair("\u00A7c${cd1}s", 2))
                list.add("\u00A77Soul Burst:" to 1)
                list.add(if (cd2 <= 0) Pair("\u00A7aREADY", 0) else Pair("\u00A7c${cd2}s", 0))
            }
            "zeus" -> {
                val chargePercent = (plugin.vowZeus.getCharge(player.uniqueId) * 100).toInt()
                list.add("\u00A77Static Charge:" to 6)
                list.add("\u00A7e$chargePercent\u00A77/\u00A7e100" to 5)

                val cd1 = plugin.abilityExecutor.getCooldownSeconds(player, "zeus-static-strike")
                val cd2 = plugin.abilityExecutor.getCooldownSeconds(player, "zeus-overcharge")
                list.add("\u00A77\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500" to 4)
                list.add("\u00A77Static Strike:" to 3)
                list.add(if (cd1 <= 0) Pair("\u00A7aREADY", 2) else Pair("\u00A7c${cd1}s", 2))
                list.add("\u00A77Overcharge:" to 1)
                list.add(if (cd2 <= 0) Pair("\u00A7aREADY", 0) else Pair("\u00A7c${cd2}s", 0))
            }
            "poseidon" -> {
                val cd1 = plugin.abilityExecutor.getCooldownSeconds(player, "poseidon-wave-crash")
                val cd2 = plugin.abilityExecutor.getCooldownSeconds(player, "poseidon-abyssal-blessing")
                list.add("\u00A77\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500" to 6)
                list.add("\u00A77Wave Crash:" to 5)
                list.add(if (cd1 <= 0) Pair("\u00A7aREADY", 4) else Pair("\u00A7c${cd1}s", 4))
                list.add("\u00A77Abyssal Blessing:" to 3)
                list.add(if (cd2 <= 0) Pair("\u00A7aREADY", 2) else Pair("\u00A7c${cd2}s", 2))
            }
            "hermes" -> {
                val dj = com.mythossmp.domain.vow.VowHermes.getDoubleJumpRemainingSeconds(player.uniqueId)
                val cd1 = plugin.abilityExecutor.getCooldownSeconds(player, "hermes-dash")
                val cd2 = plugin.abilityExecutor.getCooldownSeconds(player, "hermes-zephyr-wing")
                list.add("\u00A77\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500" to 6)
                list.add("\u00A77Double Jump:" to 5)
                list.add(if (dj <= 0) Pair("\u00A7aREADY", 4) else Pair("\u00A7c${dj}s", 4))
                list.add("\u00A77Dash:" to 3)
                list.add(if (cd1 <= 0) Pair("\u00A7aREADY", 2) else Pair("\u00A7c${cd1}s", 2))
                list.add("\u00A77Zephyr Wing:" to 1)
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
