package com.mythossmp.infrastructure.config

import com.mythossmp.MythosSMP
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.FileConfiguration

class ConfigManager(private val plugin: MythosSMP) {

    private var config: FileConfiguration = plugin.config

    data class VowConfig(
        val enabled: Boolean,
        val curseHealthReduction: Double,
        val backstabDotThreshold: Double,
        val backstabMultiplier: Double,
        val ability1Cooldown: Int,
        val ability2Cooldown: Int,
        val shadowStepRange: Double,
        val veilDuration: Double,
        val veilFirstHitMultiplier: Double,
        val maxCharges: Int = 5,
        val damageBonusPerCharge: Double = 0.1,
        val deathDefianceCooldown: Int = 90,
        val soulBurstCooldown: Int = 30,
        val soulBurstRange: Double = 8.0,
        val soulBurstBaseDamage: Double = 4.0
    )

    data class ZeusConfig(
        val enabled: Boolean,
        val staticChargePerBlock: Double,
        val conductiveMultiplier: Double,
        val staticStrikeDamage: Double
    )

    data class PoseidonConfig(
        val enabled: Boolean,
        val waveCrashCooldown: Int,
        val waveCrashRadius: Double,
        val pushVelocityFactor: Double,
        val blessingCooldown: Int,
        val strengthAmplifier: Int,
        val regenAmplifier: Int,
        val dehydrationLimitMinutes: Double,
        val dehydrationDamage: Double,
        val dehydrationIntervalSeconds: Double
    )

    data class HermesConfig(
        val enabled: Boolean,
        val doubleJumpCooldownTicks: Int,
        val dashCooldown: Int,
        val dashDistance: Double,
        val zephyrCooldown: Int,
        val zephyrDuration: Double,
        val maxArmorTier: String,
        val rejectionDamage: Double
    )

    data class HerculesConfig(
        val enabled: Boolean,
        val passiveResistanceAmplifier: Int,
        val passiveKbResistance: Double,
        val slamCooldown: Int,
        val slamRadius: Double,
        val slamDamage: Double,
        val willCooldown: Int,
        val willDurationSeconds: Double,
        val willResistanceAmplifier: Int,
        val curseSlownessAmplifier: Int
    )

    data class PlutusConfig(
        val enabled: Boolean,
        val heroOfVillageAmplifier: Int,
        val consumeBypassChance: Double,
        val fortuneCooldown: Int,
        val fortuneDurationSeconds: Double,
        val tributeCooldown: Int,
        val tributeDurationSeconds: Double,
        val greedPriceMultiplier: Int,
        val greedRadiusBlocks: Double,
        val greedDurationHours: Int
    )

    private val vowDefaults = mapOf(
        "loki" to VowConfig(
            enabled = true,
            curseHealthReduction = 4.0,
            backstabDotThreshold = 0.75,
            backstabMultiplier = 2.5,
            ability1Cooldown = 25,
            ability2Cooldown = 45,
            shadowStepRange = 15.0,
            veilDuration = 10.0,
            veilFirstHitMultiplier = 3.0
        ),
        "hades" to VowConfig(
            enabled = true,
            curseHealthReduction = 4.0,
            backstabDotThreshold = 0.75,
            backstabMultiplier = 2.5,
            ability1Cooldown = 90,
            ability2Cooldown = 30,
            shadowStepRange = 15.0,
            veilDuration = 10.0,
            veilFirstHitMultiplier = 3.0,
            maxCharges = 5,
            damageBonusPerCharge = 0.1,
            deathDefianceCooldown = 90,
            soulBurstCooldown = 30,
            soulBurstRange = 8.0,
            soulBurstBaseDamage = 4.0
        )
    )

    fun load() {
        plugin.saveDefaultConfig()
        plugin.reloadConfig()
        config = plugin.config
        validateVowConfigs()
    }

    private fun validateVowConfigs() {
        val vowsSection = config.getConfigurationSection("vows") ?: return
        for (vowKey in vowsSection.getKeys(false)) {
            val section = vowsSection.getConfigurationSection(vowKey) ?: continue
            if (vowKey == "zeus") {
                validateZeusSection(section)
                continue
            }
            if (vowKey == "poseidon") {
                validatePoseidonSection(section)
                continue
            }
            if (vowKey == "hermes") {
                validateHermesSection(section)
                continue
            }
            if (vowKey == "hercules") {
                validateHerculesSection(section)
                continue
            }
            if (vowKey == "plutus") {
                validatePlutusSection(section)
                continue
            }
            val defaults = vowDefaults[vowKey] ?: continue
            validateVowSection(vowKey, section, defaults)
        }
    }

