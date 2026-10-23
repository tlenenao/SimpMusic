# Debug APK from GitHub Actions

Workflow: **Debug APK** (`.github/workflows/debug-apk.yml`).

## Install on a phone (easiest)

Open **https://github.com/tlenenao/SimpMusic/releases/tag/nightly** in the phone's browser and download
`SimpMusic-arm64-v8a.apk` (most phones; `SimpMusic-universal.apk` if unsure), then open it and allow
"install unknown apps" for the browser. The release is replaced on every push to `dev`.
It installs next to the official app (package `com.maxrave.simpmusic.dev`, separate data).
Signed with the debug key: if an update refuses to install over the previous nightly, uninstall it first.

## Getting the APK from Actions

1. GitHub -> your fork -> **Actions** -> **Debug APK** -> open a successful run.
2. Under **Artifacts**, download `simpmusic-debug-apk-<commit>` (a zip containing the `.apk`).
   Artifacts are kept 14 days.

## Running it manually

Actions -> **Debug APK** -> **Run workflow** -> pick the branch. From a terminal:

```bash
gh workflow run "Debug APK" --ref dev
gh run watch
```

It also runs on every push to `dev` and on pull requests to `dev`.

## Logs

Open the run -> the **build** job -> expand a step. On failure the test and build reports are
uploaded as the `reports-<commit>` artifact. From a terminal: `gh run view --log-failed`.

## Installing

Unzip, then either copy the `.apk` to the phone and open it (allow "install unknown apps"), or:

```bash
adb install -r app-debug.apk
```

## Debug vs distributable

The debug APK is signed with the Android debug key, is not minified, and is debuggable. It has a
different signature than the official releases, so it cannot update an installed release (uninstall
first, which erases app data), and it is not for the Play Store or F-Droid. Release builds use the
repository's signing secrets (`android.yml`) which this workflow never touches.

Last.fm genre lookups need `LASTFM_API_KEY` / `LASTFM_SECRET` repository secrets; push and manual
runs use them if set, pull requests never get secrets. Without them only MusicBrainz is used.
