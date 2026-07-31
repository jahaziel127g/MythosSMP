package com.mythossmp.domain.vow

import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class VowZeusTest {

    private val vowZeus = VowZeus()

    @Test
    fun `charge accumulates and never exceeds 1-0`() {
        val uuid = UUID.randomUUID()
        vowZeus.addCharge(uuid, 0.4)
        vowZeus.addCharge(uuid, 0.4)
        vowZeus.addCharge(uuid, 0.4)
        assertEquals(1.0, vowZeus.getCharge(uuid), "Charge must be hard-capped at 1.0")
    }

    @Test
    fun `negative amounts never reduce charge`() {
        val uuid = UUID.randomUUID()
        vowZeus.addCharge(uuid, 0.5)
        vowZeus.addCharge(uuid, -10.0)
        assertEquals(0.5, vowZeus.getCharge(uuid), "Charge must never go negative")
    }

    @Test
    fun `consume only succeeds at full charge and resets the meter`() {
        val uuid = UUID.randomUUID()
        vowZeus.addCharge(uuid, 0.9)
        assertFalse(vowZeus.consumeCharge(uuid), "Partial charge must not consume")
        assertEquals(0.9, vowZeus.getCharge(uuid))

        vowZeus.addCharge(uuid, 0.1)
        assertTrue(vowZeus.consumeCharge(uuid), "Full charge must consume")
        assertEquals(0.0, vowZeus.getCharge(uuid), "Meter must reset after consume")
    }

    @Test
    fun `vow metadata is registered for the registry`() {
        assertEquals("zeus", vowZeus.key)
        assertEquals("Zeus", vowZeus.displayName)
        assertEquals("zeus-static-strike", vowZeus.ability1.key)
        assertEquals("zeus-overcharge", vowZeus.ability2.key)
        assertEquals(1, vowZeus.passiveEffects.size)
    }
}
