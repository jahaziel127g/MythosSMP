# Agent Guidelines for Mythos SMP Development

This document provides essential context for working in the Mythos SMP repository. Read this first to avoid common pitfalls during development.

## 1. Core Setup Commands
- **Build the plugin with dependencies:** `./gradlew clean shadowJar` (creates `build/libs/MythosSMP-1.0.0.jar` with all required libraries shaded)
- **Run tests:** `./gradlew test` (currently requires manual server testing due to lack of unit tests)

## 2. Plugin Initialization Sequence
1. Run `./gradlew clean shadowJar` to generate the JAR
2. Start a Paper 1.21.11 server
3. Deploy the JAR to `plugins/MythosSMP/`
4. Verify logs show `[MythosSMP] MythosSMP v1.0.0 enabled successfully`

## 3. Dependency Management
All dependencies (Kotlin, HikariCP, SQLite, Paper API) are **shaded into the final JAR** via `shadowJar`. Do not package them separately.

## 4. Database Verification
- Confirm `plugins/MythosSMP/mythos_smp.db` exists with tables: `mythos_players`, `mythos_trusts`, `mythos_unions`
- Use `sqlite3 plugins/MythosSMP/mythos_smp.db ".tables"` to validate

## 5. In-Game Command Testing
As a player:
- Test `/vow list`, `/vow info`, `/trust add`, `/trust remove`, `/trust list`
- Ensure commands work after server restart

## 6. CI/CD Requirements
- PRs must pass CI (`./gradlew clean shadowJar`) before code review
- Releases require `v*` tags and `contents: write` permission for GitHub Actions

## 7. Plugin Uninstallation
Run `server.stop` or `/stop` to trigger graceful shutdown. Player data is flushed to database during shutdown.