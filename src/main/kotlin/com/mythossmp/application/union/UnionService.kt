package com.mythossmp.application.union

import com.mythossmp.MythosSMP
import com.mythossmp.application.profile.ProfileRepository
import com.mythossmp.domain.event.DoubleLossEvent
import com.mythossmp.domain.model.SealOfUnion
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class UnionService(
    private val plugin: MythosSMP,
    private val unionRepository: UnionRepository,
    private val profileRepository: ProfileRepository
) {

    private val disconnectedInUnion = ConcurrentHashMap.newKeySet<UUID>()

    fun createUnion(a: UUID, b: UUID): SealOfUnion? {
        if (a == b) return null
        if (unionRepository.isInUnion(a) || unionRepository.isInUnion(b)) return null
        val union = SealOfUnion(playerAUuid = a, playerBUuid = b)
        unionRepository.saveUnion(union)
        plugin.logger.info("Seal of Union created between $a and $b (${union.id})")
        return union
    }

    fun breakUnion(playerUuid: UUID, reason: String): Boolean {
        val union = unionRepository.getUnionByPlayer(playerUuid) ?: return false
        unionRepository.removeUnion(union.id)
        disconnectedInUnion.remove(union.playerAUuid)
        disconnectedInUnion.remove(union.playerBUuid)
        plugin.logger.info("Seal of Union broken between ${union.playerAUuid} and ${union.playerBUuid}: $reason")
        return true
    }

    fun getUnion(playerUuid: UUID): SealOfUnion? = unionRepository.getUnionByPlayer(playerUuid)

    fun isInUnion(playerUuid: UUID): Boolean = unionRepository.isInUnion(playerUuid)

    fun areInSameUnion(a: UUID, b: UUID): Boolean {
        val union = unionRepository.getUnionByPlayer(a) ?: return false
        return union.contains(b) && union.active
    }

    fun markDisconnected(playerUuid: UUID) {
        if (unionRepository.isInUnion(playerUuid)) {
            disconnectedInUnion.add(playerUuid)
        }
    }

    fun clearDisconnected(playerUuid: UUID) {
        disconnectedInUnion.remove(playerUuid)
    }

    fun isDisconnectedFromUnion(playerUuid: UUID): Boolean = disconnectedInUnion.contains(playerUuid)

    fun handlePlayerDeath(deathUuid: UUID): DoubleLossEvent? {
        val union = unionRepository.getUnionByPlayer(deathUuid) ?: return null
        val partnerUuid = union.partnerOf(deathUuid) ?: return null

        val events = mutableListOf<DoubleLossEvent>()

        removeVowFromPlayer(deathUuid)
        removeVowFromPlayer(partnerUuid)

        val doubleLoss = DoubleLossEvent(
            deathUuid = deathUuid,
            partnerUuid = partnerUuid,
            unionId = union.id
        )
        events.add(doubleLoss)

        plugin.logger.warning(
            "Double-loss triggered: $deathUuid died while in union with $partnerUuid. " +
            "Both players lost their active Vows."
        )

        unionRepository.removeUnion(union.id)
        disconnectedInUnion.remove(deathUuid)
        disconnectedInUnion.remove(partnerUuid)

        return doubleLoss
    }

    private fun removeVowFromPlayer(uuid: UUID) {
        val player = plugin.server.getPlayer(uuid)
        if (player != null && player.isOnline) {
            val profile = plugin.profileService.getProfile(uuid) ?: return
            val activeKey = profile.activeVow ?: return
            val vow = plugin.vowRegistry.getVowByActiveKey(activeKey)
            vow?.passiveEffects?.forEach { it.remove(player) }
            vow?.curse?.remove(player)
            plugin.profileService.updateProfile(uuid) {
                it.copy(activeVow = null, dirty = true)
            }
            player.sendMessage(
                net.kyori.adventure.text.Component.text()
                    .color(net.kyori.adventure.text.format.NamedTextColor.RED)
                    .append(net.kyori.adventure.text.Component.text("\u2620 Your Seal of Union has been broken! "))
                    .append(net.kyori.adventure.text.Component.text("Your Vow has been revoked.", net.kyori.adventure.text.format.NamedTextColor.DARK_RED))
                    .build()
            )
        } else {
            val profile = profileRepository.findByUuid(uuid) ?: return
            if (profile.activeVow != null) {
                profileRepository.save(profile.copy(activeVow = null, dirty = true))
            }
        }
    }
}
