package com.mythossmp.command

import com.mythossmp.MythosSMP

class CommandRegistry(private val plugin: MythosSMP) {

    fun register() {
        plugin.getCommand("vow")?.setExecutor(VowCommand(plugin))
        plugin.getCommand("trust")?.setExecutor(TrustCommand(plugin))
    }
}
