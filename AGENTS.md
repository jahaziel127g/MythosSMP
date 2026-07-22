# Agent Guidelines for Mythos SMP Development

Kotlin/Paper 1.21.11 plugin. Packages shaded into a single fat JAR. No automated tests.

## Build
- **Must use JDK 21** — system JDK is 26, so run `JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew shadowJar`
- Output: `build/libs/MythosSMP-1.0.0.jar`; deploy to `~/server/plugins/` for manual testing
- Shadow relocates `kotlin`, `kotlinx`, `com.zaxxer.hikari` under `com.mythossmp.libs.*`

## Architecture (DDD layers)
```
MythosSMP.kt          — entrypoint (onEnable order: Config → DB → ProfileService → VowRegistry/Cooldown/Ability → Trial → listeners → commands)
presentation/         — commands + display + events (routers only)
application/          — services (ProfileService, AbilityExecutor, CooldownService, TrialService)
domain/               — models + vow logic (TrialState, TrialSession, TrialConfig, GeoBoundary, MobScalingFormula, PlayerProfile, Vow, VowAbility, VowRegistry, VowAres)
infrastructure/       — DB + config + items (DatabaseManager, SqlProfileRepository, ConfigManager, ScrollItemRegistry)
```
- `ProfileRepository` interface in `application/profile/` — `SqlProfileRepository` in `infrastructure/database/` implements it
- Domain files (`VowRegistry`, `VowAres`) reference `MythosSMP.instance` directly — known DDD shortcut, don't refactor
- `ActiveAbility.execute()` returns `Boolean` — cooldown only set on `true`; `RageAura` returns `false` when rage < 100%
- `StatusDisplay` (started/stopped in `onEnable`/`onDisable`) drives ActionBar + Scoreboard; call `refreshPlayer(player)` for immediate push
- Trials use a 6-state machine: `INACTIVE → SUMMONING → ACTIVE → COMPLETED/FAILED → INACTIVE`
- Scroll items use `PersistentDataContainer` with key `mythossmp:trial_key` — created via `ScrollItemRegistry.createScroll(config)`

## Init order matters
```
ConfigManager → DatabaseManager → SqlProfileRepository → ProfileService → VowRegistry → CooldownService → AbilityExecutor
  → ScrollItemRegistry → TrialService (registerDefaultTrials + start) → registerVows → registerListeners → CommandRegistry → StatusDisplay
```

## Data
- SQLite at `plugins/MythosSMP/mythos_smp.db` (tables: `mythos_players`, `mythos_trusts`, `mythos_unions`, `mythos_completed_trials`)
- Profiles cached in `ProfileService`, flushed on `/stop` — unsaved data lost on crash
- `plugins/` is gitignored

## Commands
- `/vow info|list|accept|abilities|test`, `/trust add|remove|list`, `/ability1` (`/a1`), `/ability2` (`/a2`), `/trial start|info|leave|give`
- `/vow test` and `/trial give` are OP-only; `mythossmp.ability` permission gates abilities
- `/vow accept` now requires completing the matching trial first

## Trial Engine Notes
- `TrialService` runs a 1-second repeating tick managing all active sessions
- Mob scaling: `health = base * (1 + (partySize-1)*0.5) * (1 + (wave-1)*0.35)`
- Boundaries are auto-generated as 41×16×41 cuboids centered on the initiator
- Party members can be added during `SUMMONING` phase via `addPartyMember()`
- Coordinates broadcast via ActionBar every 10s; boundary violations teleport back to center
- Trial configs are registered in `MythosSMP.registerDefaultTrials()` — hardcoded, not loaded from config.yml

## Release
- Tag `v*` → `release.yml` builds + attaches JAR via GitHub. Bump version in `build.gradle.kts` and `CHANGELOG.md`.