    private fun validateZeusSection(section: ConfigurationSection) {
        section.getBoolean("enabled", true).also { section.set("enabled", it) }

        val scpb = section.getDouble("static-charge-per-block", 0.01)
        if (scpb <= 0 || scpb > 1.0) {
            plugin.logger.warning("[zeus] static-charge-per-block ($scpb) out of range (0..1]; using default 0.01.")
            section.set("static-charge-per-block", 0.01)
        }
        val cm = section.getDouble("conductive-multiplier", 1.2)
        if (cm <= 0 || cm > 10.0) {
            plugin.logger.warning("[zeus] conductive-multiplier ($cm) out of range (0..10]; using default 1.2.")
            section.set("conductive-multiplier", 1.2)
        }
        val ssd = section.getDouble("static-strike-damage", 5.0)
        if (ssd <= 0 || ssd > 100.0) {
            plugin.logger.warning("[zeus] static-strike-damage ($ssd) out of range (0..100]; using default 5.0.")
            section.set("static-strike-damage", 5.0)
        }
    }

    fun getZeusConfig(): ZeusConfig {
        val section = config.getConfigurationSection("vows.zeus")
            ?: return ZeusConfig(true, 0.01, 1.2, 5.0)
        return ZeusConfig(
            enabled = section.getBoolean("enabled", true),
            staticChargePerBlock = section.getDouble("static-charge-per-block", 0.01),
            conductiveMultiplier = section.getDouble("conductive-multiplier", 1.2),
            staticStrikeDamage = section.getDouble("static-strike-damage", 5.0)
        )
    }

    private fun validatePoseidonSection(section: ConfigurationSection) {
        section.getBoolean("enabled", true).also { section.set("enabled", it) }

        val ab1 = section.getConfigurationSection("ability1")
        val ab2 = section.getConfigurationSection("ability2")
        val curse = section.getConfigurationSection("curse")

        if (ab1 != null) {
            val cd = ab1.getInt("cooldown-seconds", 35)
            if (cd !in 1..300) {
                plugin.logger.warning("[poseidon] ability1.cooldown-seconds ($cd) out of range [1..300]; using default 35.")
                ab1.set("cooldown-seconds", 35)
            }
            val radius = ab1.getDouble("radius-blocks", 8.0)
            if (radius <= 0 || radius > 50.0) {
                plugin.logger.warning("[poseidon] ability1.radius-blocks ($radius) out of range (0..50]; using default 8.0.")
                ab1.set("radius-blocks", 8.0)
            }
            val push = ab1.getDouble("push-velocity-factor", 1.4)
            if (push <= 0 || push > 10.0) {
                plugin.logger.warning("[poseidon] ability1.push-velocity-factor ($push) out of range (0..10]; using default 1.4.")
                ab1.set("push-velocity-factor", 1.4)
            }
        }
        if (ab2 != null) {
            val cd = ab2.getInt("cooldown-seconds", 10)
            if (cd !in 1..300) {
                plugin.logger.warning("[poseidon] ability2.cooldown-seconds ($cd) out of range [1..300]; using default 10.")
                ab2.set("cooldown-seconds", 10)
            }
            val sa = ab2.getInt("strength-amp", 0)
            if (sa !in 0..4) {
                plugin.logger.warning("[poseidon] ability2.strength-amp ($sa) out of range [0..4]; using default 0.")
                ab2.set("strength-amp", 0)
            }
            val ra = ab2.getInt("regen-amp", 0)
            if (ra !in 0..4) {
                plugin.logger.warning("[poseidon] ability2.regen-amp ($ra) out of range [0..4]; using default 0.")
                ab2.set("regen-amp", 0)
            }
        }
        if (curse != null) {
            val limit = curse.getDouble("dehydration-limit-minutes", 5.0)
            if (limit <= 0 || limit > 60.0) {
                plugin.logger.warning("[poseidon] curse.dehydration-limit-minutes ($limit) out of range (0..60]; using default 5.0.")
                curse.set("dehydration-limit-minutes", 5.0)
            }
            val dmg = curse.getDouble("damage-amount", 1.0)
            if (dmg <= 0 || dmg > 100.0) {
                plugin.logger.warning("[poseidon] curse.damage-amount ($dmg) out of range (0..100]; using default 1.0.")
                curse.set("damage-amount", 1.0)
            }
            val interval = curse.getDouble("interval-seconds", 3.0)
            if (interval <= 0 || interval > 60.0) {
                plugin.logger.warning("[poseidon] curse.interval-seconds ($interval) out of range (0..60]; using default 3.0.")
                curse.set("interval-seconds", 3.0)
            }
        }
    }

