# LensCull Implementation Plan

> Implementation status is tracked in Git history and CI.

**Goal:** Build an offline-first Android tablet photo-culling application with full shared-storage scanning, RAW previews, ratings, filtering, zoom and EXIF display.

**Architecture:** Compose UI over a Room/Paging catalog. A foreground WorkManager scan indexes supported files; focused metadata and preview services isolate EXIF/XMP and embedded RAW previews.

**Tech Stack:** Kotlin, AGP 9.3.2, Gradle 9.5, Compose, Room, Paging, WorkManager, AndroidX ExifInterface, Coil 3 and ZoomImage.

## Delivery checkpoints

- [x] Install Android CLI, JDK 17 and SDK toolchain.
- [x] Scaffold the API 33+ Compose project and initialize Git.
- [x] Add domain model, Room catalog, safe query builder and unit tests.
- [x] Add storage scanning, EXIF parsing, RAW preview extraction and XMP rating persistence.
- [x] Add adaptive tablet UI, filters, zoom preview, EXIF overlay and backup/restore.
- [x] Pass unit tests, lint, debug build and API 33/API 36 device tests.
- [ ] Create and push private GitHub repository.
