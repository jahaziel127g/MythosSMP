# Release Process

## Creating a Release

1. Update version in `build.gradle.kts` and `plugin.yml`
2. Update `CHANGELOG.md` with the new version
3. Commit and tag:
   ```bash
   git tag -a v1.0.0 -m "v1.0.0"
   git push origin v1.0.0
   ```
4. The GitHub Actions workflow will build and attach the JAR

## Version Scheme

- `major.minor.patch` — follows semantic versioning
- Pre-release suffixes: `-alpha`, `-beta`, `-rc1` etc.

## Artifacts

The built JAR is output at `build/libs/MythosSMP-<version>.jar`.