    fun getPoseidonConfig(): PoseidonConfig {
        val section = config.getConfigurationSection("vows.poseidon")
            ?: return PoseidonConfig(
                enabled = true,
                waveCrashCooldown = 35,
                waveCrashRadius = 8.0,
                pushVelocityFactor = 1.4,
                blessingCooldown = 10,
                strengthAmplifier = 0,
                regenAmplifier = 0,
                dehydrationLimitMinutes = 5.0,
                dehydrationDamage = 1.0,
                dehydrationIntervalSeconds = 3.0
            )
        val ab1 = section.getConfigurationSection("ability1")
        val ab2 = section.getConfigurationSection("ability2")
        val curse = section.getConfigurationSection("curse")
        return PoseidonConfig(
            enabled = section.getBoolean("enabled", true),
            waveCrashCooldown = ab1?.getInt("cooldown-seconds") ?: 35,
            waveCrashRadius = ab1?.getDouble("radius-blocks") ?: 8.0,
            pushVelocityFactor = ab1?.getDouble("push-velocity-factor") ?: 1.4,
            blessingCooldown = ab2?.getInt("cooldown-seconds") ?: 10,
            strengthAmplifier = ab2?.getInt("strength-amp") ?: 0,
            regenAmplifier = ab2?.getInt("regen-amp") ?: 0,
            dehydrationLimitMinutes = curse?.getDouble("dehydration-limit-minutes") ?: 5.0,
            dehydrationDamage = curse?.getDouble("damage-amount") ?: 1.0,
            dehydrationIntervalSeconds = curse?.getDouble("interval-seconds") ?: 3.0
        )
    }

    private fun validateHermesSection(section: ConfigurationSection) {
        section.getBoolean("enabled", true).also { section.set("enabled", it) }

        val passive = section.getConfigurationSection("passive")
        val ab1 = section.getConfigurationSection("ability1")
        val ab2 = section.getConfigurationSection("ability2")
        val curse = section.getConfigurationSection("curse")

        if (passive != null) {
            val ticks = passive.getInt("double-jump-cooldown-ticks", 300)
            if (ticks !in 20..1200) {
                plugin.logger.warning("[hermes] passive.double-jump-cooldown-ticks ($ticks) out of range [20..1200]; using default 300.")
                passive.set("double-jump-cooldown-ticks", 300)
            }
        }
        if (ab1 != null) {
            val cd = ab1.getInt("cooldown-seconds", 6)
            if (cd !in 1..300) {
                plugin.logger.warning("[hermes] ability1.cooldown-seconds ($cd) out of range [1..300]; using default 6.")
                ab1.set("cooldown-seconds", 6)
            }
            val dist = ab1.getDouble("dash-distance", 12.0)
            if (dist <= 0 || dist > 100.0) {
                plugin.logger.warning("[hermes] ability1.dash-distance ($dist) out of range (0..100]; using default 12.0.")
                ab1.set("dash-distance", 12.0)
            }
        }
        if (ab2 != null) {
            val cd = ab2.getInt("cooldown-seconds", 150)
            if (cd !in 1..300) {
                plugin.logger.warning("[hermes] ability2.cooldown-seconds ($cd) out of range [1..300]; using default 150.")
                ab2.set("cooldown-seconds", 150)
            }
            val dur = ab2.getDouble("duration-seconds", 8.0)
            if (dur <= 0 || dur > 60.0) {
                plugin.logger.warning("[hermes] ability2.duration-seconds ($dur) out of range (0..60]; using default 8.0.")
                ab2.set("duration-seconds", 8.0)
            }
        }
        if (curse != null) {
            val tier = curse.getString("max-armor-tier", "CHAINMAIL") ?: "CHAINMAIL"
            if (tier.uppercase() !in setOf("LEATHER", "GOLD", "CHAINMAIL", "IRON", "DIAMOND", "NETHERITE")) {
                plugin.logger.warning("[hermes] curse.max-armor-tier ($tier) invalid; using default CHAINMAIL.")
                curse.set("max-armor-tier", "CHAINMAIL")
            }
            val dmg = curse.getDouble("rejection-damage", 4.0)
            if (dmg <= 0 || dmg > 100.0) {
                plugin.logger.warning("[hermes] curse.rejection-damage ($dmg) out of range (0..100]; using default 4.0.")
                curse.set("rejection-damage", 4.0)
            }
        }
    }

