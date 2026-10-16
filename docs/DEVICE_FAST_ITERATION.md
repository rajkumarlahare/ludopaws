# Device Fast Iteration

Use the `work/device-fast-iteration` branch for small, directly tested changes. Do not create pull requests for this workflow; commit changes to this branch and test them locally before proceeding.

## Initial setup (Linux)

From the local repository root:

```bash
git fetch origin
git switch work/device-fast-iteration 2>/dev/null || git switch --track -c work/device-fast-iteration origin/work/device-fast-iteration
git pull --ff-only origin work/device-fast-iteration
```

If this is a fresh clone, clone the repository first:

```bash
git clone https://github.com/rekixo-lab/ludopaws.git
cd ludopaws
git fetch origin
git switch --track -c work/device-fast-iteration origin/work/device-fast-iteration
```

## For each UI / Android change

Make sure the Android device is connected with USB debugging enabled and authorized:

```bash
adb devices
```

Update the current branch and build/install the debug APK:

```bash
git pull --ff-only origin work/device-fast-iteration
gradle -p android :app:installDebug
```

To explicitly build then install:

```bash
gradle -p android :app:assembleDebug
adb install -r android/app/build/outputs/apk/debug/app-debug.apk
```

The Android application ID is `com.rekixo.ludopaws`.

## Local repository update after a commit

Run this from the local repository root. Commit any unrelated local work before pulling; `--ff-only` avoids accidental merge commits.

```bash
git switch work/device-fast-iteration
git pull --ff-only origin work/device-fast-iteration
```

## Backend-only deploy (production)

Backend changes must be committed and pushed to `main` before using the existing production workflow, because that workflow explicitly refuses to deploy a feature branch. Do not deploy backend changes merely to test an Android UI change.

Preferred production process after the change has been reviewed and is intentionally approved for production:

1. Merge the reviewed backend commit into `main` using the repository's approved release process.
2. In GitHub, open **Actions → LudoProof Cloudflare Production → Run workflow**, select `main`, and dispatch it.
3. The workflow runs preflight/security checks, server tests, Wrangler dry-run, then deploys `server/` to the existing Worker `ludoproof-game-api`, and validates configuration/secrets.

Do not run `npx wrangler deploy` casually against production from a feature branch. Never copy Worker secrets into source code or chat. The local server checks, without publishing, are:

```bash
cd server
npm test
npx --yes wrangler@4.135.0 deploy --dry-run --outdir .wrangler-dry-run
```

## Commit-only workflow

For a change: implement the requested focused edit on this branch, run the relevant tests, commit, and push this branch. Do not open a pull request unless explicitly asked. The main production backend is separate from immediate device testing.
