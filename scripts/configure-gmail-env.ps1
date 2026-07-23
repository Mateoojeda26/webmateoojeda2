param(
    [Parameter(Mandatory = $true)]
    [string]$CredentialFile
)

$resolved = (Resolve-Path -LiteralPath $CredentialFile).Path
$tempRoot = [IO.Path]::GetFullPath($env:TEMP).TrimEnd('\') + '\'
if (-not $resolved.StartsWith($tempRoot, [StringComparison]::OrdinalIgnoreCase)) {
    throw 'La ruta de credenciales debe estar dentro de la carpeta temporal.'
}

$credentials = Get-Content -Raw -LiteralPath $resolved | ConvertFrom-Json
if (
    [string]::IsNullOrWhiteSpace($credentials.clientId) -or
    [string]::IsNullOrWhiteSpace($credentials.clientSecret) -or
    [string]::IsNullOrWhiteSpace($credentials.encryptionKey)
) {
    throw 'El archivo temporal de OAuth está incompleto.'
}

[Environment]::SetEnvironmentVariable('GMAIL_CLIENT_ID', [string]$credentials.clientId, 'User')
[Environment]::SetEnvironmentVariable('GMAIL_CLIENT_SECRET', [string]$credentials.clientSecret, 'User')
[Environment]::SetEnvironmentVariable(
    'GMAIL_REDIRECT_URI',
    'http://localhost:8080/api/notifications/channels/gmail/callback',
    'User'
)
[Environment]::SetEnvironmentVariable(
    'CREDENTIAL_ENCRYPTION_KEY',
    [string]$credentials.encryptionKey,
    'User'
)

Remove-Item -LiteralPath $resolved -Force

$names = @(
    'GMAIL_CLIENT_ID',
    'GMAIL_CLIENT_SECRET',
    'GMAIL_REDIRECT_URI',
    'CREDENTIAL_ENCRYPTION_KEY'
)
foreach ($name in $names) {
    $value = [Environment]::GetEnvironmentVariable($name, 'User')
    [pscustomobject]@{
        Variable = $name
        Configurada = -not [string]::IsNullOrWhiteSpace($value)
    }
}
