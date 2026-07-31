# Agent Guidelines for Mythos SMP Development

Kotlin/Paper 1.21.11 plugin. Packages shaded into a single fat JAR. No automated tests.

## Build
- **Must use JDK 21** — system JDK is 26, so run `JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew shadowJar`
- Output: `build/libs/MythosSMP-1.0.0.jar`; **always copy to `~/server/plugins/`** after every build for manual testing
- Shadow relocates `kotlin`, `kotlinx`, `com.zaxxer.hikari` under `com.mythossmp.libs.*`
- Version in `build.gradle.kts:7` is source of truth (`1.0.0`); `CHANGELOG.md` is human-maintained and may reference future versions

## Architecture (DDD layers)
```
src/main/kotlin/com/mythossmp/
  MythosSMP.kt          — entrypoint
  presentation/         — commands + display + events + UI menus (routers only)
    ui/menu/            — VowSelectionMenu, MenuListener, AltarListener
  application/          — services (ProfileService, AbilityExecutor, CooldownService, TrialService, CombatService, TrustService, UnionService)
  domain/               — models, trial engine, vow logic
    event/              — VowVFXEvent (sealed) — pure-data VFX domain events
    model/              — PlayerProfile, Vow, VowAbility, TrustRelation, SealOfUnion
    trial/              — TrialState, TrialSession, TrialConfig, GeoBoundary, MobScalingFormula
    vow/                — VowRegistry, VowAres, VowLoki
  infrastructure/       — DB, config, items (DatabaseManager, SqlProfileRepository, ConfigManager, ScrollItemRegistry)
```
- `ProfileRepository` interface in `application/profile/` — `SqlProfileRepository` in `infrastructure/database/` implements it
- `VowRegistry` and vow classes reference `MythosSMP.instance` directly — known DDD shortcut, don't refactor
- `ActiveAbility.execute()` returns `Boolean` — cooldown only set on `true`; `RageAura` returns `false` when rage < 100%
- **VFX Decoupling Rule**: No `spawnParticle` or `playSound` calls are allowed inside domain logic or raw combat listeners. All visual effects must be dispatched as pure-data `VowVFXEvent` domain events (in `domain/event/`) via `VFXBroadcastService.publish(event)`. The `VFXPresentationListener` (in `presentation/listener/`) handles rendering — it is the single location for Bukkit particle/sound API calls.
- `VowVFXEvent` is a sealed class extending `Event` with a shared `HandlerList`. Each VFX context has its own data subclass carrying only `Location` and optional numeric parameters (e.g. `radius`).
- `StatusDisplay` (started/stopped in `onEnable`/`onDisable`) drives ActionBar + Scoreboard; call `refreshPlayer(player)` for immediate push
- Trial state machine (5-state): `INACTIVE → SUMMONING → ACTIVE → COMPLETED/FAILED → INACTIVE` — transitions validated by `TrialState.canTransitionTo()`
- Scroll items use `PersistentDataContainer` with key `mythossmp:trial_key` — created via `ScrollItemRegistry.createScroll(config)`
- Trial configs hardcoded in `MythosSMP.registerDefaultTrials()` (also mirrored in `config.yml` but code ignores the file)
- **Phase 5**: Vow configs loaded from `config.yml` under `vows.<key>` with strict validation and fallback to hardcoded defaults. See `ConfigManager.getVowConfig()`. All numeric fields validated at startup; out-of-range values logged and replaced.
- **Phase 6**: Vow selection via Altar + GUI menu. Altar block is `CRYING_OBSIDIAN`. Right-click opens a 9x3 `VowSelectionMenu` with deity icons (Netherite Sword/Ares, Ender Eye/Loki, Wither Skeleton Skull/Hades). `MenuListener` handles clicks/drags — all cancelled; slot clicks trigger state transition (strip old → apply new → VFX → close). Tracked via `MenuHolder` custom `InventoryHolder`. `MenuListener.markOpen/Closed` tracks open menus.
- **Phase 8**: Bootstrap & Composition Root. `MythosSMP.onEnable` delegates to `Bootstrap(this).run()`, which returns an immutable `ServiceContainer`. All services are assembled inside `Bootstrap`; `MythosSMP` exposes `container` + convenience getters (`plugin.trialService` etc.). Listener/command wiring lives in `bootstrap/ListenerRegistrar` and `bootstrap/CommandRegistrar` — never register events or commands in `MythosSMP` itself.
- **Scheduler abstraction**: `application/task/Scheduler` (interface) + `infrastructure/task/PaperScheduler` (Bukkit impl). No service may call Bukkit scheduler directly — use `scheduler.later(Duration)` / `scheduler.repeat(Duration): Cancellable`. Tests use `src/test/kotlin/com/mythossmp/task/FakeScheduler.kt`.
- **Zeus (environmental system)**: `domain/vow/VowZeus` (pure charge engine, cap 1.0) + `presentation/listener/ZeusListener` (move→charge, attack→static strike consume + VFX, damage→conductive curse multiplier for MAGIC/LIGHTNING/FIRE/FIRE_TICK, doubled in water/storm). Config under `vows.zeus.*` (`ConfigManager.ZeusConfig` + `getZeusConfig()`); rendered via `VowVFXEvent.ZeusStaticOverchargeVFXEvent` → `strikeLightningEffect`. Not registered in `VowRegistry` — no altar selection. `VowZeus` lives in `ServiceContainer.vowZeus`.
- **Entity cap guard**: `TrialService.spawnMob` scans the trial arena (boundary-centered box, extent `radius+2`) for tracked trial mobs before spawning; hard cap `MAX_CUSTOM_ENTITIES = 25` per arena. Normal waves never reach it; large-party flooding is clipped.
- **Async logout**: `ProfileService.onPlayerQuit` no longer blocks the main thread — dirty saves dispatch via `scheduler.async { saveProfileSync(snapshot) }`. `ProfileService` constructor takes `Scheduler`.
- **Stress suite**: `src/testIntegration/kotlin/com/mythossmp/stress/` (`GUIExploitStressTest`, `EntityThrottleStressTest`, `AsyncLogoutStressTest` + its own `FakeScheduler` copy — unit and testIntegration source sets cannot share test classes). Runs via `./gradlew stressTest` or bundled in `integrationTest`. testIntegration classpath includes sqlite-jdbc, HikariCP, kotlinx-coroutines.
- Known mutable-state bugs documented in `.opencode/plans/bug-fix-plan.md` — read before modifying persistence or command code

