@echo off
setlocal

echo ============================================
echo   Karaoke Live Player - Build EXE
echo ============================================
echo.

REM Percorso della JDK BellSoft Full
set JDK="C:\jdk\bellsoft-jdk-25.0.5.1-full"

REM Cartella del progetto
set PROJECT_DIR=%~dp0

REM Cartella di output
set DIST=%PROJECT_DIR%dist

echo Cartella progetto: %PROJECT_DIR%
echo Cartella output: %DIST%
echo.

REM Compila il progetto e crea il JAR prima di generare l'eseguibile
echo Avvio Maven clean package...
call mvn clean package -DskipTests
if errorlevel 1 (
    echo Errore: Maven clean package non riuscito.
    exit /b 1
)

REM Elimina la cartella dist se esiste
if exist "%DIST%" (
    echo Rimozione cartella dist esistente...
    rmdir /s /q "%DIST%"
)

REM Crea la cartella dist
echo Creazione nuova cartella dist...
mkdir "%DIST%"

echo.
echo Avvio jpackage...
%JDK%\bin\jpackage.exe ^
  --type app-image ^
  --name KaraokeLivePlayer ^
  --input "%PROJECT_DIR%target" ^
  --main-jar karaoke-live-player-1.0.0-SNAPSHOT.jar ^
  --main-class it.andrea.karaokeliveplayer.Main ^
  --dest "%DIST%" ^
  --app-version 1.0.0 ^
  --vendor "Andrea Bevilacqua" ^
  --description "Karaoke Live Player" ^
  --copyright "� 2026 Andrea"

echo.
echo ============================================
echo   Build completato!
echo   EXE generato in: dist\KaraokeLivePlayer
echo ============================================
echo.

pause
endlocal