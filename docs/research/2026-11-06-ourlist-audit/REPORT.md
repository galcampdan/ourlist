# OurList 적대적 검토 및 UI/UX 검사 — 2026-11-06

## 판정과 범위

두 사람이 공동 버킷리스트와 하트 목록을 공유하고, 개인·비밀 목록은 로컬에서 관리하는 Android 앱이다. 두 기기가 오프라인 편집 후 다시 동기화하는 정상적인 사용 규모를 기준으로 검사했다.

**판정: 동기화·백업 신뢰성과 입력 보존을 먼저 수정해야 한다.** 아래 18건은 취향상의 디자인 변경 제안이 아니라 구체적인 실패 조건이나 조작상의 불편이 있는 항목이다.

- 소스: `C:/Users/atlan/Documents/Projects/OurList`, HEAD `7abc4e9f2e43aa4bb8cde9a42a69c8fba3c80b30`.
- 검사 전부터 `AppUpdateManager.kt`에 미커밋 수정이 있었으며 보존했다. 이 검사는 제품 코드를 수정하지 않았다.
- 실행: 기존 `OurList-v1.0.7-release.apk`, package `com.ourlist.app`, versionName `1.0.7`, versionCode `8`, 14,295,488 bytes.
- APK SHA-256: `84FE854BBC211B5F2EABA747F2A008EDC8919CC5D9F15330BD934DDA634B6957`.
- Drive APK: https://drive.google.com/file/d/1BWBxUorM-YB595YJbsgXv7gnzgRsodqc/view . 로컬 실행 APK와 이름·크기는 일치한다. 커넥터가 원격 체크섬을 반환하지 않아 원격 바이트 동일성은 UNKNOWN이다.
- 격리된 API 36 에뮬레이터, 1080×2400 / 420dpi, 세로·가로, font scale 1.0 / 1.5 / 2.0, 작은 화면 960×1800 확인. 실행 중 Wi-Fi와 모바일 데이터를 꺼 실제 공유 데이터 쓰기를 방지했다.
- 기존 단위 테스트 30개와 추가 검사 7개, 총 37개 통과. 추가 검사는 결함이 존재하는 현재 동작을 단언하는 재현 검사이며 정상 동작 보증 테스트가 아니다.
- CONFIRMED: 실행·모의 응답·직접 읽은 코드로 확인한 구현. LIKELY: 그 구현에서 예상되는 실제 서버/두 기기 결과. UNKNOWN: 실제 서버 권한, 실제 계정 동기화 등 미검증 영역.

## 먼저 수정할 사항

### 1. [P1] 이미 연결된 방에 제3기기 가입을 시도한다

- CONFIRMED / 실제 클라이언트 + 모의 응답: `CoupleSyncClient.joinRoom()`은 deviceA만 자기 기기인지 확인한다. deviceB가 이미 존재해도 새 deviceB로 PATCH한다. 요청에는 인증된 사용자 정보가 없다.
- 재현: A/B가 연결된 메타데이터를 반환한 뒤 C로 가입하면 C를 deviceB로 보내고 공유 상태를 읽는다. 추가 검사 `thirdDeviceCanReplaceOccupiedPartnerInClient`로 확인했다.
- 위치: `CoupleSyncClient.kt:136–165`, `SyncEngine.kt:94–100`.
- 영향: 서버가 해당 쓰기를 허용한다면 기존 파트너 교체와 공유 데이터 노출이 가능하다. **실제 Firebase 규칙과 실서버 침입 가능성은 UNKNOWN**이다.
- 최소 수정: 서버에서 인증된 두 구성원을 검증하고 가입을 원자적으로 처리한다. 클라이언트도 만석 가입을 거부하며 현재 기기의 구성원 여부를 검사한다. 연결 해제 시 서버 권한도 철회해야 한다. 현재 해제는 `CoupleConnectScreen.kt:1066–1069`에서 로컬 코드만 비운다.

### 2. [P1] 6자리 코드 충돌 시 기존 방을 덮어쓴다

