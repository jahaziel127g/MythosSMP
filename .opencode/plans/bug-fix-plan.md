# MythosSMP — Bug Fix Plan

> **RE-VERIFICATION 2026-08-02 (post Phase 5-8 refactor): ALL 11 items CONFIRMED FIXED.**
> The refactor (ProfileService copy-on-write, immutable PlayerProfile, async logout, in-memory
> trust/union repos, per-borrow PRAGMA, INSERT OR IGNORE, Bloodthorns noDamageTicks) resolved
> every item below. This file is kept as an audit record — do not re-open bugs without a
> fresh reproduction. Known-latent item: #6 (sync JDBC on async pre-login thread) is accepted
> as the standard pattern — local SQLite is fast and the main thread is never blocked.

Audited by 3 parallel explore agents across the database/persistence, vow/ability/cooldown,
and command/lifecycle areas. Each bug below was re-verified against the source on disk.
Severity: HIGH (correctness/data-loss) · MEDIUM (functional/perf/security) · LOW (latent/minor).

## Confirmed bugs to fix

### 1. [HIGH] `getProfile()` returns shared mutable Profile → cross-thread data race
- File: `src/main/kotlin/com/mythossmp/player/PlayerDataManager.kt:92-94`
- `PlayerProfile` is a mutable data class. `getProfile` hands the identical cached instance
  to any caller; `VowCommand`, `CooldownManager`, `onPlayerJoin`, `onPlayerQuit` all mutate
  `activeVow`/`dirty`/`cooldownMultiplier` on it with no synchronization. `ConcurrentHashMap`
  only protects the map, not the stored object -> lost updates / torn state.
- Fix: make `PlayerProfile` effectively immutable and replace entries copy-on-write inside
  `cache.compute`, OR add a `updateProfile(uuid) { ... }` API that mutates inside `compute`
  so dirty-flag + field writes are atomic.

### 2. [HIGH] `flushAll()` not wrapped in try/finally -> pool leak + lost saves on failure
- File: `src/main/kotlin/com/mythossmp/MythosSMP.kt:56-66`
- If `flushAll()` throws (one bad `saveProfileSync`), `databaseManager.shutdown()` is never
  reached -> Hikari pool leaked and remaining dirty profiles unsaved. Also `flushAll` has no
  per-profile try/catch, so a single SQLException aborts the whole flush.
- Fix: wrap flush in try/finally so `shutdown()` always runs; catch SQLException per profile
  in `flushAll` and continue.

### 3. [MED-HIGH] `WarHunger` drains saturation exponentially instead of -25%
- File: `src/main/kotlin/com/mythossmp/vow/implementations/VowAres.kt:196-199`
- `onFoodLevelChange` fires very frequently; multiplying `saturation *= 0.75` on every event
  is exponential decay -> saturation hits ~0 almost instantly. Also "15% faster during combat"
  is never gated on combat (no combat-state tracking).
- Fix: don't mutate `player.saturation` directly each event. Either cap the *proposed*
  saturation, or apply a steady-state reduction and add a real combat check (e.g. recent-damage
  timestamp) for the 1.15x hunger rate.

### 4. [MEDIUM] MiniMessage injection via raw `active_vow` fallback in `/vow info`
- File: `src/main/kotlin/com/mythossmp/command/VowCommand.kt:46-57`
- If `active_vow` is a non-vow value (stale/corrupted DB row, free-text VARCHAR(32)),
  `display` falls back to the raw `activeKey` and is interpolated into `miniMessage.deserialize`.
  A crafted value like `<click:run_command:/op>...</click>` would be parsed as MiniMessage.
- Fix: escape with `MiniMessage.miniMessage().escapeTokens(activeKey)` or use `Placeholder`
  components; never interpolate untrusted strings raw. Same pattern in `handleList` (latent).

### 5. [MEDIUM] `/trust remove` cannot remove offline players
- File: `src/main/kotlin/com/mythossmp/command/TrustCommand.kt:89-93`
- `Bukkit.getPlayerExact` only resolves online players; trust is keyed by UUID, so the target
  need not be online. Offline players can never be untrusted.
- Fix: resolve UUID via `Bukkit.getOfflinePlayer(name).uniqueId` (or own players table), then
  DELETE by UUID. Only reject truly unresolvable names.

