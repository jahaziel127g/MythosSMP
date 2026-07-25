package com.mythossmp.domain.model

import java.time.Instant
import java.util.UUID

data class TrustRelation(
    val playerUuid: UUID,
    val trustedUuid: UUID,
    val createdAt: Instant = Instant.now()
)
