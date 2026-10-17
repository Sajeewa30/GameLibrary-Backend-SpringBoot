<#
  Restores the game library from the Google Drive backup (made with the
  "Back up to Google Drive" button on the Stats page).

  - Downloads the database snapshot to data/gamelibrary.db. The current database,
    if any, is kept as backups/gamelibrary-before-restore-<timestamp>.db.
  - Downloads every image and video into uploads/. Files that already exist
    locally and are unchanged are skipped; nothing local is deleted.

  STOP THE BACKEND FIRST (Ctrl+C in its window), then run:

      .\scripts\restore-from-drive.ps1

  Needs rclone with the "gdrive" remote configured (see LOCAL-SETUP.md).
#>
param(
    [string]$Remote = $(if ($env:BACKUP_DRIVE_REMOTE) { $env:BACKUP_DRIVE_REMOTE } else { 'gdrive:GameLibrary' })
)
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot

$rclone = (Get-Command rclone -ErrorAction SilentlyContinue).Source
if (-not $rclone) {
    $rclone = Get-ChildItem "$env:LOCALAPPDATA\Microsoft\WinGet\Packages\Rclone.Rclone_*\rclone-*\rclone.exe" -ErrorAction SilentlyContinue |
        Select-Object -First 1 -ExpandProperty FullName
}
if (-not $rclone) { throw 'rclone not found. Install it with: winget install Rclone.Rclone' }

if (Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue) {
    throw 'Something is running on port 8080. Stop the backend before restoring.'
}

$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$dataDir = Join-Path $root 'data'
$db = Join-Path $dataDir 'gamelibrary.db'
New-Item -ItemType Directory -Force -Path $dataDir, (Join-Path $root 'backups'), (Join-Path $root 'uploads') | Out-Null

if (Test-Path $db) {
    $kept = Join-Path $root "backups\gamelibrary-before-restore-$stamp.db"
    Copy-Item $db $kept
    Write-Host "Current database kept as $kept"
}

Write-Host 'Downloading database...'
& $rclone copyto "$Remote/gamelibrary.db" $db --progress
if ($LASTEXITCODE -ne 0) { throw "Database download failed (rclone exit code $LASTEXITCODE)." }

Write-Host 'Downloading images and videos...'
& $rclone copy "$Remote/uploads" (Join-Path $root 'uploads') --progress --transfers 4
if ($LASTEXITCODE -ne 0) { throw "Uploads download failed (rclone exit code $LASTEXITCODE)." }

Write-Host 'Restore complete. Start the backend with .\scripts\run-local.ps1'
