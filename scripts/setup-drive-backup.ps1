<#
  One-time setup for the "Back up to Google Drive" button: connects rclone to
  your Google Drive using YOUR OWN Google OAuth client (made in Google Cloud
  Console as a "Desktop app" client).

      .\scripts\setup-drive-backup.ps1

  It asks for the Client ID and Client secret (the secret is hidden as you
  type), then opens Google's sign-in in your browser. If Google says "Google
  hasn't verified this app", click Advanced -> Go to <your app> -> Allow.

  rclone only gets the drive.file permission: it can see and change the files
  it creates (the GameLibrary backup folder) and nothing else in your Drive.
#>
param(
    [string]$RemoteName = 'gdrive'
)
$ErrorActionPreference = 'Stop'

$rclone = (Get-Command rclone -ErrorAction SilentlyContinue).Source
if (-not $rclone) {
    $rclone = Get-ChildItem "$env:LOCALAPPDATA\Microsoft\WinGet\Packages\Rclone.Rclone_*\rclone-*\rclone.exe" -ErrorAction SilentlyContinue |
        Select-Object -First 1 -ExpandProperty FullName
}
if (-not $rclone) { throw 'rclone not found. Install it with: winget install Rclone.Rclone' }

# -q hides rclone's "config file not found" notice, which Windows PowerShell
# would otherwise turn into a terminating error under ErrorActionPreference=Stop.
$existing = & $rclone listremotes -q
if ($existing -contains "${RemoteName}:") {
    $answer = Read-Host "A remote named '$RemoteName' already exists. Replace it? (y/n)"
    if ($answer -notmatch '^[yY]') { Write-Host 'Nothing changed.'; exit 0 }
    & $rclone config delete $RemoteName
}

$clientId = (Read-Host 'Client ID (ends with .apps.googleusercontent.com)').Trim()
if ($clientId -notmatch '\.apps\.googleusercontent\.com$') {
    throw "That doesn't look like a Google client ID. Copy it from Google Cloud Console -> Clients."
}
$secure = Read-Host 'Client secret (hidden)' -AsSecureString
$bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
try {
    $clientSecret = ([Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)).Trim()
} finally {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
}
if (-not $clientSecret) { throw 'Client secret is empty.' }

Write-Host ''
Write-Host 'Opening Google sign-in in your browser. Sign in with the Google account'
Write-Host 'whose Drive should hold the backups, then click Allow...'
& $rclone config create $RemoteName drive scope=drive.file "client_id=$clientId" "client_secret=$clientSecret"
$exit = $LASTEXITCODE
$clientSecret = $null
if ($exit -ne 0) { throw "rclone config create failed (exit code $exit). See the message above." }

Write-Host ''
Write-Host 'Checking the connection...'
& $rclone about "${RemoteName}:"
if ($LASTEXITCODE -ne 0) { throw 'Connected, but Google Drive did not answer. See the message above.' }
Write-Host ''
Write-Host "Done. The '$RemoteName' remote is ready; restart the backend and use the button on the Stats page."
