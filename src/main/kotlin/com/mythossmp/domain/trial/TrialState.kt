package com.mythossmp.domain.trial

enum class TrialState {
    INACTIVE, SUMMONING, ACTIVE, COMPLETED, FAILED;

    fun canTransitionTo(next: TrialState): Boolean = when (this) {
        INACTIVE -> next == SUMMONING
        SUMMONING -> next == ACTIVE || next == FAILED
        ACTIVE -> next == COMPLETED || next == FAILED
        COMPLETED -> next == INACTIVE
        FAILED -> next == INACTIVE
    }
}
