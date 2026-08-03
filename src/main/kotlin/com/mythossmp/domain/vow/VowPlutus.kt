package com.mythossmp.domain.vow

import com.mythossmp.MythosSMP
import com.mythossmp.application.task.Cancellable
import com.mythossmp.domain.event.VowVFXEvent
import com.mythossmp.domain.model.ActiveAbility
import com.mythossmp.domain.model.Curse
import com.mythossmp.domain.model.PassiveEffect
import com.mythossmp.domain.model.Vow
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.entity.Villager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.inventory.MerchantInventory
import org.bukkit.inventory.MerchantRecipe
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.time.Duration
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt
import kotlin.random.Random

class VowPlutus : Vow {
    override val key = "plutus"
    override val displayName = "Plutus"
    override val enabled = true
    override val description = "God of Wealth — Economic Catalyst"

    override val passiveEffects: List<PassiveEffect> = listOf(PlutusPassive())
    override val ability1: ActiveAbility = MerchantsFortune()
    override val ability2: ActiveAbility = GoldenTribute()
    override val curse: Curse = curseInstance

    companion object {
        val curseInstance = GraveGreed()
        val fortunePlayers = ConcurrentHashMap.newKeySet<UUID>()
        val tributePlayers = ConcurrentHashMap.newKeySet<UUID>()
    }
}

private fun hasPlutus(playerUuid: UUID): Boolean {
    val plugin = MythosSMP.instance
    val profile = plugin.profileService.getProfile(playerUuid) ?: return false
    if (profile.activeVow?.removePrefix("vow-") != "plutus") return false
    return plugin.configManager.getPlutusConfig().enabled
}

class PlutusPassive : PassiveEffect {
    override val key = "plutus-passive"
    override val description = "Permanent Hero of the Village V + chance to preserve consumables"

    private val refreshTasks = ConcurrentHashMap<UUID, Cancellable>()

    override fun apply(player: Player) {
        val cfg = MythosSMP.instance.configManager.getPlutusConfig()
        player.addPotionEffect(PotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE, 20 * 30, cfg.heroOfVillageAmplifier, false, false))
        refreshTasks[player.uniqueId] = MythosSMP.instance.scheduler.repeat(Duration.ofSeconds(25)) {
            if (!player.isOnline) return@repeat
            if (!hasPlutus(player.uniqueId)) return@repeat
            player.addPotionEffect(PotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE, 20 * 30, cfg.heroOfVillageAmplifier, false, false))
        }
    }

    override fun remove(player: Player) {
        refreshTasks.remove(player.uniqueId)?.cancel()
        player.removePotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE)
    }
}

private val ORE_MATERIALS = setOf(
    Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE,
    Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE,
    Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE,
    Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE,
    Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE,
    Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE,
    Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE,
    Material.NETHER_GOLD_ORE, Material.NETHER_QUARTZ_ORE,
    Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE,
    Material.ANCIENT_DEBRIS
)

class PlutusListener : Listener {

    private val modifiedVillagers = ConcurrentHashMap<UUID, List<Pair<Int, List<ItemStack>>>>()

    @EventHandler
    fun onConsume(event: PlayerItemConsumeEvent) {
        val player = event.player
        if (!hasPlutus(player.uniqueId)) return

        val chance = MythosSMP.instance.configManager.getPlutusConfig().consumeBypassChance
        if (Random.nextDouble() > chance) return

        event.isCancelled = true
        player.sendMessage(Component.text("\uD83E\uDE99 Your wealth preserves the item!", NamedTextColor.GOLD))
    }

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        val player = event.player
        if (!VowPlutus.fortunePlayers.contains(player.uniqueId)) return
        if (event.block.type !in ORE_MATERIALS) return

        val drops = event.block.getDrops(player.inventory.itemInMainHand)
        if (drops.isEmpty()) return

