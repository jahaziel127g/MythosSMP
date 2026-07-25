package com.mythossmp.application.trust

import com.mythossmp.MythosSMP
import com.mythossmp.domain.model.TrustRelation
import java.util.UUID

class TrustService(
    private val plugin: MythosSMP,
    private val trustRepository: TrustRepository
) {

    fun addTrust(playerUuid: UUID, targetUuid: UUID): Boolean {
        if (playerUuid == targetUuid) return false
        if (trustRepository.isTrusted(playerUuid, targetUuid)) return false
        trustRepository.saveTrust(TrustRelation(playerUuid = playerUuid, trustedUuid = targetUuid))
        return true
    }

    fun removeTrust(playerUuid: UUID, targetUuid: UUID): Boolean {
        if (!trustRepository.isTrusted(playerUuid, targetUuid)) return false
        trustRepository.removeTrust(playerUuid, targetUuid)
        return true
    }

    fun getTrusted(playerUuid: UUID): List<UUID> {
        return trustRepository.getTrusted(playerUuid)
    }

    fun isTrusted(playerUuid: UUID, targetUuid: UUID): Boolean {
        return trustRepository.isTrusted(playerUuid, targetUuid)
    }

    fun areMutuallyTrusted(a: UUID, b: UUID): Boolean {
        return trustRepository.isTrusted(a, b) || trustRepository.isTrusted(b, a)
    }

    fun removeAllTrust(playerUuid: UUID) {
        trustRepository.removeAllTrust(playerUuid)
    }
}
