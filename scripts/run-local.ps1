<#
  Starts the backend locally against the SQLite database (data/gamelibrary.db).
  Leave this window open while you use the app. Stop with Ctrl+C.

  The frontend should point its API base URL at http://localhost:8080
  (see LOCAL-SETUP.md).
#>
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
New-Item -ItemType Directory -Force -Path (Join-Path $root 'data') | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $root 'uploads') | Out-Null
& (Join-Path $root 'gradlew.bat') bootRun
