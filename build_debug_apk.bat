@echo off
chcp 65001 >nul
if "%~1"=="" (
  echo Drive 앱 보관 폴더의 로컬 절대 경로를 첫 번째 인수로 지정하세요.
  exit /b 2
)
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\build_and_archive.ps1" -Variant Debug -DriveAppDirectory "%~1"
exit /b %ERRORLEVEL%
