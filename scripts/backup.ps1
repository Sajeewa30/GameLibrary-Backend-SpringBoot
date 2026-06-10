<#
  Backs up your entire game library (the SQLite database + uploaded images)
  into a timestamped .zip under backups/.

  Optionally also copies the zip to another folder — e.g. a Google Drive or
  OneDrive synced folder — for off-laptop safety:

      .\scripts\backup.ps1 -Destination "G:\My Drive\GameLibraryBackups"

  Or set a default once (e.g. in your PowerShell profile):
      $env:GAMELIBRARY_BACKUP_DEST = "G:\My Drive\GameLibraryBackups"

  Tip: run this when the app is idle (not mid-upload) so the database file is
  in a clean state.
#>
param(
    [string]$Destination = $env:GAMELIBRARY_BACKUP_DEST
)
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'

$backupDir = Join-Path $root 'backups'
New-Item -ItemType Directory -Force -Path $backupDir | Out-Null

$sources = @()
foreach ($name in @('data', 'uploads')) {
    $p = Join-Path $root $name
    if (Test-Path $p) { $sources += $p }
}
if ($sources.Count -eq 0) {
    Write-Warning 'Nothing to back up yet (no data/ or uploads/ folder).'
    exit 0
}

$zip = Join-Path $backupDir "gamelibrary-backup-$stamp.zip"
Compress-Archive -Path $sources -DestinationPath $zip -Force
Write-Host "Backup created: $zip"

if ($Destination) {
    New-Item -ItemType Directory -Force -Path $Destination | Out-Null
    Copy-Item -Path $zip -Destination $Destination -Force
    Write-Host "Copied backup to: $Destination"
}
