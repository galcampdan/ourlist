@echo off
chcp 65001 >nul
echo ========================================================
echo   Our List - Release Keystore Generator
echo ========================================================
echo.

if exist "release.keystore" (
    echo [!] WARNING: "release.keystore" already exists in the project root!
    echo     Overwriting it will break app updates for existing installations!
    echo.
    set /p "OVERWRITE=Do you really want to overwrite it? (yes/no): "
    if /i not "%OVERWRITE%"=="yes" (
        echo Keystore generation cancelled.
        pause
        exit /b 0
    )
)

echo Generating release keystore (RSA 2048-bit, 25 years validity)...
echo You will be prompted to enter a password and organization details.
echo.
keytool -genkeypair -v -keystore release.keystore -alias ourlist_release -keyalg RSA -keysize 2048 -validity 10000

if errorlevel 1 (
    echo.
    echo [ERROR] Keystore generation failed.
    pause
    exit /b 1
)

echo.
echo ========================================================
echo Keystore created: release.keystore
echo Now setting up keystore.properties (git-ignored)...
echo ========================================================
echo.
set /p "STORE_PASS=Enter the password you used for the keystore: "

(
echo storeFile=release.keystore
echo storePassword=%STORE_PASS%
echo keyAlias=ourlist_release
echo keyPassword=%STORE_PASS%
) > keystore.properties

echo.
echo [SUCCESS] keystore.properties created!
echo DO NOT commit release.keystore or keystore.properties to Git.
echo Keep a secure backup of release.keystore in a safe place.
echo.
pause
