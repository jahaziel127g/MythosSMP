package com.mythossmp.presentation.listener

import com.mythossmp.application.profile.ProfileService
import com.mythossmp.application.task.Scheduler
import kotlinx.coroutines.runBlocking
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.world.WorldSaveEvent

class WorldSaveListener(
    private val profileService: ProfileService,
    private val scheduler: Scheduler
) : Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    fun onWorldSave(event: WorldSaveEvent) {
        val dirty = profileService.collectDirtySnapshots()
        if (dirty.isEmpty()) return
        scheduler.async { runBlocking { profileService.persistSnapshots(dirty) } }
    }
}