- CONFIRMED / 코드 + 모의 응답: 화면에서 난수 하나를 뽑고 기존 방 확인 없이 `createRoom()`을 호출한다. 클라이언트는 meta와 state에 조건 없는 PUT을 보낸다.
- 위치: `CoupleConnectScreen.kt:737–750`, `CoupleSyncClient.kt:103–130`.
- 영향: 같은 코드가 선택되고 서버가 허용하면 기존 커플의 메타·현재 스냅샷이 교체된다. 모의 요청에서 사전 GET/조건부 쓰기가 없는 것을 확인했다. 실서버 재현은 하지 않았다.
- 최소 수정: 서버의 조건부 생성/트랜잭션으로 코드 점유를 보장하고 충돌 때 새 코드를 발급한다. 존재 확인 후 PUT만 추가하면 경쟁 상태가 남는다.

### 3. [P1] 공동 ↔ 하트 이동 항목이 상대 기기에서 숨겨진다

- CONFIRMED / 실제 SyncEngine 분기 재현: 이동은 같은 itemId에 DELETE와 CREATE를 만든다. 수신 CREATE는 같은 ID가 이미 있으면 휴지통 항목이어도 무시한다.
- 재현: SHARED의 ID X가 이동 DELETE로 휴지통에 남은 상태에서 HEART CREATE를 적용하면 쓰기가 0회이며 X는 SHARED 휴지통에 그대로 남는다. `MoveAuditTest`에서 실제 비공개 적용 함수를 메모리 DAO로 실행했다. 전체 두 기기 네트워크 검사는 아니다.
- 위치: `BucketRepository.kt:371–420`, `SyncEngine.kt:286–304`, `SyncEngine.kt:480–488`.
- 최소 수정: moveId를 사용해 이동을 한 번의 collection 변경으로 적용하거나, 이동 CREATE가 같은 ID의 기존 항목을 올바르게 갱신하도록 한다.

### 4. [P1] 변경하지 않은 옛 필드가 새 값을 덮어쓰고 체크리스트 삭제가 복구된다

- CONFIRMED / 코드: UPDATE_FIELD 수신은 op.fieldPath로 적용 범위를 제한하지 않고 전체 스냅샷의 모든 필드를 비교한다. pending이 없는 필드는 오래된 값이어도 적용한다. 체크리스트는 합집합으로만 병합한다.
- 구체적 사례: A가 제목을 바꾸고 업로드를 마친 뒤, B가 이전 제목을 포함한 메모 수정 스냅샷을 전송하면 A의 pending이 없어 제목이 옛 값으로 돌아갈 수 있다. 체크 항목을 삭제한 스냅샷에도 수신기의 기존 항목은 남는다.
- 위치: `BucketRepository.kt:116–139`, `SyncEngine.kt:274–283`, `SyncEngine.kt:319–409`.
- 최소 수정: 실제 변경한 필드만 적용하고 필드별 기준 버전으로 충돌을 판단한다. 체크 항목 삭제도 명시적인 작업으로 표현한다.
- LIKELY: 두 기기 값 불일치·수정 소실. 실제 두 기기 교차 편집은 미검증이다.

### 5. [P1] 동기화 실패가 성공으로 표시되거나 미전송 작업이 전송 완료가 된다

- CONFIRMED / 코드 + 모의 응답: Firebase 작업 조회의 HTTP 오류는 빈 목록으로 바뀐다. pushOperation=false와 pushState=false도 최종 Success를 막지 않는다.
- Drive uploadFile은 토큰이 없으면 null을 반환하지만 호출자는 반환값을 확인하지 않고 markUploaded한다. 실제 클라이언트에서 403→빈 목록, 토큰 없음→null을 재현했다.
- 위치: `CoupleSyncClient.kt:180–207`, `SyncEngine.kt:129–169`, `DriveSyncClient.kt:89`, `SyncEngine.kt:212–217`.
- 영향: 상대방에게 전달되지 않았는데 동기화 시각·완료 건수가 올라가며 재전송 대상에서 빠질 수 있다.
- 최소 수정: 인증·전송·수신 오류를 빈 데이터와 구분하고 모든 쓰기 확인 후에만 업로드 표시와 성공 시각을 갱신한다.

### 6. [P1] Drive 작업 내역은 첫 페이지만 읽는다

