# Expiry Radar privacy policy
Effective date: 12 October 2026

Expiry Radar is a local item and expiry-date organizer. It needs no account, subscription, internet access, analytics service or advertising SDK. The release manifest explicitly removes internet access and network-state permissions contributed by dependencies.

## Local information
Item names, dates, notes, quantities, prices, categories, household profiles, barcodes, reminder history and compressed product images are stored in the app's private device storage. They are not uploaded by Expiry Radar. Android's cloud backup is disabled for this app. Uninstalling or clearing app storage deletes the data; export a backup beforehand.

## Camera and photos
Optional camera permission supports product-barcode scanning and photographing packaging for date recognition. ZXing decodes barcodes locally. ML Kit's bundled Latin-script recognition model processes packaging on device. OCR candidates require user selection; no remote product database or AI API is queried. A packaging capture is temporarily retained in private cache and replaced by the next capture. Selected product photos are copied and compressed locally. The Android photo picker may fall back to the system document picker on older devices. No broad storage permission is used.

ML Kit brings Google runtime component and encoding libraries transitively. No Firebase project, cloud backend, Firebase configuration, remote model or account is used. Network permission is removed. Google's ML Kit SDK terms still apply; the samples' Apache license does not cover the proprietary SDK binary.

## Notifications
Reminders use local WorkManager jobs and Android notifications. Permission is optional and may be revoked. Reminders may be delayed or missed by Android battery restrictions, force stop, clock changes or device shutdown. Reminder history is stored locally. Do not rely on the app for medical or safety-critical decisions.

## Backup and restore
Backups are exported only when requested through Android's file picker. They are **unencrypted**. They may contain personal information or document/medicine names. Keep them in a trusted location. A destination you select may belong to a cloud-storage provider, whose privacy policy applies to that destination. Full ZIP backups include images; CSV does not. ZIP checksums detect corruption; they are not cryptographic authentication or encryption.

## Deletion
Delete individual records in the app or use Settings → Reset All Data. External backups remain wherever you exported them; remove those separately. There is no server-held copy for us to delete.

## Publishing contact
Before a public release, the publisher must add their actual support email to the store listing and provide a publicly accessible URL for this policy. No contact address is invented here.
