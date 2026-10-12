# Test report — Expiry Radar

Executed 12 October 2026. These are executed test results, not planned checks.

**51 tests passed; 0 failures; 0 errors.** JUnit, Robolectric and Compose UI simulation run on the host JVM using Android API 28; the notification permission-denial case uses API 35. These are not physical-device tests.

| Suite | Executed | Result |
| --- | ---: | --- |
| BarcodeOfflineTest | 1 | Passed |
| ReminderIntegrationTest | 5 | Passed |
| BackupIntegrationTest | 6 | Passed |
| UiFlowTest | 5 | Passed |
| DomainTest | 22 | Passed |
| LocalIntegrationTest | 12 | Passed |

## Verified behavior
- Expiry boundaries, warning windows, leap years, year rollover and local dates across time zones.
- OCR numeric-date parsing, month/year end dates, invalid dates, ambiguous alternatives and MFG/EXP labels on the same line.
- Offline EAN-13 decoding from a generated barcode image. This verifies decoding, not the live camera pipeline.
- Room creation, update, delete, multiple batches, unique labels, protection for assigned labels, deleted-label persistence and disk database reopening.
- Preferences persist; invalid/missing/out-of-range dates and invalid numeric/status inputs are rejected.
- ZIP export/restore with images/settings/history, duplicate reimports, tampering, path traversal, CSV round trips, corrupted CSV rejection and reset.
- Due notification worker posts once, links to the item, skips completed items, respects disabled notifications and runtime permission denial, and removes a visible notification on deletion.
- Compose UI: onboarding completion; home item link/counts; search; calendar date list/month navigation; five-tab navigation; explicit date selection and item saving through the form.
- 10,000-item Room insertion/query test and 20,000-date calculation test executed. These measure host simulation behavior, not low-end phone performance.

## Checks that need a device
- Physical APK installation, force-stop/reopen/reboot behavior and OEM battery restrictions.
- Live camera permission-denial UI, barcode camera lifecycle, capture cancellation and actual packaging OCR accuracy.
- Full airplane-mode OCR on a fresh installation, especially on a device without Google Play Services.
- Snooze/action delivery under device sleep, clock/timezone changes and system notification settings.
- Photo-picker/camera app compatibility, TalkBack, large fonts, real scrolling/memory/startup measurements and release R8 runtime smoke testing.
- No migration from an earlier production schema exists: this is schema version 1. Room schema export and reopening are tested; a future schema upgrade must supply explicit migration tests.

During the initial local build, the instrumentation APK compiled but no connected device was available. Subsequent GitHub Actions execution used real temporary Android 9 and Android 16 emulators: all 9 instrumented tests passed on each. Actual MainActivity onboarding, five-tab navigation, Add Item opening, screenshots and fatal-crash checks also passed on both. See EMULATOR_REPORT.md. No physical phone was used. Native OCR models are bundled and the manifest denies networking; camera/OCR accuracy and power-management checks still require a physical device.

Initial host JUnit XML, HTML reports, lint report and build log are under `verification/` in the delivered source archive. Fresh host/emulator HTML reports and native screenshots are attached to the GitHub preview release. Earlier failing runs found and resolved the month/year parser and dependency issues; final reports contain the executed passing run.
