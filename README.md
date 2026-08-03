# Mythos SMP

A production-grade Kotlin/Paper 1.21.11 Minecraft plugin featuring mythological divine Vows, high-stakes trials, and dynamic PvP progression.

## Status

| Phase | Component | Status |
|-------|-----------|--------|
| 1 | Foundation — Gradle, DB schema, profile caching, /vow & /trust commands | ✅ Complete |
| 2 | Core Framework — Vow/Ability abstractions, cooldowns, custom events, Vow of Ares | ✅ Complete |
| 3 | Trial Engine — Scrolls, state machines, mob waves, coordinate broadcasts | ✅ Complete |
| 4 | Multiplayer — Trust system AOE checks, Seal of Union | ✅ Complete |
| 5 | God Expansion — All 14 Vow mechanics, YAML configs, particles | 🔄 In Progress |
|   | ├ Auto-validating Vow YAML config | ✅ Complete |
|   | ├ Vow of Ares (War) | ✅ Complete |
|   | ├ Vow of Loki (Trickster) | ✅ Complete |
|   | ├ Vow of Zeus (Lightning) | ✅ Complete |
|   | ├ Vow of Hades (Death) | ✅ Complete |
|   | ├ Vow of Poseidon (Sea) | ✅ Complete |
|   | ├ Vow of Hermes (Travel) | ✅ Complete |
|   | ├ Vow of Hercules (Strength) | ✅ Complete |
|   | ├ Vow of Plutus (Wealth) | ✅ Complete |
|   | ├ Vow of Chronos (Time) | ⏳ Pending |
|   | ├ Vow of Hephaestus (Fire) | ⏳ Pending |
|   | ├ Vow of Boreas (Ice) | ⏳ Pending |
|   | ├ Vow of Erebus (Darkness) | ⏳ Pending |
|   | ├ Vow of Apollo (Light) | ⏳ Pending |
|   | └ Vow of Dionysus (Wine) | ⏳ Pending |
| 6 | QA & Security — Pen testing, combat logging, duplication prevention | ⏳ Pending |
| 7 | Optimization & Launch — World borders, Paper tuning, 48h beta | ⏳ Pending |

## Build

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew shadowJar
```

Output: `build/libs/MythosSMP-<version>.jar`

> Requires JDK 21 (the system default JDK 26 will fail Kotlin compilation).

## Commands

| Command | Description |
|---------|-------------|
| `/vow info` | View your current Vow status |
| `/vow list` | List all available Vows |
| `/vow abilities` | View ability cooldowns |
| `/vow test <vow>` | [OP] Activate a vow for testing |
| `/trust add <player>` | Add a trusted player |
| `/trust list` | List trusted players |
| `/trust remove <player>` | Remove a trusted player |
| `/ability1` (`/a1`) | Use your first Vow ability |
| `/ability2` (`/a2`) | Use your second Vow ability |
