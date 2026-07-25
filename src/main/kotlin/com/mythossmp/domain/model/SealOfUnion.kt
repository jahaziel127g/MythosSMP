package com.mythossmp.domain.model

import java.time.Instant
import java.util.UUID

data class SealOfUnion(
    val id: UUID = UUID.randomUUID(),
    val playerAUuid: UUID,
    val playerBUuid: UUID,
    val createdAt: Instant = Instant.now(),
    val active: Boolean = true
) {
    fun partnerOf(uuid: UUID): UUID? = when (uuid) {
        playerAUuid -> playerBUuid
        playerBUuid -> playerAUuid
        else -> null
    }

    fun contains(uuid: UUID): Boolean = uuid == playerAUuid || uuid == playerBUuid
}
