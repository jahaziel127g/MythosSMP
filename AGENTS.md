# Agent Guidelines for Mythos SMP Development

This document provides essential context for working in the Mythos SMP repository. Read this first to understand core conventions, commands, and architecture.

## 1. Project Overview
- **Type:** Minecraft Paper Plugin (Kotlin/Paper 1.21.11+)
- **Core Language:** Kotlin (v1.9.22)
- **Build System:** Gradle (Kotlin DSL)
- **Target Java:** JDK 21 (for build and runtime)
- **Main Plugin Class:** `com.mythossmp.MythosSMP` (defined in `src/main/kotlin/`)
- **Entrypoint (plugin.yml):** `main: com.mythossmp.MythosSMP`

## 2. Key Developer Commands
- **Clean & Build:** `./gradlew clean build` (produces `build/libs/MythosSMP-1.0.0.jar`)
- **Run Tests:** (No dedicated unit tests yet, focus on manual server testing for Phase 1)

## 3. Architecture & Conventions
- **Service-Oriented:** Decoupled UI/Events, Business Logic, Data Access layers.
- **Asynchrony:** Kotlin Coroutines used to offload heavy operations from the main server thread.
- **Database:** SQLite (`mythos_smp.db` in `plugins/MythosSMP/`) managed by HikariCP.
    - **Schema:** Defined in `src/main/kotlin/com/mythossmp/database/TableSchema.kt`.
- **Data Handling:** `PlayerDataManager` caches `PlayerProfile` objects in memory, flushes on quit/shutdown.
- **Commands:** Handled via `org.bukkit.command.CommandExecutor` in `src/main/kotlin/com/mythossmp/command/`.
- **Dependencies:** Kotlin Stdlib, Kotlin Coroutines, HikariCP, Adventure API are *shaded* into the final JAR. `org.sqlite` (JDBC driver) is *not* relocated due to native library requirements. Paper API is `compileOnly` as it's provided by the server.

## 4. GitHub Actions Workflows
- **CI (`.github/workflows/ci.yml`):** Automatically builds the project on every push to `main` and on pull requests. Ensures code compiles.
- **Release (`.github/workflows/release.yml`):** Triggers on `v*` tags (`e.g., v1.0.0`). Builds the plugin, then creates a GitHub Release and attaches the compiled JAR.
  - **Required Permission:** `contents: write` is necessary for the release action to create the release.

## 5. Plugin Verification (Phase 1 Checklist)
To verify plugin functionality on a local Paper 1.21.11 server:
- **Start Server:** Ensure plugin loads without `ERROR` messages.
  - Check log for: `[MythosSMP] MythosSMP v1.0.0 enabled successfully.`
- **Database Check:** Verify `plugins/MythosSMP/mythos_smp.db` exists with `mythos_players`, `mythos_trusts`, `mythos_unions` tables.
  - Use `sqlite3 plugins/MythosSMP/mythos_smp.db ".tables"`
- **In-game Commands (as a player):**
  - `/vow list` - Lists all 14 Vows.
  - `/vow info` - Shows current vow status.
  - `/trust add <player>` - Adds a player to the trust list.
  - `/trust list` - Lists trusted players.
  - `/trust remove <player>` - Removes player from trust list.
- **Data Persistence:** Stop the server cleanly (`/stop`), restart, verify data (e.g., trusted players) persists.
