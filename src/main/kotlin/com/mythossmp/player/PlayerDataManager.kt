package com.mythossmp.player

import com.mythossmp.MythosSMP
import com.mythossmp.database.DatabaseManager
import com.mythossmp.vow.implementations.Bloodthorns
import com.mythossmp.vow.implementations.VowAres
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

class PlayerDataManager(
    private val plugin: MythosSMP,
    private val databaseManager: DatabaseManager
) : Listener {

    private val cache: ConcurrentHashMap<UUID, PlayerProfile> = ConcurrentHashMap()

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

        val profile =         cache.compute(uuid) { _, existing ->
            if (existing == null) {
                try {
                    loadOrCreateProfile(uuid, name)
                } catch (e: SQLException) {
                    plugin.logger.severe("Failed to load profile for $uuid during join: ${e.message}")
                    null
                }
            } else {
                existing.lastKnownName = name
                existing.dirty = true
                existing
            }
        } ?: return

        if (profile.activeVow != null) {
            val vow = plugin.vowRegistry.getVowByActiveKey(profile.activeVow!!)
            if (vow != null) {
                vow.passiveEffects.forEach { it.apply(event.player) }
                vow.curse.apply(event.player)
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val uuid = event.player.uniqueId
        val profile = cache[uuid]

        if (profile != null && profile.dirty) {
            saveProfileSync(profile)
        }
        cache.remove(uuid)

        VowAres.passive.remove(event.player)
        Bloodthorns.activePlayers.remove(uuid)
        plugin.cooldownManager.removePlayer(event.player)
    }

    fun getProfile(uuid: UUID): PlayerProfile? {
        return cache[uuid]
    }

    fun markDirty(uuid: UUID) {
        cache.computeIfPresent(uuid) { _, profile ->
            profile.dirty = true
            profile
        }
    }

    private fun saveProfileSync(profile: PlayerProfile) {
        databaseManager.getConnection().use { conn ->
            conn.prepareStatement(
                """
                INSERT INTO mythos_players (player_uuid, last_known_name, active_vow, cooldown_multiplier, last_updated)
                VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)
                ON CONFLICT(player_uuid) DO UPDATE SET
                    last_known_name = EXCLUDED.last_known_name,
                    active_vow = EXCLUDED.active_vow,
                    cooldown_multiplier = EXCLUDED.cooldown_multiplier,
                    last_updated = CURRENT_TIMESTAMP
                """.trimIndent()
            ).use { stmt ->
                stmt.setString(1, profile.playerUuid.toString())
                stmt.setString(2, profile.lastKnownName)
                stmt.setString(3, profile.activeVow)
                stmt.setDouble(4, profile.cooldownMultiplier)
                stmt.executeUpdate()
            }
        }
        profile.dirty = false
    }

    suspend fun flushAll() = withContext(Dispatchers.IO) {
        cache.values.forEach { profile ->
            if (profile.dirty) {
                saveProfileSync(profile)
            }
        }
    }

    private fun loadOrCreateProfile(uuid: UUID, name: String): PlayerProfile {
        try {
            databaseManager.getConnection().use { conn ->
                conn.prepareStatement(
                    "SELECT * FROM mythos_players WHERE player_uuid = ?"
                ).use { stmt ->
                    stmt.setString(1, uuid.toString())
                    stmt.executeQuery().use { rs ->
                        if (rs.next()) {
                            return PlayerProfile(
                                playerUuid = uuid,
                                lastKnownName = rs.getString("last_known_name"),
                                activeVow = rs.getString("active_vow"),
                                cooldownMultiplier = rs.getDouble("cooldown_multiplier")
                            )
                        }
                    }
                }

                conn.prepareStatement(
                    "INSERT INTO mythos_players (player_uuid, last_known_name) VALUES (?, ?)"
                ).use { stmt ->
                    stmt.setString(1, uuid.toString())
                    stmt.setString(2, name)
                    stmt.executeUpdate()
                }
            }
        } catch (e: Exception) {
            plugin.logger.severe("Failed to load/create profile for $uuid: ${e.message}")
        }

        return PlayerProfile(
            playerUuid = uuid,
            lastKnownName = name
        )
    }
}
