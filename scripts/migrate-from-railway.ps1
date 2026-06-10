<#
  ONE-TIME migration of your games from the old Railway MySQL database into the
  new local SQLite database.

  1) Find your Railway DB connection info: Railway dashboard -> your MySQL
     service -> "Variables" / "Connect". You need the public proxy host, port,
     database name (usually "railway") and the root password.
  2) Run, for example:

       .\scripts\migrate-from-railway.ps1 `
         -MysqlUrl "jdbc:mysql://metro.proxy.rlwy.net:46798/railway?sslMode=REQUIRED&allowPublicKeyRetrieval=true&serverTimezone=UTC" `
         -Password "YOUR_DB_PASSWORD"

  It exports every game from Railway to games-export.json, then imports them
  into data/gamelibrary.db. Safe to re-run: games already present are skipped.

  Requires firebase-service-account.json to be present (the app needs it to
  start). Read-only against Railway (ddl-auto=none) — it will not modify the
  old database.
#>
param(
    [Parameter(Mandatory = $true)][string]$MysqlUrl,
    [Parameter(Mandatory = $true)][string]$Password,
    [string]$Username = 'root',
    [string]$ExportFile = 'games-export.json'
)
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$gradlew = Join-Path $root 'gradlew.bat'
New-Item -ItemType Directory -Force -Path (Join-Path $root 'data') | Out-Null

Write-Host '==> Step 1/2: exporting games from Railway MySQL ...' -ForegroundColor Cyan
$env:SPRING_DATASOURCE_URL = $MysqlUrl
$env:SPRING_DATASOURCE_USERNAME = $Username
$env:SPRING_DATASOURCE_PASSWORD = $Password
$env:JPA_DIALECT = 'org.hibernate.dialect.MySQLDialect'
$env:JPA_DDL_AUTO = 'none'
$env:DB_POOL_MAX = '2'
& $gradlew bootRun "--args=--app.migrate.export=$ExportFile"

# Clear the MySQL overrides so step 2 falls back to the local SQLite defaults.
Remove-Item Env:SPRING_DATASOURCE_URL, Env:SPRING_DATASOURCE_USERNAME, Env:SPRING_DATASOURCE_PASSWORD, Env:JPA_DIALECT, Env:JPA_DDL_AUTO, Env:DB_POOL_MAX -ErrorAction SilentlyContinue

Write-Host '==> Step 2/2: importing games into local SQLite ...' -ForegroundColor Cyan
& $gradlew bootRun "--args=--app.migrate.import=$ExportFile"

Write-Host 'Migration finished. Start the app with: .\scripts\run-local.ps1' -ForegroundColor Green