    fun getHermesConfig(): HermesConfig {
        val section = config.getConfigurationSection("vows.hermes")
            ?: return HermesConfig(
                enabled = true,
                doubleJumpCooldownTicks = 300,
                dashCooldown = 6,
                dashDistance = 12.0,
                zephyrCooldown = 150,
                zephyrDuration = 8.0,
                maxArmorTier = "CHAINMAIL",
                rejectionDamage = 4.0
            )
        val passive = section.getConfigurationSection("passive")
        val ab1 = section.getConfigurationSection("ability1")
        val ab2 = section.getConfigurationSection("ability2")
        val curse = section.getConfigurationSection("curse")
        return HermesConfig(
            enabled = section.getBoolean("enabled", true),
            doubleJumpCooldownTicks = passive?.getInt("double-jump-cooldown-ticks") ?: 300,
            dashCooldown = ab1?.getInt("cooldown-seconds") ?: 6,
            dashDistance = ab1?.getDouble("dash-distance") ?: 12.0,
            zephyrCooldown = ab2?.getInt("cooldown-seconds") ?: 150,
            zephyrDuration = ab2?.getDouble("duration-seconds") ?: 8.0,
            maxArmorTier = curse?.getString("max-armor-tier") ?: "CHAINMAIL",
            rejectionDamage = curse?.getDouble("rejection-damage") ?: 4.0
        )
    }

    private fun validateHerculesSection(section: ConfigurationSection) {
        section.getBoolean("enabled", true).also { section.set("enabled", it) }

        val passive = section.getConfigurationSection("passive")
        val ab1 = section.getConfigurationSection("ability1")
        val ab2 = section.getConfigurationSection("ability2")
        val curse = section.getConfigurationSection("curse")

        if (passive != null) {
            val ra = passive.getInt("resistance-amplifier", 0)
            if (ra !in 0..4) {
                plugin.logger.warning("[hercules] passive.resistance-amplifier ($ra) out of range [0..4]; using default 0.")
                passive.set("resistance-amplifier", 0)
            }
            val kb = passive.getDouble("kb-resistance", 0.20)
            if (kb < 0 || kb > 1.0) {
                plugin.logger.warning("[hercules] passive.kb-resistance ($kb) out of range [0..1]; using default 0.20.")
                passive.set("kb-resistance", 0.20)
            }
        }
        if (ab1 != null) {
            val cd = ab1.getInt("cooldown-seconds", 45)
            if (cd !in 1..300) {
                plugin.logger.warning("[hercules] ability1.cooldown-seconds ($cd) out of range [1..300]; using default 45.")
                ab1.set("cooldown-seconds", 45)
            }
            val radius = ab1.getDouble("radius-blocks", 5.0)
            if (radius <= 0 || radius > 50.0) {
                plugin.logger.warning("[hercules] ability1.radius-blocks ($radius) out of range (0..50]; using default 5.0.")
                ab1.set("radius-blocks", 5.0)
            }
            val dmg = ab1.getDouble("slam-damage", 8.0)
            if (dmg <= 0 || dmg > 100.0) {
                plugin.logger.warning("[hercules] ability1.slam-damage ($dmg) out of range (0..100]; using default 8.0.")
                ab1.set("slam-damage", 8.0)
            }
        }
        if (ab2 != null) {
            val cd = ab2.getInt("cooldown-seconds", 180)
            if (cd !in 1..300) {
                plugin.logger.warning("[hercules] ability2.cooldown-seconds ($cd) out of range [1..300]; using default 180.")
                ab2.set("cooldown-seconds", 180)
            }
            val dur = ab2.getDouble("duration-seconds", 10.0)
            if (dur <= 0 || dur > 60.0) {
                plugin.logger.warning("[hercules] ability2.duration-seconds ($dur) out of range (0..60]; using default 10.0.")
                ab2.set("duration-seconds", 10.0)
            }
            val ra = ab2.getInt("resistance-amplifier", 1)
            if (ra !in 0..4) {
                plugin.logger.warning("[hercules] ability2.resistance-amplifier ($ra) out of range [0..4]; using default 1.")
                ab2.set("resistance-amplifier", 1)
            }
        }
        if (curse != null) {
            val sa = curse.getInt("slowness-amplifier", 0)
            if (sa !in 0..4) {
                plugin.logger.warning("[hercules] curse.slowness-amplifier ($sa) out of range [0..4]; using default 0.")
                curse.set("slowness-amplifier", 0)
            }
        }
    }

