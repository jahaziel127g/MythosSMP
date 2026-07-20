package com.mythossmp.domain.vow

import com.mythossmp.MythosSMP
import com.mythossmp.domain.model.Vow

class VowRegistry(private val plugin: MythosSMP) {

    private val vows = mutableMapOf<String, Vow>()

    fun register(vow: Vow) {
        val normalizedKey = vow.key.lowercase()
        vows[normalizedKey] = vow
        plugin.logger.info("Registered vow: ${vow.displayName} (${normalizedKey})")
    }

    fun getVow(key: String): Vow? = vows[key.lowercase()]

    fun normalizeActiveKey(activeKey: String): String {
        return activeKey.lowercase().removePrefix("vow-")
    }

    fun getVowByActiveKey(activeKey: String): Vow? {
        return vows[normalizeActiveKey(activeKey)]
    }

    fun getAllVows(): Collection<Vow> = vows.values

    fun getActiveVowForPlayer(playerUuid: java.util.UUID): Vow? {
        val profile = plugin.profileService.getProfile(playerUuid) ?: return null
        val activeKey = profile.activeVow ?: return null
        return getVowByActiveKey(activeKey)
    }
}
