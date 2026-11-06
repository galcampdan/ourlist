# Next task — resolve Play Protect before promoting 1.0.8

Read PROJECT_STATE.md, then research/2026-11-06-ourlist-fixes/PLAY_PROTECT_REVIEW.md and STATUS.md. Source and APK fixes are complete and archived; deployment installation validation is blocked by Play Protect generic_malware classification, not by an identified source defect. Do not call this a confirmed false positive or bypass the warning.

User approved holding the release: app_version is restored to 1.0.7(8), GitHub v1.0.8 is a prerelease. Secure server rules remain deployed and must not be reset to public rules. Existing local lists remain; old legacy sync is denied. Both phones will need the eventual approved update and one-time new pairing.

Next: with explicit authorization, upload the exact public APK to VirusTotal and submit the prepared Google classification review. Browser automation is still blocked by locked BitLocker E:; user must unlock or submit through the official form. Do not send review communications without explicit permission. Current review material: PLAY_PROTECT_REVIEW.md; SHA-256 25F30BD60C3129FAFF2940D6B92AA53E5A277051644EEB0FF03DCAE72E50D74A.

After external resolution, verify old-app download/install again, validate preserved fixture data, then promote prerelease and conditionally republish matching Firebase notice. No extra publication confirmation needed beyond the existing task authorization once the blocker is resolved. Any rebuilt APK must immediately be archived by scripts/build_and_archive.ps1 to G:\내 드라이브\03_앱개발\ourlist, with original signing preserved.

Cleanup is complete: room 944157, requests and both temporary accounts deleted/readback verified after separate identification-query authorization. Do not repeat account enumeration.

Latest user clarification: app is personal use for two people; user felt the task had grown too much. Keep work focused on the two-phone APK and essential data protection. No VirusTotal upload or Google review submission is authorized or required by default. User says E: unlocked; no further browser testing performed. Approved app-version rollback/verification hold remains in force; do not infer that the personal-use clarification revoked it.
