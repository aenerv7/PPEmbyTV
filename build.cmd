@echo off
rem ============================================================
rem  PPEmbyTV one-click build script
rem  All build env is self-contained inside the project:
rem    .tools\jdk17 / .android-sdk / .gradle-home
rem  Usage: build.cmd [gradle task...]  e.g. build.cmd :app:assembleRelease
rem ============================================================
setlocal
cd /d "%~dp0"

set "JAVA_HOME=%CD%\.tools\jdk17\jdk-17.0.20+8"
set "GRADLE_USER_HOME=%CD%\.gradle-home"
set "ANDROID_HOME=%CD%\.android-sdk"
set "ANDROID_SDK_ROOT=%CD%\.android-sdk"
rem Android prefs (user home + prefs root) -> project-local, avoids ~/.android residue
set "ANDROID_USER_HOME=%CD%\.android"
set "ANDROID_PREFS_ROOT=%CD%"

if not exist "%JAVA_HOME%\bin\java.exe" (
    echo [ERROR] project-local JDK not found: %JAVA_HOME%
    exit /b 1
)
if not exist "%ANDROID_HOME%\platform-tools\adb.exe" (
    echo [ERROR] project-local Android SDK not found: %ANDROID_HOME%
    exit /b 1
)

echo [PPEmbyTV] JAVA_HOME=%JAVA_HOME%
echo [PPEmbyTV] GRADLE_USER_HOME=%GRADLE_USER_HOME%
echo [PPEmbyTV] ANDROID_HOME=%ANDROID_HOME%
echo.

call gradlew.bat %*
exit /b %ERRORLEVEL%
