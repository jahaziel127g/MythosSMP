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
|   | └ Remaining 12 Vows | ⏳ Pending |
| 6 | QA & Security — Pen testing, combat logging, duplication prevention | ⏳ Pending |
| 7 | Optimization & Launch — World borders, Paper tuning, 48h beta | ⏳ Pending |

## Build

```bash
./gradlew build
```

Output: `build/libs/MythosSMP-1.0.0.jar`

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
