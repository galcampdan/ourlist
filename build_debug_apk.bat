@echo off
chcp 65001 >nul
echo ========================================================
echo   Our List - Debug APK 빌드 및 동기화 스크립트
echo ========================================================

cd /d "%~dp0"

echo [1/3] 단위 테스트 실행 (testDebugUnitTest)...
call gradlew.bat testDebugUnitTest
if errorlevel 1 (
    echo [ERROR] 단위 테스트 실패! 빌드를 중단합니다.
    pause
    exit /b 1
)

echo.
echo [2/3] Debug APK 컴파일 및 패키징 (assembleDebug)...
call gradlew.bat assembleDebug
if errorlevel 1 (
    echo [ERROR] APK 빌드 실패!
    pause
    exit /b 1
)

echo.
echo [3/3] APK 복사 및 무결성 검증...
set "VER=v1.0.3"
set "SRC_APK=%~dp0app\build\outputs\apk\debug\app-debug.apk"
set "DEST_APK=%~dp0OurList-%VER%-debug.apk"
set "GDRIVE_DEV=G:\내 드라이브\03_앱개발\ourlist"

if exist "%SRC_APK%" (
    copy /y "%SRC_APK%" "%DEST_APK%" >nul
    echo  - 로컬 복사 완료: %DEST_APK%
    
    if exist "%GDRIVE_DEV%" (
        copy /y "%SRC_APK%" "%GDRIVE_DEV%\OurList-%VER%-debug.apk" >nul
        echo  - Google Drive (03_앱개발\ourlist) 동기화 완료!
    )
    
    echo.
    echo ========================================================
    echo   빌드 성공!
    echo ========================================================
    powershell -NoProfile -Command "Get-FileHash -Algorithm SHA256 '%DEST_APK%' | Format-List"
) else (
    echo [ERROR] 빌드 산출물 APK를 찾을 수 없습니다: %SRC_APK%
    pause
    exit /b 1
)

echo.
pause
