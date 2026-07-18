package com.mythossmp.vow

import com.mythossmp.MythosSMP
import com.mythossmp.model.Vow

class VowRegistry(private val plugin: MythosSMP) {

    private val vows = mutableMapOf<String, Vow>()

    fun register(vow: Vow) {
        vows[vow.key] = vow
        plugin.logger.info("Registered vow: ${vow.displayName} (${vow.key})")
    }

    fun getVow(key: String): Vow? = vows[key.lowercase()]

    fun getVowByActiveKey(activeKey: String): Vow? {
        val normalized = activeKey.lowercase().removePrefix("vow-")
        return vows[normalized]
    }

    fun getAllVows(): Collection<Vow> = vows.values

    fun getEnabledVows(): List<Vow> = vows.values.filter { it.enabled }

    fun getActiveVowForPlayer(playerUuid: java.util.UUID): Vow? {
        val profile = plugin.playerDataManager.getProfile(playerUuid) ?: return null
        val activeKey = profile.activeVow ?: return null
        return getVowByActiveKey(activeKey)
    }
}
