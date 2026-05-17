$ErrorActionPreference = "Stop"
$src = "C:\Users\sevryn\Desktop\weather"
$dst = "C:\Users\sevryn\Desktop\audio-visual-troll-app"
$repo = "https://github.com/7ayderr/audio-visual-troll-app.git"

if (Test-Path $dst) { Remove-Item $dst -Recurse -Force }
git clone $repo $dst
if ($LASTEXITCODE -ne 0) { throw "git clone failed: $LASTEXITCODE" }

$exclude = @(".git", "build", ".gradle", "app\build")
Get-ChildItem $src -Recurse -File | ForEach-Object {
    $rel = $_.FullName.Substring($src.Length + 1)
    $skip = $false
    foreach ($e in $exclude) {
        if ($rel -like "$e*") { $skip = $true; break }
    }
    if ($skip) { return }
    $target = Join-Path $dst $rel
    $dir = Split-Path $target -Parent
    if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir -Force | Out-Null }
    Copy-Item $_.FullName $target -Force
}

Set-Location $dst
git add -A
git status
git commit -m "Complete Prompt 9 execution engine and fix CI gradlew"
git push origin main

Write-Host "DONE" | Out-File "C:\Users\sevryn\Desktop\weather\sync-result.txt"
