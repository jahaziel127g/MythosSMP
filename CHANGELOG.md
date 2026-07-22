# Changelog

## 1.2.0 — 2026-07-22

### Phase 3: Trial Engine
- Trial state machine: `INACTIVE → SUMMONING → ACTIVE → COMPLETED/FAILED`
- `TrialService` with 1-second tick loop managing active sessions
- Wave-based mob spawning with party and wave scaling
- `MobScalingFormula`: health = base × partyMult × waveMult
- `GeoBoundary` cuboid containment with teleport-back enforcement
- `ScrollItemRegistry` — custom scroll items via PersistentDataContainer
- ActionBar coordinate broadcasts every 10s, per-wave timeout (120s)
- `/trial start|info|leave|give` commands
- `/vow accept` now requires completing the matching trial first
- `mythos_completed_trials` table in SQLite schema

### Fixes
- DDD refactor: 22 files moved into presentation/application/domain/infrastructure layers
- Cooldown display bug: wrong lookup key showed 0s remaining
- Cooldown gate bug: cooldown was set even when ability returned false
- Trial state crash: `ACTIVE → ACTIVE` transition on wave advance
- Mobs suffocating in floor: Y offset moved to surface + 1
- Replace Wolf and IronGolem with naturally player-hostile mobs (Creeper, Vindicator, Evoker)

## 1.0.0 — 2026-07-17

### Phase 2: Core Framework
- Vow, ActiveAbility, PassiveEffect, Curse interfaces
- VowRegistry with registration API and player lookup
- CooldownManager with action-bar countdowns and global multiplier
- AbilityManager with permission and cooldown checks
- Custom Bukkit events: VowUnlockEvent, VowLossEvent, TrialStartEvent
- Full Vow of Ares implementation:
  - Passive: Strength I + Rage Meter (1 rage per dmg dealt, 2 per dmg taken)
  - Ability 1: Bloodthorns — 15s damage-scaling mode
  - Ability 2: Rage Aura — 8-block shockwave, 12 damage, trusted-player skip
  - Curse: War Hunger — reduced saturation
- `/ability1` (`/a1`) and `/ability2` (`/a2`) commands
- `/vow test` (OP-only) for dev testing
- `/vow abilities` for cooldown status

### Phase 1: Foundation
- Gradle project setup with Kotlin DSL, Paper 1.21.11 API
- SQLite database with HikariCP connection pool
- Tables: `mythos_players`, `mythos_trusts`, `mythos_unions`
- Profile caching via `AsyncPlayerPreLoginEvent` with dirty-flag tracking
- Commands: `/vow info|list|accept`, `/trust add|remove|list`
- Config system and graceful shutdown with dirty profile flush
