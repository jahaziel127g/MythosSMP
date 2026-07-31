package com.mythossmp.application.vfx

import com.mythossmp.domain.event.VowVFXEvent
import org.bukkit.plugin.java.JavaPlugin

class VFXBroadcastService(private val plugin: JavaPlugin) {

    fun publish(event: VowVFXEvent) {
        plugin.server.pluginManager.callEvent(event)
    }
}
