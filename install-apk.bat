@echo off
setlocal
cd /d "%~dp0"

echo ========================================================
echo        Lim Edu - Android Installer ^& Launcher
echo ========================================================
echo.

adb devices
echo.

echo Checking for connected Android devices...
for /f "skip=1 tokens=1" %%d in ('adb devices') do (
    if not "%%d"=="" (
        set DEVICE=%%d
        goto :found
    )
)

echo [!] No device detected by ADB.
echo.
echo Please ensure:
echo  1. Your phone is connected with a USB cable.
echo  2. "Developer Options" -^> "USB Debugging" is enabled on your phone.
echo  3. Look at your phone screen and tap "Allow USB Debugging".
echo  4. If prompted on phone, set USB mode to "File Transfer / MTP".
echo.
pause
exit /b 1

:found
echo [+] Device detected: %DEVICE%
echo [*] Installing Lim-Edu.apk...
adb -s %DEVICE% install -r "Lim-Edu.apk"
if %ERRORLEVEL% equ 0 (
    echo [+] Installed successfully!
    echo [*] Launching Lim Edu...
    adb -s %DEVICE% shell am start -n com.limedu.app/.MainActivity
) else (
    echo [!] Installation failed. If using Xiaomi/Redmi, enable "Install via USB" in Developer Options.
)

echo.
pause
