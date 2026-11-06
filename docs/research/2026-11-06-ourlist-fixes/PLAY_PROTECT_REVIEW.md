# Play Protect 검토 요청 준비 — OurList 1.0.8 (9)

## 확인된 상태

- API 36 Google Play 에뮬레이터에서 1.0.7의 앱 내 다운로드로 받은 공식 GitHub APK 설치를 시도했다.
- 다운로드/Android 출처 허용/업데이트 설치 화면까지 성공했다. Play Protect의 권장 검사를 실행한 뒤 `Harmful app blocked`가 표시됐다. 설치된 버전은 1.0.7(8) 그대로다.
- Play Protect 로그: JIT scan HTTP 200, `POTENTIALLY_UNWANTED`, `generic_malware`, 설명 `This app may be harmful.` 구체적인 행위·원인은 UNKNOWN.
- 설치 차단을 우회하지 않았다. 해시·서명·테스트 통과는 Play Protect 해제나 APK 무해성의 독립 보증이 아니다.
- 자체 소스 검사에서 SMS/연락처/접근성/알림 읽기 권한, 외부 DEX 로더, 셸 실행은 발견하지 못했다. 병합 manifest는 인터넷·네트워크 상태·알림 발송·부팅·앱 설치 요청 및 WorkManager의 foreground/wake-lock 권한을 포함한다.
- 저장소는 Google/Maven Central 의존성을 사용한다. APK의 표준 헤더와 서명은 유효하다. 자동 백업/Google 로그인 실제 계정 흐름 및 모든 SDK의 독립 악성코드 판정은 미검증이다.

## 제출 정보

- 공식 양식: https://support.google.com/googleplay/android-developer/contact/protectappeals
- Google 안내: https://developers.google.com/android/play-protect/warning-dev-guidance
- Package: com.ourlist.app
- Version: 1.0.8 / 9
- APK: https://github.com/galcampdan/ourlist/releases/download/v1.0.8/OurList-v1.0.8-release.apk
- APK SHA-256: 25F30BD60C3129FAFF2940D6B92AA53E5A277051644EEB0FF03DCAE72E50D74A
- Signing certificate SHA-256: c4c604557e04357c719371d51f8a92572032ec5249e4b6f52d377f88bbf69ebb
- Size: 14,361,024 bytes
- Developer/contact email: 사용자 지정 필요. 자격 증명·keystore는 제출하지 않는다.
- 양식은 VirusTotal에 업로드한 APK의 SHA-256을 요구한다. 해당 서비스 업로드와 Google 재검토 제출은 아직 하지 않았다.

## Additional information 초안

Please review the Google Play Protect classification of OurList 1.0.8 (versionCode 9), package com.ourlist.app. In an Android API 36 Google Play emulator, updating from the existing signed 1.0.7 build through our user-initiated in-app updater prompted a recommended scan. The completed JIT scan returned POTENTIALLY_UNWANTED with threatType generic_malware and the generic message “This app may be harmful.” We did not override the warning or disable Play Protect.

OurList is a two-person shared wish-list application. Shared/heart lists synchronize through Firebase with authenticated two-device membership and owner approval. Personal/secret lists are local and the user can explicitly create encrypted backups to a chosen file or their connected Google Drive appData storage. The app requests package-install access only for a user-initiated update, presents Android's installer, and does not silently install apps. The current updater verifies the expected APK size, SHA-256, package, version and existing signing certificate.

Our source/merged-manifest review found no SMS/contact-reading permissions, Accessibility service, notification-listener service, external DEX loading or shell execution. The app uses AndroidX, Kotlin coroutines/serialization, OkHttp and official Google sign-in libraries from Google/Maven Central repositories. Standard APK headers and the existing signing certificate verify. The APK, source tag and exact hashes are provided above for review. We have not identified the specific behavior responsible for the classification and do not claim the classification is a confirmed false positive. Please review this exact artifact under your Play Protect policies.

## 증거

play-protect-verdict.log, evidence/update-play-protect-result.*, evidence/update-play-protect-details.*, public-release-verification.json.


## 접수 진행 상태 — 사용자 명시 승인 이후

- 사용자가 정확한 APK의 VirusTotal 보안 커뮤니티 공유·약관 동의와 현재 로그인된 Google 계정으로 재검토 제출을 허용했다.
- Google 영문 양식에 패키지/정확한 SHA-256/929자 설명을 작성했다. 최종 Submit은 누르지 않았다.
- 사용자가 기존 Drive APK를 선택했다. VirusTotal의 3MB/복수 파일 archive 경고에는 업로드는 가능하다고 명시되어 있다. 원본 APK 그대로 Confirm upload를 눌렀으나 빈 reCAPTCHA 창과 연결 오류로 멈췄다. 업로드나 검사 완료는 확인되지 않았다.
- 다음 작업은 보안문자 연결을 정상적으로 해결하고 동일 해시의 VT 결과 페이지를 확인한 후 Google 양식을 제출하는 것이다. 공식 크기 설명: https://docs.virustotal.com/docs/compressed-files .
