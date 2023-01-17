@ECHO OFF

@rem Enable access to non-SDK interfaces
@rem For Android 9 (API level 28)
adb shell settings put global hidden_api_policy_pre_p_apps 1
adb shell settings put global hidden_api_policy_p_apps 1

@rem For Android 10 (API level 29) or higher
adb shell settings put global hidden_api_policy 1

@rem Disable system night mode.
adb shell "cmd uimode night no"

for /f "tokens=1,* delims= " %%a in ("%*") do set ALL_BUT_FIRST=%%b

@rem Script params
@rem Module is first param of the script (ex: :codebase:compose).
@rem Other params are all params from the second one to the end.
set module=%1
set params=%ALL_BUT_FIRST%

@rem Set module to codebase compose when module param was null.
IF [%module%]==[] (
    set module=":codebase:compose"
)

@rem Change module to codebase compose and change to params from first to end when module param does
@rem not start with ":" (ex: :codebase would not be replaced but -Precord would be).
echo %module% | findstr /r "^( )*:.*" >nul 2>&1
if errorlevel 1 (
    set module=":codebase:compose"
    set params="%*"
)

ECHO "Running screenshot tests for module %module% with params %params%"

@rem Run light mode tests annotated by [ScreenshotTest].
gradlew %module%:executeScreenshotTests ^
    -Pandroid.testInstrumentationRunnerArguments.annotation=cz.csob.smartbanking.compose.core.ScreenshotTest ^
    -PdirectorySuffix=light ^
    "%params%"

@rem Enable system night mode.
adb shell "cmd uimode night yes"

@rem Run dark mode tests annotated by [ScreenshotTest].
gradlew %module%:executeScreenshotTests ^
    -Pandroid.testInstrumentationRunnerArguments.annotation=cz.csob.smartbanking.compose.core.ScreenshotTest ^
    -PdirectorySuffix=dark ^
    "%params%"

@rem Disable system night mode.
adb shell "cmd uimode night no"

@rem Disable access to non-SDK interfaces
@rem for Android 9 (API level 28)
adb shell settings delete global hidden_api_policy_pre_p_apps >/dev/null
adb shell settings delete global hidden_api_policy_p_apps >/dev/null

@rem for Android 10 (API level 29) or higher
adb shell settings delete global hidden_api_policy >/dev/null
