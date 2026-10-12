# Build report — Expiry Radar

Build date: 12 October 2026.

## Executed build
`gradle :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:bundleRelease :app:assembleDebugAndroidTest --no-daemon --max-workers=1 --console=plain`

Final Gradle result: **BUILD SUCCESSFUL**, exit code 0, 133 tasks (31 executed, 102 up-to-date). Release minification/resource shrinking completed with R8.

## Environment
- Linux host; JDK 17.0.20.1; Gradle 8.13; Android Gradle Plugin 8.10.1; Kotlin 2.1.20.
- Installed SDK platform 36; min SDK 28; target SDK 36. Build Tools 35.0.0 selected by AGP; 36.0.0 used for package checks.
- Room 2.7.1, Compose BOM 2025.04.01, DataStore 1.1.7, WorkManager 2.10.1, ZXing Embedded 4.3.0 and bundled ML Kit text recognition 16.0.1.

## Produced artifacts
- `ExpiryRadar-debug.apk`: 77,165,763 bytes (73.6 MiB). SHA-256: `7eca3f43eed10b4060f26bff4e232b9344269bc6af1257982620dc5a18221913`.
- `ExpiryRadar-release-unsigned.aab`: 23,741,136 bytes (22.6 MiB). SHA-256: `f6acc5ef09d3455efcff06f35d66342ab7fdb7415d52dba8f9478b30d157e941`.
- A device instrumentation APK was also compiled. It is included in build outputs rather than presented as a user-installable app.

## Package checks
- Android lint: **0 errors, 13 warnings**. Remaining warnings concern newer dependency versions or optional KTX style suggestions; pinned stable versions are deliberately retained for compatibility.
- Debug APK signature verification passed (APK signing v2, one debug signer). The debug key is not included in the source archive.
- Debug APK ZIP alignment passed, including 16 KiB native-library ZIP alignment.
- readelf checks: all bundled arm64-v8a and x86_64 native libraries have LOAD segment alignment of at least 16 KiB. The 32-bit ML Kit libraries use 4 KiB alignment and are retained for older 32-bit phones; no 16 KiB runtime device was tested.
- Permissions inspected from the compiled APK: camera, notifications, WorkManager wake/reboot and an AndroidX signature-scoped internal receiver permission. No internet, network-state, foreground-service, broad storage, location, SMS or contacts permission is present.
- Android cloud-backup and device-transfer exclusion rules are packaged.
- Bundled OCR model assets and all four ABI native pipelines are in the package.

## Resolved build and quality issues
- Provisioned missing SDK, JDK/compiler and Gradle; corrected the build proxy configuration.
- Replaced DataStore 1.1.5 after verifying its documented broken metadata with the corrected 1.1.7 release.
- Resolved Kotlin serialization/resource import conflicts; corrected month/year date recognition.
- New records start without an expiry date and require explicit selection; date ranges match the picker.
- MFG and EXP tags are matched to their individual dates, including when sharing a line.
- Removed transitive internet/network/foreground-service permissions, added Android backup exclusions and guarded clock receiver actions.
- Reduced unnecessary icon/tooling dependencies, preserved photo orientation, and made policy/license content scrollable.
- Deleted categories stay deleted after reseeding. Backup export rejects data beyond the documented restore bounds before writing its payload.

## Release status
The unsigned AAB exists and compiled successfully. It is **not upload-ready** until signed with your own private upload key and validated through device/Play testing. No production signing key/password was generated or committed. No Play submission occurred. A debug-installed app cannot be updated by a differently signed production build: export a backup before switching installations.

After authorization to choose any repository, the complete source was committed to the empty public `7899kk/kknews` repository. The subsequent successful GitHub Actions build published a tested debug APK; see EMULATOR_REPORT.md for the new artifact hash and emulator evidence. Artifact sizes and hashes above describe the initial local build.

Physical-device testing, real Play screenshots, a public privacy-policy URL, actual support contact and Console declarations remain publisher tasks. See PLAY_RELEASE.md and TEST_REPORT.md.
