# Manual checks and recording guide

## Before recording
- Start an Android emulator/device with internet available.
- Install the debug APK: `adb install -r app/build/outputs/apk/debug/app-debug.apk`.
- Test blank/invalid email, blank password, and incorrect credentials. Confirm inline errors, loading, and retry.
- Log in with `demo@example.com / password123`; verify three courses and lesson counts 20/16/28.
- Open Python, complete Functions (lesson 14), return, and verify 65% → 70%.
- Refresh and verify 70% remains. Choose Failure and refresh: saved cards remain with a warning.
- Enable airplane mode and disable Wi-Fi. Refresh: saved cards remain.
- Force-stop and reopen; log in offline. Open details and confirm completed Functions persists.
- Complete another lesson offline, restart, and confirm it persists.
- Test fresh-cache failure: clear app storage (`adb shell pm clear com.example.learning`), keep internet disabled, and log in. Expect error and Refresh; restore internet and retry.
- Test fresh-cache empty: with internet disabled, log in after clearing storage; choose Empty; restore internet and Refresh. Expect No courses yet. Choose Success and Refresh to recover.
- Rotate the device on login, dashboard, and details. Ensure progress and loading state remain sensible.

## Two-minute recording
| Time | Show |
| --- | --- |
| 0:00–0:20 | App launch, demo credentials, login loading |
| 0:20–0:35 | Three course cards and progress |
| 0:35–1:00 | Python details, mark lesson complete, return to updated dashboard |
| 1:00–1:15 | Refresh and show completion remains |
| 1:15–1:35 | Airplane mode / Wi-Fi off, Refresh, cached-data warning |
| 1:35–2:00 | Force-stop, reopen offline, login, show saved lesson completion |

Record via emulator Extended Controls → Record and Playback, or use `adb shell screenrecord --time-limit 120 /sdcard/learning-demo.mp4`, then `adb pull /sdcard/learning-demo.mp4`.

## Submission checklist
- GitHub/GitLab source repository, including Gradle wrapper and Room schema.
- Debug APK produced by a successful build.
- Recording no longer than two minutes.
- One-page README and passing test results.