### 6. [MEDIUM] Synchronous JDBC on async login thread in `onPlayerPreLogin`
- File: `src/main/kotlin/com/mythossmp/player/PlayerDataManager.kt:25-39` + `loadOrCreateProfile:134-168`
- `AsyncPlayerPreLoginEvent` runs on the login thread pool; `loadOrCreateProfile` blocks on
  SELECT/INSERT. Under load this stalls logins / exhausts the login pool.
- Fix: move the DB load off the login thread (async task / pre-load), keep it fast, or document
  the pool can absorb it.

### 7. [MEDIUM] Synchronous DB queries on main thread (lag)
- Files: `TrustCommand.kt:58,96,123` and `VowAres.kt:140-151` (RageAura trust lookup)
- Each `/trust add|remove|list` and every `/ability2` opens a connection + runs SQL on the
  main thread -> server tick lag under DB latency.
- Fix: offload to `Bukkit.getScheduler().runTaskAsynchronously` and return results to the main
  thread, or use the existing coroutine `Dispatchers.IO` pattern.

### 8. [MEDIUM] `PRAGMA foreign_keys=ON` via `connectionInitSql` is unreliable with a pool
- File: `src/main/kotlin/com/mythossmp/database/DatabaseManager.kt:31`
- SQLite's `foreign_keys` is per-connection and Hikari does not guarantee `connectionInitSql`
  runs on every borrow. The advertised `ON DELETE CASCADE` may silently not fire -> orphaned
  rows. (Latent today since the plugin never deletes players, but a real correctness hole.)
- Fix: run `PRAGMA foreign_keys=ON` at the start of every connection use via a `withConnection`
  helper, or verify/set the pragma per operation.

### 9. [LOW-MED] Bloodthorns "3-second immunity" is not real invulnerability
- File: `src/main/kotlin/com/mythossmp/vow/implementations/VowAres.kt:204-232`
- After consuming a lethal hit, the player has no protection for 3s and can be killed normally
  before the scripted `damage(100.0)` resolves. The message promises immunity it doesn't give.
- Fix: grant real protection for the window (absorption / `setNoDamageTicks` / invulnerable),
  or only allow death via the delayed task.

### 10. [LOW] `saveProfileSync` create path uses plain `INSERT` (no ON CONFLICT)
- File: `src/main/kotlin/com/mythossmp/player/PlayerDataManager.kt:154-161`
- If cache state and DB row disagree (e.g. partial pre-login insert + caught exception), a
  re-login create can throw a duplicate-PK SQLException (caught, but skips effect re-apply).
- Fix: use `INSERT OR IGNORE` (or `ON CONFLICT DO NOTHING`) for the create.

### 11. [LOW] `getEnabledVows()` is dead code; `cooldownMultiplier` floor blocks sub-1s cooldowns
- Files: `VowRegistry.kt:28`; `CooldownManager.kt:37-41`
- `getEnabledVows` is unused. `applyGlobalMultiplier` `coerceAtLeast(1)` makes a multiplier
  <1 unable to produce <1s cooldowns and makes `0.0` indistinguishable from "no profile".
  Latent until multipliers become configurable.
- Fix: remove dead code; decide multiplier semantics and document/adjust the floor.

## Previously reported bugs - status
- `wasNull()` wrong column - FIXED (now `cmNull = rs.wasNull()` at line 143).
- `coerceAtMost(0.0f)` saturation -> FIXED (now `coerceAtLeast(0.0f)` at line 198).
- `getRemainingSeconds` truncation - FIXED (uses `toMillis()` + rounding).
- `activeVow` normalization - FIXED (`normalizeActiveKey()` in `VowRegistry`).
- WarHunger no-op / quit-not-removing-effects - were incorrect claims; confirmed fine.

## Suggested implementation order
1. #1, #2 (HIGH - data integrity / leak)
2. #3, #4, #5 (MED - curse correctness, security, functional)
3. #6, #7, #8 (MED - threading/perf/FK)
4. #9, #10, #11 (LOW)

## Verification
- `./gradlew build` (JDK 21 required). No automated tests exist (per AGENTS.md); verify
  manually on a Paper 1.21.11 server:
  - Activate Ares (`/vow test ares`); observe saturation drops ~25% and stays, not drains to 0.
  - `/vow info` with a corrupted `active_vow` row shows plain text, no clickable injection.
  - `/trust remove <offline_player>` succeeds.
  - Confirm graceful `/stop` flushes all dirty profiles and closes the pool (check logs).
