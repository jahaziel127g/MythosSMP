package com.mythossmp.presentation.ui.menu

import com.mythossmp.MythosSMP
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent

class AltarListener(
    private val plugin: MythosSMP
) : Listener {

    @EventHandler(priority = EventPriority.NORMAL)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return
        val clicked = event.clickedBlock ?: return

        if (clicked.type != ALTAR_MATERIAL) return

        event.isCancelled = true

        VowSelectionMenu(plugin).open(event.player)
    }

    companion object {
        val ALTAR_MATERIAL: Material = Material.CRYING_OBSIDIAN
    }
}
