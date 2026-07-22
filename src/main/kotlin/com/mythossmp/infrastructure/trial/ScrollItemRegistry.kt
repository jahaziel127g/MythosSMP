package com.mythossmp.infrastructure.trial

import com.mythossmp.MythosSMP
import com.mythossmp.domain.trial.TrialConfig
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType

class ScrollItemRegistry(private val plugin: MythosSMP) {

    private val trialKey = NamespacedKey(plugin, "trial_key")

    fun createScroll(config: TrialConfig): ItemStack {
        val item = ItemStack(Material.PAPER)
        val meta = item.itemMeta ?: return item

        meta.displayName(Component.text()
            .append(Component.text("\uD83D\uDCDC ", NamedTextColor.GOLD))
            .append(Component.text("Scroll of ", NamedTextColor.WHITE))
            .append(Component.text(config.displayName, NamedTextColor.GOLD))
            .build()
        )

        meta.lore(listOf(
            Component.text(config.description, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
            Component.empty(),
            Component.text("\u2694 Waves: ${config.totalWaves}", NamedTextColor.DARK_AQUA),
            Component.text("\uD83D\uDC65 Max Party: ${config.maxPartySize}", NamedTextColor.DARK_AQUA),
            Component.text("\u231B Time Limit: ${config.timeLimitSeconds}s", NamedTextColor.DARK_AQUA),
            Component.empty(),
            Component.text("Right-click to begin the trial.", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, true)
        ))

        meta.persistentDataContainer.set(trialKey, PersistentDataType.STRING, config.key)

        item.itemMeta = meta
        return item
    }

    fun getTrialKey(item: ItemStack): String? {
        val meta = item.itemMeta ?: return null
        return meta.persistentDataContainer.get(trialKey, PersistentDataType.STRING)
    }

    fun isScroll(item: ItemStack): Boolean {
        return item.type == Material.PAPER && getTrialKey(item) != null
    }
}