        event.isDropItems = false
        val loc = event.block.location.add(0.5, 0.5, 0.5)
        drops.forEach { drop ->
            val doubled = drop.clone().apply { amount *= 2 }
            event.block.world.dropItemNaturally(loc, doubled)
        }
    }

    @EventHandler
    fun onInteractVillager(event: PlayerInteractEntityEvent) {
        val player = event.player
        if (!VowPlutus.tributePlayers.contains(player.uniqueId)) return
        val villager = event.rightClicked as? Villager ?: return

        overrideRecipes(villager, cost = 1)
        player.sendMessage(Component.text("\uD83D\uDCB0 Golden Tribute: this merchant sells for 1 emerald!", NamedTextColor.GREEN))
    }

    @EventHandler
    fun onDamageVillager(event: EntityDamageByEntityEvent) {
        val damager = event.damager as? Player ?: return
        val villager = event.entity as? Villager ?: return
        if (!hasPlutus(damager.uniqueId)) return

        val cfg = MythosSMP.instance.configManager.getPlutusConfig()
        val plugin = MythosSMP.instance

        plugin.vfxBroadcastService.publish(
            VowVFXEvent.PlutusGreedPenalty(villager.location.clone(), cfg.greedRadiusBlocks)
        )
        damager.sendMessage(Component.text(
            "\uD83D\uDC1A Grave Greed! Villagers within ${cfg.greedRadiusBlocks.roundToInt()} blocks now charge double prices.",
            NamedTextColor.DARK_RED
        ))

        for (nearby in villager.world.getNearbyEntities(villager.location, cfg.greedRadiusBlocks, cfg.greedRadiusBlocks, cfg.greedRadiusBlocks)) {
            val v = nearby as? Villager ?: continue
            overrideRecipes(v, costMultiplier = cfg.greedPriceMultiplier, restoreAfter = Duration.ofHours(cfg.greedDurationHours.toLong()))
        }
    }

    @EventHandler
    fun onClose(event: InventoryCloseEvent) {
        if (event.inventory.type != InventoryType.MERCHANT) return
        val merchant = event.inventory as? MerchantInventory ?: return
        val villager = merchant.merchant as? Villager ?: return
        restoreRecipes(villager)
    }

    private fun overrideRecipes(villager: Villager, cost: Int = 1, costMultiplier: Int = 1, restoreAfter: Duration? = null) {
        val current = villager.recipes
        if (current.isEmpty()) return

        val snapshot = mutableListOf<Pair<Int, List<ItemStack>>>()
        val modified = current.map { recipe ->
            snapshot.add(recipe.ingredients.size to recipe.ingredients.toList())
            copyWithCost(recipe, cost, costMultiplier)
        }
        modifiedVillagers[villager.uniqueId] = snapshot
        villager.setRecipes(modified)

        if (restoreAfter != null) {
            MythosSMP.instance.scheduler.later(restoreAfter) {
                val v = MythosSMP.instance.server.getEntity(villager.uniqueId) as? Villager ?: return@later
                restoreRecipes(v)
            }
        }
    }

    private fun copyWithCost(recipe: MerchantRecipe, cost: Int, costMultiplier: Int): MerchantRecipe {
        val copy = MerchantRecipe(
            recipe.result.clone(),
            recipe.uses,
            recipe.maxUses,
            recipe.hasExperienceReward(),
            recipe.villagerExperience,
            recipe.priceMultiplier,
            recipe.demand,
            recipe.specialPrice
        )
        val emerald = recipe.ingredients.firstOrNull { it.type == Material.EMERALD }
        copy.setIngredients(
            if (emerald != null) listOf(ItemStack(Material.EMERALD, (cost * costMultiplier).coerceAtLeast(1)))
            else recipe.ingredients
        )
        return copy
    }

    private fun restoreRecipes(villager: Villager) {
        val snapshot = modifiedVillagers.remove(villager.uniqueId) ?: return
        val current = villager.recipes
        if (current.size != snapshot.size) return
        villager.setRecipes(current.mapIndexed { i, recipe ->
            val copy = MerchantRecipe(
                recipe.result.clone(),
                recipe.uses,
                recipe.maxUses,
                recipe.hasExperienceReward(),
                recipe.villagerExperience,
                recipe.priceMultiplier,
                recipe.demand,
                recipe.specialPrice
            )
            copy.setIngredients(snapshot[i].second)
            copy
        })
    }
}

class MerchantsFortune : ActiveAbility {
    override val key = "plutus-merchants-fortune"
    override val displayName = "Merchant's Fortune"
    override val cooldownSeconds = 180
    override val description = "30s: all broken ores drop exactly double (stacks with Fortune)."

    override fun execute(player: Player, plugin: Any): Boolean {
        val mythosPlugin = plugin as MythosSMP
        val cfg = mythosPlugin.configManager.getPlutusConfig()
        val uuid = player.uniqueId

        VowPlutus.fortunePlayers.add(uuid)
        mythosPlugin.vfxBroadcastService.publish(
            VowVFXEvent.PlutusMerchantFortune(player.location.clone(), 3.0)
        )
        player.sendMessage(Component.text("\uD83D\uDCB0 Merchant's Fortune: double ore drops for ${cfg.fortuneDurationSeconds.roundToInt()}s!", NamedTextColor.GOLD))

        mythosPlugin.scheduler.later(Duration.ofSeconds(cfg.fortuneDurationSeconds.toLong())) {
            VowPlutus.fortunePlayers.remove(uuid)
            if (player.isOnline) {
                player.sendMessage(Component.text("Merchant's Fortune fades...", NamedTextColor.GRAY))
            }
        }
        return true
    }
}

class GoldenTribute : ActiveAbility {
    override val key = "plutus-golden-tribute"
    override val displayName = "Golden Tribute"
    override val cooldownSeconds = 300
    override val description = "30s: every villager shop you open sells for exactly 1 emerald."

    override fun execute(player: Player, plugin: Any): Boolean {
        val mythosPlugin = plugin as MythosSMP
        val cfg = mythosPlugin.configManager.getPlutusConfig()
        val uuid = player.uniqueId

        VowPlutus.tributePlayers.add(uuid)
        mythosPlugin.vfxBroadcastService.publish(
            VowVFXEvent.PlutusGoldenTribute(player.location.clone())
        )
        player.sendMessage(Component.text("\uD83D\uDCB0 Golden Tribute: all trades cost 1 emerald!", NamedTextColor.GREEN))

        mythosPlugin.scheduler.later(Duration.ofSeconds(cfg.tributeDurationSeconds.toLong())) {
            VowPlutus.tributePlayers.remove(uuid)
            if (player.isOnline) {
                player.sendMessage(Component.text("Golden Tribute fades...", NamedTextColor.GRAY))
            }
        }
        return true
    }
}

class GraveGreed : Curse {
    override val key = "plutus-grave-greed"
    override val description = "Damaging villagers enrages the market: nearby villagers charge double for 4 hours."

    override fun apply(player: Player) {}
    override fun remove(player: Player) {}
}
