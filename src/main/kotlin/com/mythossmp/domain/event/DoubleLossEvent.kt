package com.mythossmp.domain.event

import java.time.Instant
import java.util.UUID

data class DoubleLossEvent(
    val deathUuid: UUID,
    val partnerUuid: UUID,
    val unionId: UUID,
    val timestamp: Instant = Instant.now()
)
