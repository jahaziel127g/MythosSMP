package com.mythossmp.presentation.listener

import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class VowUnlockEvent(player: Player, vowKey: String) : Event() {
    val player: Player = player
    val vowKey: String = vowKey

    override fun getHandlers(): HandlerList = HANDLERS

    companion object {
        private val HANDLERS = HandlerList()
        @JvmStatic fun getHandlerList() = HANDLERS
    }
}

class VowLossEvent(player: Player, vowKey: String) : Event() {
    val player: Player = player
    val vowKey: String = vowKey

    override fun getHandlers(): HandlerList = HANDLERS

    companion object {
        private val HANDLERS = HandlerList()
        @JvmStatic fun getHandlerList() = HANDLERS
    }
}

class TrialStartEvent(player: Player, vowKey: String, location: org.bukkit.Location) : Event() {
    val player: Player = player
    val vowKey: String = vowKey
    val location: org.bukkit.Location = location

    override fun getHandlers(): HandlerList = HANDLERS

    companion object {
        private val HANDLERS = HandlerList()
        @JvmStatic fun getHandlerList() = HANDLERS
    }
}
