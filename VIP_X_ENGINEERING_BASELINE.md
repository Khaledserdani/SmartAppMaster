# VIP X — Engineering baseline

## Product goal
VIP X is a personal Android app-analysis and enhancement workbench. It must expose real capabilities, report platform restrictions honestly, and never label a recommendation as a completed modification.

## Implemented foundation in this branch
- Launcher-app inventory using Android PackageManager launcher queries.
- Package identity, version, installer source (where Android exposes it), base APK path, file size, and SHA-256 report.
- User-selected export of the installed base APK through Storage Access Framework.
- Clear notice that app-private databases and private files are sandboxed by Android and are not extracted.
- The original MainActivity remains in the project; VipXActivity is the new launcher.

## Important Android limits
- A normal third-party app cannot read another app's private /data/data database or internal files.
- Installed APK access may expose a base APK and split APK paths, but split apps need all required splits to be reconstructed and installed as a set. Current export intentionally saves only the base APK and warns when splits exist.
- APK extraction is not source-code recovery. Decompiled resources/code are not equivalent to the original project and may be restricted by license.
- Repackaging, resigning, modifying paid features, bypassing authentication, DRM, anti-cheat, or server-side entitlements is not a supported feature.
- Creating a genuinely enhanced build requires lawful source code or a documented extension API / SDK and a reproducible build pipeline.

## Planned engineering phases
1. Build and verify the installed-package inspector.
2. Add split-APK set export as an archive with hashes and a manifest.
3. Add source/project import for user-owned or permissively licensed apps.
4. Add static analysis and dependency/resource inventory with explicit findings.
5. Add AI-assisted patch proposals with human approval, diffs, tests, and rollback.
6. Add isolated Gradle build workers and APK signing using a key kept under the user's control.
7. Add secure OpenAI API and GitHub OAuth integrations. Never embed API secrets or OAuth client secrets in the APK.

## Definition of done
A feature is complete only when a test demonstrates its behavior on a real device or CI build. The app must distinguish: supported, unsupported, permission-required, and not-tested.
