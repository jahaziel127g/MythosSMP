package com.mythossmp

import com.mythossmp.command.CommandRegistry
import com.mythossmp.config.ConfigManager
import com.mythossmp.database.DatabaseManager
import com.mythossmp.player.PlayerDataManager
import com.mythossmp.vow.AbilityManager
import com.mythossmp.vow.CooldownManager
import com.mythossmp.vow.VowRegistry
import com.mythossmp.vow.implementations.VowAres
import kotlinx.coroutines.runBlocking
import org.bukkit.plugin.java.JavaPlugin

class MythosSMP : JavaPlugin() {

    lateinit var configManager: ConfigManager
        private set
    lateinit var databaseManager: DatabaseManager
        private set
    lateinit var playerDataManager: PlayerDataManager
        private set
    lateinit var commandRegistry: CommandRegistry
        private set
    lateinit var vowRegistry: VowRegistry
        private set
    lateinit var cooldownManager: CooldownManager
        private set
    lateinit var abilityManager: AbilityManager
        private set

    override fun onEnable() {
        configManager = ConfigManager(this)
        configManager.load()

        databaseManager = DatabaseManager(this)
        databaseManager.initialize()

        playerDataManager = PlayerDataManager(this, databaseManager)

        vowRegistry = VowRegistry(this)
        cooldownManager = CooldownManager(this)
        abilityManager = AbilityManager(this, vowRegistry, cooldownManager)

        registerVows()
        registerListeners()

        commandRegistry = CommandRegistry(this)
        commandRegistry.register()

        logger.info("MythosSMP v${pluginMeta.version} enabled successfully.")
    }

    override fun onDisable() {
        if (::playerDataManager.isInitialized) {
            runBlocking {
                playerDataManager.flushAll()
            }
        }
        if (::databaseManager.isInitialized) {
            databaseManager.shutdown()
        }
        logger.info("MythosSMP disabled.")
    }

    private fun registerVows() {
        vowRegistry.register(VowAres())
    }

    private fun registerListeners() {
        server.pluginManager.registerEvents(playerDataManager, this)
        server.pluginManager.registerEvents(VowAres.passive, this)
    }

    companion object {
        lateinit var instance: MythosSMP
            private set
    }
}
