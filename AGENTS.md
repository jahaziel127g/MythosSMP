# Agent Guidelines for Mythos SMP Development

Kotlin/Paper 1.21.11 Minecraft plugin. Packages are shaded into a single fat JAR.

## Commands
- **Build (and produce JAR):** `./gradlew build` — CI and releases run this. `build` depends on `shadowJar`, which emits `build/libs/MythosSMP-1.0.0.jar`.
- **Build JAR directly:** `./gradlew shadowJar`.
- **No automated tests exist.** There is no `src/test` and `./gradlew test` has nothing to run. Verification is manual: deploy to a Paper 1.21.11 server and test in-game.

## Toolchain
- Requires **JDK 21** (`gradle.properties` pins `org.gradle.java.installations.paths` and `build.gradle.kts` sets the toolchain to 21). Build fails on other Java versions.
- Shadow plugin relocates `kotlin`, `kotlinx`, and `com.zaxxer.hikari` under `com.mythossmp.libs.*`. Do not reference un-relocated names in downstream code expecting them at top level.

## Plugin lifecycle
- Entrypoint: `com.mythossmp.MythosSMP` (`src/main/kotlin/com/mythossmp/MythosSMP.kt`). `onEnable` order matters: Config → Database → PlayerData → VowRegistry/Cooldown/Ability → register vows & listeners → commands.
- Vows are registered manually in `registerVows()` (only `VowAres` today). Adding a Vow requires both a registry entry and a listener registration.
- Data persists in SQLite at `plugins/MythosSMP/mythos_smp.db` (tables: `mythos_players`, `mythos_trusts`, `mythos_unions`). Profiles are cached and flushed on `onDisable`; always `/stop` gracefully or unsaved state is lost.
- `plugins/` is gitignored, so deployment artifacts are never committed.

## In-game verification
- Commands: `/vow info|list|accept|abilities|test`, `/trust add|remove|list`, `/ability1` (`/a1`), `/ability2` (`/a2`).
- `/vow test` and `mythossmp.ability` are OP/permission-gated. Confirm persistence survives a server restart.

## Releasing
- Tag `v*` → GitHub Actions (`release.yml`) builds and attaches the JAR, using `CHANGELOG.md` as the release body.
- Bump version in `build.gradle.kts` (the `version` property; `archiveFileName` reuses it) and `plugin.yml` (`version:` is `expand`ed from it in `processResources`). Update `CHANGELOG.md` before tagging.

## What NOT to do
- Don't add external dependencies as `compileOnly` unless the server supplies them (Paper provides `adventure`); everything else must be shaded `implementation`.
- Don't commit JARs, `plugins/`, or `.gradle/` (gitignored).
