package com.mythossmp.domain.vow

import org.bukkit.Location
import org.bukkit.World
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class VowLokiTest {

    private lateinit var world: World

    @BeforeEach
    fun setUp() {
        world = mock()
        whenever(world.name).thenReturn("world")
    }

    @Test
    fun `backstab when attacker directly behind target`() {
        val targetLoc = Location(world, 0.0, 0.0, 0.0, 0f, 0f)
        val attackerLoc = Location(world, 0.0, 0.0, -5.0, 0f, 0f)

        val toAttacker = attackerLoc.toVector().subtract(targetLoc.toVector()).setY(0.0).normalize()
        val targetFacing = targetLoc.direction.clone().setY(0.0).normalize()
        val dot = targetFacing.dot(toAttacker)

        assertTrue(dot < -0.75, "Behind: dot=$dot should be < -0.75")
    }

    @Test
    fun `no backstab when attacker directly in front`() {
        val targetLoc = Location(world, 0.0, 0.0, 0.0, 0f, 0f)
        val attackerLoc = Location(world, 0.0, 0.0, 5.0, 0f, 0f)

        val toAttacker = attackerLoc.toVector().subtract(targetLoc.toVector()).setY(0.0).normalize()
        val targetFacing = targetLoc.direction.clone().setY(0.0).normalize()
        val dot = targetFacing.dot(toAttacker)

        assertTrue(dot > 0.75, "In front: dot=$dot should be > 0.75")
    }

    @Test
    fun `side attack gives dot near zero`() {
        val targetLoc = Location(world, 0.0, 0.0, 0.0, 0f, 0f)
        val attackerLoc = Location(world, 5.0, 0.0, 0.0, 0f, 0f)

        val toAttacker = attackerLoc.toVector().subtract(targetLoc.toVector()).setY(0.0).normalize()
        val targetFacing = targetLoc.direction.clone().setY(0.0).normalize()
        val dot = targetFacing.dot(toAttacker)

        assertTrue(Math.abs(dot) < 0.1, "Side: dot=$dot should be near 0")
    }

    @Test
    fun `y-axis flattened so pitch does not affect detection`() {
        val targetLoc = Location(world, 0.0, 0.0, 0.0, 0f, 0f)
        val attackerLoc = Location(world, 0.0, 0.0, -5.0, 0f, 90f)

        val toAttacker = attackerLoc.toVector().subtract(targetLoc.toVector()).setY(0.0).normalize()
        val targetFacing = targetLoc.direction.clone().setY(0.0).normalize()
        val dot = targetFacing.dot(toAttacker)

        assertTrue(dot < -0.75, "Looking up should still detect backstab: dot=$dot")
    }

    @Test
    fun `backstab behind target facing north`() {
        val targetLoc = Location(world, 0.0, 0.0, 0.0, 180f, 0f)
        val attackerLoc = Location(world, 0.0, 0.0, 5.0, 0f, 0f)

        val toAttacker = attackerLoc.toVector().subtract(targetLoc.toVector()).setY(0.0).normalize()
        val targetFacing = targetLoc.direction.clone().setY(0.0).normalize()
        val dot = targetFacing.dot(toAttacker)

        assertTrue(dot < -0.75, "North-facing target, behind from south: dot=$dot")
    }

    @Test
    fun `threshold discriminates front side and behind`() {
        val targetLoc = Location(world, 0.0, 0.0, 0.0, 0f, 0f)
        val behind = Location(world, 0.0, 0.0, -1.0, 0f, 0f)
        val side = Location(world, 1.0, 0.0, 0.0, 0f, 0f)
        val front = Location(world, 0.0, 0.0, 1.0, 0f, 0f)

        val targetFacing = targetLoc.direction.clone().setY(0.0).normalize()

        val behindDot = targetFacing.dot(behind.toVector().subtract(targetLoc.toVector()).setY(0.0).normalize())
        val sideDot = targetFacing.dot(side.toVector().subtract(targetLoc.toVector()).setY(0.0).normalize())
        val frontDot = targetFacing.dot(front.toVector().subtract(targetLoc.toVector()).setY(0.0).normalize())

        assertTrue(behindDot < -0.75, "Behind: dot=$behindDot")
        assertTrue(Math.abs(sideDot) < 0.75, "Side: dot=$sideDot")
        assertTrue(frontDot > 0.75, "Front: dot=$frontDot")
    }
}
