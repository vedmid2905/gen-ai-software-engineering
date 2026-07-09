@echo off
setlocal

set SCRIPT_DIR=%~dp0
set PROJECT_DIR=%SCRIPT_DIR%..\src

cd /d "%PROJECT_DIR%"
echo Building and starting the Banking Transactions API on http://localhost:8080 ...
call mvn spring-boot:run

endlocal
