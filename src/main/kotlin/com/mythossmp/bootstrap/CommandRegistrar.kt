package com.mythossmp.bootstrap

import com.mythossmp.presentation.command.AbilityCommand
import com.mythossmp.presentation.command.TrialCommand
import com.mythossmp.presentation.command.TrustCommand
import com.mythossmp.presentation.command.UnionCommand
import com.mythossmp.presentation.command.VowCommand

class CommandRegistrar(private val container: ServiceContainer) {

    fun register() {
        val plugin = container.plugin

        val vowCmd = VowCommand(container.plugin)
        plugin.getCommand("vow")?.setExecutor(vowCmd)
        plugin.getCommand("vow")?.tabCompleter = vowCmd

        val trustCmd = TrustCommand(container.plugin)
        plugin.getCommand("trust")?.setExecutor(trustCmd)
        plugin.getCommand("trust")?.tabCompleter = trustCmd

        val unionCmd = UnionCommand(container.plugin)
        plugin.getCommand("union")?.setExecutor(unionCmd)
        plugin.getCommand("union")?.tabCompleter = unionCmd

        val trialCmd = TrialCommand(container.plugin)
        plugin.getCommand("trial")?.setExecutor(trialCmd)
        plugin.getCommand("trial")?.tabCompleter = trialCmd

        plugin.getCommand("ability1")?.setExecutor(AbilityCommand(container.plugin, 1))
        plugin.getCommand("ability2")?.setExecutor(AbilityCommand(container.plugin, 2))
    }
}
