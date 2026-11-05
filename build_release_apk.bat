@echo off
chcp 65001 >nul
echo ========================================================
echo   Our List - Release APK 서명 및 빌드 스크립트
echo ========================================================

cd /d "%~dp0"

if not exist "keystore.properties" (
    echo [안내] keystore.properties 파일이 없습니다.
    echo 릴리즈 키스토어가 필요합니다. 아래 명령어로 키를 생성할 수 있습니다:
    echo.
    echo keytool -genkeypair -v -keystore release.keystore -alias ourlist -keyalg RSA -keysize 2048 -validity 10000
    echo.
    echo 그 후 keystore.properties 파일을 생성하고 아래 내용을 입력하세요:
    echo storeFile=release.keystore
    echo storePassword=YOUR_STORE_PASSWORD
    echo keyAlias=ourlist
    echo keyPassword=YOUR_KEY_PASSWORD
    echo ========================================================
    pause
    exit /b 1
)

echo [1/3] 단위 테스트 실행 (testReleaseUnitTest)...
call gradlew.bat testReleaseUnitTest
if errorlevel 1 (
    echo [ERROR] 단위 테스트 실패!
    pause
    exit /b 1
)

echo.
echo [2/3] Release APK 빌드 (assembleRelease)...
call gradlew.bat assembleRelease
if errorlevel 1 (
    echo [ERROR] Release APK 빌드 실패!
    pause
    exit /b 1
)

echo.
echo [3/3] 릴리즈 산출물 확인 및 동기화...
set "VER=v1.0.6"
set "REL_APK=%~dp0app\build\outputs\apk\release\app-release.apk"
set "DEST_APK=%~dp0OurList-%VER%-release.apk"
set "GDRIVE_DEV=G:\내 드라이브\03_앱개발\ourlist"

if exist "%REL_APK%" (
    copy /y "%REL_APK%" "%DEST_APK%" >nul
    echo  - 릴리즈 APK 생성 완료: %DEST_APK%
    
    if exist "%GDRIVE_DEV%" (
        copy /y "%REL_APK%" "%GDRIVE_DEV%\OurList-%VER%-release.apk" >nul
        echo  - Google Drive (03_앱개발\ourlist) 동기화 완료!
    )
    
    echo.
    echo ========================================================
    echo   빌드 성공!
    echo ========================================================
    powershell -NoProfile -Command "Get-FileHash -Algorithm SHA256 '%DEST_APK%' | Format-List"
) else (
    echo [ERROR] 릴리즈 APK를 찾을 수 없습니다: %REL_APK%
    pause
    exit /b 1
)

echo.
pause
