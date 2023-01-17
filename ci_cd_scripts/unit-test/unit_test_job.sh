#!/bin/bash

set -e pipefail

#
# Configures and runs unit tests based on env variables and test task list loaded using gradle task
# "listTestTasks".
#
configAndRunTests() {
  echo "Creating test task list"
  ./gradlew listTestTasks -q > $TASKS_LIST_FILE
  echo "Configuring unit tests"
  source $UNIT_TEST_DIR/config_unit_tests.sh
  echo "Running unit tests"
  $UNIT_TEST_DIR/run_unit_tests.sh
}

#
# Checks if source filtering should be skipped. It is used in test report merging to either report
# all source files or only for modules which have been changed. Due to this MR can show partial
# report only with proper percentage.
#
checkIgnoreSourceFiltering() {
  echo "Checking source filtering"
  ignoreSourceFiltering=false
  IFS='/' read -r -a brancheNameArray <<< "$CI_COMMIT_BRANCH"
  if [[ $SKIP_CHECKS_FOR_BRANCHES == *"${brancheNameArray[0]}"* ]]; then
    echo "Will ignore source filtering"
    ignoreSourceFiltering=true
  fi
}

#
# Merges coverage report using gradle task "mergeCoverageReports". Runs only when there are modules
# that did run tests, else it does nothing.
#
mergeCoverageReports() {
  echo "Merging unit test reports"
  if [[ ! -n "${MODULES// /}" ]]; then
    echo "Tests did not run = not merging reports -> finishing"
    exit 0
  fi

  ./gradlew mergeCoverageReports -PignoreSourceFiltering=$ignoreSourceFiltering
}

configAndRunTests
checkIgnoreSourceFiltering
mergeCoverageReports
