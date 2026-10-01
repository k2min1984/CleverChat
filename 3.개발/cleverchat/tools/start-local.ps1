#requires -Version 5.1
<# Local Windows setup/launcher. See ../../../README.md. Never use for production. #>
[CmdletBinding()]
param(
    [ValidateRange(1024, 65535)][int]$Port = 8080,
    [ValidateRange(1024, 65535)][int]$DatabasePort = 5433,
    [ValidatePattern('^cleverchat-[a-z0-9-]+$')][string]$DatabaseContainer = 'cleverchat-postgres',
    [switch]$CheckOnly,
    [switch]$SkipBuild,
    [switch]$NonInteractive
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$script:AppRoot = Split-Path -Parent $PSScriptRoot
$script:Container = $DatabaseContainer
# V20-V23 contain a fixed search_path. Do not offer unsafe arbitrary-schema installation.
$Schema = 'cleverchat_dev'

function ConvertTo-NativeArgument([string]$Value) {
    # Windows CommandLineToArgvW quoting; no shell evaluation.
    if ($Value -ne '' -and $Value -notmatch '[\s"]') { return $Value }
    return '"' + [regex]::Replace([regex]::Replace($Value, '(\\*)"', '$1$1\"'), '(\\+)$', '$1$1') + '"'
}

function Invoke-Captured {
    param([string]$File, [string[]]$Arguments, [string]$InputText = '',
          [hashtable]$ChildEnvironment = @{}, [int]$TimeoutSeconds = 30)
    $info = New-Object System.Diagnostics.ProcessStartInfo
    $info.FileName = $File
    $info.Arguments = ($Arguments | ForEach-Object { ConvertTo-NativeArgument $_ }) -join ' '
    $info.UseShellExecute = $false
    $info.CreateNoWindow = $true
    $info.RedirectStandardOutput = $true
    $info.RedirectStandardError = $true
    $info.RedirectStandardInput = $true
    $info.StandardOutputEncoding = New-Object System.Text.UTF8Encoding($false)
    $info.StandardErrorEncoding = New-Object System.Text.UTF8Encoding($false)
    foreach ($key in $ChildEnvironment.Keys) { $info.EnvironmentVariables[$key] = $ChildEnvironment[$key] }
    $process = New-Object System.Diagnostics.Process
    $process.StartInfo = $info
    try {
        [void]$process.Start()
        $stdout = $process.StandardOutput.ReadToEndAsync()
        $stderr = $process.StandardError.ReadToEndAsync()
        if ($InputText) {
            $inputBytes = [Text.Encoding]::UTF8.GetBytes($InputText)
            $process.StandardInput.BaseStream.Write($inputBytes, 0, $inputBytes.Length)
            $process.StandardInput.BaseStream.Flush()
        }
        $process.StandardInput.Close()
        if (-not $process.WaitForExit($TimeoutSeconds * 1000)) {
            $process.Kill()
            throw "Command timed out: $([IO.Path]::GetFileName($File)). Check the service/network and retry."
        }
        return [pscustomobject]@{ Code = $process.ExitCode; Out = $stdout.Result.Trim(); Error = $stderr.Result.Trim() }
    } finally { $process.Dispose() }
}

function Get-RequiredTool([string]$Name, [string]$Hint) {
    $command = Get-Command $Name -CommandType Application -ErrorAction SilentlyContinue | Select-Object -First 1
    if (-not $command) { throw "$Name was not found. $Hint" }
    return $command.Source
}

function Invoke-Database([string]$Sql) {
    $result = Invoke-Captured $script:Docker @('exec', '-i', '-e', 'PGPASSWORD', $script:Container,
        'psql', '-h', $script:DatabaseHost, '-U', 'cleverchat', '-d', 'cleverchat', '-X', '-q', '-A', '-t',
        '-v', 'ON_ERROR_STOP=1') -InputText $Sql -ChildEnvironment @{ PGPASSWORD = $script:DbPassword }
    if ($result.Code -ne 0) {
        throw "PostgreSQL query failed. Check the existing DB password/schema; no DB reset was performed. $($result.Error)"
    }
    return $result.Out
}

function Assert-FreePort([int]$Number) {
    $listener = New-Object System.Net.Sockets.TcpListener([Net.IPAddress]::Loopback, $Number)
    try { $listener.Start() }
    catch { throw "Port $Number is in use. Keep the existing server or choose -Port 8081. No process was stopped." }
    finally { $listener.Stop() }
}

function Get-DatabaseState([string]$Name) {
    # Name is validated by the public parameter before it is used as a SQL identifier.
    $tableCount = [int](Invoke-Database "SELECT count(*) FROM pg_tables WHERE schemaname='$Name';")
    $history = Invoke-Database "SELECT to_regclass('$Name.flyway_schema_history') IS NOT NULL;"
    if ($tableCount -gt 0 -and $history -ne 't') {
        throw "Schema '$Name' contains tables without Flyway history. Choose an empty schema or ask the DB owner to review it."
    }
    if ($history -eq 't') {
        $failed = [int](Invoke-Database "SELECT count(*) FROM $Name.flyway_schema_history WHERE NOT success;")
        if ($failed -gt 0) { throw 'A failed Flyway migration exists. Inspect it before retrying; history was not changed.' }
        $latest = Invoke-Database "SELECT version FROM $Name.flyway_schema_history WHERE type='SQL' ORDER BY installed_rank DESC LIMIT 1;"
        Write-Host "[OK] Existing schema: $Name; latest applied SQL version: $latest"
    } else { Write-Host "[OK] Empty schema: $Name. Flyway will install the tables at startup." }
    $userTable = Invoke-Database "SELECT to_regclass('$Name.tb_user') IS NOT NULL;"
    # Pre-V20 installations used users. They are upgrades, not new installs.
    $legacyUsers = Invoke-Database "SELECT to_regclass('$Name.users') IS NOT NULL;"
    if ($userTable -eq 't') { return [int](Invoke-Database "SELECT count(*) FROM $Name.tb_user;") }
    if ($legacyUsers -eq 't') { return [int](Invoke-Database "SELECT count(*) FROM $Name.users;") }
    return 0
}

function Test-BasicData([switch]$Strict) {
    $checkFile = Join-Path $script:AppRoot 'deploy/seed/check-basic-data.sql'
    if (-not (Test-Path -LiteralPath $checkFile)) {
        throw 'deploy/seed/check-basic-data.sql is missing. Download the complete project before running setup.'
    }
    $sql = [IO.File]::ReadAllText($checkFile, [Text.Encoding]::UTF8)
    $report = Invoke-Database $sql | ConvertFrom-Json
    $counts = $report.counts
    Write-Host "[INFO] Base data: roles=$($counts.tb_role), auth groups=$($counts.tb_auth), menus=$($counts.tb_menu), codes=$($counts.tb_code), menu permissions=$($counts.tb_auth_menu_adm), users=$($counts.tb_user)."
    $differences = @()
    foreach ($issue in $report.issues.PSObject.Properties) {
        $items = @($issue.Value)
        if ($items.Count -gt 0) {
            $differences += "$($issue.Name): $($items -join ', ')"
        }
    }
    if ($differences.Count -gt 0) {
        $message = 'Base data differs from the installation baseline. ' + ($differences -join '; ')
        if ($Strict) { throw "$message See deploy/seed/README.md. No existing data was overwritten." }
        Write-Warning "$message Review intentional customization or missing data in deploy/seed/README.md; permissions were not restored automatically."
    } else {
        Write-Host '[OK] Default menus/codes/roles/menu permissions and the administrator link are ready.'
    }
}

function Read-InitialAccount {
    $username = $env:CLEVERCHAT_SETUP_ADMIN_USERNAME
    $password = $env:CLEVERCHAT_SETUP_ADMIN_PASSWORD
    if (-not $username) {
        if ($NonInteractive) { throw 'Empty installation: set CLEVERCHAT_SETUP_ADMIN_USERNAME and CLEVERCHAT_SETUP_ADMIN_PASSWORD, then retry.' }
        $username = Read-Host 'First local administrator ID (letters, digits, dot, underscore, hyphen)'
    }
    if ($username -notmatch '^[A-Za-z0-9][A-Za-z0-9_.-]{2,49}$') {
        throw 'Administrator ID must be 3-50 characters: letters, digits, dot, underscore, hyphen.'
    }
    if (-not $password) {
        if ($NonInteractive) { throw 'Set CLEVERCHAT_SETUP_ADMIN_PASSWORD for the first administrator.' }
        $secure = Read-Host 'First administrator password (at least 12 characters)' -AsSecureString
        $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
        try { $password = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer) }
        finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer); $secure.Dispose() }
    }
    if ($password.Length -lt 12 -or [Text.Encoding]::UTF8.GetByteCount($password) -gt 72) {
        throw 'Use a password of at least 12 characters and no more than 72 UTF-8 bytes.'
    }
    return @{ Username = $username; Password = $password }
}

