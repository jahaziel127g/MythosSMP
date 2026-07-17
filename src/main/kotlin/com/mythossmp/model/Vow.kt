package com.mythossmp.model

import java.util.UUID

interface Vow {
    val key: String
    val displayName: String
    val enabled: Boolean
}

data class ActiveVow(
    val vowKey: String,
    val playerUuid: UUID,
    val obtainedAt: Long = System.currentTimeMillis()
)
