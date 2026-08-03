package com.mythossmp.application.profile

import com.mythossmp.MythosSMP
import com.mythossmp.application.task.Scheduler
import com.mythossmp.domain.model.PlayerProfile
import com.mythossmp.domain.vow.Bloodthorns
import com.mythossmp.domain.vow.VowAres
import com.mythossmp.domain.vow.VowHades
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.sql.SQLException
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class ProfileService(
    private val plugin: MythosSMP,
    private val profileRepository: ProfileRepository,
    private val scheduler: Scheduler
) : Listener {

    private val cache: ConcurrentHashMap<UUID, PlayerProfile> = ConcurrentHashMap()

    fun updateProfile(uuid: UUID, transform: (PlayerProfile) -> PlayerProfile) {
        cache.compute(uuid) { _, existing ->
            existing?.let(transform)
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerPreLogin(event: AsyncPlayerPreLoginEvent) {
        val uuid = event.uniqueId
        val name = event.name

        val profile = try {
            loadOrCreateProfile(uuid, name)
        } catch (e: SQLException) {
            plugin.logger.severe("Failed to load profile for $uuid during pre-login: ${e.message}")
            null
        }
        if (profile != null) {
            cache[uuid] = profile
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val uuid = event.player.uniqueId
        val name = event.player.name

        val profile = cache.compute(uuid) { _, existing ->
            if (existing == null) {
                try {
                    loadOrCreateProfile(uuid, name)
                } catch (e: SQLException) {
                    plugin.logger.severe("Failed to load profile for $uuid during join: ${e.message}")
                    null
                }
            } else {
                if (existing.lastKnownName != name) {
                    existing.copy(lastKnownName = name, dirty = true)
                } else {
                    existing
                }
            }
        } ?: return

        profile.activeVow?.let { activeKey ->
            val vow = plugin.vowRegistry.getVowByActiveKey(activeKey)
            if (vow != null) {
                vow.passiveEffects.forEach { it.apply(event.player) }
                vow.curse.apply(event.player)
            }
            when (activeKey.removePrefix("vow-")) {
                "ares" -> VowAres.restoreRage(uuid, profile.aresRage)
                "hades" -> VowHades.restoreCharges(uuid, profile.hadesCharges)
                "zeus" -> plugin.vowZeus.restoreCharge(uuid, profile.zeusCharge)
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val uuid = event.player.uniqueId
        val profile = cache.remove(uuid)

        if (profile != null) {
            val vow = profile.activeVow?.let { plugin.vowRegistry.getVowByActiveKey(it) }

            val snapshot = when (profile.activeVow?.removePrefix("vow-")) {
                "ares" -> profile.copy(aresRage = VowAres.getRage(uuid), dirty = true)
                "hades" -> profile.copy(hadesCharges = VowHades.getCharges(uuid), dirty = true)
                "zeus" -> profile.copy(zeusCharge = plugin.vowZeus.getCharge(uuid), dirty = true)
                else -> profile
            }

            if (vow != null) {
                vow.passiveEffects.forEach { it.remove(event.player) }
                vow.curse.remove(event.player)
            }

            if (snapshot.dirty) {
                scheduler.async { saveProfileSync(snapshot) }
            }
        }

        Bloodthorns.activePlayers.remove(uuid)
        plugin.cooldownService.removePlayer(event.player)
    }

    fun getProfile(uuid: UUID): PlayerProfile? {
        return cache[uuid]
    }

    fun markDirty(uuid: UUID) {
        cache.computeIfPresent(uuid) { _, profile ->
            profile.copy(dirty = true)
        }
    }

    private fun saveProfileSync(profile: PlayerProfile) {
        profileRepository.save(profile)
    }

    suspend fun flushAll() = withContext(Dispatchers.IO) {
        val entries = cache.entries.toList()
        for ((uuid, profile) in entries) {
            if (profile.dirty) {
                try {
                    saveProfileSync(profile)
                    cache.computeIfPresent(uuid) { _, _ -> profile.copy(dirty = false) }
                } catch (e: Exception) {
                    plugin.logger.severe("Failed to flush profile $uuid: ${e.message}")
                }
            }
        }
    }

    private fun loadOrCreateProfile(uuid: UUID, name: String): PlayerProfile {
        return profileRepository.findByUuid(uuid) ?: profileRepository.create(uuid, name)
    }
}
