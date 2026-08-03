package com.mythossmp.infrastructure.database

import com.mythossmp.application.trust.TrustRepository
import com.mythossmp.domain.model.TrustRelation
import java.util.UUID

class SqlTrustRepository(private val databaseManager: DatabaseManager) : TrustRepository {

    override fun saveTrust(relation: TrustRelation) {
        databaseManager.getConnection().use { conn ->
            conn.prepareStatement(
                "INSERT OR IGNORE INTO mythos_trusts (player_uuid, trusted_uuid) VALUES (?, ?)"
            ).use { stmt ->
                stmt.setString(1, relation.playerUuid.toString())
                stmt.setString(2, relation.trustedUuid.toString())
                stmt.executeUpdate()
            }
        }
    }

    override fun removeTrust(playerUuid: UUID, trustedUuid: UUID) {
        databaseManager.getConnection().use { conn ->
            conn.prepareStatement(
                "DELETE FROM mythos_trusts WHERE player_uuid = ? AND trusted_uuid = ?"
            ).use { stmt ->
                stmt.setString(1, playerUuid.toString())
                stmt.setString(2, trustedUuid.toString())
                stmt.executeUpdate()
            }
        }
    }

    override fun getTrusted(playerUuid: UUID): List<UUID> {
        databaseManager.getConnection().use { conn ->
            conn.prepareStatement(
                "SELECT trusted_uuid FROM mythos_trusts WHERE player_uuid = ?"
            ).use { stmt ->
                stmt.setString(1, playerUuid.toString())
                stmt.executeQuery().use { rs ->
                    val result = mutableListOf<UUID>()
                    while (rs.next()) {
                        runCatching { UUID.fromString(rs.getString("trusted_uuid")) }
                            .getOrNull()?.let { result.add(it) }
                    }
                    return result
                }
            }
        }
    }

    override fun isTrusted(playerUuid: UUID, targetUuid: UUID): Boolean {
        databaseManager.getConnection().use { conn ->
            conn.prepareStatement(
                "SELECT 1 FROM mythos_trusts WHERE player_uuid = ? AND trusted_uuid = ?"
            ).use { stmt ->
                stmt.setString(1, playerUuid.toString())
                stmt.setString(2, targetUuid.toString())
                stmt.executeQuery().use { rs ->
                    return rs.next()
                }
            }
        }
    }

    override fun removeAllTrust(playerUuid: UUID) {
        databaseManager.getConnection().use { conn ->
            conn.prepareStatement(
                "DELETE FROM mythos_trusts WHERE player_uuid = ?"
            ).use { stmt ->
                stmt.setString(1, playerUuid.toString())
                stmt.executeUpdate()
            }
        }
    }
}