    fun getHerculesConfig(): HerculesConfig {
        val section = config.getConfigurationSection("vows.hercules")
            ?: return HerculesConfig(
                enabled = true,
                passiveResistanceAmplifier = 0,
                passiveKbResistance = 0.20,
                slamCooldown = 45,
                slamRadius = 5.0,
                slamDamage = 8.0,
                willCooldown = 180,
                willDurationSeconds = 10.0,
                willResistanceAmplifier = 1,
                curseSlownessAmplifier = 0
            )
        val passive = section.getConfigurationSection("passive")
        val ab1 = section.getConfigurationSection("ability1")
        val ab2 = section.getConfigurationSection("ability2")
        val curse = section.getConfigurationSection("curse")
        return HerculesConfig(
            enabled = section.getBoolean("enabled", true),
            passiveResistanceAmplifier = passive?.getInt("resistance-amplifier") ?: 0,
            passiveKbResistance = passive?.getDouble("kb-resistance") ?: 0.20,
            slamCooldown = ab1?.getInt("cooldown-seconds") ?: 45,
            slamRadius = ab1?.getDouble("radius-blocks") ?: 5.0,
            slamDamage = ab1?.getDouble("slam-damage") ?: 8.0,
            willCooldown = ab2?.getInt("cooldown-seconds") ?: 180,
            willDurationSeconds = ab2?.getDouble("duration-seconds") ?: 10.0,
            willResistanceAmplifier = ab2?.getInt("resistance-amplifier") ?: 1,
            curseSlownessAmplifier = curse?.getInt("slowness-amplifier") ?: 0
        )
    }

