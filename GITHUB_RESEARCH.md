# GitHub research — Expiry Radar

Research performed 12 October 2026 using GitHub repository metadata, recursive trees, README/license files and the source files listed below. The requested Expiry Tracker URL returned HTTP 404. No code from it was reused. Metadata, inspected-file hashes and provenance are included in `research-evidence/`.

Maintenance dates below distinguish latest inspected-file commit from repository pushed-at. Pushed-at can reflect automated changes and is not proof of meaningful maintenance across the whole project.

## akshat-1903/Expiry-Tracker
- URL: https://github.com/akshat-1903/Expiry-Tracker
- Functionality inspected: Unavailable
- License: Unverified / unavailable
- Repository pushed-at: Unavailable; archived: unknown
- Latest commit on an inspected source path: Unavailable
- Commit: Unavailable
- Decision / actual integration: No reuse. GitHub API returned 404.
- Compatibility: Not assessable
- Attribution: No source redistributed from this repository; no derivative-code attribution obligations incurred.
- Files inspected: 

## BloodBlinker/WarrantyBoxx-
- URL: https://github.com/BloodBlinker/WarrantyBoxx-
- Functionality inspected: Flutter/Drift offline items and WorkManager reminders
- License: GPL-3.0
- Repository pushed-at: 2026-06-24T02:51:54Z; archived: False
- Latest commit on an inspected source path: 2026-06-10T05:49:19Z
- Commit: 834571073575504332cd3212ef224d73a0744ae6
- Decision / actual integration: Concepts only; original Kotlin implementation. No GPL code copied.
- Compatibility: Patterns reviewed; dependency versions are independently pinned in app/build.gradle.kts for API 28–36.
- Attribution: No source redistributed from this repository; no derivative-code attribution obligations incurred.
- Files inspected: `README.md`, `lib/domain/services/notification_service.dart`, `lib/data/database/database.dart`, `lib/data/database/tables.dart`, `pubspec.yaml`, `android/app/build.gradle.kts`, `LICENSE`

## android/architecture-samples
- URL: https://github.com/android/architecture-samples
- Functionality inspected: DefaultTaskRepository, Room ToDoDatabase, Compose theme, version catalog
- License: Apache-2.0
- Repository pushed-at: 2026-10-09T01:00:45Z; archived: False
- Latest commit on an inspected source path: 2024-12-10T14:05:55Z
- Commit: 7f157fcbb004600af13e2060c31f035aa7dbc55a
- Decision / actual integration: Repository + reactive state pattern reimplemented; no sample code copied.
- Compatibility: Patterns reviewed; dependency versions are independently pinned in app/build.gradle.kts for API 28–36.
- Attribution: Apache 2.0 license and existing notices retained for redistributed dependencies; no source copied.
- Files inspected: `README.md`, `gradle/libs.versions.toml`, `app/src/main/java/com/example/android/architecture/blueprints/todoapp/data/DefaultTaskRepository.kt`, `app/src/main/java/com/example/android/architecture/blueprints/todoapp/data/source/local/ToDoDatabase.kt`, `app/build.gradle.kts`, `app/src/main/java/com/example/android/architecture/blueprints/todoapp/TodoTheme.kt`, `LICENSE`

## android/nowinandroid
- URL: https://github.com/android/nowinandroid
- Functionality inspected: NiaDatabase, design-system Theme, worker delegation, version catalog
- License: Apache-2.0
- Repository pushed-at: 2026-10-12T01:46:54Z; archived: False
- Latest commit on an inspected source path: 2023-12-09T20:05:50Z
- Commit: f27a2e176e539c4c9fb00ec941bec1aef426137f
- Decision / actual integration: Theme/state/layering references only; single module avoids enterprise complexity.
- Compatibility: Patterns reviewed; dependency versions are independently pinned in app/build.gradle.kts for API 28–36.
- Attribution: Apache 2.0 license and existing notices retained for redistributed dependencies; no source copied.
- Files inspected: `core/database/src/main/kotlin/com/google/samples/apps/nowinandroid/core/database/NiaDatabase.kt`, `README.md`, `gradle/libs.versions.toml`, `sync/work/src/main/kotlin/com/google/samples/apps/nowinandroid/sync/workers/DelegatingWorker.kt`, `app-nia-catalog/build.gradle.kts`, `LICENSE`, `core/data-test/src/main/kotlin/com/google/samples/apps/nowinandroid/core/data/test/repository/FakeNewsRepository.kt`, `core/designsystem/src/main/kotlin/com/google/samples/apps/nowinandroid/core/designsystem/theme/Theme.kt`

