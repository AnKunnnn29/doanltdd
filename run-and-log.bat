@echo off
echo ========================================
echo Building and Installing App...
echo ========================================

call gradlew.bat clean assembleDebug installDebug

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo Build successful! Starting app...
    echo ========================================
    
    REM Clear logcat
    adb logcat -c
    
    REM Start app
    adb shell am start -n com.example.doan/.Activities.SplashActivity
    
    echo.
    echo ========================================
    echo Watching logs... Press Ctrl+C to stop
    echo ========================================
    echo.
    
    REM Show logs
    adb logcat | findstr /C:"SplashActivity" /C:"WelcomeActivity" /C:"LoginActivity" /C:"AndroidRuntime" /C:"FATAL"
) else (
    echo.
    echo ========================================
    echo Build failed! Check errors above.
    echo ========================================
)

pause
