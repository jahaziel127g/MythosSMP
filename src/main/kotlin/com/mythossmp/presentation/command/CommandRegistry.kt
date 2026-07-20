package com.mythossmp.presentation.command

import com.mythossmp.MythosSMP

class CommandRegistry(private val plugin: MythosSMP) {

    fun register() {
        plugin.getCommand("vow")?.setExecutor(VowCommand(plugin))
        plugin.getCommand("trust")?.setExecutor(TrustCommand(plugin))
        plugin.getCommand("ability1")?.setExecutor(AbilityCommand(plugin, 1))
        plugin.getCommand("ability2")?.setExecutor(AbilityCommand(plugin, 2))
    }
}
