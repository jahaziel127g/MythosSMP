package com.mythossmp.application.profile

import com.mythossmp.domain.model.PlayerProfile
import java.util.UUID

interface ProfileRepository {
    fun findByUuid(uuid: UUID): PlayerProfile?
    fun create(uuid: UUID, name: String): PlayerProfile
    fun save(profile: PlayerProfile)
}
