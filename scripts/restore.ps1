<#
  Restores a backup .zip created by scripts/backup.ps1, replacing the current
  data/ and uploads/ folders. Stop the backend before running this.

      .\scripts\restore.ps1 -ZipPath .\backups\gamelibrary-backup-20260609-101500.zip

  Use this on a new laptop after cloning the repo to bring your data back.
#>
param(
    [Parameter(Mandatory = $true)][string]$ZipPath
)
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
if (-not (Test-Path $ZipPath)) { throw "Backup file not found: $ZipPath" }

Expand-Archive -Path $ZipPath -DestinationPath $root -Force
Write-Host "Restored data/ and uploads/ from $ZipPath"
