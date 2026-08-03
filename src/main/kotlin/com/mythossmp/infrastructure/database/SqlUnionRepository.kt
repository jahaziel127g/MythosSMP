package com.mythossmp.infrastructure.database

import com.mythossmp.application.union.UnionRepository
import com.mythossmp.domain.model.SealOfUnion
import java.sql.ResultSet
import java.time.Instant
import java.util.UUID

class SqlUnionRepository(private val databaseManager: DatabaseManager) : UnionRepository {

    override fun saveUnion(union: SealOfUnion) {
        databaseManager.getConnection().use { conn ->
            conn.prepareStatement(
                """
                INSERT INTO mythos_unions (union_id, party_a_uuid, party_b_uuid, active, creation_timestamp)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT(union_id) DO UPDATE SET
                    party_a_uuid = EXCLUDED.party_a_uuid,
                    party_b_uuid = EXCLUDED.party_b_uuid,
                    active = EXCLUDED.active
                """.trimIndent()
            ).use { stmt ->
                stmt.setString(1, union.id.toString())
                stmt.setString(2, union.playerAUuid.toString())
                stmt.setString(3, union.playerBUuid.toString())
                stmt.setInt(4, if (union.active) 1 else 0)
                stmt.setString(5, union.createdAt.toString())
                stmt.executeUpdate()
            }
        }
    }

    override fun removeUnion(unionId: UUID) {
        databaseManager.getConnection().use { conn ->
            conn.prepareStatement(
                "DELETE FROM mythos_unions WHERE union_id = ?"
            ).use { stmt ->
                stmt.setString(1, unionId.toString())
                stmt.executeUpdate()
            }
        }
    }

    override fun getUnion(unionId: UUID): SealOfUnion? {
        databaseManager.getConnection().use { conn ->
            conn.prepareStatement(
                "SELECT * FROM mythos_unions WHERE union_id = ?"
            ).use { stmt ->
                stmt.setString(1, unionId.toString())
                stmt.executeQuery().use { rs ->
                    return if (rs.next()) mapRow(rs) else null
                }
            }
        }
    }

    override fun getUnionByPlayer(playerUuid: UUID): SealOfUnion? {
        databaseManager.getConnection().use { conn ->
            conn.prepareStatement(
                "SELECT * FROM mythos_unions WHERE party_a_uuid = ? OR party_b_uuid = ? LIMIT 1"
            ).use { stmt ->
                stmt.setString(1, playerUuid.toString())
                stmt.setString(2, playerUuid.toString())
                stmt.executeQuery().use { rs ->
                    return if (rs.next()) mapRow(rs) else null
                }
            }
        }
    }

    override fun isInUnion(playerUuid: UUID): Boolean {
        databaseManager.getConnection().use { conn ->
            conn.prepareStatement(
                "SELECT 1 FROM mythos_unions WHERE party_a_uuid = ? OR party_b_uuid = ? LIMIT 1"
            ).use { stmt ->
                stmt.setString(1, playerUuid.toString())
                stmt.setString(2, playerUuid.toString())
                stmt.executeQuery().use { rs ->
                    return rs.next()
                }
            }
        }
    }

    override fun removeUnionByPlayer(playerUuid: UUID) {
        databaseManager.getConnection().use { conn ->
            conn.prepareStatement(
                "DELETE FROM mythos_unions WHERE party_a_uuid = ? OR party_b_uuid = ?"
            ).use { stmt ->
                stmt.setString(1, playerUuid.toString())
                stmt.setString(2, playerUuid.toString())
                stmt.executeUpdate()
            }
        }
    }

    private fun mapRow(rs: ResultSet): SealOfUnion? {
        val id = runCatching { UUID.fromString(rs.getString("union_id")) }.getOrNull() ?: return null
        val partyA = runCatching { UUID.fromString(rs.getString("party_a_uuid")) }.getOrNull() ?: return null
        val partyB = runCatching { UUID.fromString(rs.getString("party_b_uuid")) }.getOrNull() ?: return null
        val active = rs.getInt("active") != 0
        val createdAt = runCatching { Instant.parse(rs.getString("creation_timestamp")) }
            .getOrElse { Instant.EPOCH }
        return SealOfUnion(
            id = id,
            playerAUuid = partyA,
            playerBUuid = partyB,
            createdAt = createdAt,
            active = active
        )
    }
}