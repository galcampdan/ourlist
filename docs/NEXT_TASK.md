# Next task

Continue the authorized OurList 1.0.8(9) publication; do not request publication permission again.

Source/admin/security/APK/UI checks are complete. Read PROJECT_STATE and research/2026-11-06-ourlist-fixes/STATUS.md. Commit on codex/ branch, archive any newly rebuilt APK immediately using scripts/build_and_archive.ps1 and G:\내 드라이브\03_앱개발\ourlist. Publish matching v1.0.8 release, verify public APK hash, conditionally update Firebase app_version with hash/size and read back. Verify 1.0.7 in-app download/install using only disposable emulator fixture data.

Firebase CLI admin OAuth is connected in temporary CreatorTemp/OurList-firebase-admin install; never print tokens/keystore settings. Rules already deployed; both phones must update and pair once. Do not roll back to public rules. Real Google Drive OAuth/automatic backup remains untested and must be stated accurately.
