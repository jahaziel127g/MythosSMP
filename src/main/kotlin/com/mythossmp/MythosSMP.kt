package com.mythossmp

import com.mythossmp.application.ability.AbilityExecutor
import com.mythossmp.application.ability.CooldownService
import com.mythossmp.application.profile.ProfileService
import com.mythossmp.domain.vow.AresListeners
import com.mythossmp.domain.vow.VowAres
import com.mythossmp.domain.vow.VowRegistry
import com.mythossmp.infrastructure.config.ConfigManager
import com.mythossmp.infrastructure.database.DatabaseManager
import com.mythossmp.infrastructure.database.SqlProfileRepository
import com.mythossmp.presentation.command.CommandRegistry
import com.mythossmp.presentation.display.StatusDisplay
import kotlinx.coroutines.runBlocking
import org.bukkit.plugin.java.JavaPlugin

class MythosSMP : JavaPlugin() {

    lateinit var configManager: ConfigManager
        private set
    lateinit var databaseManager: DatabaseManager
        private set
    lateinit var profileService: ProfileService
        private set
    lateinit var commandRegistry: CommandRegistry
        private set
    lateinit var vowRegistry: VowRegistry
        private set
    lateinit var cooldownService: CooldownService
        private set
    lateinit var abilityExecutor: AbilityExecutor
        private set
    lateinit var statusDisplay: StatusDisplay
        private set

    override fun onEnable() {
        instance = this

        configManager = ConfigManager(this)
        configManager.load()

        databaseManager = DatabaseManager(this)
        databaseManager.initialize()

        val profileRepository = SqlProfileRepository(databaseManager)
        profileService = ProfileService(this, profileRepository)

        vowRegistry = VowRegistry(this)
        cooldownService = CooldownService(this)
        abilityExecutor = AbilityExecutor(this, vowRegistry, cooldownService)

        registerVows()
        registerListeners()

        commandRegistry = CommandRegistry(this)
        commandRegistry.register()

        statusDisplay = StatusDisplay(this)
        statusDisplay.start()

        logger.info("MythosSMP v${pluginMeta.version} enabled successfully.")
    }

    override fun onDisable() {
        if (::statusDisplay.isInitialized) {
            statusDisplay.stop()
        }
        try {
            if (::profileService.isInitialized) {
                runBlocking {
                    profileService.flushAll()
                }
            }
        } finally {
            if (::databaseManager.isInitialized) {
                databaseManager.shutdown()
            }
            logger.info("MythosSMP disabled.")
        }
    }

    private fun registerVows() {
        vowRegistry.register(VowAres())
    }

    private fun registerListeners() {
        server.pluginManager.registerEvents(profileService, this)
        server.pluginManager.registerEvents(VowAres.passive, this)
        server.pluginManager.registerEvents(VowAres.curseInstance, this)
        server.pluginManager.registerEvents(AresListeners(), this)
    }

    companion object {
        lateinit var instance: MythosSMP
            private set
    }
}
