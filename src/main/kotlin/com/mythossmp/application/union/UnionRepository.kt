package com.mythossmp.application.union

import com.mythossmp.domain.model.SealOfUnion
import java.util.UUID

interface UnionRepository {
    fun saveUnion(union: SealOfUnion)
    fun removeUnion(unionId: UUID)
    fun getUnion(unionId: UUID): SealOfUnion?
    fun getUnionByPlayer(playerUuid: UUID): SealOfUnion?
    fun isInUnion(playerUuid: UUID): Boolean
    fun removeUnionByPlayer(playerUuid: UUID)
}
