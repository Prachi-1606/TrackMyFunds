@echo off
setlocal

set "MAVEN_VERSION=3.9.9"
set "MAVEN_ZIP=apache-maven-%MAVEN_VERSION%-bin.zip"
set "MAVEN_URL=https://archive.apache.org/dist/maven/maven-3/%MAVEN_VERSION%/binaries/%MAVEN_ZIP%"
set "WRAPPER_DIR=%~dp0.mvn\wrapper"
set "MAVEN_HOME=%WRAPPER_DIR%\apache-maven-%MAVEN_VERSION%"
set "MVN_CMD=%MAVEN_HOME%\bin\mvn.cmd"

if not exist "%MVN_CMD%" (
    echo [mvnw] Maven %MAVEN_VERSION% not found. Downloading from Apache...
    if not exist "%WRAPPER_DIR%" mkdir "%WRAPPER_DIR%"

    powershell -NoProfile -Command ^
        "Invoke-WebRequest -Uri '%MAVEN_URL%' -OutFile '%WRAPPER_DIR%\%MAVEN_ZIP%' -UseBasicParsing"

    if errorlevel 1 (
        echo [mvnw] ERROR: Download failed. Check your internet connection.
        exit /b 1
    )

    powershell -NoProfile -Command ^
        "Expand-Archive -Path '%WRAPPER_DIR%\%MAVEN_ZIP%' -DestinationPath '%WRAPPER_DIR%' -Force"

    del /q "%WRAPPER_DIR%\%MAVEN_ZIP%"
    echo [mvnw] Maven %MAVEN_VERSION% ready at %MAVEN_HOME%
    echo.
)

"%MVN_CMD%" %*
endlocal
