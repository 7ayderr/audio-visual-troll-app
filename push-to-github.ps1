$ErrorActionPreference = "Stop"
$repoDir = $PSScriptRoot
$log = Join-Path $repoDir "push-log.txt"

function Log($msg) {
    "$(Get-Date -Format o) $msg" | Tee-Object -FilePath $log -Append
}

$gitCandidates = @(
    "C:\Program Files\Git\cmd\git.exe",
    "C:\Program Files (x86)\Git\cmd\git.exe"
)
$git = $null
foreach ($c in $gitCandidates) {
    if (Test-Path $c) { $git = $c; break }
}
if (-not $git) {
    $cmd = Get-Command git -ErrorAction SilentlyContinue
    if ($cmd) { $git = $cmd.Source }
}
if (-not $git) {
    Log "ERROR: git not found. Install Git for Windows: https://git-scm.com/download/win"
    exit 1
}
Log "Using git: $git"

Set-Location $repoDir

# Git needs a name and email before it can commit (run once on your PC).
$name = (& $git config user.name 2>$null)
$email = (& $git config user.email 2>$null)
if (-not $name -or -not $email) {
    Log "ERROR: Git does not know your name/email yet."
    Log "Run these once in PowerShell (use your real name and GitHub email):"
    Log '  git config --global user.name "Your Name"'
    Log '  git config --global user.email "your-email@example.com"'
    Log "Then double-click PUSH_ME.bat again."
    exit 1
}

if (-not (Test-Path ".git")) {
    & $git init
    Log "git init done"
}

$remotes = & $git remote 2>$null
if ($remotes -notcontains "origin") {
    & $git remote add origin "https://github.com/7ayderr/audio-visual-troll-app.git"
    Log "remote origin added"
}

& $git add -A
Log "git add done"

$status = & $git status --porcelain
if (-not $status) {
    Log "Nothing new to commit. Trying push only..."
} else {
    & $git commit -m "Complete Prompt 9 execution engine and fix CI gradlew"
    if ($LASTEXITCODE -ne 0) {
        Log "ERROR: git commit failed (exit $LASTEXITCODE)."
        exit $LASTEXITCODE
    }
    Log "git commit done"
}

& $git branch -M main

# Merge with existing GitHub history if remote main exists
$remoteMain = & $git ls-remote --heads origin main 2>$null
if ($remoteMain) {
    & $git fetch origin main 2>&1 | Tee-Object -FilePath $log -Append
    $hasCommit = & $git rev-parse HEAD 2>$null
    if ($LASTEXITCODE -eq 0) {
        & $git pull origin main --allow-unrelated-histories --no-edit 2>&1 | Tee-Object -FilePath $log -Append
        if ($LASTEXITCODE -ne 0) {
            Log "WARN: pull had conflicts or failed. Fix conflicts, then run: git push -u origin main"
            exit $LASTEXITCODE
        }
        Log "Merged with origin/main"
    }
}

& $git push -u origin main 2>&1 | Tee-Object -FilePath $log -Append
if ($LASTEXITCODE -eq 0) {
    Log "PUSH OK: https://github.com/7ayderr/audio-visual-troll-app"
} else {
    Log "PUSH FAILED exit=$LASTEXITCODE. Sign in when prompted, or run: git push -u origin main"
    exit $LASTEXITCODE
}
