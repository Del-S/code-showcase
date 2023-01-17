#!/usr/bin/env bash

# This script expands functionality from create_logging_list.sh by creating logging reports for
# current and previous version of the app and using git diff to compare reports and creating
# difference file containing the log differences. Differences are done only for logs with variables.
# It is used for a quick check and security does not need to check all logs (just those that
# changed).

# This script is used mainly for Security reasons to allow audit of logs used in the app. For now it
# tracks public logs which have INFO, WARN or ERROR level. Debug logs are not listed.

# How it works: check current version, finds previous version of the app (using git tag command).
# Generates logging report for current version, checks out to previous and generates a logging
# report for it. When both reports are done it uses git diff command to compare current version with
# previous and writes the result into the file.

# Options (variables):
#   TARGET_DIR - destination where list files will be created.
#   SCRIPTS_PATH - path to scripts (to trigger create_logging_list.sh).
#
# TEST VALUES:
# TARGET_DIR="./build/log-lists"
# SCRIPTS_PATH="./fastlane/scripts"

targetDirDiff="$TARGET_DIR/diff"
targetDirCurrent="$TARGET_DIR/current"
targetDirPrevious="$TARGET_DIR/previous"
variableLogFileDiff="$targetDirDiff/logVariablesDifference.txt"
variableLogFileCurrent="$targetDirCurrent/logVariables.txt"
variableLogFilePrevious="$targetDirPrevious/logVariables.txt"

# Sets error and pipeline fail only for non CSOB CI/CD. We do not want to crash the build due to
# logging report. It also modifies which commands should run since CSOB CI/CD runs on macOS with
## additional GNU features separated (gsed/ggrep).
grep_command=grep
sed_command=sed
if [[ $CSOB_CI == "true" || "$(uname)" == "Darwin" ]]; then
    echo "Pipeline failing disabled"
    echo "Forcing GNU commands"
    grep_command=ggrep
    sed_command=gsed
else
    echo "Enabling pipeline failing"
    set -euo pipefail
fi

# Initializes directories for this script. Makes sure target directory exists.
init() {
    echo "Initializing"
    if [ ! -d $targetDirDiff ]; then
        echo "Creating target dir"
        mkdir -p $targetDirDiff
    fi
}

# Decides the versions to compare. Takes current version from Dependencies.kt in buildSrc. Previous
# version is loaded by listing all tags using git, adding a current version to it. Each tag is then
# compared against current version and if the string is the same it will take previous version
# string. Previous version string will be a previous version because all tags are sorted with
# version comparator. After versions are decided it will prints them out.
function decideVersionsToCompare() {
    echo "Deciding versions to compare"
    currentVersion=$($grep_command -r "versionName = \"\(0\|[1-9][0-9]*\).\(0\|[1-9][0-9]*\)\(.\(0\|[1-9][0-9]*\)\)" ./buildSrc/src/main/kotlin/Dependencies.kt | $grep_command -o "\(0\|[1-9][0-9]*\).\(0\|[1-9][0-9]*\)\(.\(0\|[1-9][0-9]*\)\)" | tail -1)
    previousVersions=($(git tag | $grep_command "\(0\|[1-9][0-9]*\).\(0\|[1-9][0-9]*\)\(.\(0\|[1-9][0-9]*\)\)$" | awk '{print $0} END{print "'$currentVersion'"}' | sort -V | tail -20))
    for i in "${!previousVersions[@]}"; do
        if [[ "${previousVersions[$i]}" == "${currentVersion}" ]]; then
            previousVersion=${previousVersions[$i - 1]}
            break
        fi
    done
    echo "Current version $currentVersion"
    echo "Previous version $previousVersion"
}

# Generates logging report for current version (current HEAD) using create_logging_list.sh. Sets env
# variable TARGET_DIR to targetDirCurrent.
function generateCurrentVersionLogReport() {
    echo "Generate current version log report"
    export TARGET_DIR=$targetDirCurrent
    $SCRIPTS_PATH/create_logging_list.sh
    echo "Generate current version log report complete"
}

# Generates logging report for previous version (current HEAD) using create_logging_list.sh. Stashes
# git changes just to be sure it can check out. Checks out onto previousVersion tag (commit), sets
# env variable TARGET_DIR to targetDirPrevious and runs the logging report script.
function generatePreviousVersionLogReport() {
    echo "Generate previous version log report"
    git stash
    git checkout $previousVersion
    export TARGET_DIR=$targetDirPrevious
    $SCRIPTS_PATH/create_logging_list.sh
    echo "Generate previous version log report complete"
}

# Compares two generated log report files using git diff command (with no context since it is not
# needed) and writes the result into file with path variableLogFileDiff.
function compareLogReports() {
    echo "Compare log reports"
    >$variableLogFileDiff
    if ! git diff --exit-code --no-index --unified=0 $variableLogFilePrevious $variableLogFileCurrent | $sed_command 's/@@/\n&/' >>$variableLogFileDiff; then
        echo "Compare log reports finished with differences"
    else
        echo "Compare log reports found no differences"
    fi
}

echo "Log difference report script started"
init
decideVersionsToCompare
generateCurrentVersionLogReport
generatePreviousVersionLogReport
compareLogReports
echo "Log difference report script finished"
