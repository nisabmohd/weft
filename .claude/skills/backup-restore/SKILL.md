---
name: backup-restore
description: JSON export/import (habits, entries, notes and preferences) and CSV export through the system file picker. Use when changing the backup format, import or export.
---

# Backup and restore

**Code:** `data/backup/Backup.kt` — `BackupFile` (versioned DTOs, separate from Room entities; `settings` is optional so older files still import), `BackupManager.parse` (validation, clear errors), `restore` (one transaction, then preferences), `csv`. UI: `ui/more/BackupSection.kt` (`CreateDocument` / `OpenDocument`, no storage permission). Unit tests: `BackupFormatTest`.

**Verify:** More → Backups → Export to file (foss: Export now) → JSON → Save; read it with `adb exec-out cat /sdcard/Download/<file>`; change data or settings; Backups → Import from file → Replace; confirm everything is back. A non-backup JSON must show "This isn't a Weft backup file." and change nothing.

**Pitfalls:** bump `FORMAT_VERSION` only for incompatible changes; add new fields with defaults. Check under R8 with a release build (kotlinx.serialization).

## Google Drive (play and github, 1.5.0)
Everything is in `docs/DRIVE-BACKUP.md`: the UI (`ui/more/BackupRestoreScreen.kt`, `DriveSection.kt`; file flows in `BackupSection.kt`), the data format (gzipped JSON in `drive.appdata`, newest 10 kept), the JobScheduler schedule (`DriveBackupJob`, `domain/BackupSchedule.kt`), every edge case handled, and how to test. Code in `src/drive`, stand-in in `src/noDrive` (foss). Debug builds fake the connected state with `DriveStateActivity`. Smoke: `python3 tools/verify/smoke.py drive`.
