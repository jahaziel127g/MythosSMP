package com.mythossmp.bootstrap

import com.mythossmp.domain.vow.AresListeners
import com.mythossmp.domain.vow.HadesListener
import com.mythossmp.domain.vow.HermesListener
import com.mythossmp.domain.vow.HerculesListener
import com.mythossmp.domain.vow.LokiBackstabListener
import com.mythossmp.domain.vow.PlutusListener
import com.mythossmp.domain.vow.PoseidonListener
import com.mythossmp.domain.vow.VeilFirstHitListener
import com.mythossmp.domain.vow.VowAres
import com.mythossmp.domain.vow.VowHades
import com.mythossmp.domain.vow.VowHermes
import com.mythossmp.domain.vow.VowHercules
import com.mythossmp.domain.vow.VowPlutus
import com.mythossmp.domain.vow.VowPoseidon
import com.mythossmp.presentation.listener.CombatListener
import com.mythossmp.presentation.listener.TrialListener
import com.mythossmp.presentation.listener.UnionListener
import com.mythossmp.presentation.listener.VFXPresentationListener
import com.mythossmp.presentation.listener.WorldSaveListener
import com.mythossmp.presentation.listener.ZeusListener
import com.mythossmp.presentation.ui.menu.AltarListener
import com.mythossmp.presentation.ui.menu.MenuListener
import com.mythossmp.presentation.ui.menu.VowConfirmationListener

class ListenerRegistrar(private val container: ServiceContainer) {

    fun register() {
        val plugin = container.plugin
        val pm = plugin.server.pluginManager

        pm.registerEvents(container.profileService, plugin)
        pm.registerEvents(VowAres.passive, plugin)
        pm.registerEvents(VowAres.curseInstance, plugin)
        pm.registerEvents(AresListeners(), plugin)
        pm.registerEvents(CombatListener(container.combatService), plugin)
        pm.registerEvents(UnionListener(container.unionService), plugin)
        pm.registerEvents(TrialListener(plugin, container.trialService, container.scrollItemRegistry), plugin)
        pm.registerEvents(LokiBackstabListener(), plugin)
        pm.registerEvents(VeilFirstHitListener(), plugin)
        pm.registerEvents(VowHades.passive, plugin)
        pm.registerEvents(VowHades.curseInstance, plugin)
        pm.registerEvents(HadesListener(), plugin)
        pm.registerEvents(ZeusListener(
            container.plugin,
            container.vowZeus,
            container.vfxBroadcastService,
            container.configManager.getZeusConfig()
        ), plugin)
        pm.registerEvents(PoseidonListener(), plugin)
        pm.registerEvents(VowPoseidon.curseInstance, plugin)
        pm.registerEvents(HermesListener(), plugin)
        pm.registerEvents(VowHermes.curseInstance, plugin)
        pm.registerEvents(HerculesListener(), plugin)
        pm.registerEvents(PlutusListener(), plugin)
        pm.registerEvents(VFXPresentationListener(), plugin)
        pm.registerEvents(WorldSaveListener(container.profileService, container.scheduler), plugin)
        pm.registerEvents(MenuListener(container.plugin), plugin)
        pm.registerEvents(AltarListener(container.plugin), plugin)
        pm.registerEvents(VowConfirmationListener(container.plugin, container.scheduler), plugin)
    }
}
