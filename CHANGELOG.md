# Changelog

## 1.6.0-Alpha — 2026-08-02

### Two new selectable vows (per implementation plan)
- **Poseidon (God of the Sea)** — altar slot 22 (Trident):
  - Passive: Dolphin's Grace + water breathing + Haste V while in water
  - `Wave Crash` (35s): pushes all untrusted entities outward in a wave
  - `Abyssal Blessing` (10s): Strength I + Regeneration I while in water, stripped 3s after leaving
  - Curse `Dehydration`: no water/rain contact for 5 minutes → 1 damage every 3s
  - Trial: 3 waves of Drowned/Zombie → Drowned/Skeleton → Drowned/Phantom
- **Hermes (God of Travel)** — altar slot 25 (Feather):
  - Passive: double jump (space, 15s cooldown) + complete fall damage immunity
  - `Dash` (6s): launch forward 12 blocks in facing direction
  - `Zephyr Wing` (150s): 8s of flight; taking damage cancels it into 1s slow falling
  - Curse `Light Constitution`: armor heavier than chainmail is ejected + 4 damage
  - Trial: 3 waves of Spider/Zombie → Spider/Skeleton → Spider/Phantom
- Altar menu reworked to 2 rows of 3 (slots 10/13/16/19/22/25); `GUIExploitStressTest` updated
- Config sections `vows.poseidon.*` and `vows.hermes.*` with strict validation + fallback defaults

## 1.5.1-Alpha — 2026-07-31

### Zeus: full selectable vow
- `VowZeus` now implements `Vow` — registered in `VowRegistry`, selectable via altar (Lightning Rod icon, slot 14) and `/vow accept`
- Zeus trial added (`/vow accept zeus` requires completing it): 3 waves ending in Blaze/Skeleton
- Abilities: `Static Strike` (30s CD — strikes the nearest enemy in front of you with lightning) and `Overcharge` (60s CD — fills the static meter to 100%)
- All Zeus mechanics (charge accumulation, static strike on full charge, conductive curse) now require `zeus` as the active vow — no longer global
- StatusDisplay: static charge bar + ability cooldowns in the ActionBar and dev sidebar
- `VowZeusTest` — charge cap, negative-input guard, consume semantics, registry metadata

## 1.5.0-Alpha — 2026-07-31

### Phase 7: Vow of Zeus (Environmental System)
- `VowZeus` — pure domain charge engine (thread-safe, hard cap 1.0)
- `ZeusListener` — horizontal movement charges static energy; a full charge unleashes a static strike on attack (+5 flat damage) with decoupled VFX
- Conductive curse — MAGIC/LIGHTNING/FIRE/FIRE_TICK damage amplified ×1.2, doubled while in water or during storms
- `ZeusStaticOverchargeVFXEvent` → `strikeLightningEffect` rendered by `VFXPresentationListener`
- `vows.zeus.*` config with startup validation and fallback defaults

### Phase 8: Bootstrap & Composition Root
- `Bootstrap` — 8-step init pipeline with step-level failure tracing; `MythosSMP.onEnable` delegates entirely to it
- `ServiceContainer` — immutable holder of all 16 services; `MythosSMP` exposes container-backed getters
- `Scheduler` abstraction (`application/task`) + `PaperScheduler` impl — every background task (trial tick, status display, deferred vow abilities) decoupled from the Bukkit scheduler
- `ListenerRegistrar` / `CommandRegistrar` — centralized event + command wiring; legacy `CommandRegistry` removed
- Async logout — dirty profile saves dispatch via `scheduler.async`; no more main-thread SQLite writes on quit
- Trial entity cap — spawn guard clips custom mobs at 25 per arena via boundary-centered scan

### Stress Suite
- `GUIExploitStressTest` — 110 exploit inputs (SHIFT/DOUBLE_CLICK/NUMBER_KEY/drags) all cancelled, zero inventory mutation
- `EntityThrottleStressTest` — 200-spawn flood clipped at exactly 25 custom entities
- `AsyncLogoutStressTest` — 80 simultaneous quits against real SQLite with zero data loss
- Runs via `./gradlew stressTest` or bundled in `integrationTest` (71 tests, all green)

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
