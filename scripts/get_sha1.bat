@echo off
chcp 65001 >nul
echo ========================================================
echo   Our List - Keystore SHA-1 / SHA-256 Fingerprint Tool
echo ========================================================
echo.

set "DEBUG_KEYSTORE=%USERPROFILE%\.android\debug.keystore"
if exist "%DEBUG_KEYSTORE%" (
    echo [1] Debug Keystore Fingerprints:
    echo     File: %DEBUG_KEYSTORE%
    echo --------------------------------------------------------
    keytool -list -v -keystore "%DEBUG_KEYSTORE%" -alias androiddebugkey -storepass android -keypass android | findstr /i "SHA1 SHA256"
    echo.
) else (
    echo [!] Debug keystore not found at: %DEBUG_KEYSTORE%
)

set "RELEASE_KEYSTORE=release.keystore"
if exist "%RELEASE_KEYSTORE%" (
    echo [2] Release Keystore Fingerprints:
    echo     File: %RELEASE_KEYSTORE%
    echo --------------------------------------------------------
    keytool -list -v -keystore "%RELEASE_KEYSTORE%" | findstr /i "SHA1 SHA256"
    echo.
) else (
    echo [!] Release keystore not found in project root (%RELEASE_KEYSTORE%).
    echo     Run "scripts\generate_keystore.bat" to create one.
)

echo ========================================================
echo * Google Cloud Console Android OAuth Client:
echo   Package Name: com.ourlist.app
echo   Copy the SHA-1 from above into Google Cloud Console.
echo ========================================================
pause
