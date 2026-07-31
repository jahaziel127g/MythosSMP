# Changelog

## 1.4.0-Alpha — 2026-07-30

### Phase 6: Altar & Vow Selection UI
- `VowSelectionMenu` — 9x3 chest GUI with deity icons (Netherite Sword/Ares, Ender Eye/Loki, Wither Skeleton Skull/Hades), active-state indicators, ability lore with cooldown status, and curse descriptions
- `MenuListener` — strict click/drag cancellation on menu inventory; slot clicks trigger full state transition (strip old vow modifiers → update profile → apply new modifiers → VFX confirmation → close)
- `AltarListener` — right-click `CRYING_OBSIDIAN` opens the vow selection menu; default block behavior cancelled
- `VowSelectionActivate` VFX event added — colored dust burst + End Rod particles on vow switch
- Inventory tracked via `MenuHolder` (custom `InventoryHolder`); `MenuListener.markOpen/Closed` maintains open-set per player
- `MenuInteractionTest` integration test asserts altar interaction, menu size, and click cancellation

## 1.3.0-Alpha — 2026-07-29

### Phase 5: God Expansion (Foundation)
- Auto-validating Vow configuration system in `ConfigManager` — strict startup validation with fallback to hardcoded defaults
- `ConfigManager.getVowConfig(key)` loads validated settings per vow from `config.yml`
- `VowLoki` — Vow of the Trickster Rogue:
  - Curse: Fragility of Deceit — reduces max health by 4 HP via `Attribute.MAX_HEALTH`
  - Passive: Backstab — 2.5x damage multiplier when dot(attackerDirection, targetDirection) > 0.75
  - Ability 1: Shadow Step — teleport behind nearest enemy within configurable range
  - Ability 2: Veil of Lies — invisibility + first-attack 3x damage bonus
- VowLoki registered in `VowRegistry`; backstab listener registered globally

### Aesthetic Decoupling Layer (VFX Pipeline)
- `VowVFXEvent` sealed class in `domain/event/` — 12 pure-data subclasses for all particle contexts across Ares, Loki, and Hades
- `VFXBroadcastService` (in `application/vfx/`) — publishes events via Bukkit's `callEvent()`, replacing direct particle calls
- `VFXPresentationListener` (in `presentation/listener/`) — single responsibility for all particle rendering; registered in global listener chain
- All 16 `spawnParticle` calls extracted from domain and combat listeners; domain modules now publish lightweight data events only
- `VFXArchitectureTest` integration test verifies end-to-end event dispatch through the Bukkit pipeline

### Fixes & Improvements
- WarHunger curse: saturation now properly reduced by 25% after eating (was missing entirely)
- WarHunger curse: 15% hunger bonus now correctly gated on combat state (within 10s of last damage)
- RageAura: raw JDBC replaced with `TrustService.isTrusted()` — no more main-thread blocking
- `LokiAbilitiesTest` unit test updated to mock `VFXBroadcastService` on the plugin stub

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