- CONFIRMED / 실제 클라이언트 + 모의 응답: nextPageToken이 있는 응답을 줘도 추가 요청하지 않고 첫 목록만 반환한다. fields에도 다음 페이지 토큰이 없다.
- 위치: `DriveSyncClient.kt:32–64`, `SyncEngine.kt:242–265`.
- 영향: 작업 파일이 누적되면 일부 변경을 받지 못하며 반복 동기화해도 같은 첫 페이지를 읽을 수 있다.
- 최소 수정: nextPageToken을 요청하고 마지막 페이지까지 처리한다. 생성·수정 순서 보장을 포함한 작업 재생 테스트도 추가한다.

### 7. [P1] 안내된 복구 코드로 수동 백업 비밀번호를 복구할 수 없다

- CONFIRMED / 암호화 실코드 재현 + release 화면: 수동 백업은 입력 비밀번호만으로 암호화한다. 별도 발급된 복구 코드는 같은 데이터 키를 복호화하는 수단으로 저장되지 않는다.
- 재현: 수동 비밀번호로 암호화한 데이터는 원래 비밀번호로 열리지만 새 복구 코드로는 인증 태그 검사에 실패한다. UI는 비밀번호를 잊으면 복구 코드로 복원 가능하다고 안내한다.
- 위치: `BackupScreen.kt:279–280, 325–326, 395`, `BackupAndExport.kt:30–46`, `CryptoEngine.kt:31–85`.
- 최소 수정: 안내를 실제 백업 방식에 맞게 즉시 고치고, 복구를 지원하려면 데이터 키를 비밀번호·복구 코드 각각으로 감싸 저장한다. 단순히 다른 문자열을 decrypt에 넣는 방식으로는 복구되지 않는다.

### 8. [P1] 자동 암호화 백업의 권한·복원·보존이 완성되지 않았다

