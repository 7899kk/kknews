# Expiry Radar

**Track it today. Save it before it expires.**

Original native Android application, written in Kotlin and Jetpack Compose. It runs on Android 9+ and stores data locally in Room. It has no login, ads, subscription, backend or internet permission.

## Open and build
1. Unzip the source archive and open the `ExpiryRadar` directory in Android Studio.
2. Use JDK 17 and install SDK platform 36. Android Studio will download the pinned Gradle and Maven dependencies during development; installed-app use is offline.
3. Let Gradle sync finish. Run on an Android 9+ device.
4. Terminal equivalents: `./gradlew assembleDebug`, `./gradlew testDebugUnitTest`, `./gradlew lintDebug`, `./gradlew bundleRelease`.
5. Debug APK output: `app/build/outputs/apk/debug/app-debug.apk`. Release bundle output: `app/build/outputs/bundle/release/app-release.aab`. A bundle must be signed with your own private upload key before Play submission.

The archive omits machine-specific SDK paths, signing keys, generated caches and build intermediates. Source, the Gradle wrapper, Room schema, original icons, store assets, tests, reports and research provenance are included. See BUILD_REPORT.md and TEST_REPORT.md for what actually executed in this environment.

## Implemented behavior
- Three-page onboarding; no account creation.
- Five destinations: Home, Items, Calendar, Insights, Settings.
- Real dashboard counts, local time greeting and nearest expiries.
- Item creation/editing, images, notes, batches, categories, profiles, price, quantities, barcodes, dates and reminder offsets.
- New records require explicit expiry-date selection. Past/inconsistent dates require confirmation. Empty names, invalid dates/numbers and invalid reminder offsets are rejected.
- LocalDate-based safe/soon/today/expired statuses; global and item warning windows.
- Search, category/status/profile/location filters, five sorts, detail view, duplicate, archive/restore, used/discarded and confirmed deletion.
- Monthly expiry grid, date selection, month navigation, date jump, category/profile filters.
- Database-derived category/month bars, completed-item counts, discarded-price estimate and usage history.
- WorkManager periodic date reconciliation, persistent jobs, local notification channel, item links, used/discarded/snooze actions, history deduplication and cancellation. No exact-alarm guarantee.
- ZXing offline barcode scanner. A result requires confirmation and may reuse locally stored product details; it never invents an expiry.
- Bundled Latin-script OCR from a camera photo. Recognizes numeric date formats and labels MFG/EXP/Best Before/Use By. Ambiguous day/month alternatives are shown. Selection fills the form; saving remains a separate action.
- Selected product photos are copied, downsampled, oriented and JPEG-compressed into private storage. No broad-storage permission.
- Full versioned ZIP backups with SHA-256 integrity manifest, images, labels, settings and history; validated merge restore skips existing item IDs. CSV export/import preserves record fields and omits images. Both use the system file picker.
- Theme/date/week/reminder preferences in DataStore, local category/profile management and confirmed reset.

## Architecture
`UI → RadarViewModel → Repository → Room` with StateFlow and lifecycle-aware collection. `RadarApplication` is a small dependency container. Background workers use the same database and expiry policy. The backup manager validates all incoming data before a Room transaction. Standard AndroidX scheduling survives reboot without a custom always-running service.

Directories: `data/` for entities, DAO, database, repository and preferences; `domain/` for expiry/OCR/reminder logic; `ui/` for separate Compose screens/components; `workers/` for notifications and reconciliation; `backup/` for ZIP/CSV; `src/test/` for JUnit/Robolectric; `src/androidTest/` for device persistence tests.

## Permissions and offline use
Camera and notification permissions are optional. The photo picker falls back to a system document picker on Android versions without a photo-picker implementation. Denied permissions retain manual entry. The merged manifest should contain no internet, network-state, broad-storage, account, location or SMS permission. WorkManager supplies its scheduling/wake/reboot permissions.

ML Kit is an on-device SDK, not a free AI API call. Its bundled model adds native libraries and increases package size. It transitively includes Google runtime libraries with Firebase-related names, but there is no Firebase project/configuration, service, database or cloud connection. The manifest removes networking permissions. See PRIVACY_POLICY.md and GITHUB_RESEARCH.md.

## Backup format v1
ZIP entries: `data.json`, `integrity.json`, `images/<local-name>.jpg`. SHA-256 values cover every data/image entry. Unknown versions/entries, duplicate entries/IDs, path traversal, unsupported states, invalid dates/settings and oversized input are rejected. Limits: 20,000 imported items, 32 MiB per entry, 256 MiB decompressed ZIP, 16 MiB CSV. Reimporting an item ID skips it instead of overwriting an existing record. CSV contains the serialized item field names as readable columns; editing the ID creates a new record. Backups are not encrypted. Exported external files are not deleted by Reset All Data.

## Limits and release status
Read the build/test reports before distributing. A successful build is not proof of physical-device reliability. Camera/OCR accuracy, notification timing, OEM battery restrictions, TalkBack, 200% text scaling and release-install behavior require device QA. WorkManager reminders check due calendar days; a device that remains powered off/force-stopped for the entire due date may miss that reminder. On reopening, the app dashboard still shows current status. Reminders are organizational aids, never medical or food-safety advice.

Version 1 has no prior production schema to migrate. Its Room schema is exported; future versions must add explicit migrations and tests rather than destructive fallback. Static UI copy is in English string resources; some composed status/validation text remains English and must be localized along with categories when adding Hindi/Telugu. English is the only supported language in this build.

Your GitHub account is connected, but no Expiry Radar repository was found and no destination repository was specified, so nothing was pushed to your account. The included GitHub Actions workflow is ready to run after you upload the project. Signing, real Play screenshots and Console submission remain publisher tasks. See PLAY_RELEASE.md.
