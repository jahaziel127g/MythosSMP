package com.mythossmp.application.trust

import com.mythossmp.domain.model.TrustRelation
import java.util.UUID

interface TrustRepository {
    fun saveTrust(relation: TrustRelation)
    fun removeTrust(playerUuid: UUID, trustedUuid: UUID)
    fun getTrusted(playerUuid: UUID): List<UUID>
    fun isTrusted(playerUuid: UUID, targetUuid: UUID): Boolean
    fun removeAllTrust(playerUuid: UUID)
}
