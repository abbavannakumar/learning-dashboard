# Validation

Verified locally on 7 October 2026.

- `gradlew.bat assembleDebug`: passed; generated an installable debug APK.
- Original `gradlew.bat testDebugUnitTest` run: 2 tests passed, 0 failures/errors. The separate progress test was subsequently removed; the repository persistence test is retained.
- `gradlew.bat lintDebug --rerun-tasks --offline`: passed, 0 errors. The 14 advisory warnings concern newer dependency versions and Room's support for KSP instead of kapt.
- Installed and ran on the available Android emulator (API 37).
- Demo login navigated to the three seeded courses.
- Completing a Python lesson updated progress from 65% to 70%.
- Refresh preserved the completion and 70% progress.
- Airplane mode with Wi-Fi disabled caused refresh to fail while saved courses remained visible.
- Force-stop/relaunch and mock login while offline retained the course and 70% progress in details.
- Reviewed screenshots and sampled video frames. Recording duration: 118.16 seconds; includes login, dashboard, details, completion, offline refresh, and offline restart.

APK, MP4, screenshots, and test/lint reports are generated in `artifacts/` and excluded from source control. Fresh-cache empty/failure, validation, and rotation checks are documented in `DEMO.md`; they are not claimed as emulator-verified here.

The mock API intentionally reads bundled JSON and simulates network unavailability. This submission does not claim to integrate a live HTTP backend.
