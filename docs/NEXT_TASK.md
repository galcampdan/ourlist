# Next task — personal two-phone use of published OurList 1.0.8

Read PROJECT_STATE.md. User explicitly requested making 1.0.8 available now despite the known Play Protect warning. This supersedes the earlier hold. Firebase app_version is 1.0.8(9), forceUpdate=false, with exact hash/size and disclosed installation limitation; public readback passed. GitHub v1.0.8 is a normal latest release. Do not roll back to 1.0.7 based on obsolete hold instructions.

The signed APK is unchanged: SHA-256 25F30BD60C3129FAFF2940D6B92AA53E5A277051644EEB0FF03DCAE72E50D74A, 14,361,024 bytes. Public download reverified. User can reopen app and check updates to download it. Latest normal installer test (17:38–17:39 KST) still showed Play Protect blocking, so do not claim installation or warning removal succeeded. VirusTotal 0/67 and Google appeal submission are recorded; no duplicate submissions or automatic monitoring.

Keep this personal two-user workflow minimal. After both phones install the update, pair once using a new code and confirmation number. Existing local lists remain; secure Firebase rules stay deployed and old unauthenticated legacy sync remains denied. No new build is needed. Future builds must preserve Release signing and immediately archive every new APK to G:\내 드라이브\03_앱개발\ourlist with size/hash verification.

Republication evidence: research/2026-11-06-ourlist-fixes/app-version-republished.json. All trial accounts/rooms are cleaned up. Other external integrations and physical-device flows remain unverified as listed in PROJECT_STATE.
