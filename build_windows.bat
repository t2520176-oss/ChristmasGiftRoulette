@echo off
REM ============================================================================
REM  FigureCraft - Windows build (portable folder + ZIP [+ installer if Inno Setup exists])
REM  Run from anywhere:  build_windows.bat            (build + packaged self-test)
REM                      build_windows.bat /test      (also run the unit tests first)
REM  Result: output\FigureCraft\FigureCraft.exe, output\FigureCraft-portable.zip, output\BUILD_INFO.txt
REM ============================================================================
setlocal EnableExtensions
cd /d "%~dp0"
set ROOT=%CD%
set OUT=%ROOT%\output
set VENV=%ROOT%\.venv-build

where py >nul 2>nul
if %ERRORLEVEL%==0 ( set PYEXE=py -3.11 ) else ( set PYEXE=python )
%PYEXE% --version || ( echo [ERROR] Python 3.11+ was not found. Install it from python.org and retry. & exit /b 1 )

if not exist "%VENV%\Scripts\python.exe" (
  echo === Creating build environment ===
  %PYEXE% -m venv "%VENV%" || exit /b 1
)
call "%VENV%\Scripts\activate.bat"
python -m pip install --upgrade pip || exit /b 1
echo === Installing dependencies ===
pip install -r desktop\requirements-dev.txt || exit /b 1

if /I "%1"=="/test" (
  echo === Running unit tests ===
  python -m pytest desktop\tests -q || exit /b 1
)

echo === Building with PyInstaller ===
if exist "%OUT%\FigureCraft" rmdir /s /q "%OUT%\FigureCraft"
pyinstaller desktop\figurecraft.spec --noconfirm --clean --distpath "%OUT%" --workpath "%ROOT%\build\pyinstaller" || ( echo [ERROR] PyInstaller failed & exit /b 1 )
if not exist "%OUT%\FigureCraft\FigureCraft.exe" ( echo [ERROR] FigureCraft.exe was not created & exit /b 1 )

python desktop\tools\slim_dist.py "%OUT%\FigureCraft"

echo === Packaged self-test (headless) ===
"%OUT%\FigureCraft\FigureCraft.exe" --self-test > "%OUT%\selftest.log" 2>&1
type "%OUT%\selftest.log"
findstr /C:"SELF-TEST PASSED" "%OUT%\selftest.log" >nul || ( echo [ERROR] packaged self-test failed & exit /b 1 )

echo === GUI smoke test (starts the window, 8 seconds, then exits) ===
set FIGURECRAFT_SMOKE_MS=8000
set FIGURECRAFT_HOME=%TEMP%\figurecraft-smoke
"%OUT%\FigureCraft\FigureCraft.exe" --smoke-test
if errorlevel 1 (set "SMOKE=FAILED") else (set "SMOKE=PASSED")
set FIGURECRAFT_HOME=

echo === Creating portable ZIP ===
copy /y README_KR.md "%OUT%\FigureCraft\README_KR.md" >nul
if exist "%OUT%\FigureCraft-portable.zip" del "%OUT%\FigureCraft-portable.zip"
powershell -NoProfile -Command "Compress-Archive -Path '%OUT%\FigureCraft' -DestinationPath '%OUT%\FigureCraft-portable.zip' -Force" || exit /b 1

echo === Build info ===
python desktop\tools\make_build_info.py --out "%OUT%" --exe "%OUT%\FigureCraft\FigureCraft.exe" --selftest "%OUT%\selftest.log" --smoke "%SMOKE%" --target Windows || exit /b 1

REM Optional installer (needs Inno Setup 6: https://jrsoftware.org/isinfo.php)
set ISCC=
if exist "%ProgramFiles(x86)%\Inno Setup 6\ISCC.exe" set ISCC=%ProgramFiles(x86)%\Inno Setup 6\ISCC.exe
if exist "%ProgramFiles%\Inno Setup 6\ISCC.exe" set ISCC=%ProgramFiles%\Inno Setup 6\ISCC.exe
if defined ISCC (
  echo === Creating installer ===
  "%ISCC%" /Qp /O"%OUT%" desktop\installer\FigureCraft.iss
) else (
  echo [info] Inno Setup not found - skipping installer ^(portable folder/ZIP are ready^).
)

echo.
echo DONE.
echo   Folder : %OUT%\FigureCraft\FigureCraft.exe
echo   ZIP    : %OUT%\FigureCraft-portable.zip
echo   Info   : %OUT%\BUILD_INFO.txt
endlocal
exit /b 0
