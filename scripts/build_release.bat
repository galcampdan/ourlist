@echo off
chcp 65001 >nul
echo ========================================================
echo   Our List - Build Signed Release APK
echo ========================================================
echo.

if not exist "keystore.properties" (
    echo [!] WARNING: "keystore.properties" not found!
    echo     Please run "scripts\generate_keystore.bat" first,
    echo     or configure keystore.properties manually.
    echo.
    set /p "PROCEED=Proceed with unsigned release build? (y/n): "
    if /i not "%PROCEED%"=="y" exit /b 0
)

echo Starting Gradle assembleRelease...
call gradlew.bat assembleRelease

if errorlevel 1 (
    echo.
    echo [ERROR] Release build failed!
    pause
    exit /b 1
)

echo.
echo ========================================================
echo [SUCCESS] Release build finished!
echo ========================================================
if exist "app\build\outputs\apk\release\app-release.apk" (
    echo APK Location: app\build\outputs\apk\release\app-release.apk
    powershell -Command "(Get-Item 'app\build\outputs\apk\release\app-release.apk').Length / 1MB"
    powershell -Command "Get-FileHash -Path 'app\build\outputs\apk\release\app-release.apk' -Algorithm SHA256"
) else (
    dir app\build\outputs\apk\release\
)
echo.
pause
