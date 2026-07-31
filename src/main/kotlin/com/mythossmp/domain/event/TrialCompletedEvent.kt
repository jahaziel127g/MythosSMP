package com.mythossmp.domain.event

import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import java.util.UUID

class TrialCompletedEvent(
    val playerUuid: UUID,
    val trialKey: String,
    val displayName: String
) : Event() {

    override fun getHandlers(): HandlerList = HANDLERS

    companion object {
        private val HANDLERS = HandlerList()
        @JvmStatic
        fun getHandlerList(): HandlerList = HANDLERS
    }
}
