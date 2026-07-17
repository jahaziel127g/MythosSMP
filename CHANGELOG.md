# Changelog

## 1.0.0 — 2026-07-17

### Phase 1: Foundation
- Gradle project setup with Kotlin DSL, Paper 1.21.11 API
- SQLite database with HikariCP connection pool
- Tables: `mythos_players`, `mythos_trusts`, `mythos_unions`
- Profile caching via `AsyncPlayerPreLoginEvent` with dirty-flag tracking
- Commands: `/vow info|list|accept`, `/trust add|remove|list`
- Config system and graceful shutdown with dirty profile flush
