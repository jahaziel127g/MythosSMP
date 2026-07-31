package com.mythossmp

import com.mythossmp.application.ability.AbilityExecutor
import com.mythossmp.application.ability.CooldownService
import com.mythossmp.application.combat.CombatService
import com.mythossmp.application.profile.ProfileService
import com.mythossmp.application.task.Scheduler
import com.mythossmp.application.trial.TrialService
import com.mythossmp.application.trust.TrustService
import com.mythossmp.application.union.UnionService
import com.mythossmp.application.vfx.VFXBroadcastService
import com.mythossmp.bootstrap.Bootstrap
import com.mythossmp.bootstrap.ServiceContainer
import com.mythossmp.domain.vow.VowRegistry
import com.mythossmp.infrastructure.config.ConfigManager
import com.mythossmp.infrastructure.database.DatabaseManager
import com.mythossmp.infrastructure.trial.ScrollItemRegistry
import com.mythossmp.presentation.display.StatusDisplay
import kotlinx.coroutines.runBlocking
import org.bukkit.plugin.java.JavaPlugin

class MythosSMP : JavaPlugin() {

    lateinit var container: ServiceContainer
        internal set

    lateinit var scheduler: Scheduler
        internal set

    val configManager: ConfigManager get() = container.configManager
    val databaseManager: DatabaseManager get() = container.databaseManager
    val profileService: ProfileService get() = container.profileService
    val trustService: TrustService get() = container.trustService
    val unionService: UnionService get() = container.unionService
    val combatService: CombatService get() = container.combatService
    val vowRegistry: VowRegistry get() = container.vowRegistry
    val cooldownService: CooldownService get() = container.cooldownService
    val abilityExecutor: AbilityExecutor get() = container.abilityExecutor
    val statusDisplay: StatusDisplay get() = container.displayService
    val trialService: TrialService get() = container.trialService
    val scrollItemRegistry: ScrollItemRegistry get() = container.scrollItemRegistry
    val vfxBroadcastService: VFXBroadcastService get() = container.vfxBroadcastService

    override fun onEnable() {
        instance = this
        Bootstrap(this).run()
        logger.info("MythosSMP v${pluginMeta.version} enabled successfully.")
    }

    override fun onDisable() {
        if (::container.isInitialized) {
            try {
                runBlocking {
                    container.profileService.flushAll()
                }
            } finally {
                container.displayService.stop()
                container.trialService.stop()
                container.databaseManager.shutdown()
                logger.info("MythosSMP disabled.")
            }
        }
    }

    companion object {
        lateinit var instance: MythosSMP
            private set
    }
}
