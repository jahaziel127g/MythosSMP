package com.mythossmp.presentation.ui.menu

import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder

class VowMenuHolder(val menu: VowSelectionMenu) : InventoryHolder {
    private var inv: Inventory? = null

    override fun getInventory(): Inventory = inv!!

    internal fun setInventory(inventory: Inventory) {
        inv = inventory
    }
}
