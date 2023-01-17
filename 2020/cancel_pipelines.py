#!/usr/bin/python3 -u
'''
This script tries to cancel all previous pipelines for specific ref (usually a
branch). It does not cancel currently running pipeline and latest.

Logic:
- Loads reference from currently running pipeline.
- Loads list of running pipelines for specific reference. If there are no running
  pipelines then this script ends.
- Loads latest pipeline sha to make sure it will not get canceled.
- Goes thought the list of currently running pipelines to cancel all of them except
  latest and currently running one.

How to use:
- Copy this code to your repository or used docker image.
- Run it in your .gitlab-ci.yml file, possibly in `before_script`.

Requirements:
- A key for the Gitlab API, with at least maintainer role, must be available and
  configured as ENV var 'GITLAB_API_TOKEN'.
- Python 3.

Limitations:
- Does not check if the pipeline was actually canceled.
'''

import os
import time
import json
from urllib.request import Request, urlopen

# Env vars
envs = {
    'project_id': 'CI_PROJECT_ID',
    'this_pipeline_id': 'CI_PIPELINE_ID',
    'api_token': 'GITLAB_API_TOKEN'
}

# Global vars
current_ref = None
running_pipelines = None

# Loads anc checks all environmental variables used in this script.
# Exists the script if any ENV variable is missing.
def load_env_vars():
    print("Loading env variables")
    fail = False
    for key, env_var_name in envs.items():
        env_var_value = os.environ.get(env_var_name)
        if not env_var_value:
            print('Error: %s not set.' % env_var_name)
            fail = True
        envs[key] = env_var_value
    if fail:
        exit(1)

# Creates a curl call to Gitlab API.
# Used to get pipeline list and their cancellation.
def curl(url: str, post=False):
    print("Running curl call: %s" % url)
    req = Request(url)
    req.add_header('PRIVATE-TOKEN', envs['api_token'])
    content = urlopen(req, data=bytes() if post else None).read()
    return json.loads(content.decode('utf-8'))

# Loads current ref from currently running pipeline. Variable CI_COMMIT_REF_NAME cannot be
# used because it would not work with detached pipelines.
def load_current_ref():
    global current_ref
    print("Getting ref from current pipeline")
    current_ref = curl(base_url + envs['this_pipeline_id'])['ref']

# Lists of all running pipelines for current ref (usually branch).
def load_running_pipelines_for_ref() -> list:
    global running_pipelines
    print("Getting list of running pipelines for ref: %s" % current_ref)
    running_pipelines = curl(base_url + '?per_page=100&status=running&ref=%s' % current_ref)

# Gets latest pipeline for specific ref (usually branch). This is used to load latest sha.
def latest_pipeline_for_ref() -> list:
    print("Getting latest pipeline for ref: %s" % current_ref)
    return curl(base_url + '?per_page=1&ref=%s' % current_ref)

# Gets latest commit sha from latest pipeline.
def get_latest_commit_sha() -> str:
   pipeline = latest_pipeline_for_ref()
   if not pipeline:
        print("Failed to load latest commit sha")
        exit(1)
   return pipeline[0]['sha']

# Cancels specific pipeline. Runs POST using curl.
def cancel_pipeline(pipeline_id: str) -> dict:
    return curl(base_url + pipeline_id + '/cancel', post=True)

# Goes thought the list of pipelines and cancels all except latest and current one.
def run_cancel_pipelines(latest_sha: str):
    global running_pipelines
    for pipeline in running_pipelines:
        pipeline_id = pipeline['id']
        if  pipeline['sha'] != latest_sha and str(pipeline_id) != envs['this_pipeline_id']:
            print('Canceling pipeline with id: %s' % pipeline_id)
            result = cancel_pipeline(str(pipeline_id))
            print('Cancel result: %s' % result)
        else:
            print('Ignoring current pipeline (id: %s) ' % pipeline_id)

# Checks running pipelines and tries to cancel old ones.
def cancel_old_pipelines():
    global running_pipelines
    load_current_ref()
    load_running_pipelines_for_ref()
    if not running_pipelines:
        print("There are no running pipelines -> Finishing")
        exit(0)

    latest_sha = get_latest_commit_sha()
    print("Latest sha: %s", latest_sha)
    run_cancel_pipelines(latest_sha)

if __name__ == "__main__":
    load_env_vars()
    base_url = 'https://gitlab.eman.cz/api/v4/projects/%s/pipelines/' % envs['project_id']
    cancel_old_pipelines()
    print("Cancelation complete")
