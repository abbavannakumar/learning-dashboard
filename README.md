# Learning Dashboard — Android

Kotlin / Jetpack Compose, Android 8+ (API 26). Three screens: login, course dashboard, and lesson details.

**Run:** Open in Android Studio with JDK 17+ and run `app`. CLI: `./gradlew assembleDebug testDebugUnitTest lintDebug` (Windows: `gradlew.bat`). APK: `app/build/outputs/apk/debug/app-debug.apk`. Demo login: **demo@example.com / password123**, including offline. A cold launch requires login; passwords are never persisted.

## 1. Architecture
Compose → ViewModels/StateFlow → repositories → mock API + Room. This separates rendering, presentation state, and data access without unnecessary layers. Manual application-scoped injection keeps dependencies explicit. Both screens observe Room flows; progress derives from completed lessons. Coroutines preserve cancellation, refreshes are serialized, and loading/success/empty/error states are handled.

## 2. Offline support
Refresh transactionally saves courses and lessons to Room. Completion persists immediately and survives refresh/restart. Metadata updates preserve local completion. API failures and empty responses retain cached courses with a warning. Without a cache, errors show retry and empty responses show an empty screen.

The API is bundled JSON with simulated latency and a device-network check, not HTTP. Dashboard demo controls simulate Failure/Empty/Success. For a fresh empty-state test, clear app storage, log in offline, choose Empty, restore connectivity, and refresh. AI starts with 6/16 completed (38% rounded); displayed progress always matches lessons.

## 3. Security
Production: keep short-lived access tokens in memory; encrypt persisted refresh tokens using an Android Keystore-backed key in private storage. Use TLS, server authorization, token rotation/revocation, and redacted logs. Never store passwords. Backup is disabled. Mock login is not a production security boundary.

## 4. Scale
For one million users and hundreds of courses:

1. Paginated APIs and Room Paging to bound memory/loading.
2. Incremental refresh, ETags, cache freshness, and retry backoff.
3. Durable completion outbox with WorkManager, idempotency, and conflict resolution.
4. Account-scoped caches, database migrations, and real session expiry.
5. Crash/performance monitoring, backend caching, and rate limiting.

## 5. Second platform
SwiftUI, observable presentation models, async/await repositories, URLSession, and SwiftData/Core Data as the persistent source of truth. Store tokens in Keychain; use XCTest for completion and cache persistence.

**Test:** Repository test covers completion/progress, refresh preservation, API failure, and reopening a disk database. See [demo guide](docs/DEMO.md) for manual checks and the two-minute recording script. Limits: no backend, cloud sync, account switching, or deletion reconciliation. Debug APK is for evaluation.