function New-AsciiMapping {
    foreach ($letter in 'ZYXWVUTSRQPONMLKJIHGFED'.ToCharArray()) {
        $drive = "$letter`:"
        if ((Get-PSDrive -Name $letter -ErrorAction SilentlyContinue) -or (Test-Path "$drive\")) { continue }
        $result = Invoke-Captured "$env:SystemRoot\System32\subst.exe" @($drive, (Split-Path -Parent $script:AppRoot))
        if ($result.Code -eq 0) { return $drive }
    }
    throw 'No free drive letter is available for the temporary build path. Free one and retry.'
}

function Start-LocalApplication {
    if ($Port -eq $DatabasePort) { throw 'Application and database ports must be different.' }
    $overrides = @(Get-ChildItem Env: | Where-Object {
        $_.Name -match '^SPRING_(DATASOURCE_|FLYWAY_|CONFIG_|APPLICATION_JSON$)'
    })
    if ($overrides.Count -gt 0) {
        throw 'Direct Spring datasource/Flyway/config overrides are present. Use a fresh terminal or the manual guide; this launcher must use the DB it checked.'
    }
    $expectedUrl = "jdbc:postgresql://localhost:$DatabasePort/cleverchat?currentSchema=$Schema,public"
    if ($env:DB_URL -and $env:DB_URL -ne $expectedUrl) {
        throw 'DB_URL points outside this local preset. Open a fresh terminal or follow the manual environment guide. No connection settings were changed.'
    }
    if ($env:DB_USER -and $env:DB_USER -ne 'cleverchat') { throw 'This local launcher requires DB_USER=cleverchat. Use the manual guide for other databases.' }
    if ($env:SPRING_PROFILES_ACTIVE -and $env:SPRING_PROFILES_ACTIVE -ne 'dev') { throw 'This launcher is for dev only. Open a fresh terminal; stage/prod are not supported.' }

    Write-Host '[1/6] Checking JDK 17 and Docker Compose...'
    if ($env:JAVA_HOME) {
        $java = Join-Path $env:JAVA_HOME 'bin/java.exe'
        $javac = Join-Path $env:JAVA_HOME 'bin/javac.exe'
        if (-not (Test-Path $java) -or -not (Test-Path $javac)) { throw 'JAVA_HOME must point to a JDK 17 installation, not a JRE.' }
    } else {
        $java = Get-RequiredTool 'java.exe' 'Install JDK 17, then open a new terminal.'
        $javac = Get-RequiredTool 'javac.exe' 'Install JDK 17, then open a new terminal.'
    }
    foreach ($tool in @($java, $javac)) {
        $version = Invoke-Captured $tool @('-version')
        if ($version.Code -ne 0 -or ($version.Out + $version.Error) -notmatch '(version\s+"|javac\s+)17[.]') {
            throw 'Both java and javac must be JDK 17. Check JAVA_HOME and PATH.'
        }
    }
    $script:Docker = Get-RequiredTool 'docker.exe' 'Install/start Docker Desktop (Linux containers), then open a new terminal.'
    $compose = Invoke-Captured $script:Docker @('compose', 'version')
    if ($compose.Code -ne 0) { throw 'Docker Compose v2 is required (docker compose version).' }
    $engine = Invoke-Captured $script:Docker @('info', '--format', '{{.OSType}}')
    if ($engine.Code -ne 0 -or $engine.Out -ne 'linux') { throw 'Start Docker Desktop and use Linux containers. Retry after the engine is ready.' }
    $context = Invoke-Captured $script:Docker @('context', 'inspect', '--format', '{{.Endpoints.docker.Host}}')
    if ($context.Code -ne 0 -or $context.Out -notmatch '^npipe://') { throw 'Select a local Docker Desktop context. Remote Docker hosts are not supported by this launcher.' }
    Write-Host '[OK] JDK 17 and local Docker engine.'

    Write-Host '[2/6] Checking local PostgreSQL container and TCP authentication...'
    $inspect = Invoke-Captured $script:Docker @('container', 'inspect', $script:Container)
    if ($inspect.Code -ne 0) {
        if ($CheckOnly) { throw 'PostgreSQL is not installed yet. Run without -CheckOnly to create the local Compose service.' }
        Assert-FreePort $DatabasePort
        $composeArguments = @('compose', '-f', (Join-Path $script:AppRoot 'docker/docker-compose.yml'))
        # A separate container gets its own Compose project and volume (useful for clean install verification).
        if ($script:Container -ne 'cleverchat-postgres') { $composeArguments += @('--project-name', $script:Container) }
        $composeArguments += @('up', '-d', 'postgres')
        Write-Host '[INFO] Creating the local DB service; the first image download may take several minutes...'
        $up = Invoke-Captured $script:Docker $composeArguments -ChildEnvironment @{
            CLEVERCHAT_LOCAL_DB_CONTAINER_NAME = $script:Container
            CLEVERCHAT_LOCAL_DB_PORT = "$DatabasePort"
        } -TimeoutSeconds 600
        if ($up.Code -ne 0) { throw "Compose could not start PostgreSQL: $($up.Error)" }
        $inspect = Invoke-Captured $script:Docker @('container', 'inspect', $script:Container)
    }
    if ($inspect.Code -ne 0) { throw 'Could not inspect the PostgreSQL container.' }
    $containerInfo = @($inspect.Out | ConvertFrom-Json)[0]
    if ($containerInfo.Config.Image -notmatch '^postgres:16([.-]|$)') { throw 'The existing cleverchat-postgres container is not the expected postgres:16 image. It was not changed.' }
    $bindings = @($containerInfo.HostConfig.PortBindings.'5432/tcp')
    if (-not ($bindings | Where-Object { $_.HostPort -eq "$DatabasePort" -and $_.HostIp -in @('', '0.0.0.0', '127.0.0.1') })) {
        throw "The existing DB container must expose port 5432 on local port $DatabasePort. Review its mapping; it was not recreated."
    }
    if (-not $containerInfo.State.Running) {
        if ($CheckOnly) { throw 'PostgreSQL is stopped. Run without -CheckOnly to start the existing container.' }
        $started = Invoke-Captured $script:Docker @('start', $script:Container)
        if ($started.Code -ne 0) { throw "Could not start PostgreSQL: $($started.Error)" }
    }
    $ready = $false
    $deadline = [DateTime]::UtcNow.AddSeconds(60)
    do {
        $probe = Invoke-Captured $script:Docker @('exec', $script:Container, 'pg_isready', '-h', '127.0.0.1', '-U', 'cleverchat', '-d', 'cleverchat') -TimeoutSeconds 5
        if ($probe.Code -eq 0) { $ready = $true; break }
        if ($CheckOnly) { break }
        Start-Sleep -Seconds 2
    } while ([DateTime]::UtcNow -lt $deadline)
    if (-not $ready) { throw "PostgreSQL is not ready. Check docker logs $script:Container and retry." }
    # Container loopback can use pg_hba trust. Use its network address to verify the password.
    $networkInspect = Invoke-Captured $script:Docker @('container', 'inspect', $script:Container)
    if ($networkInspect.Code -ne 0) { throw 'Could not inspect the running DB network.' }
    $runningContainer = @($networkInspect.Out | ConvertFrom-Json)[0]
    $script:DatabaseHost = $runningContainer.NetworkSettings.Networks.PSObject.Properties.Value |
        ForEach-Object { $_.IPAddress } | Where-Object { $_ } | Select-Object -First 1
    if (-not $script:DatabaseHost) { throw 'No local DB network address was found.' }
    # Default is the repository's local Compose password, never a production credential.
    $script:DbPassword = if ($env:DB_PASSWORD) { $env:DB_PASSWORD } else { 'cleverchat' }
    $databaseVersion = Invoke-Database "SELECT current_setting('server_version_num')::integer / 10000;"
    if ($databaseVersion -ne '16') { throw 'This setup requires PostgreSQL 16.' }
    $userCount = Get-DatabaseState $Schema
    Assert-FreePort $Port
    if ($CheckOnly) {
        $settingsPresent = Invoke-Database "SELECT to_regclass('$Schema.tb_system_settings') IS NOT NULL;"
        if ($settingsPresent -eq 't') { Test-BasicData }
        else { Write-Host '[INFO] Base data will be checked after Flyway initializes/upgrades the schema.' }
        Write-Host "[OK] Read-only checks passed. Existing users: $userCount. Port $Port is free. No build/server/DB changes performed."
        return
    }

    $account = $null
    if ($userCount -eq 0) { $account = Read-InitialAccount }
    else { Write-Host '[OK] Existing accounts will be reused. No account/password/role changes requested.' }

    $mapping = $null
    $application = $null
    $originalLocation = Get-Location
    $savedEnvironment = @{}
    try {
        # Avoid a drive root: older Maven .cmd quotes its trailing backslash incorrectly.
        # Mapping the parent also gives forked Java tools an ASCII project working path.
        $mapping = New-AsciiMapping
        $runRoot = "$mapping\$(Split-Path -Leaf $script:AppRoot)"
        Set-Location $runRoot
        Write-Host '[3/6] Preparing the application JAR...'
        if (-not $SkipBuild) {
            $previousPreference = $ErrorActionPreference
            try {
                $ErrorActionPreference = 'Continue'
                & (Join-Path $runRoot 'mvnw.cmd') '-B' '-DskipTests' 'package'
                $buildExit = $LASTEXITCODE
            } finally { $ErrorActionPreference = $previousPreference }
            if ($buildExit -ne 0) { throw 'Maven build failed. See the output above; check the dependency download/proxy and retry.' }
        }
        $jar = Join-Path $runRoot 'target/cleverchat.jar'
        if (-not (Test-Path $jar)) { throw 'target/cleverchat.jar is missing. Run without -SkipBuild.' }
        Assert-FreePort $Port

        $launchEnvironment = @{
            DB_URL = $expectedUrl; DB_USER = 'cleverchat'; DB_PASSWORD = $script:DbPassword; DB_SCHEMA = $Schema
            SPRING_PROFILES_ACTIVE = 'dev'; DEFAULT_ADMIN_SEED_ENABLED = 'false'
            MANAGEMENT_HEALTH_DISKSPACE_PATH = [IO.Path]::GetPathRoot($script:AppRoot)
            CLEVERCHAT_OPERATOR_SEED_USERNAME = ''; CLEVERCHAT_OPERATOR_SEED_PASSWORD = ''
            CLEVERCHAT_OPERATOR_SEED_ENABLED = 'false'
            CLEVERCHAT_SETUP_ADMIN_USERNAME = ''; CLEVERCHAT_SETUP_ADMIN_PASSWORD = ''
        }
        if ($account) {
            $launchEnvironment.CLEVERCHAT_OPERATOR_SEED_USERNAME = $account.Username
            $launchEnvironment.CLEVERCHAT_OPERATOR_SEED_PASSWORD = $account.Password
            $launchEnvironment.CLEVERCHAT_OPERATOR_SEED_ENABLED = 'true'
        }
        foreach ($key in $launchEnvironment.Keys) {
            $savedEnvironment[$key] = [Environment]::GetEnvironmentVariable($key, 'Process')
            [Environment]::SetEnvironmentVariable($key, $launchEnvironment[$key], 'Process')
        }
        $logDirectory = Join-Path $script:AppRoot 'logs'
        [void](New-Item -ItemType Directory -Force -Path $logDirectory)
        $stamp = Get-Date -Format 'yyyyMMdd-HHmmss-fff'
        $outLog = Join-Path $logDirectory "local-$Port-$stamp.out.log"
        $errLog = Join-Path $logDirectory "local-$Port-$stamp.err.log"
        Write-Host '[4/6] Starting the dev server. Flyway will validate/apply migrations...'
        $application = Start-Process -FilePath $java -WorkingDirectory $runRoot -WindowStyle Hidden -PassThru `
            -RedirectStandardOutput $outLog -RedirectStandardError $errLog -ArgumentList @(
                '-Dfile.encoding=UTF-8', '-Duser.timezone=Asia/Seoul', '-jar', 'target/cleverchat.jar',
                '--spring.config.location=classpath:/', '--spring.profiles.active=dev',
                "--spring.datasource.url=$expectedUrl", '--spring.datasource.username=cleverchat',
                '--server.address=127.0.0.1', "--server.port=$Port",
                '--server.servlet.context-path=', '--spring.flyway.baseline-on-migrate=false')
        $null = $application.Handle
        # Only the child app needs the startup credentials; restore the launcher's environment now.
        foreach ($key in $savedEnvironment.Keys) { [Environment]::SetEnvironmentVariable($key, $savedEnvironment[$key], 'Process') }
        $launchEnvironment.Clear()

        $baseUrl = "http://127.0.0.1:$Port"
        $healthy = $false
        $deadline = [DateTime]::UtcNow.AddSeconds(180)
        Write-Host "[INFO] Server PID: $($application.Id); startup log: $outLog"
        while ([DateTime]::UtcNow -lt $deadline) {
            if ($application.HasExited) { throw "Server exited before startup (code $($application.ExitCode)). See $outLog and $errLog" }
            try {
                $response = Invoke-RestMethod "$baseUrl/actuator/health" -TimeoutSec 3
                if ($response.status -eq 'UP') { $healthy = $true; break }
            } catch { }
            Start-Sleep -Seconds 1
        }
        if (-not $healthy) { throw "Server did not become healthy within 180 seconds. See $outLog and $errLog" }

        Write-Host '[5/6] Checking first administrator, menus/codes/permissions and migrations...'
        if ($account) {
            # ApplicationRunner may finish after health first becomes available.
            $seeded = $false
            $deadline = [DateTime]::UtcNow.AddSeconds(30)
            do {
                $exists = Invoke-Database "SELECT count(*) FROM $Schema.tb_user WHERE username='$($account.Username)' AND auth_source='LOCAL' AND use_yn='Y';"
                if ($exists -eq '1') { $seeded = $true; break }
                Start-Sleep -Seconds 1
            } while ([DateTime]::UtcNow -lt $deadline)
            if (-not $seeded) { throw "The initial account was not created. See $outLog" }
            # Only promote the sole account of an installation observed empty before startup.
            # Existing installations never reach this branch; no password or existing role is reset.
            $promoted = Invoke-Database @"
BEGIN;
LOCK TABLE $Schema.tb_user, $Schema.tb_user_role IN SHARE ROW EXCLUSIVE MODE;
INSERT INTO $Schema.tb_user_role(user_no,role_no)
SELECT u.user_no,r.role_no FROM $Schema.tb_user u CROSS JOIN $Schema.tb_role r
WHERE u.username='$($account.Username)' AND u.auth_source='LOCAL' AND u.use_yn='Y'
  AND r.code='ADMIN' AND (SELECT count(*) FROM $Schema.tb_user)=1
ON CONFLICT DO NOTHING;
SELECT count(*) FROM $Schema.tb_user u JOIN $Schema.tb_user_role ur ON ur.user_no=u.user_no
JOIN $Schema.tb_role r ON r.role_no=ur.role_no WHERE u.username='$($account.Username)' AND r.code='ADMIN';
COMMIT;
"@
            if ($promoted -ne '1') { throw 'Initial ADMIN grant was not completed. Use the handover guide to review account roles; existing accounts were not changed.' }
            Write-Host "[OK] First local administrator: $($account.Username). Use the password you entered."
            $account.Clear()
        }
        Test-BasicData -Strict:($userCount -eq 0)
        $latest = Invoke-Database "SELECT version FROM $Schema.flyway_schema_history WHERE type='SQL' ORDER BY installed_rank DESC LIMIT 1;"
        Write-Host "[6/6] READY - Flyway V$latest; health UP"
        Write-Host "Login: $baseUrl/login"
        Write-Host "Chat : $baseUrl/chat"
        Write-Host 'Keep this window open. Ctrl+C stops this app; the PostgreSQL data/container is kept.'
        Write-Host "Logs : $outLog"
        Write-Host "Errors: $errLog"
        while (-not $application.HasExited) { Start-Sleep -Seconds 1 }
        if ($application.ExitCode -ne 0) { throw "Server exited with code $($application.ExitCode). See $errLog" }
    } finally {
        if ($account) { $account.Clear() }
        foreach ($key in $savedEnvironment.Keys) { [Environment]::SetEnvironmentVariable($key, $savedEnvironment[$key], 'Process') }
        # This is the specific Process object created by this invocation, never a port/PID search.
        if ($application) {
            if (-not $application.HasExited) { $application.Kill(); $application.WaitForExit() }
            $application.Dispose()
        }
        Set-Location $originalLocation
        if ($mapping) {
            $removed = Invoke-Captured "$env:SystemRoot\System32\subst.exe" @($mapping, '/D')
            if ($removed.Code -ne 0) { Write-Warning "Temporary mapping $mapping could not be removed. Check it after closing the server." }
        }
        $script:DbPassword = $null
    }
}

if ($MyInvocation.InvocationName -ne '.') {
    try { Start-LocalApplication; exit 0 }
    catch { Write-Host "[STOP] $($_.Exception.Message)" -ForegroundColor Red; exit 1 }
}
