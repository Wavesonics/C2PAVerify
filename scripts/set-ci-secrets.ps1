# Sets the GitHub Actions signing secrets for release.yml from the local keystores.
# Run locally with an authenticated `gh`. Keystores and passwords never leave your machine
# beyond the encrypted secret store; nothing here is committed with secret values.
#
#   ./scripts/set-ci-secrets.ps1
#
param(
    [string]$Repo = "Wavesonics/C2PAVerify"
)

$ErrorActionPreference = "Stop"
$root = Split-Path $PSScriptRoot -Parent

function Set-FileSecret($name, $path) {
    $full = Resolve-Path $path
    [Convert]::ToBase64String([IO.File]::ReadAllBytes($full)) | gh secret set $name --repo $Repo
    Write-Host "set $name (from $([IO.Path]::GetFileName($full)))"
}

function Set-SecretFromPrompt($name, $prompt, [switch]$Secret) {
    if ($Secret) {
        $sec = Read-Host $prompt -AsSecureString
        $bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($sec)
        try { $value = [Runtime.InteropServices.Marshal]::PtrToStringAuto($bstr) }
        finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr) }
    } else {
        $value = Read-Host $prompt
    }
    $value | gh secret set $name --repo $Repo
    Write-Host "set $name"
}

# Play upload keystore (Play-managed, F6:70) -> signs the AAB for the Play job.
Set-FileSecret      "PLAY_KEYSTORE_BASE64"    "$root\darkrock_googleplay_managed.keystore"
Set-SecretFromPrompt "PLAY_KEYSTORE_PASSWORD" "Managed keystore store password" -Secret
Set-SecretFromPrompt "PLAY_KEY_ALIAS"         "Managed keystore key alias"
Set-SecretFromPrompt "PLAY_KEY_PASSWORD"      "Managed keystore key password" -Secret

Write-Host "`nDone. Verify with: gh secret list --repo $Repo"
