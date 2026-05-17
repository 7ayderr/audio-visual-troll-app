# Push Prompt 9 to GitHub

Repo: https://github.com/7ayderr/audio-visual-troll-app

## Current status

| Location | Prompt 9 |
|----------|----------|
| **This folder** (`Desktop\weather`) | Complete — wired EXECUTE, freeze, effects, CI fix |
| **GitHub `main`** | Incomplete — stub `ExecutionEngine` with TODOs, `onExecute()` only sets "OK" |

## One-command sync (PowerShell)

```powershell
powershell -ExecutionPolicy Bypass -File "C:\Users\sevryn\Desktop\weather\sync-to-github.ps1"
```

That script clones your repo, copies all project files (except `build/`), commits, and pushes to `main`.

## Manual sync

```powershell
cd $env:USERPROFILE\Desktop
git clone https://github.com/7ayderr/audio-visual-troll-app.git
robocopy weather audio-visual-troll-app /E /XD .git build .gradle "app\build"
cd audio-visual-troll-app
git add -A
git commit -m "Complete Prompt 9 execution engine and fix CI gradlew"
git push origin main
```

## CI fix included

- `.gitattributes` — `gradlew` uses LF line endings
- `.github/workflows/build.yml` — uses `bash ./gradlew` (fixes "required file not found" on Ubuntu)