    private fun validatePlutusSection(section: ConfigurationSection) {
        section.getBoolean("enabled", true).also { section.set("enabled", it) }

        val passive = section.getConfigurationSection("passive")
        val ab1 = section.getConfigurationSection("ability1")
        val ab2 = section.getConfigurationSection("ability2")
        val curse = section.getConfigurationSection("curse")

        if (passive != null) {
            val hva = passive.getInt("hero-of-village-level", 4)
            if (hva !in 0..4) {
                plugin.logger.warning("[plutus] passive.hero-of-village-level ($hva) out of range [0..4]; using default 4.")
                passive.set("hero-of-village-level", 4)
            }
            val chance = passive.getDouble("consume-bypass-chance", 0.30)
            if (chance !in 0.0..1.0) {
                plugin.logger.warning("[plutus] passive.consume-bypass-chance ($chance) out of range [0..1]; using default 0.30.")
                passive.set("consume-bypass-chance", 0.30)
            }
        }
        if (ab1 != null) {
            val cd = ab1.getInt("cooldown-seconds", 180)
            if (cd !in 1..300) {
                plugin.logger.warning("[plutus] ability1.cooldown-seconds ($cd) out of range [1..300]; using default 180.")
                ab1.set("cooldown-seconds", 180)
            }
            val dur = ab1.getDouble("duration-seconds", 30.0)
            if (dur <= 0 || dur > 120.0) {
                plugin.logger.warning("[plutus] ability1.duration-seconds ($dur) out of range (0..120]; using default 30.0.")
                ab1.set("duration-seconds", 30.0)
            }
        }
        if (ab2 != null) {
            val cd = ab2.getInt("cooldown-seconds", 300)
            if (cd !in 1..300) {
                plugin.logger.warning("[plutus] ability2.cooldown-seconds ($cd) out of range [1..300]; using default 300.")
                ab2.set("cooldown-seconds", 300)
            }
            val dur = ab2.getDouble("duration-seconds", 30.0)
            if (dur <= 0 || dur > 120.0) {
                plugin.logger.warning("[plutus] ability2.duration-seconds ($dur) out of range (0..120]; using default 30.0.")
                ab2.set("duration-seconds", 30.0)
            }
        }
        if (curse != null) {
            val pm = curse.getInt("price-multiplier", 2)
            if (pm !in 1..10) {
                plugin.logger.warning("[plutus] curse.price-multiplier ($pm) out of range [1..10]; using default 2.")
                curse.set("price-multiplier", 2)
            }
            val radius = curse.getDouble("radius-blocks", 128.0)
            if (radius <= 0 || radius > 512.0) {
                plugin.logger.warning("[plutus] curse.radius-blocks ($radius) out of range (0..512]; using default 128.0.")
                curse.set("radius-blocks", 128.0)
            }
            val hours = curse.getInt("duration-hours", 4)
            if (hours !in 1..72) {
                plugin.logger.warning("[plutus] curse.duration-hours ($hours) out of range [1..72]; using default 4.")
                curse.set("duration-hours", 4)
            }
        }
    }

    fun getPlutusConfig(): PlutusConfig {
        val section = config.getConfigurationSection("vows.plutus")
            ?: return PlutusConfig(
                enabled = true,
                heroOfVillageAmplifier = 4,
                consumeBypassChance = 0.30,
                fortuneCooldown = 180,
                fortuneDurationSeconds = 30.0,
                tributeCooldown = 300,
                tributeDurationSeconds = 30.0,
                greedPriceMultiplier = 2,
                greedRadiusBlocks = 128.0,
                greedDurationHours = 4
            )
        val passive = section.getConfigurationSection("passive")
        val ab1 = section.getConfigurationSection("ability1")
        val ab2 = section.getConfigurationSection("ability2")
        val curse = section.getConfigurationSection("curse")
        return PlutusConfig(
            enabled = section.getBoolean("enabled", true),
            heroOfVillageAmplifier = passive?.getInt("hero-of-village-level") ?: 4,
            consumeBypassChance = passive?.getDouble("consume-bypass-chance") ?: 0.30,
            fortuneCooldown = ab1?.getInt("cooldown-seconds") ?: 180,
            fortuneDurationSeconds = ab1?.getDouble("duration-seconds") ?: 30.0,
            tributeCooldown = ab2?.getInt("cooldown-seconds") ?: 300,
            tributeDurationSeconds = ab2?.getDouble("duration-seconds") ?: 30.0,
            greedPriceMultiplier = curse?.getInt("price-multiplier") ?: 2,
            greedRadiusBlocks = curse?.getDouble("radius-blocks") ?: 128.0,
            greedDurationHours = curse?.getInt("duration-hours") ?: 4
        )
    }

