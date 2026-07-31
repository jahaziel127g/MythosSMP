package com.mythossmp.bootstrap

import com.mythossmp.MythosSMP
import com.mythossmp.application.ability.AbilityExecutor
import com.mythossmp.application.ability.CooldownService
import com.mythossmp.application.combat.CombatService
import com.mythossmp.application.profile.ProfileRepository
import com.mythossmp.application.profile.ProfileService
import com.mythossmp.application.task.Scheduler
import com.mythossmp.application.trial.TrialService
import com.mythossmp.application.trust.TrustRepository
import com.mythossmp.application.trust.TrustService
import com.mythossmp.application.union.UnionRepository
import com.mythossmp.application.union.UnionService
import com.mythossmp.application.vfx.VFXBroadcastService
import com.mythossmp.domain.trial.MobSpawnEntry
import com.mythossmp.domain.trial.TrialConfig
import com.mythossmp.domain.trial.WaveConfig
import com.mythossmp.domain.vow.VowAres
import com.mythossmp.domain.vow.VowHades
import com.mythossmp.domain.vow.VowLoki
import com.mythossmp.domain.vow.VowRegistry
import com.mythossmp.domain.vow.VowZeus
import com.mythossmp.infrastructure.config.ConfigManager
import com.mythossmp.infrastructure.database.DatabaseManager
import com.mythossmp.infrastructure.database.InMemoryTrustRepository
import com.mythossmp.infrastructure.database.InMemoryUnionRepository
import com.mythossmp.infrastructure.database.SqlProfileRepository
import com.mythossmp.infrastructure.task.PaperScheduler
import com.mythossmp.infrastructure.trial.ScrollItemRegistry
import com.mythossmp.presentation.display.StatusDisplay
import org.bukkit.entity.EntityType

class Bootstrap(private val plugin: MythosSMP) {

    private var currentStep = "initializing"

    private lateinit var configManager: ConfigManager
    private lateinit var databaseManager: DatabaseManager
    private lateinit var scheduler: Scheduler

    private lateinit var profileRepository: ProfileRepository
    private lateinit var trustRepository: TrustRepository
    private lateinit var unionRepository: UnionRepository

    private lateinit var profileService: ProfileService
    private lateinit var trustService: TrustService
    private lateinit var unionService: UnionService
    private lateinit var combatService: CombatService
    private lateinit var vowRegistry: VowRegistry
    private lateinit var cooldownService: CooldownService
    private lateinit var abilityExecutor: AbilityExecutor
    private lateinit var vfxBroadcastService: VFXBroadcastService
    private lateinit var scrollItemRegistry: ScrollItemRegistry
    private lateinit var trialService: TrialService
    private lateinit var displayService: StatusDisplay
    private lateinit var vowZeus: VowZeus

    fun run(): ServiceContainer {
        try {
            scheduler = PaperScheduler(plugin)
            plugin.scheduler = scheduler

            step("loadConfig") { loadConfig() }
            step("connectDatabase") { connectDatabase() }
            step("initializeRepositories") { initializeRepositories() }
            step("initializeServices") { initializeServices() }
            step("registerVowsAndTrials") { registerVowsAndTrials() }

            val container = step("assembleContainer") { assembleContainer() }
            plugin.container = container
            step("registerPresentation") { registerPresentation(container) }
            step("startBackgroundServices") { startBackgroundServices(container) }

            return container
        } catch (e: Exception) {
            logFatalTrace(e)
            throw e
        }
    }

    private fun <T> step(name: String, block: () -> T): T {
        currentStep = name
        return block()
    }

    private fun loadConfig() {
        configManager = ConfigManager(plugin)
        configManager.load()
    }

    private fun connectDatabase() {
        databaseManager = DatabaseManager(plugin, configManager)
        databaseManager.initialize()
    }

    private fun initializeRepositories() {
        profileRepository = SqlProfileRepository(databaseManager)
        trustRepository = InMemoryTrustRepository()
        unionRepository = InMemoryUnionRepository()
    }

    private fun initializeServices() {
        profileService = ProfileService(plugin, profileRepository, scheduler)
        trustService = TrustService(plugin, trustRepository)
        unionService = UnionService(plugin, unionRepository, profileRepository)
        combatService = CombatService(trustService, unionService)
        vowRegistry = VowRegistry(plugin)
        vowZeus = VowZeus()
        cooldownService = CooldownService(plugin)
        abilityExecutor = AbilityExecutor(plugin, vowRegistry, cooldownService)
        vfxBroadcastService = VFXBroadcastService(plugin)
        scrollItemRegistry = ScrollItemRegistry(plugin)
        trialService = TrialService(plugin, scheduler)
        displayService = StatusDisplay(plugin, scheduler)
    }

