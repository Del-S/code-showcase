#!/bin/bash

# This script triggers fastlanes for multiple modules in one pipeline to prevent running out of
# gitlab runners and keep them in one context. This is mainly used to runs multiple test fastlanes
# in one pipeline.
# Options (variables):
#   MODULES - list of modules to runs these fastlanes on. Format: "module1, module2" (whitespace required).
#   TASKS_LIST_FILE - file containing all gradle tasks. Used for flavor and build type checks.
#   FLAVOR - specifies flavor of the build.
#   BUILD_TYPE - specifies build type.
#
# TEST VALUES:
#MODULES=":app:app-apk, :app:library"
#TASKS_LIST_FILE="tasks.txt"
#FLAVOR="ApiSwitcherEnvDevelopment"
#BUILD_TYPE="DebugUnitTest"

set -euo pipefail

# Initializes this script by checking required variables. If one of the required variables is not
# set then this script fails. Required variables: MODULES and TASKS_LIST_FILE. Loads gradle tasks
# from file provided in TASKS_LIST_FILE to be checked later in the script.
init() {
  if [[ ! -n "${MODULES// /}" ]]; then
    echo "No modules to run tests on"
    exit 0
  elif [[ ! -n "${TASKS_LIST_FILE// /}" ]]; then
    echo "No task list file provided"
    exit 0
  else
    echo "Running tests for modules: ${MODULES}"
  fi

  echo "Loading gradle tasks from tasks file"
  gradleTasks=($(cat $TASKS_LIST_FILE))
}

# Returns flavor or build type (by printing) if any of the module tasks contains that specific
# flavor or build type. Else it returns empty value. Thanks to this pipeline can run default
# java tasks and android specific tasks combined without the need to define separate pipeline for
# them.
#
# Params
#   $1 - name of the module being tested
#   $2 - flavor or build type being searched for
getFlavorBuildType() {
  if [ -n "$2" ]; then
    regEx="^$1.*$2.*"
    for task in "${gradleTasks[@]}"; do
      if [[ "$task" =~ $regEx ]]; then
        echo "$2"
        return
      fi
    done
  fi
  echo ""
}

# Loads build parameters such as flavor and build type. Checks if current module provides this
# flavor or build type. If not then it will not set them.
#
# Params:
#   $1 - name of the module build parameters are loaded for
loadBuildParams() {
  if [ -n "$FLAVOR" ]; then
    flavor=$(getFlavorBuildType $1 $FLAVOR)
  fi

  if [ -n "$BUILD_TYPE" ]; then
    build_type=$(getFlavorBuildType $1 $BUILD_TYPE)
  fi
}

# Runs tests for all modules defined in environmental variable MODULES. Searchers for specific test
# task using loadBuildParams with FLAVOR and BUILD_TYPE params. If they are found then they are
# added to test task. Resulting task can be like this:
# - module:test
# - module:testFlavor
# - module:testBuildType
# - module:TestFlavorBuildType
#
# Note: test task are run with --continue to run all test even on failure. Result should still be
# fail with information which modules failed and why.
runTestsForModules() {
  echo "Running tests for flavor: $FLAVOR and buildType: $BUILD_TYPE"
  gradleTasks=""
  IFS=', ' read -r -a modulesArray <<< "${MODULES}"
  for module in "${modulesArray[@]}"; do
    if [[ -n "${module// /}" ]]; then
      loadBuildParams $module
      gradleTasks="${gradleTasks}${module}:test${flavor^}${build_type^} "
    fi
  done
  echo "Running command '${CI_PROJECT_DIR}/gradlew --continue ${gradleTasks}'"
  $CI_PROJECT_DIR/gradlew --continue ${gradleTasks}
}

init
runTestsForModules