    private fun validateVowSection(key: String, section: ConfigurationSection, defaults: VowConfig) {        section.getBoolean("enabled", defaults.enabled).also { section.set("enabled", it) }

        val curseSection = section.getConfigurationSection("curse")
        if (curseSection != null) {
            val hr = curseSection.getDouble("health-reduction", defaults.curseHealthReduction)
            if (hr <= 0 || hr > 20) {
                plugin.logger.warning("[$key] curse.health-reduction ($hr) out of range [1..20]; using default ${defaults.curseHealthReduction}.")
                curseSection.set("health-reduction", defaults.curseHealthReduction)
            }
        }

        val passiveSection = section.getConfigurationSection("passive")
        if (passiveSection != null) {
            val thresh = passiveSection.getDouble("backstab-dot-threshold", defaults.backstabDotThreshold)
            if (thresh !in 0.0..1.0) {
                plugin.logger.warning("[$key] passive.backstab-dot-threshold ($thresh) out of range [0..1]; using default ${defaults.backstabDotThreshold}.")
                passiveSection.set("backstab-dot-threshold", defaults.backstabDotThreshold)
            }
            val mult = passiveSection.getDouble("backstab-multiplier", defaults.backstabMultiplier)
            if (mult <= 0 || mult > 10) {
                plugin.logger.warning("[$key] passive.backstab-multiplier ($mult) out of range [0.1..10]; using default ${defaults.backstabMultiplier}.")
                passiveSection.set("backstab-multiplier", defaults.backstabMultiplier)
            }
            if (key == "hades") {
                val mc = passiveSection.getInt("max-charges", defaults.maxCharges)
                if (mc <= 0 || mc > 20) {
                    plugin.logger.warning("[$key] passive.max-charges ($mc) out of range [1..20]; using default ${defaults.maxCharges}.")
                    passiveSection.set("max-charges", defaults.maxCharges)
                }
                val dbpc = passiveSection.getDouble("damage-bonus-per-charge", defaults.damageBonusPerCharge)
                if (dbpc <= 0 || dbpc > 2.0) {
                    plugin.logger.warning("[$key] passive.damage-bonus-per-charge ($dbpc) out of range [0.01..2.0]; using default ${defaults.damageBonusPerCharge}.")
                    passiveSection.set("damage-bonus-per-charge", defaults.damageBonusPerCharge)
                }
            }
        }

        val abilitiesSection = section.getConfigurationSection("abilities")
        if (abilitiesSection != null) {
            for (abilityKey in abilitiesSection.getKeys(false)) {
                val abilitySection = abilitiesSection.getConfigurationSection(abilityKey) ?: continue
                val cd = abilitySection.getInt("cooldown", when (abilityKey) {
                    "shadow-step" -> defaults.ability1Cooldown
                    "veil-of-lies" -> defaults.ability2Cooldown
                    "death-defiance" -> defaults.deathDefianceCooldown
                    "soul-burst" -> defaults.soulBurstCooldown
                    else -> 30
                })
                if (cd <= 0 || cd > 300) {
                    plugin.logger.warning("[$key] abilities.$abilityKey.cooldown ($cd) out of range [1..300]; using default.")
                    abilitySection.set("cooldown", when (abilityKey) {
                        "shadow-step" -> defaults.ability1Cooldown
                        "veil-of-lies" -> defaults.ability2Cooldown
                        "death-defiance" -> defaults.deathDefianceCooldown
                        "soul-burst" -> defaults.soulBurstCooldown
                        else -> 30
                    })
                }
                if (abilityKey == "shadow-step") {
                    val range = abilitySection.getDouble("range", defaults.shadowStepRange)
                    if (range <= 0 || range > 100) {
                        plugin.logger.warning("[$key] abilities.shadow-step.range ($range) out of range [1..100]; using default ${defaults.shadowStepRange}.")
                        abilitySection.set("range", defaults.shadowStepRange)
                    }
                }
                if (abilityKey == "veil-of-lies") {
                    val dur = abilitySection.getDouble("duration", defaults.veilDuration)
                    if (dur <= 0 || dur > 120) {
                        plugin.logger.warning("[$key] abilities.veil-of-lies.duration ($dur) out of range [1..120]; using default ${defaults.veilDuration}.")
                        abilitySection.set("duration", defaults.veilDuration)
                    }
                    val fhm = abilitySection.getDouble("first-hit-multiplier", defaults.veilFirstHitMultiplier)
                    if (fhm <= 0 || fhm > 20) {
                        plugin.logger.warning("[$key] abilities.veil-of-lies.first-hit-multiplier ($fhm) out of range [0.1..20]; using default ${defaults.veilFirstHitMultiplier}.")
                        abilitySection.set("first-hit-multiplier", defaults.veilFirstHitMultiplier)
                    }
                }
                if (abilityKey == "soul-burst") {
                    val range = abilitySection.getDouble("range", defaults.soulBurstRange)
                    if (range <= 0 || range > 100) {
                        plugin.logger.warning("[$key] abilities.soul-burst.range ($range) out of range [1..100]; using default ${defaults.soulBurstRange}.")
                        abilitySection.set("range", defaults.soulBurstRange)
                    }
                    val bd = abilitySection.getDouble("base-damage", defaults.soulBurstBaseDamage)
                    if (bd <= 0 || bd > 100) {
                        plugin.logger.warning("[$key] abilities.soul-burst.base-damage ($bd) out of range [1..100]; using default ${defaults.soulBurstBaseDamage}.")
                        abilitySection.set("base-damage", defaults.soulBurstBaseDamage)
                    }
                }
            }
        }
    }