    private fun registerVowsAndTrials() {
        vowRegistry.register(VowAres())
        vowRegistry.register(VowLoki())
        vowRegistry.register(VowHades())
        vowRegistry.register(vowZeus)
        registerDefaultTrials()
    }

    private fun assembleContainer(): ServiceContainer {
        return ServiceContainer(
            plugin = plugin,
            configManager = configManager,
            databaseManager = databaseManager,
            profileService = profileService,
            trustService = trustService,
            unionService = unionService,
            combatService = combatService,
            cooldownService = cooldownService,
            abilityExecutor = abilityExecutor,
            trialService = trialService,
            displayService = displayService,
            scheduler = scheduler,
            vfxBroadcastService = vfxBroadcastService,
            vowRegistry = vowRegistry,
            vowZeus = vowZeus,
            scrollItemRegistry = scrollItemRegistry
        )
    }

    private fun registerPresentation(container: ServiceContainer) {
        ListenerRegistrar(container).register()
        CommandRegistrar(container).register()
    }

    private fun startBackgroundServices(container: ServiceContainer) {
        container.trialService.start()
        container.displayService.start()
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
            baseHealth = 60.0,
            baseDamage = 6.0,
            timeLimitSeconds = 600,
            maxPartySize = 4
        ))
        trialService.registerDefaultConfig(TrialConfig(
            key = "loki",
            displayName = "Loki",
            vowKey = "loki",
            description = "Outwit the God of Mischief",
            waves = listOf(
                WaveConfig(1, listOf(MobSpawnEntry(EntityType.SPIDER, 3), MobSpawnEntry(EntityType.CREEPER, 2))),
                WaveConfig(2, listOf(MobSpawnEntry(EntityType.SKELETON, 4), MobSpawnEntry(EntityType.SPIDER, 3))),
                WaveConfig(3, listOf(MobSpawnEntry(EntityType.VEX, 3), MobSpawnEntry(EntityType.CREEPER, 3)))
            ),
            baseHealth = 45.0,
            baseDamage = 5.0,
            timeLimitSeconds = 600,
            maxPartySize = 4
        ))
        trialService.registerDefaultConfig(TrialConfig(
            key = "hades",
            displayName = "Hades",
            vowKey = "hades",
            description = "Survive the depths of the Underworld",
            waves = listOf(
                WaveConfig(1, listOf(MobSpawnEntry(EntityType.ZOMBIE, 4), MobSpawnEntry(EntityType.SKELETON, 2))),
                WaveConfig(2, listOf(MobSpawnEntry(EntityType.WITHER_SKELETON, 3), MobSpawnEntry(EntityType.SKELETON, 3))),
                WaveConfig(3, listOf(MobSpawnEntry(EntityType.BLAZE, 2), MobSpawnEntry(EntityType.WITHER_SKELETON, 4)))
            ),
            baseHealth = 65.0,
            baseDamage = 6.5,
            timeLimitSeconds = 600,
            maxPartySize = 4
        ))
        trialService.registerDefaultConfig(TrialConfig(
            key = "zeus",
            displayName = "Zeus",
            vowKey = "zeus",
            description = "Survive the storm of the Sky God",
            waves = listOf(
                WaveConfig(1, listOf(MobSpawnEntry(EntityType.ZOMBIE, 4), MobSpawnEntry(EntityType.SPIDER, 2))),
                WaveConfig(2, listOf(MobSpawnEntry(EntityType.SKELETON, 4), MobSpawnEntry(EntityType.CREEPER, 2))),
                WaveConfig(3, listOf(MobSpawnEntry(EntityType.BLAZE, 3), MobSpawnEntry(EntityType.SKELETON, 3)))
            ),
            baseHealth = 55.0,
            baseDamage = 5.5,
            timeLimitSeconds = 600,
            maxPartySize = 4
        ))
    }

    private fun logFatalTrace(e: Exception) {
        plugin.logger.severe("==========================================")
        plugin.logger.severe("[MythosSMP] BOOTSTRAP FAILED at step [$currentStep]")
        plugin.logger.severe("[MythosSMP] Plugin will NOT load to avoid corrupted state.")
        plugin.logger.severe("[MythosSMP] Exception: ${e.javaClass.simpleName}: ${e.message}")
        e.stackTrace.forEach { plugin.logger.severe("    at $it") }
        plugin.logger.severe("==========================================")
    }
}
