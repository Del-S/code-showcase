#!/bin/bash

set -e

## Enable access to non-SDK interfaces
#  For Android 9 (API level 28)
adb shell settings put global hidden_api_policy_pre_p_apps 1
adb shell settings put global hidden_api_policy_p_apps 1

#  For Android 10 (API level 29) or higher
adb shell settings put global hidden_api_policy 1

## Disable system night mode.
adb shell "cmd uimode night no"

## Script params
# Module is first param of the script (ex: :codebase:compose).
# Other params are all params from the second one to the end.
module=$1
params="${@:2}"

# Set module to codebase compose when module param was null.
if [ -z $module ]; then
    module=":codebase:compose"
fi

# Change module to codebase compose and change to params from first to end when module param does
# not start with ":" (ex: :codebase would not be replaced but -Precord would be).
if [[ ! "$module" =~ ^( )*:.* ]]; then
    module=":codebase:compose"
    params="${@:1}"
fi

echo "Running screenshot tests for module $module with params $params"

## Run light mode tests annotated by [ScreenshotTest].
./gradlew "$module:executeScreenshotTests" \
    -Pandroid.testInstrumentationRunnerArguments.annotation=cz.csob.smartbanking.compose.core.ScreenshotTest \
    -PdirectorySuffix=light \
    $params


## Enable system night mode.
adb shell "cmd uimode night yes"

## Run dark mode tests annotated by [ScreenshotTest].
./gradlew $module:executeScreenshotTests \
    -Pandroid.testInstrumentationRunnerArguments.annotation=cz.csob.smartbanking.compose.core.ScreenshotTest \
    -PdirectorySuffix=dark \
    $params

## Disable system night mode.
adb shell "cmd uimode night no"

## Disable access to non-SDK interfaces
#  For Android 9 (API level 28)
adb shell settings delete global hidden_api_policy_pre_p_apps >/dev/null
adb shell settings delete global hidden_api_policy_p_apps >/dev/null

#  For Android 10 (API level 29) or higher
adb shell settings delete global hidden_api_policy >/dev/null
