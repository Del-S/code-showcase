# Fast lint script.
#
# This script runs lints: Ktlint, Detekt and Android Lint without using Gradle. Using Gradle makes
# the checks very slow. It also runs checks only for modified Kotlin (.kt) and XML (.xml) files.
#
# This script requires:
#   - Ruby version 2+.
#   - Ruby json.
#   - Git CLI version 2+.
#   - Ktlint CLI version 0.36.0+.
#   - Detekt CLI version 1.6.0+.
#   - Lint (Android) CLI version 26+.
# Required environmental variable:
#   - CI_PROJECT_DIR used to modify reports path to make sure reports are created in project root.
#   - CI_MERGE_REQUEST_TARGET_BRANCH_NAME containing target branch name (ex: dev). This is only set
#     for merge request pipelines.
#
# This script first loads current branch and target branch commit sha (using git). If any of them
# are empty this script ends. After that it loads file differences using git (difference between
# two commits). Each file will have file path modified from relative to absolute and After that it
# runs lints in this order Ktlint -> Detekt -> Android Lint.

require 'fileutils'

# Global variables
$current_branch_sha = nil
$target_branch_sha = nil
$modified_files = nil
$modified_xml_files = nil
$reports_path = "#{ENV['CI_PROJECT_DIR']}/build/reports"

# Loads commit sha from current branch. It is used later but also here as an anchor to
# return git to because this script also loads commit sha from target branch by checking
# into it and parsing HEAD. After this is done current branch sha is used to return.
#
# Requires environmental variables: CI_MERGE_REQUEST_TARGET_BRANCH_NAME. Target branch
# env variable is only related (set) to merge request.
def load_target_branch_sha()
  puts("Loading target branch sha")
  target_branch_name = ENV['CI_MERGE_REQUEST_TARGET_BRANCH_NAME']
  $current_branch_sha = `git rev-parse HEAD`.gsub("\n","")
  `git fetch origin #{target_branch_name}:#{target_branch_name}`
  $target_branch_sha = `git show-ref --verify --hash refs/heads/#{target_branch_name}`.gsub("\n","")
end

# Checks commit hashes that are used for getting modified files. They are loaded using
# "load_target_branch_sha function". After they are loaded they are checked if they are
# empty. If they are then this script ends, else they are printed and script continues.
def check_commit_hashes()
  puts("Loading commit hashes")
  load_target_branch_sha()
  if $current_branch_sha.empty? or $target_branch_sha.empty?
    puts("Failed to load commit hashes")
    exit(0)
  else
    puts("Current commit: #{$current_branch_sha}")
    puts("Target commit: #{$target_branch_sha}")
  end
end

# Creates a directory where lint reports will be saved. Makes sure that the directory exists before
# Lint scripts run. Mainly because of Android Lint since it had problems creating directory.
def create_reports_directory()
  puts("Checking reports directory")
  unless File.directory?($reports_path)
    puts("Creating reports directory #{$reports_path}")
    FileUtils.mkdir_p($reports_path)
  end
end

# Loads modified Kotlin and XML files. Checks differences between two commits. It also does
# not load deleted files differences since the files cannot be linted anymore (it does not exist).
def load_modified_files()
  puts("Loading modified files using git")
  $modified_files = `git diff --name-only --diff-filter=d #{$target_branch_sha} #{$current_branch_sha} | cat | grep ".kt$"`
  $modified_xml_files = `git diff --name-only --diff-filter=d #{$target_branch_sha} #{$current_branch_sha} | cat | grep ".xml$"`
end

# Loops through all modified files and completes their paths. Changing them from relative to
# absolute. Detekt cli has problems when this script is not run in root of the project. That
# is why path need to be absolute so it could find modified files.
# Each line is composed of: CI_PROJECT_DIR + / + original file path.
def modified_files_absolute_path()
  result = ""
  $modified_files.each_line do |line|
    line.prepend("#{ENV['CI_PROJECT_DIR']}", "/")
    result << line
  end
  $modified_files = result

  result = ""
  $modified_xml_files.each_line do |line|
    line.prepend("#{ENV['CI_PROJECT_DIR']}", "/")
    result << line
  end
  $modified_xml_files = result
end

# Runs Ktlint check on modified Kotlin files. If there are no modified Kotlin files then
# this function creates a dummy report file so it does not fail following scripts.
def run_ktlint()
  if !$modified_files.empty?
    puts("Running Ktlint for modified files")
    files = $modified_files.gsub("\n"," ")
    `ktlint #{files} --android --reporter=checkstyle,output=#{$reports_path}/ktlint.xml`
  else
    puts("Ktlint skipped (no modified Kotlin files)")
    File.new("#{$reports_path}/ktlint.xml", "w")
  end
end

# Runs Detekt check on modified Kotlin files. If there are no modified Kotlin files then
# this function creates a dummy report file so it does not fail following scripts.
def run_detekt()
  if !$modified_files.empty?
    puts("Running Detekt for modified files")
    files = $modified_files.gsub("\n",",").delete_suffix(",")
    `detekt --input #{files} --config #{ENV['CI_PROJECT_DIR']}/config/lint/detekt.yml --report xml:#{$reports_path}/detekt.xml`
  else
    puts("Detekt skipped (no modified Kotlin files)")
    File.new("#{$reports_path}/detekt.xml", "w")
  end
end

# Runs Android Lint check on modified Kotlin and XML files. If there are no modified files then
# this function creates a dummy report file so it does not fail following scripts.
def run_android_lint()
  if !$modified_xml_files.empty? or !$modified_files.empty?
    puts("Running Android Lint for modified files")
    files = $modified_files.gsub("\n"," ")
    xml_files = $modified_xml_files.gsub("\n"," ")
    File.new("#{$reports_path}/lint.xml", "w")
    `lint #{files} #{xml_files} --config #{ENV['CI_PROJECT_DIR']}/config/lint/lint.xml --xml #{$reports_path}/lint.xml`
  else
    puts("Android Lint skipped (no modified XML files)")
    File.new("#{$reports_path}/lint.xml", "w")
  end
end

# Main script.
Dir.chdir("#{ENV['CI_PROJECT_DIR']}") do
  check_commit_hashes()
  load_modified_files()
  modified_files_absolute_path()
  create_reports_directory()
  run_ktlint()
  run_detekt()
  run_android_lint()
end