## Init order (matches `Bootstrap.run()`)
```
loadConfig (ConfigManager) → connectDatabase (DatabaseManager) → initializeRepositories (SqlProfileRepository)
  → initializeServices (ProfileService, TrustService, UnionService, CombatService, CooldownService, VFXBroadcastService, AbilityExecutor, ScrollItemRegistry)
  → registerVowsAndTrials (VowRegistry + registerDefaultTrials: ares/loki/hades, TrialService.start, StatusDisplay.start)
  → assembleContainer (ServiceContainer) → registerPresentation (ListenerRegistrar + CommandRegistrar) → startBackgroundServices
```
- `Bootstrap` sets `plugin.scheduler` early — domain vow classes use `MythosSMP.instance.scheduler` (known DDD shortcut, don't refactor)
- `onDisable` flushes profiles → stops trial service → stops status display → shuts down DB (in that order)

## Data
- SQLite at `plugins/MythosSMP/mythos_smp.db` (tables: `mythos_players`, `mythos_trusts`, `mythos_unions`, `mythos_completed_trials`)
- Profiles cached in `ProfileService`, flushed on `/stop` — unsaved data lost on crash
- `plugins/` is gitignored

## Commands
- `/vow info|list|accept|abilities|test`, `/trust add|remove|list`, `/ability1` (`/a1`, `/ab1`), `/ability2` (`/a2`, `/ab2`), `/trial start|info|leave|give`
- `/vow test` and `/trial give` are OP-only; `mythossmp.ability` permission gates abilities (default: true)
- `/vow accept` requires completing the matching trial first
- Plugin aliases in `plugin.yml` — check there before adding new ones

## Trial Engine
- `TrialService` runs a 1-second repeating tick managing all active sessions
- Mob scaling: `health = base * (1 + (partySize-1)*0.5) * (1 + (wave-1)*0.35)`
- Boundaries are auto-generated as 41 wide × 16 tall × 41 deep cuboids centered on the initiator (±20 X/Z, −5/+10 Y)
- Party members can be added during `SUMMONING` phase via `addPartyMember()`
- Coordinates broadcast via ActionBar every 10s; boundary violations teleport back to center
- `spawnMob` enforces the 25-custom-entity arena cap (see entity cap guard above)

## Testing
- **Unit tests** (`src/test/`): Pure-logic tests only (vector math, config validation, data classes, mocks that don't touch Bukkit registries). Run with `JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew test`. Includes `FakeScheduler` + `SchedulerAbstractionTest` for the scheduler boundary.
- **Integration tests** (`src/testIntegration/`): Registry-dependent tests (PotionEffectType, DamageType, events), database transactions, live player interaction logic. Run with `JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew integrationTest`.
- The `integrationTest` task depends on the `test` task passing and requires a bootstrapped Paper server. Tests in this source set are excluded from the default `check` lifecycle until the server-bootstrap harness is wired.
- Place all registry-bound or server-context tests in `src/testIntegration/`; keep `src/test/` for isolated, mockable logic.
- **MockBukkit** (`org.mockbukkit.mockbukkit:mockbukkit-v1.21:4.110.0`) bootstraps registry access via `MockBukkit.mock()` in `@BeforeEach`. Do NOT call `MockBukkit.load(pluginClass)` — `MythosSMP` is a Kotlin `final` class and ByteBuddy cannot subclass it. Instead, test abilities by instantiating them directly against the mock server.
- `MythosSMP` class is `final` (Kotlin default). MockBukkit's `load()` fails with `Cannot subclass final class`. Use `MockBukkit.mock()` only for the server scaffold.

## Release
- Tag `v*` → GitHub Actions `release.yml` builds + attaches JAR. Bump version in `build.gradle.kts` and `plugin.yml`, update `CHANGELOG.md`.
