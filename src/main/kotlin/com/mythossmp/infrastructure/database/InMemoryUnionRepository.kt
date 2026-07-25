package com.mythossmp.infrastructure.database

import com.mythossmp.application.union.UnionRepository
import com.mythossmp.domain.model.SealOfUnion
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class InMemoryUnionRepository : UnionRepository {

    private val unionsById = ConcurrentHashMap<UUID, SealOfUnion>()
    private val unionsByPlayer = ConcurrentHashMap<UUID, UUID>()

    override fun saveUnion(union: SealOfUnion) {
        unionsById[union.id] = union
        unionsByPlayer[union.playerAUuid] = union.id
        unionsByPlayer[union.playerBUuid] = union.id
    }

    override fun removeUnion(unionId: UUID) {
        val union = unionsById.remove(unionId) ?: return
        unionsByPlayer.remove(union.playerAUuid, unionId)
        unionsByPlayer.remove(union.playerBUuid, unionId)
    }

    override fun getUnion(unionId: UUID): SealOfUnion? = unionsById[unionId]

    override fun getUnionByPlayer(playerUuid: UUID): SealOfUnion? {
        val unionId = unionsByPlayer[playerUuid] ?: return null
        return unionsById[unionId]
    }

    override fun isInUnion(playerUuid: UUID): Boolean = unionsByPlayer.containsKey(playerUuid)

    override fun removeUnionByPlayer(playerUuid: UUID) {
        val unionId = unionsByPlayer[playerUuid] ?: return
        removeUnion(unionId)
    }
}
