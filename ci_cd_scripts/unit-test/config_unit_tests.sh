#!/bin/bash

# This script checks all modules and keeps only those that have been changed. It is used to run lanes only on
# code modifications and not entire repository. Supports run on branches and MR's where for branches it checks
# it agains previous code and for MR it uses current sha and target branch sha to get all the differences
# between them.
#
# This scipt can be skipped by setting SKIP_CHECKS_FOR_BRANCHES to skip these checks for specific branches.
#
# Options (variables):
#   MODULES - list of modules to runs these fastlanes on. Format: "module1, module2" (whitespace required).
#   SKIP_CHECKS_FOR_BRANCHES - list of branches to skip these tests for because on some types of branches these
#                              test must not run since it would not run all required tests.
#
# TEST VALUES:
#MODULES=":app:app-apk, :app:library"
#SKIP_CHECKS_FOR_BRANCHES="dev"

# Checks if this script can run. It checks current branch name if it is not set to be ignored in
# SKIP_CHECKS_FOR_BRANCHES variable. It checks only if first part (split by "/") is contained in
# skip variable.
init() {
  skip=0
  if [ -n "$CI_COMMIT_BRANCH" ]; then
    IFS='/' read -r -a brancheNameArray <<< "${CI_COMMIT_BRANCH}"
    if [[ $SKIP_CHECKS_FOR_BRANCHES == *"${brancheNameArray[0]}"* ]]; then
      echo "Skipping checks for current branch '$CI_COMMIT_BRANCH' due to branch skip configuration '$SKIP_CHECKS_FOR_BRANCHES'."
      skip=1
    fi
  fi
}

# Loads sha of target branch for MR's. Runs only if "CI_MERGE_REQUEST_TARGET_BRANCH_NAME" is set. It loads current sha
# and checks out into target branch to get it's sha and then it checks out back into current sha.
loadTargetBranchSha() {
  if [[ -n "$CI_MERGE_REQUEST_TARGET_BRANCH_NAME" ]]; then
    echo "Loading target branch ($CI_MERGE_REQUEST_TARGET_BRANCH_NAME) sha"
    currentBranchSha=$(git rev-parse HEAD)
    git fetch origin "$CI_MERGE_REQUEST_TARGET_BRANCH_NAME":"$CI_MERGE_REQUEST_TARGET_BRANCH_NAME"
    targetBranchSha=$(git show-ref --verify --hash refs/heads/"$CI_MERGE_REQUEST_TARGET_BRANCH_NAME")
    echo "Loading differences between target ($targetBranchSha) and current ($currentBranchSha) sha"
  else
    echo "Loading differences with previous HEAD"
  fi
}

# Loads differences between 2 sha's. If target and current branch sha is set (this script runs for MR's) if will load
# differences between them. Else it loads difference between current HEAD na previous HEAD.
loadDifferences() {
  if [[ -n "$targetBranchSha" && -n "$currentBranchSha" ]]; then
    git diff "$targetBranchSha" "$currentBranchSha" --name-only
  else
    git diff HEAD^ HEAD --name-only
  fi
}

# Modifies MODULES variable based on changed files in git. If module path is contained in list of
# changed files then it is kept in the list else it is removed and saved into IGNORED_MODULES
# variable.
checkModules() {
  mapfile -t gitDiffsArray < <(loadDifferences | cat | grep ".kt$")
  NEW_MODULES=""
  if [ -n "${gitDiffsArray[*]}" ]; then
    IFS=', ' read -r -a modulesArray <<< "${MODULES}"
    for module in "${modulesArray[@]}"; do
      modulePath=$(echo "$module" | tr : / | sed 's/^[/]//g')
      for diff in "${gitDiffsArray[@]}"; do
        if [[ $diff == *"$modulePath"* ]]; then
          NEW_MODULES="$NEW_MODULES, $module"
          break
        fi
      done
    done
  fi
  MODULES=$(echo "$NEW_MODULES" | sed 's/^[,][[:space:]]//g')
  export MODULES
}

init
if [ $skip -eq 0 ]; then
  echo "Modules before checking: $MODULES"
  loadTargetBranchSha
  checkModules
  echo "Modules after checking: $MODULES"
fi
