package com.mythossmp.domain.event

import com.mythossmp.domain.model.SealOfUnion
import java.util.UUID

data class UnionBrokenEvent(
    val unionId: UUID,
    val playerAUuid: UUID,
    val playerBUuid: UUID,
    val reason: String
) {
    constructor(union: SealOfUnion, reason: String) : this(
        unionId = union.id,
        playerAUuid = union.playerAUuid,
        playerBUuid = union.playerBUuid,
        reason = reason
    )
}
