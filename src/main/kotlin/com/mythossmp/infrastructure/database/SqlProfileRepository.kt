package com.mythossmp.infrastructure.database

import com.mythossmp.application.profile.ProfileRepository
import com.mythossmp.domain.model.PlayerProfile
import java.sql.SQLException
import java.util.UUID

class SqlProfileRepository(private val databaseManager: DatabaseManager) : ProfileRepository {

    override fun findByUuid(uuid: UUID): PlayerProfile? {
        databaseManager.getConnection().use { conn ->
            conn.prepareStatement(
                "SELECT * FROM mythos_players WHERE player_uuid = ?"
            ).use { stmt ->
                stmt.setString(1, uuid.toString())
                stmt.executeQuery().use { rs ->
                    if (rs.next()) {
                        val cooldownMultiplier = rs.getDouble("cooldown_multiplier")
                        val cmNull = rs.wasNull()
                        val rawActiveVow = rs.getString("active_vow")
                        return PlayerProfile(
                            playerUuid = uuid,
                            lastKnownName = rs.getString("last_known_name"),
                            activeVow = rawActiveVow?.lowercase()?.removePrefix("vow-"),
                            cooldownMultiplier = if (cmNull) 1.0 else cooldownMultiplier
                        )
                    }
                }
            }
        }
        return null
    }

    override fun create(uuid: UUID, name: String): PlayerProfile {
        databaseManager.getConnection().use { conn ->
            conn.prepareStatement(
                "INSERT OR IGNORE INTO mythos_players (player_uuid, last_known_name) VALUES (?, ?)"
            ).use { stmt ->
                stmt.setString(1, uuid.toString())
                stmt.setString(2, name)
                stmt.executeUpdate()
            }
        }

        return PlayerProfile(
            playerUuid = uuid,
            lastKnownName = name
        )
    }

    override fun save(profile: PlayerProfile) {
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
    }
}