## journeyapps/zxing-android-embedded
- URL: https://github.com/journeyapps/zxing-android-embedded
- Functionality inspected: ScanContract source and library API
- License: Apache-2.0
- Repository pushed-at: 2024-08-04T04:24:21Z; archived: False
- Latest commit on an inspected source path: 2021-10-25T12:57:43Z
- Commit: 0c97cf70cc9d562431e771fcc026c9c8bdf412ca
- Decision / actual integration: Actual Maven dependency 4.3.0; ScanContract + ScanOptions integrated in Editor.
- Compatibility: Patterns reviewed; dependency versions are independently pinned in app/build.gradle.kts for API 28–36.
- Attribution: Apache 2.0 license and existing notices retained for redistributed dependencies; no source copied.
- Files inspected: `zxing-android-embedded/src/com/journeyapps/barcodescanner/ScanContract.java`, `README.md`, `COPYING`, `zxing-android-embedded/build.gradle`

## googlesamples/mlkit
- URL: https://github.com/googlesamples/mlkit
- Functionality inspected: TextRecognitionProcessor Kotlin/Java and app build dependencies
- License: Apache-2.0
- Repository pushed-at: 2026-07-21T20:29:12Z; archived: False
- Latest commit on an inspected source path: 2022-07-29T19:24:53Z
- Commit: 1e9d8f1a991259e13acc6b9c00f8199820f6b44d
- Decision / actual integration: Bundled com.google.mlkit:text-recognition:16.0.1 integrated; original capture/parsing/confirmation code.
- Compatibility: Patterns reviewed; dependency versions are independently pinned in app/build.gradle.kts for API 28–36.
- Attribution: Apache 2.0 license and existing notices retained for redistributed dependencies; no source copied.
- Files inspected: `android/vision-quickstart/app/build.gradle`, `README.md`, `android/vision-quickstart/app/src/main/java/com/google/mlkit/vision/demo/kotlin/textdetector/TextRecognitionProcessor.kt`, `LICENSE`, `android/vision-quickstart/app/src/main/java/com/google/mlkit/vision/demo/java/textdetector/TextRecognitionProcessor.java`

## android/architecture-components-samples
- URL: https://github.com/android/architecture-components-samples
- Functionality inspected: Room UsersDatabase, repository, CoroutineWorker sample
- License: Apache-2.0
- Repository pushed-at: 2024-12-06T00:06:51Z; archived: True
- Latest commit on an inspected source path: 2023-01-09T16:36:49Z
- Commit: 2f7fd72eadf77bccb360e85d124f000d067e83af
- Decision / actual integration: Conceptual reference; archived repository; no code copied.
- Compatibility: Patterns reviewed; dependency versions are independently pinned in app/build.gradle.kts for API 28–36.
- Attribution: Apache 2.0 license and existing notices retained for redistributed dependencies; no source copied.
- Files inspected: `README.md`, `GithubBrowserSample/app/src/main/java/com/android/example/github/repository/RepoRepository.kt`, `WorkManagerMultiprocessSample/app/src/main/java/com/example/background/multiprocess/ExampleRemoteCoroutineWorker.kt`, `BasicRxJavaSampleKotlin/app/src/main/java/com/example/android/observability/persistence/UsersDatabase.kt`, `LICENSE`

## PhilJay/MPAndroidChart
- URL: https://github.com/PhilJay/MPAndroidChart
- Functionality inspected: PieChart.kt, BarChartRenderer.kt, version catalog
- License: Apache-2.0
- Repository pushed-at: 2026-10-09T22:20:14Z; archived: False
- Latest commit on an inspected source path: 2026-09-26T17:29:20Z
- Commit: 084c127592762023b362ea738c4f75f424188131
- Decision / actual integration: No dependency; Compose progress bars provide inexpensive accessible analytics.
- Compatibility: Patterns reviewed; dependency versions are independently pinned in app/build.gradle.kts for API 28–36.
- Attribution: Apache 2.0 license and existing notices retained for redistributed dependencies; no source copied.
- Files inspected: `README.md`, `gradle/libs.versions.toml`, `MPChartLib/src/main/kotlin/com/github/mikephil/charting/charts/PieChart.kt`, `MPChartCompose/build.gradle.kts`, `MPChartLib/src/main/kotlin/com/github/mikephil/charting/renderer/BarChartRenderer.kt`, `LICENSE`

