param([switch]$Test, [switch]$Integration, [switch]$CheckOnly, [switch]$SkipBuild)
$ErrorActionPreference='Stop'
$root=Split-Path -Parent $PSScriptRoot
Set-Location $root
$configPath=Join-Path $root '.env.workstation.json'
if(-not (Test-Path $configPath)){throw 'Missing .env.workstation.json; see local setup guide'}
$config=Get-Content -LiteralPath $configPath -Raw | ConvertFrom-Json
$env:JAVA_HOME=$config.javaHome
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
New-Item -ItemType Directory -Force -Path $config.javaTmp | Out-Null
$env:JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8 -Djdk.net.unixdomain.tmpdir=$($config.javaTmp)"
$env:PLAYWRIGHT_BROWSERS_PATH='D:/00.WORK/AI/dev-tools/playwright'
$env:PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD='1'
if($Test -or $Integration){
    if($Integration){ & .\mvnw.cmd -B -Pit test }else{ & .\mvnw.cmd -B test }
    exit $LASTEXITCODE
}
$env:DB_PASSWORD=$config.dbPassword
$env:CLEVERCHAT_SETUP_ADMIN_USERNAME=$config.adminUsername
$env:CLEVERCHAT_SETUP_ADMIN_PASSWORD=$config.adminPassword
& (Join-Path $PSScriptRoot 'start-local.ps1') -Port 8080 -DatabasePort 15433 -DatabaseContainer cleverchat-workstation-pg -NonInteractive -CheckOnly:$CheckOnly -SkipBuild:$SkipBuild
exit $LASTEXITCODE