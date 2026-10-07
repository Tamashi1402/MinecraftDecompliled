@echo off
setlocal
cd /d "%~dp0"

rem Prefer the py launcher (avoids Windows Store python alias issues)
where py >nul 2>nul
if %errorlevel%==0 (
    py -3 "%~dp0nfdecompile.py" %*
) else (
    python "%~dp0nfdecompile.py" %*
)

echo.
pause
