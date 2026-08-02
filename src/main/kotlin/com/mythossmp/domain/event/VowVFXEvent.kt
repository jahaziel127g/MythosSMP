package com.mythossmp.domain.event

import org.bukkit.Location
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

sealed class VowVFXEvent(
    open val location: Location
) : Event() {

    data class AresBloodthorns(override val location: Location) : VowVFXEvent(location)
    data class AresRageAura(override val location: Location, val radius: Double) : VowVFXEvent(location)

    data class LokiBackstab(override val location: Location) : VowVFXEvent(location)
    data class LokiShadowStep(override val location: Location) : VowVFXEvent(location)
    data class LokiVeilActivate(override val location: Location) : VowVFXEvent(location)
    data class LokiVeilFirstHit(override val location: Location) : VowVFXEvent(location)

    data class HadesSoulHarvest(override val location: Location) : VowVFXEvent(location)
    data class HadesSoulDamage(override val location: Location) : VowVFXEvent(location)
    data class HadesDeathDefianceArm(override val location: Location) : VowVFXEvent(location)
    data class HadesDeathDefianceSave(override val location: Location) : VowVFXEvent(location)
    data class HadesSoulBurst(override val location: Location, val radius: Double) : VowVFXEvent(location)

    data class ZeusStaticOverchargeVFXEvent(override val location: Location) : VowVFXEvent(location)

    data class PoseidonWaveCrash(override val location: Location, val radius: Double) : VowVFXEvent(location)
    data class PoseidonAbyssalBlessing(override val location: Location) : VowVFXEvent(location)

    data class HermesDoubleJump(override val location: Location) : VowVFXEvent(location)
    data class HermesDash(override val location: Location) : VowVFXEvent(location)
    data class HermesZephyrActivate(override val location: Location) : VowVFXEvent(location)

    data class VowSelectionActivate(override val location: Location, val vowKey: String) : VowVFXEvent(location)

    override fun getHandlers(): HandlerList = HANDLERS

    companion object {
        private val HANDLERS = HandlerList()
        @JvmStatic
        fun getHandlerList(): HandlerList = HANDLERS
    }
}
