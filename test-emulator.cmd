@echo off
rem ============================================================
rem  PPEmbyTV emulator test script (all env points into the project)
rem  Usage:
rem    test-emulator.cmd avd-list         list project-local AVDs
rem    test-emulator.cmd install-debug    install debug APK on emulator
rem    test-emulator.cmd boot             boot headless emulator (keep this terminal)
rem
rem  Note: the emulator stores its adb key in %USERPROFILE%\.android
rem  (fixed by the emulator itself). This script removes that folder
rem  after the session so nothing is left outside the project.
rem ============================================================
setlocal
cd /d "%~dp0"

set "ANDROID_HOME=%CD%\.android-sdk"
set "ANDROID_SDK_ROOT=%CD%\.android-sdk"
set "ANDROID_USER_HOME=%CD%\.android"
set "ANDROID_AVD_HOME=%CD%\.android\avd"
set "ANDROID_EMULATOR_HOME=%CD%\.android"
rem adb looks for keys in %HOME%\.android
set "HOME=%CD%"

set "ADB=%ANDROID_HOME%\platform-tools\adb.exe"
set "EMU=%ANDROID_HOME%\emulator\emulator.exe"

if "%1"=="avd-list" (
    "%EMU%" -list-avds
    goto cleanup
)
if "%1"=="install-debug" (
    "%ADB%" devices
    for /f "usebackq tokens=1" %%d in (`"%ADB%" devices ^| findstr "emulator-"`) do (
        "%ADB%" -s %%d install -r "app\build\outputs\apk\debug\app-universal-debug.apk"
    )
    goto cleanup
)
if "%1"=="boot" (
    "%EMU%" -avd ppembytv_tv -no-window -no-audio -no-boot-anim -gpu swiftshader_indirect -no-snapshot
    goto cleanup
)

echo Usage: test-emulator.cmd ^<avd-list^|install-debug^|boot^>
exit /b 1

:cleanup
rem stop adb server and remove emulator key residue outside the project
"%ADB%" kill-server >nul 2>&1
if exist "%USERPROFILE%\.android" (
    rd /s /q "%USERPROFILE%\.android"
)
exit /b 0