## material-components/material-components-android
- URL: https://github.com/material-components/material-components-android
- Functionality inspected: MaterialDatePicker.java, README and version catalog
- License: Apache-2.0
- Repository pushed-at: 2026-09-14T20:08:09Z; archived: False
- Latest commit on an inspected source path: 2026-01-08T18:00:48Z
- Commit: 1fd35bc7e8426cd383e55e00102fc6aa9b4251a1
- Decision / actual integration: Views library in maintenance mode; use Compose Material 3 date picker, dialogs, forms.
- Compatibility: Patterns reviewed; dependency versions are independently pinned in app/build.gradle.kts for API 28–36.
- Attribution: Apache 2.0 license and existing notices retained for redistributed dependencies; no source copied.
- Files inspected: `README.md`, `gradle/libs.versions.toml`, `lib/java/com/google/android/material/datepicker/MaterialDatePicker.java`, `LICENSE`

## androidx/androidx
- URL: https://github.com/androidx/androidx
- Functionality inspected: Material3 DatePicker.kt, SQLite transaction, worker and state references
- License: Apache-2.0
- Repository pushed-at: 2026-10-12T00:42:31Z; archived: False
- Latest commit on an inspected source path: 2026-09-09T19:23:44Z
- Commit: 8f7f44ef20e5970e19451a347f58465bad485c23
- Decision / actual integration: Stable Maven Room, WorkManager, Compose, DataStore, Lifecycle dependencies; no framework source copied.
- Compatibility: Patterns reviewed; dependency versions are independently pinned in app/build.gradle.kts for API 28–36.
- Attribution: Apache 2.0 license and existing notices retained for redistributed dependencies; no source copied.
- Files inspected: `LICENSE.txt`, `README.md`, `compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/DatePicker.kt`, `core/core/src/main/java/androidx/core/database/sqlite/SQLiteDatabase.kt`, `development/ab-benchmarking/app/build.gradle.kts`, `biometric/biometric/src/main/java/androidx/biometric/internal/data/AuthenticationStateRepository.kt`, `camera/integration-tests/featurecombotestapp/src/main/java/androidx/camera/integration/featurecombo/ui/theme/Theme.kt`, `car/app/app-samples/gradle/libs.versions.toml`, `benchmark/integration-tests/macrobenchmark-target/src/main/java/androidx/benchmark/integration/macrobenchmark/target/NoOpWorker.kt`

## Implementation decisions
- MVVM uses lifecycle-aware StateFlow and a lightweight Application dependency container. Room is the local source of truth; no network repository exists.
- Calendar dates use LocalDate, never millisecond arithmetic. UTC conversion is used only at the Material date-picker boundary.
- Periodic WorkManager reconciliation reads current local dates and database records. Unique history keys prevent duplicate notifications. WorkManager retains periodic jobs across reboot; no exact alarm or always-running service.
- Barcode recognition stores an identifier; no expiry date or remote product lookup is invented.
- OCR uses the bundled Latin model. It returns labelled/ambiguous candidate dates and requires selection; MFG candidates are clearly labelled.
- ML Kit sample source is Apache 2.0, but the distributed ML Kit SDK is governed by Google SDK terms, not that sample license. Its transitive Firebase-component/encoder utilities do not represent a configured Firebase service; no Firebase backend/config/account/database exists.
- Dependency version numbers were inspected in source catalogs; pinned stable compatible versions are deliberately older than some repositories’ current experimental dependencies.
- Official API requirement verified: https://developer.android.com/google/play/requirements/target-sdk (new phone apps API 36 from August 31, 2026).
- Bundled OCR API verified: https://developers.google.com/ml-kit/vision/text-recognition/v2/android.
