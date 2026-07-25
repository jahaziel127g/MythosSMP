package com.mythossmp.infrastructure.database

import com.mythossmp.application.trust.TrustRepository
import com.mythossmp.domain.model.TrustRelation
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class InMemoryTrustRepository : TrustRepository {

    private val trustMap: ConcurrentHashMap<UUID, CopyOnWriteArrayList<TrustRelation>> = ConcurrentHashMap()

    override fun saveTrust(relation: TrustRelation) {
        trustMap.computeIfAbsent(relation.playerUuid) { CopyOnWriteArrayList() }
            .add(relation)
    }

    override fun removeTrust(playerUuid: UUID, trustedUuid: UUID) {
        trustMap.computeIfPresent(playerUuid) { _, list ->
            list.removeAll { it.trustedUuid == trustedUuid }
            if (list.isEmpty()) null else list
        }
    }

    override fun getTrusted(playerUuid: UUID): List<UUID> {
        return trustMap[playerUuid]?.map { it.trustedUuid } ?: emptyList()
    }

    override fun isTrusted(playerUuid: UUID, targetUuid: UUID): Boolean {
        return trustMap[playerUuid]?.any { it.trustedUuid == targetUuid } == true
    }

    override fun removeAllTrust(playerUuid: UUID) {
        trustMap.remove(playerUuid)
    }
}
