@REM ---------------------------------------------------------------------------
@REM Maven Wrapper for the Blood Donation Card Management System.
@REM
@REM Resolves Apache Maven into %USERPROFILE%\.m2\wrapper\dists, downloading it
@REM once if it is not already there, then runs it. No Maven installation on
@REM PATH is required - only a JDK.
@REM
@REM Usage:  mvnw.cmd clean test
@REM         mvnw.cmd clean package
@REM         mvnw.cmd clean package cargo:run
@REM ---------------------------------------------------------------------------
@echo off
setlocal

@REM Trust the Windows certificate store as well as the JDK's own.
@REM On machines where antivirus or a corporate proxy inspects TLS, the
@REM interception certificate is installed into the Windows store but not into
@REM the JDK's cacerts, and Maven downloads then fail with:
@REM   PKIX path building failed ... unable to find valid certification path
@REM This makes the JVM use the Windows store, which already trusts it.
if not defined MAVEN_OPTS set "MAVEN_OPTS=-Djavax.net.ssl.trustStoreType=WINDOWS-ROOT"

set "WRAPPER_PROPS=%~dp0.mvn\wrapper\maven-wrapper.properties"
set "DIST_URL="
set "DIST_PATH="

for /f "usebackq tokens=1,* delims==" %%A in ("%WRAPPER_PROPS%") do (
    if "%%A"=="distributionUrl"  set "DIST_URL=%%B"
    if "%%A"=="distributionPath" set "DIST_PATH=%%B"
)

set "MAVEN_HOME=%USERPROFILE%\%DIST_PATH:/=\%"

if not exist "%MAVEN_HOME%\bin\mvn.cmd" (
    echo [mvnw] Apache Maven not found locally. Downloading once...
    echo [mvnw]   from %DIST_URL%
    echo [mvnw]   into %MAVEN_HOME%
    for %%I in ("%MAVEN_HOME%\..") do set "DIST_PARENT=%%~fI"
    powershell -NoProfile -ExecutionPolicy Bypass -Command ^
        "$ErrorActionPreference='Stop';" ^
        "$zip = Join-Path $env:TEMP 'bdcms-maven-dist.zip';" ^
        "New-Item -ItemType Directory -Force -Path '%DIST_PARENT%' | Out-Null;" ^
        "[Net.ServicePointManager]::SecurityProtocol=[Net.SecurityProtocolType]::Tls12;" ^
        "Invoke-WebRequest -Uri '%DIST_URL%' -OutFile $zip;" ^
        "Expand-Archive -Path $zip -DestinationPath '%DIST_PARENT%' -Force;" ^
        "Remove-Item $zip -Force"
    if errorlevel 1 (
        echo [mvnw] ERROR: could not download Apache Maven. 1>&2
        exit /b 1
    )
)

if not exist "%MAVEN_HOME%\bin\mvn.cmd" (
    echo [mvnw] ERROR: Maven still not found at "%MAVEN_HOME%". 1>&2
    exit /b 1
)

call "%MAVEN_HOME%\bin\mvn.cmd" %*
exit /b %ERRORLEVEL%
