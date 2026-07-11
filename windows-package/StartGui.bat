@echo off
cd /d "%~dp0"
echo Starter Org.nr verktoy...
java -jar OrgNrGui.jar
if errorlevel 1 (
    echo.
    echo Feil ved oppstart. Er Java 21 installert?
    echo Last ned fra: https://adoptium.net/
)
pause
