# Executed Android preview verification

Executed 12 October 2026 using GitHub-hosted temporary emulators with 2 CPU cores and 2 GB RAM. No physical phone or paid device service was used.

Run: https://github.com/7899kk/kknews/actions/runs/38210437410
Build commit: c74dedb980125917a236ce5d5a5f7e36edebc50b
Release: https://github.com/7899kk/kknews/releases/tag/expiry-radar-preview-3

| Executed check | Result |
| --- | --- |
| Kotlin/Compose compilation; debug APK and instrumentation APK | Passed |
| JUnit/Robolectric host tests | 51 passed, 0 failed, 0 errors |
| Android 9 / API 28 instrumentation | 9 passed, 0 failed, 0 errors |
| Android 16 / API 36 instrumentation | 9 passed, 0 failed, 0 errors |
| Lint debug | Passed (warnings remain) |
| Installed MainActivity: onboarding, five destinations, Add Item | Passed on both emulators |
| Fatal-crash log during installed-app navigation | No fatal crashes on either emulator |
| APK download, archive integrity and SHA-256 match | Passed |

The 9 tests per emulator cover Room item create/edit/delete, notification-history deduplication, unique labels, insertion/query of 10,000 items, and Compose onboarding, dashboard item opening, search, calendar navigation, and the five-tab item-save/date-picker flow. These are 60 distinct automated cases, with the 9 device cases executed twice (69 total automated test executions).

The installed app was tested separately after instrumentation cleanup: fresh APK installation, first-launch onboarding, Home/Items/Calendar/Insights/Settings, and opening Add Item. Mobile data and Wi-Fi were disabled during those navigation checks. Fourteen actual PNG screenshots and corresponding UI XML captures (7 per emulator) are in the preview ZIP. Empty-state counts came from a fresh empty database.

## Download artifact

- APK: https://github.com/7899kk/kknews/releases/download/expiry-radar-preview-3/ExpiryRadar-debug.apk
- Size: 76,234,610 bytes (72.7 MiB).
- SHA-256: `67b97fbd252e6aa67d8eeb333d7ad290cf943a4215f3cb6a930b56a6820cbf55`.
- Preview/reports ZIP: https://github.com/7899kk/kknews/releases/download/expiry-radar-preview-3/ExpiryRadar-Android-preview.zip
- ZIP SHA-256: `e48bede2e1f2a07932afc1b2219523af14b3e5634d746dcaa54da30ac5dd4985`.

This is a debug-signed test build, not a Google Play production release. Debug keys generated in different environments can differ; an earlier differently signed APK cannot update in place. Export an existing installation's data before uninstalling it to change signing identities. No signing key or password is committed.

## Remaining physical-device checks

Live camera scanning/OCR accuracy, camera/photo-picker compatibility, fresh offline OCR model execution, reboot/snooze delivery and OEM battery restrictions, TalkBack/large fonts, physical-device performance, and production/R8/signing validation remain. Emulator navigation does not establish those results.

The first CI attempt failed on a removed legacy SDK `tools` package. That setup was fixed. A later attempt passed all Android 9 tests but failed while pulling screenshots after the instrumentation runner had removed app storage. Screenshot collection was replaced with installed-app UI checks and direct adb captures. The final linked run completed successfully.
