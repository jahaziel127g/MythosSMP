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

    private fun validateVowSection(key: String, section: ConfigurationSection, defaults: VowConfig) {
        section.getBoolean("enabled", defaults.enabled).also { section.set("enabled", it) }

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
