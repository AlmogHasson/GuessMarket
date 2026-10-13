@echo off
title Guess Market - Web Client
cd /d "%~dp0"

where node >nul 2>nul
if errorlevel 1 (
    echo Node.js was not found. Install Node.js and run this file again.
    pause
    exit /b 1
)

echo Starting the Guess Market web client on http://localhost:3000
echo Tomcat with guess-market.war must already be running on port 8080.
echo.
node server.js --open
pause
