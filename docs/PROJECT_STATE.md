# Project state

- CONFIRMED (2026-11-06): source version 1.0.8 / versionCode 9. Audit fixes include authenticated owner-approved pairing, sync merge/transaction/error fixes, private backup/export controls, draft preservation, accessible large-font layout and APK integrity/signature checks.
- CONFIRMED: Firebase Anonymous Auth enabled and member-only rules deployed to existing ourlist-4462e project. Legacy /rooms is denied without deleting data; app_version remains public-read/admin-write. Both phones must update and pair once with a new code; local lists are retained.
- CONFIRMED: 59 Debug + 59 Release JVM tests pass; Release lint Error/Fatal 0. 33 security probes pass on official emulators and 33 on an isolated live fixture. Live fixture and its three temporary accounts were removed.
- CONFIRMED: API 36 emulator verified 1.0.7→1.0.8 data/migration, draft rotation, cost overflow, checkbox state, 200% font controls, owner approval, cross-app item creation and SHARED→HEART movement. Manual encrypted file save/open and recovery-code restoration of one personal item succeeded.
- CONFIRMED: newly generated Debug/Release APKs archived with byte/SHA-256 verification to user-confirmed G:\내 드라이브\03_앱개발\ourlist. Previous archives retained. Existing Release signer unchanged. Cloud sync remains UNKNOWN.
- PENDING: matching source commit/tag, GitHub v1.0.8 publication, Firebase update notice and old-app download/install verification.
- UNKNOWN: real Google-account OAuth / Drive appData backup end-to-end, physical phone behavior and long-duration simultaneous offline edits. Network contracts and actual reducer/crypto regressions pass; do not claim these untested external flows were executed.
- Details and evidence: docs/research/2026-11-06-ourlist-fixes/STATUS.md; APK receipts: reports/releases/.
