package com.mythossmp.bootstrap

import com.mythossmp.MythosSMP
import com.mythossmp.application.ability.AbilityExecutor
import com.mythossmp.application.ability.CooldownService
import com.mythossmp.application.combat.CombatService
import com.mythossmp.application.profile.ProfileService
import com.mythossmp.application.task.Scheduler
import com.mythossmp.application.trial.TrialService
import com.mythossmp.application.trust.TrustService
import com.mythossmp.application.union.UnionService
import com.mythossmp.application.vfx.VFXBroadcastService
import com.mythossmp.domain.vow.VowRegistry
import com.mythossmp.domain.vow.VowZeus
import com.mythossmp.infrastructure.config.ConfigManager
import com.mythossmp.infrastructure.database.DatabaseManager
import com.mythossmp.infrastructure.trial.ScrollItemRegistry
import com.mythossmp.presentation.display.StatusDisplay

class ServiceContainer(
    val plugin: MythosSMP,
    val configManager: ConfigManager,
    val databaseManager: DatabaseManager,
    val profileService: ProfileService,
    val trustService: TrustService,
    val unionService: UnionService,
    val combatService: CombatService,
    val cooldownService: CooldownService,
    val abilityExecutor: AbilityExecutor,
    val trialService: TrialService,
    val displayService: StatusDisplay,
    val scheduler: Scheduler,
    val vfxBroadcastService: VFXBroadcastService,
    val vowRegistry: VowRegistry,
    val vowZeus: VowZeus,
    val scrollItemRegistry: ScrollItemRegistry
)
