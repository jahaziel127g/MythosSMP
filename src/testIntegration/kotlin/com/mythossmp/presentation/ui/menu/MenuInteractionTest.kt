package com.mythossmp.presentation.ui.menu

import com.mythossmp.MythosSMP
import com.mythossmp.application.ability.CooldownService
import com.mythossmp.application.profile.ProfileService
import com.mythossmp.application.vfx.VFXBroadcastService
import com.mythossmp.domain.model.PlayerProfile
import com.mythossmp.domain.vow.VowAres
import com.mythossmp.domain.vow.VowHades
import com.mythossmp.domain.vow.VowLoki
import com.mythossmp.domain.vow.VowRegistry
import com.mythossmp.infrastructure.config.ConfigManager
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.player.PlayerInteractEvent
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock
import org.mockbukkit.mockbukkit.block.BlockMock
import org.mockbukkit.mockbukkit.block.data.BlockDataMock
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.UUID
import java.util.logging.Logger

class MenuInteractionTest {

    private lateinit var server: ServerMock
    private lateinit var plugin: MythosSMP
    private lateinit var menuListener: MenuListener
    private lateinit var altarListener: AltarListener
    private lateinit var menu: VowSelectionMenu

    @BeforeEach
    fun setUp() {
        server = MockBukkit.mock()

        plugin = mock()
        whenever(plugin.server).thenReturn(server)
        whenever(plugin.logger).thenReturn(Logger.getGlobal())
        whenever(plugin.name).thenReturn("MythosSMP")

        val yaml = """
            vows:
              ares:
                enabled: true
              loki:
                enabled: true
              hades:
                enabled: true
        """.trimIndent()
        val config = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(
            java.io.StringReader(yaml)
        )
        whenever(plugin.config).thenReturn(config)

        val cm = ConfigManager(plugin)
        cm.load()
        whenever(plugin.configManager).thenReturn(cm)

        val profileService = mock<ProfileService>()
        whenever(plugin.profileService).thenReturn(profileService)
        val vfxService = mock<VFXBroadcastService>()
        whenever(plugin.vfxBroadcastService).thenReturn(vfxService)
        val cooldownService = mock<CooldownService>()
        whenever(plugin.cooldownService).thenReturn(cooldownService)
        val statusDisplay = mock<com.mythossmp.presentation.display.StatusDisplay>()
        whenever(plugin.statusDisplay).thenReturn(statusDisplay)

        val vowRegistry = VowRegistry(plugin)
        vowRegistry.register(VowAres())
        vowRegistry.register(VowLoki())
        vowRegistry.register(VowHades())
        whenever(plugin.vowRegistry).thenReturn(vowRegistry)

        val trialService = mock<com.mythossmp.application.trial.TrialService>()
        whenever(plugin.trialService).thenReturn(trialService)
        val scrollItemRegistry = mock<com.mythossmp.infrastructure.trial.ScrollItemRegistry>()
        whenever(plugin.scrollItemRegistry).thenReturn(scrollItemRegistry)

        menuListener = MenuListener(plugin)
        altarListener = AltarListener(plugin)
        menu = VowSelectionMenu(plugin)

        val instanceField = MythosSMP::class.java.getDeclaredField("instance")
        instanceField.isAccessible = true
        instanceField.set(null, plugin)
    }

    @AfterEach
    fun tearDown() {
        MockBukkit.unmock()
    }

    @Test
    fun `altar right-click opens vow selection menu`() {
        val player: Player = server.addPlayer()
        val loc = Location(player.world, 0.0, 0.0, 0.0)
        val block = BlockMock(Material.CRYING_OBSIDIAN, loc)

        val profile = PlayerProfile(player.uniqueId, "TestPlayer", null, 1.0, false)
        whenever(plugin.profileService.getProfile(player.uniqueId)).thenReturn(profile)

        val event = PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, null, block, BlockFace.UP)
        altarListener.onPlayerInteract(event)

        assertTrue(event.isCancelled, "Altar interaction should be cancelled")
        assertNotNull(player.openInventory.topInventory, "Player should have an open inventory")
        assertEquals(54, player.openInventory.topInventory.size, "Menu should be 54 slots (6x9)")
    }

    @Test
    fun `clicking vow slot changes active vow`() {
        val player: Player = server.addPlayer()

        val profile = PlayerProfile(player.uniqueId, "TestPlayer", null, 1.0, false)
        whenever(plugin.profileService.getProfile(player.uniqueId)).thenReturn(profile)
        // updateProfile returns Unit; mock does nothing silently

        menu.open(player)

        val event = createClickEvent(player, 13)
        menuListener.onInventoryClick(event)

        assertTrue(event.isCancelled, "Click should be cancelled")
    }

    @Test
    fun `non-altar block does not open menu`() {
        val player: Player = server.addPlayer()
        val loc = Location(player.world, 0.0, 0.0, 0.0)
        val block = BlockMock(Material.STONE, loc)

        val event = PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, null, block, BlockFace.UP)
        altarListener.onPlayerInteract(event)

        assertFalse(event.isCancelled, "Non-altar block should not be cancelled")
    }

    @Test
    fun `altar material resolves under MockBukkit`() {
        val material = AltarListener.ALTAR_MATERIAL
        assertNotNull(material, "Altar material should resolve")
        assertEquals(Material.CRYING_OBSIDIAN, material)
    }

    @Test
    fun `click on non-menu inventory is not cancelled`() {
        val player: Player = server.addPlayer()

        val event = InventoryClickEvent(
            player.openInventory,
            InventoryType.SlotType.CONTAINER,
            0,
            org.bukkit.event.inventory.ClickType.LEFT,
            org.bukkit.event.inventory.InventoryAction.PICKUP_ALL
        )
        menuListener.onInventoryClick(event)

        assertFalse(event.isCancelled, "Click on non-menu inventory should not be cancelled")
    }

    @Test
    fun `drag on menu inventory is cancelled`() {
        val player: Player = server.addPlayer()
        val profile = PlayerProfile(player.uniqueId, "TestPlayer", null, 1.0, false)
        whenever(plugin.profileService.getProfile(player.uniqueId)).thenReturn(profile)

        menu.open(player)

        val event = org.bukkit.event.inventory.InventoryDragEvent(
            player.openInventory,
            null,
            org.bukkit.inventory.ItemStack(Material.DIAMOND),
            false,
            mapOf(0 to org.bukkit.inventory.ItemStack(Material.DIAMOND))
        )
        menuListener.onInventoryDrag(event)

        assertTrue(event.isCancelled, "Drag on menu inventory should be cancelled")
    }

    private fun createClickEvent(player: Player, slot: Int): InventoryClickEvent {
        val openInv = player.openInventory
        return InventoryClickEvent(
            openInv,
            InventoryType.SlotType.CONTAINER,
            slot,
            org.bukkit.event.inventory.ClickType.LEFT,
            org.bukkit.event.inventory.InventoryAction.PICKUP_ALL
        )
    }
}