    fun getVowConfig(vowKey: String): VowConfig {
        val defaults = vowDefaults[vowKey] ?: return VowConfig(
            enabled = true, curseHealthReduction = 4.0, backstabDotThreshold = 0.75,
            backstabMultiplier = 2.5, ability1Cooldown = 30, ability2Cooldown = 30,
            shadowStepRange = 15.0, veilDuration = 10.0, veilFirstHitMultiplier = 3.0
        )
        val section = config.getConfigurationSection("vows.$vowKey") ?: return defaults

        val curseSection = section.getConfigurationSection("curse")
        val curseHp = curseSection?.getDouble("health-reduction") ?: defaults.curseHealthReduction

        val passiveSection = section.getConfigurationSection("passive")
        val dotThresh = passiveSection?.getDouble("backstab-dot-threshold") ?: defaults.backstabDotThreshold
        val bstMult = passiveSection?.getDouble("backstab-multiplier") ?: defaults.backstabMultiplier
        val maxCh = if (vowKey == "hades") passiveSection?.getInt("max-charges") ?: defaults.maxCharges else defaults.maxCharges
        val dbpc = if (vowKey == "hades") passiveSection?.getDouble("damage-bonus-per-charge") ?: defaults.damageBonusPerCharge else defaults.damageBonusPerCharge

        val abSection = section.getConfigurationSection("abilities")
        val ab1Key = when (vowKey) { "loki" -> "shadow-step"; "hades" -> "death-defiance"; else -> null }
        val ab2Key = when (vowKey) { "loki" -> "veil-of-lies"; "hades" -> "soul-burst"; else -> null }
        val ab1Section = if (ab1Key != null) abSection?.getConfigurationSection(ab1Key) else null
        val ab2Section = if (ab2Key != null) abSection?.getConfigurationSection(ab2Key) else null

        return VowConfig(
            enabled = section.getBoolean("enabled", defaults.enabled),
            curseHealthReduction = curseHp,
            backstabDotThreshold = dotThresh,
            backstabMultiplier = bstMult,
            ability1Cooldown = ab1Section?.getInt("cooldown") ?: defaults.ability1Cooldown,
            ability2Cooldown = ab2Section?.getInt("cooldown") ?: defaults.ability2Cooldown,
            shadowStepRange = ab1Section?.getDouble("range") ?: defaults.shadowStepRange,
            veilDuration = ab2Section?.getDouble("duration") ?: defaults.veilDuration,
            veilFirstHitMultiplier = ab2Section?.getDouble("first-hit-multiplier") ?: defaults.veilFirstHitMultiplier,
            maxCharges = maxCh,
            damageBonusPerCharge = dbpc,
            deathDefianceCooldown = ab1Section?.getInt("cooldown") ?: defaults.deathDefianceCooldown,
            soulBurstCooldown = ab2Section?.getInt("cooldown") ?: defaults.soulBurstCooldown,
            soulBurstRange = if (vowKey == "hades") ab2Section?.getDouble("range") ?: defaults.soulBurstRange else defaults.soulBurstRange,
            soulBurstBaseDamage = if (vowKey == "hades") ab2Section?.getDouble("base-damage") ?: defaults.soulBurstBaseDamage else defaults.soulBurstBaseDamage
        )
    }

    fun getDatabasePath(): String {
        return config.getString("database.path") ?: "${plugin.dataFolder.absolutePath}/mythos_smp.db"
    }

    fun getPoolSize(): Int {
        val size = config.getInt("database.pool-size", 4)
        if (size <= 0) {
            plugin.logger.warning("Invalid database.pool-size ($size); falling back to 4.")
            return 4
        }
        return size
    }

    fun getMaxBackups(): Int {
        return config.getInt("database.max-backups", 5)
    }

    fun isDebugMode(): Boolean {
        return config.getBoolean("debug", false)
    }
}
