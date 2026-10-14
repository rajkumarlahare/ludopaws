# AI-Assisted Development Workflow

## Project status

Ludo Paws is still under active development. It has **never been uploaded to the Google Play Store production track**. Do not describe the project as production-ready until the remaining release, Play Console, billing, ads, privacy, deployment, and real-device checks have been completed. Do not deploy or publish without explicit approval.

## Branch workflow

- `main` is the stable integration branch.
- `dev/workbench` is the active collaboration branch for ongoing implementation and quick local testing.
- Make small related commits on `dev/workbench`; do not create a new PR for every tiny edit.
- The developer syncs the branch locally, runs focused tests, and shares any error output or screenshots. Fixes go back onto the same branch.
- Open one PR from `dev/workbench` to `main` when a coherent batch is ready.
- Run the full CI on that PR before merging. Prefer a normal merge commit for ongoing use of `dev/workbench` so its commit ancestry stays useful.
- The post-merge CI on `main` may run while the next independent task is being worked on; do not falsely report it as passed until it finishes.

## Local sync

First use:
```powershell
git fetch origin
git switch --track origin/dev/workbench
```

For later updates:
```powershell
git switch dev/workbench
git pull --ff-only origin dev/workbench
```

If the local branch already exists, use the second command sequence. Commit or stash unrelated local changes before switching branches.

## Testing policy

- For each small change, run only the focused local test/build relevant to the changed area, plus a quick manual check where useful.
- Avoid repeatedly triggering full Android instrumentation, release APK/AAB packaging, and all server tests for unrelated tiny edits.
- At the end of a coherent batch, run the complete PR CI and review the result before merging.
- Never claim a test passed unless its actual result is visible and successful.

## Local-workspace limitation

GitHub-assisted edits update the remote branch. They do not directly change files on the developer's PC; `git pull` is the sync step. A connected local IDE/agent workspace would be needed for true direct local edits.
