package com.mythossmp

import com.mythossmp.application.ability.AbilityExecutor
import com.mythossmp.application.ability.CooldownService
import com.mythossmp.application.combat.CombatService
import com.mythossmp.application.profile.ProfileService
import com.mythossmp.application.trial.TrialService
import com.mythossmp.application.trust.TrustService
import com.mythossmp.application.union.UnionService
import com.mythossmp.domain.trial.MobSpawnEntry
import com.mythossmp.domain.trial.TrialConfig
import com.mythossmp.domain.trial.WaveConfig
import com.mythossmp.domain.vow.AresListeners
import com.mythossmp.domain.vow.VowAres
import com.mythossmp.domain.vow.VowRegistry
import com.mythossmp.infrastructure.config.ConfigManager
import com.mythossmp.infrastructure.database.DatabaseManager
import com.mythossmp.infrastructure.database.InMemoryTrustRepository
import com.mythossmp.infrastructure.database.InMemoryUnionRepository
import com.mythossmp.infrastructure.database.SqlProfileRepository
import com.mythossmp.infrastructure.trial.ScrollItemRegistry
import com.mythossmp.presentation.command.CommandRegistry
import com.mythossmp.presentation.display.StatusDisplay
import com.mythossmp.presentation.listener.CombatListener
import com.mythossmp.presentation.listener.TrialListener
import com.mythossmp.presentation.listener.UnionListener
import kotlinx.coroutines.runBlocking
import org.bukkit.entity.EntityType
import org.bukkit.plugin.java.JavaPlugin

class MythosSMP : JavaPlugin() {

    lateinit var configManager: ConfigManager
        private set
    lateinit var databaseManager: DatabaseManager
        private set
    lateinit var profileService: ProfileService
        private set
    lateinit var trustService: TrustService
        private set
    lateinit var unionService: UnionService
        private set
    lateinit var combatService: CombatService
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
    lateinit var trialService: TrialService
        private set
    lateinit var scrollItemRegistry: ScrollItemRegistry
        private set

    override fun onEnable() {
        instance = this

        configManager = ConfigManager(this)
        configManager.load()

        databaseManager = DatabaseManager(this)
        databaseManager.initialize()

        val profileRepository = SqlProfileRepository(databaseManager)
        profileService = ProfileService(this, profileRepository)

        val trustRepository = InMemoryTrustRepository()
        val unionRepository = InMemoryUnionRepository()
        trustService = TrustService(this, trustRepository)
        unionService = UnionService(this, unionRepository, profileRepository)
        combatService = CombatService(trustService, unionService)

        vowRegistry = VowRegistry(this)
        cooldownService = CooldownService(this)
        abilityExecutor = AbilityExecutor(this, vowRegistry, cooldownService)

        scrollItemRegistry = ScrollItemRegistry(this)
        trialService = TrialService(this)
        registerDefaultTrials()
        trialService.start()

        registerVows()
        registerListeners()

        commandRegistry = CommandRegistry(this)
        commandRegistry.register()

        statusDisplay = StatusDisplay(this)
        statusDisplay.start()

        logger.info("MythosSMP v${pluginMeta.version} enabled successfully.")
    }

    override fun onDisable() {
        if (::statusDisplay.isInitialized) statusDisplay.stop()
        if (::trialService.isInitialized) trialService.stop()
        try {
            if (::profileService.isInitialized) {
                runBlocking {
                    profileService.flushAll()
                }
            }
        } finally {
            if (::databaseManager.isInitialized) databaseManager.shutdown()
            logger.info("MythosSMP disabled.")
        }
    }

    private fun registerVows() {
        vowRegistry.register(VowAres())
    }

    private fun registerDefaultTrials() {
        trialService.registerDefaultConfig(TrialConfig(
            key = "ares",
            displayName = "Ares",
            vowKey = "ares",
            description = "Prove your worth to the God of War",
            waves = listOf(
                WaveConfig(1, listOf(MobSpawnEntry(EntityType.ZOMBIE, 4), MobSpawnEntry(EntityType.SPIDER, 2))),
                WaveConfig(2, listOf(MobSpawnEntry(EntityType.SKELETON, 4), MobSpawnEntry(EntityType.CREEPER, 2))),
                WaveConfig(3, listOf(MobSpawnEntry(EntityType.VINDICATOR, 3), MobSpawnEntry(EntityType.EVOKER, 1)))
            ),
            baseHealth = 40.0,
            baseDamage = 6.0,
            timeLimitSeconds = 600,
            maxPartySize = 4
        ))
    }

    private fun registerListeners() {
        server.pluginManager.registerEvents(profileService, this)
        server.pluginManager.registerEvents(VowAres.passive, this)
        server.pluginManager.registerEvents(VowAres.curseInstance, this)
        server.pluginManager.registerEvents(AresListeners(), this)
        server.pluginManager.registerEvents(CombatListener(combatService), this)
        server.pluginManager.registerEvents(UnionListener(unionService), this)
        server.pluginManager.registerEvents(TrialListener(this), this)
    }

    companion object {
        lateinit var instance: MythosSMP
            private set
    }
}
