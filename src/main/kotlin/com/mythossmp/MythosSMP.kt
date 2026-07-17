package com.mythossmp

import com.mythossmp.command.CommandRegistry
import com.mythossmp.config.ConfigManager
import com.mythossmp.database.DatabaseManager
import com.mythossmp.player.PlayerDataManager
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

    override fun onEnable() {
        configManager = ConfigManager(this)
        configManager.load()

        databaseManager = DatabaseManager(this)
        databaseManager.initialize()

        playerDataManager = PlayerDataManager(this, databaseManager)

        commandRegistry = CommandRegistry(this)
        commandRegistry.register()

        server.pluginManager.registerEvents(playerDataManager, this)

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

    companion object {
        lateinit var instance: MythosSMP
            private set
    }
}
