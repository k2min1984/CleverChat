param(
    [string]$BackupDir = $env:CLEVERCHAT_BACKUP_DIR,
    [int]$RetentionDays = 14
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($BackupDir)) {
    $BackupDir = Join-Path (Get-Location) "backups"
}

if ($env:CLEVERCHAT_BACKUP_RETENTION_DAYS -match "^\d+$") {
    $RetentionDays = [int]$env:CLEVERCHAT_BACKUP_RETENTION_DAYS
}

if ($RetentionDays -lt 7) {
    $RetentionDays = 7
}

$requiredEnv = @("PGHOST", "PGPORT", "PGDATABASE", "PGUSER", "PGPASSWORD")
foreach ($name in $requiredEnv) {
    if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($name))) {
        throw "Missing required environment variable: $name"
    }
}

New-Item -ItemType Directory -Force -Path $BackupDir | Out-Null

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$database = $env:PGDATABASE
$backupFile = Join-Path $BackupDir "cleverchat-$database-$timestamp.dump"
$checksumFile = "$backupFile.sha256"

pg_dump -Fc --no-owner --no-acl --file $backupFile $database

if ($LASTEXITCODE -ne 0) {
    throw "pg_dump failed with exit code $LASTEXITCODE"
}

$hash = Get-FileHash -Algorithm SHA256 -Path $backupFile
"$($hash.Hash.ToLowerInvariant())  $(Split-Path -Leaf $backupFile)" | Set-Content -Encoding ascii -Path $checksumFile

$cutoff = (Get-Date).AddDays(-$RetentionDays)
Get-ChildItem -Path $BackupDir -File -Include "*.dump", "*.dump.sha256" |
    Where-Object { $_.LastWriteTime -lt $cutoff } |
    Remove-Item -Force

Write-Host "Backup complete: $backupFile"
Write-Host "Checksum: $checksumFile"