- CONFIRMED / 코드와 공식 규격: 자동 백업 대상은 appDataFolder인데 발급 토큰 범위는 drive.file이다. 공식 문서는 appDataFolder에 drive.appdata 범위가 필요하다고 설명한다. 목록 조회도 spaces=appDataFolder를 사용하지 않는다.
- 최근 7개 보관의 삭제 분기는 비어 있다. 자동 백업 목록 선택·다운로드·복원 UI가 없으며, Worker는 복구 코드가 없거나 백업이 실패해도 success로 끝난다.
- 위치: `OurListApp.kt:85–90`, `DriveSetupScreen.kt:162–166`, `BackupAndExport.kt:71–92`, `AutoBackupWorker.kt:24–33`, `BackupScreen.kt:488`.
- 최소 수정: 필요한 범위를 받는 인증, 숨김 폴더 조회, 백업 선택/복원, 실제 보존 정책, 실패·미설정 상태를 연결한다. 일반 계정에서 실제 성공 여부는 UNKNOWN이다.
- 근거: [Google Drive appDataFolder 공식 문서](https://developers.google.com/workspace/drive/api/guides/appdata).

### 9. [P1] 폴더 자동 백업에 개인 목록이 평문으로 포함된다

- CONFIRMED / 코드: SafFullBackup에는 personal과 coupleCode가 들어가고, 선택 폴더에 암호화 없이 ourlist_backup_full.json으로 저장된다. UI의 폴더 백업 안내는 bucketlist.json과 heart.json만 설명한다.
- 위치: `SafBackupManager.kt:18–24, 59–81`, `MoreScreen.kt:665–666`.
- 구체적 사례: 상대방과 공유한 OurList 폴더를 자동 백업 폴더로 지정하면 개인 항목도 그 폴더의 전체 백업에 들어간다. 실제 개인 데이터를 읽거나 업로드하지 않았다.
- 최소 수정: 개인 목록은 기본 제외하고 명시적 범위 선택·암호화를 제공한다. 실제 저장 내용과 공유 여부를 폴더 선택 전에 안내한다.

### 10. [P1] 일반 내보내기에 비밀 목록과 휴지통이 함께 섞인다

- CONFIRMED / 코드: 내보내기는 getAllItems를 그대로 JSON/Markdown/CSV로 전달한다. 해당 쿼리는 collection과 deletedAt을 제한하지 않는다. 확인창은 평문 안내만 하며 비밀·휴지통 포함 여부를 선택할 수 없다.
- 위치: `MoreScreen.kt:803–850`, `BucketDao.kt:41–42`.
- 영향: 공동 목록을 상대방에게 보내려다 개인 비밀과 삭제한 내용까지 포함할 수 있다.
- 최소 수정: 공유/하트/개인/비밀 및 휴지통을 구분해 선택하게 하고, 비밀·휴지통은 기본 제외한다.

### 11. [P1] 화면 회전으로 작성 중 내용이 사라진다

- CONFIRMED / release 앱 실행: 새 항목 제목에 AuditDraft 입력 → 가로 회전 → 제목이 빈 입력칸으로 돌아왔다. 회전 전후 XML·스크린샷을 보존했다.
- 위치: `ItemDetailScreen.kt:203–212`의 remember 상태. 초안은 ViewModel/SavedStateHandle/rememberSaveable에 저장되지 않는다.
- 최소 수정: 편집 초안을 화면 재생성에 안전한 상태로 보존한다. 저장 전 경고만으로 회전 손실을 막을 수 없다.
- 증거: `evidence/draft-before-rotation.*`, `evidence/draft-rotation-result.*`.

## 사용성·접근성 보완

### 12. [P2] 앱을 계속 보고 있는 수신자에게 실시간 변경 수신 계기가 없다

- CONFIRMED / 호출 경로: sync는 로컬 pending 변화, onResume, 수동 버튼에서 발생한다. 커플 연결 화면의 2.5초 polling은 메타만 읽으며 연결 상태가 바뀔 때만 sync한다. 이미 연결된 상대의 항목 변경을 구독하지 않는다.
- 위치: `OurListApp.kt:131–137`, `MainActivity.kt:43–48`, `CoupleConnectScreen.kt:145–168`.
- 영향: A가 수정해도 B가 앱을 계속 열어놓고 있으면 수동 동기화나 재진입 전까지 새 값이 보이지 않을 수 있다. 실제 두 기기 지연 측정은 미실시다.
- 최소 수정: foreground 수신 구독 또는 제한된 polling과 마지막 수신/오류 상태를 제공한다.

### 13. [P2] 너무 큰 비용 입력이 경고 없이 사라진다

- CONFIRMED / release 앱 실행: 99999999999999999999999 입력 → 저장 성공 → 다시 열면 비용이 비어 있다.
- 위치: `ItemDetailScreen.kt:389, 711, 769`의 숫자 필터와 toLongOrNull 조합. 변환 실패를 미입력 null과 구분하지 않는다.
- 최소 수정: 범위·자릿수 오류를 필드 아래에 표시하고 잘못된 값의 저장을 차단한다.
- 증거: `evidence/overflow-cost-before-save.*`, `evidence/overflow-reopened.*`.

### 14. [P2] 큰 글씨에서 동기화 버튼이 세로 선처럼 찌그러지고 필터 문구가 잘린다

- CONFIRMED / release 앱 font scale 2.0: 더보기 상단의 지금 동기화 버튼이 가느다란 세로 영역이 되어 문구가 보이지 않는다. 홈의 고정 높이 필터는 문구가 잘리고 상태 pill도 부자연스럽게 줄바꿈된다.
- 위치: `MoreScreen.kt:243–285`, `FilterSortBar.kt:320–340`.
- 최소 수정: 확대 글씨에서는 동기화 버튼을 다음 줄로 배치하고 텍스트 영역에 가중치/최소 폭을 준다. 필터는 유동 높이 또는 가로 스크롤을 사용한다.
- 증거: `evidence/more-font-200.png`, `evidence/home-font-200.png`.

### 15. [P2] 완료 체크박스와 필터의 이름·선택 상태가 접근성 트리에 없다

- CONFIRMED / release UI 트리: 미완료 원은 NAF=true, 이름 없음, checkable=false로 노출된다. 켜진 필터도 CheckBox 역할이지만 checked=false이며 선택 상태가 없다.
- 위치: `BucketItemComponents.kt:353–380`, `FilterSortBar.kt:320–329`.
- 완료 원의 실제 노출 영역은 126×126px, 이 기기의 밀도에서 48×48dp다. 따라서 완료 원 자체를 작은 터치 영역이라고 단정하지 않았다. 필터는 코드상 36dp 높이다.
- 최소 수정: toggleable/checked 상태와 항목명이 포함된 동작 이름을 제공하고 필터도 선택 상태를 전달한다. 접근성 음성 낭독 전체 흐름은 미실시다.
- 증거: `evidence/final-home.xml`.

### 16. [P2] 백업 복원에 파일 선택이 없고 긴 Base64 붙여넣기를 요구한다

- CONFIRMED / release UI: 암호화 백업 파일을 생성하지만 복원창은 전체 암호문 붙여넣기만 받는다. 일반 사용자는 .enc 파일을 직접 열어 복사하는 경로가 필요하다.
- 위치: `BackupScreen.kt:119–129, 185–188`.
- 최소 수정: 파일 선택으로 백업을 읽고 파일명·항목 수·백업 일시를 확인한 뒤 복원한다.
- 증거: `evidence/restore-dialog.png`.

### 17. [P2] 백업 비밀번호가 기본적으로 평문 표시된다

- CONFIRMED / release UI: AuditPassword123이 그대로 표시되며 XML의 password=false다. 수동 백업 입력도 암호 가림 처리가 없다.
- 위치: `BackupScreen.kt:140–154, 370–384`.
- 최소 수정: 비밀번호 입력을 기본 마스킹하고 사용자가 요청할 때만 잠깐 표시한다. 복구 코드 표시는 비밀번호와 분리한다.
- 증거: `evidence/restore-password-visible.*` (가짜 검사 문자열만 포함).

### 18. [P2] 목표 날짜 알림을 눌러도 해당 항목으로 이동하지 않는다

- CONFIRMED / 코드: 알림 PendingIntent는 navigate_to_item_id를 넣지만 MainActivity와 탐색 코드 어디에서도 읽지 않는다. 시작 목적지는 항상 공동 화면이다.
- 위치: `ReminderScheduler.kt:136–145`, `MainActivity.kt:25–40`, `AppNavGraph.kt:64–67`.
- 최소 수정: 최초 Intent와 onNewIntent에서 항목 ID를 처리해 상세 화면으로 이동한다. 실기기 알림 게시·탭은 미검증이다.

## 검증과 한계

- 정상 확인: 일반 항목 생성·저장·재진입, 홈 검색/필터의 결과 없음 안내, 글자 크기 1.5에서 상세 입력, 세로/가로 화면 진입. 검사 중 crash buffer는 비어 있었다. 포괄적인 무충돌 보증은 아니다.
- 기존 30개 테스트 중 SyncConflictMergeTest와 일부 UIBugFixRegressionTest는 실제 엔진/화면을 호출하지 않고 테스트 안에 병합·필터 로직을 다시 작성한다. 기존 테스트 통과가 #3–#6을 배제하지 못한다.
- 추가 7개는 실제 CryptoEngine, CoupleSyncClient, DriveSyncClient와 SyncEngine CREATE 분기를 사용한다. 네트워크는 OkHttp 메모리 interceptor, DAO는 필요한 인터페이스만 구현한 메모리 fixture다. 실제 서버 요청은 없다.
- UNKNOWN: 실서버 Firebase 규칙, 실제 Google OAuth·토큰 만료/갱신, 두 기기 동시 수정의 전체 결과, 실제 백업 생성/복원, 실기기 알림·TalkBack·성능/메모리. 업데이트 배포와 서명 검증은 이번 우선 검사 범위 밖이다.
- 새 APK를 빌드하지 않았다. 기존 APK 설치와 JVM 테스트만 수행했다. 따라서 새 APK Drive 보관 대상은 없다.
- 테스트 결과는 `test-results/`, 추가 검사 코드는 `probes/`에 보존했다. `audit.init.gradle`로 검사 소스를 임시 추가하며 제품 소스셋은 바꾸지 않는다.

```powershell
.\gradlew.bat :app:testDebugUnitTest --offline --console=plain -I docs/research/2026-11-06-ourlist-audit/audit.init.gradle
```

## 권장 다음 작업

먼저 #1–#6의 접근 제어·이동·병합·전송 확인을 고친다. 이어 #7–#11의 복구·백업 범위·입력 보존을 고친 뒤, 두 개의 격리된 테스트 계정/기기로 동시 편집과 복원을 검증한다. 그 다음 #12–#18의 실시간 수신 및 UI/UX를 반영한다. 실제 사용자 데이터를 사용한 파괴적 검사는 필요하지 않다.
