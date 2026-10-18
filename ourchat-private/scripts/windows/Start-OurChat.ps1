# Windows PowerShell 5.1. Keep this console open while using the local app.
param([string]$InstallRoot = $PSScriptRoot, [switch]$Configure)

$ErrorActionPreference = 'Stop'
$server = $null
$mutex = $null
$ownsMutex = $false
$failed = $false
$oldTelemetry = $env:NEXT_TELEMETRY_DISABLED
$oldPath = $env:PATH

function Quote-OurChatArgument([string]$Value) {
    return '"' + $Value.Replace('"', '\"') + '"'
}

function Test-OurChatPort([int]$Port) {
    $listener = New-Object System.Net.Sockets.TcpListener([System.Net.IPAddress]::Loopback, $Port)
    try { $listener.Start(); return $true }
    catch { return $false }
    finally { $listener.Stop() }
}

try {
    $InstallRoot = [System.IO.Path]::GetFullPath($InstallRoot)
    $sid = [System.Security.Principal.WindowsIdentity]::GetCurrent().User.Value
    $mutex = New-Object System.Threading.Mutex($false, ('Local\OurChat-Run-' + $sid))
    try { $ownsMutex = $mutex.WaitOne(0) }
    catch [System.Threading.AbandonedMutexException] { $ownsMutex = $true }
    if (-not $ownsMutex) { throw 'OurChat is already running. Use its existing window, or close that window first.' }

    $installationPath = Join-Path $InstallRoot 'installation.json'
    if (-not (Test-Path -LiteralPath $installationPath)) { throw 'Installation settings are missing. Run the OurChat installer again.' }
    $installation = Get-Content -LiteralPath $installationPath -Raw | ConvertFrom-Json
    $appPath = [string]$installation.appPath
    $nodePath = [string]$installation.nodePath
    $configPath = [string]$installation.configPath
    if (-not $appPath -or -not $nodePath -or -not $configPath -or [int]$installation.port -ne 3000) {
        throw 'Installation settings are incomplete. Run the OurChat installer again.'
    }
    if (-not (Test-Path -LiteralPath $appPath -PathType Container) -or -not (Test-Path -LiteralPath $nodePath -PathType Leaf)) {
        throw 'The app or its private Node.js runtime is missing. Run the installer again.'
    }
    $env:PATH = (Split-Path -Parent $nodePath) + [System.IO.Path]::PathSeparator + $env:PATH
    if ($Configure) {
        . (Join-Path $PSScriptRoot 'Configure-OurChat.ps1')
        [void](Show-OurChatConfiguration -ConfigPath $configPath -AppPath $appPath)
    }
    if (-not (Test-Path -LiteralPath $configPath -PathType Leaf)) {
        throw 'The private configuration file is missing. Run the installer again.'
    }
    if (-not (Test-OurChatPort -Port 3000)) {
        throw 'Port 3000 is already used by another program. Close that program and try again. No existing program was stopped.'
    }

    $appEnv = Join-Path $appPath '.env.local'
    if ([System.IO.Path]::GetFullPath($configPath) -ne [System.IO.Path]::GetFullPath($appEnv)) {
        Copy-Item -LiteralPath $configPath -Destination $appEnv -Force
    }
    $configHash = (Get-FileHash -LiteralPath $configPath -Algorithm SHA256).Hash
    $logs = Join-Path $InstallRoot 'logs'
    [void][System.IO.Directory]::CreateDirectory($logs)
    $stamp = (Get-Date -Format 'yyyyMMdd-HHmmss') + '-' + $PID
    $env:NEXT_TELEMETRY_DISABLED = '1'
    $buildId = Join-Path $appPath '.next\BUILD_ID'
    $needsBuild = (-not (Test-Path -LiteralPath $buildId)) -or ([string]$installation.buildConfigHash -ne $configHash)
    if ($needsBuild) {
        Write-Host 'Preparing OurChat. The first build may take several minutes...'
        $npmCli = Join-Path (Split-Path -Parent $nodePath) 'node_modules\npm\bin\npm-cli.js'
        if (-not (Test-Path -LiteralPath $npmCli)) { throw 'The private npm runtime is missing. Run the installer again.' }
        $buildOut = Join-Path $logs ($stamp + '-build-output.log')
        $buildError = Join-Path $logs ($stamp + '-build-error.log')
        $buildArguments = (Quote-OurChatArgument $npmCli) + ' run build'
        $builder = Start-Process -FilePath $nodePath -ArgumentList $buildArguments -WorkingDirectory $appPath -NoNewWindow -Wait -PassThru -RedirectStandardOutput $buildOut -RedirectStandardError $buildError
        if ($builder.ExitCode -ne 0 -or -not (Test-Path -LiteralPath $buildId)) {
            throw ('OurChat could not build. See the private logs in: ' + $logs)
        }
        $installation | Add-Member -NotePropertyName buildConfigHash -NotePropertyValue $configHash -Force
        $temporary = $installationPath + '.new'
        [System.IO.File]::WriteAllText($temporary, ($installation | ConvertTo-Json -Depth 8), (New-Object System.Text.UTF8Encoding($false)))
        Move-Item -LiteralPath $temporary -Destination $installationPath -Force
    }

    $nextCli = Join-Path $appPath 'node_modules\next\dist\bin\next'
    if (-not (Test-Path -LiteralPath $nextCli)) { throw 'App dependencies are missing. Run the installer again.' }
    $serverOut = Join-Path $logs ($stamp + '-server-output.log')
    $serverError = Join-Path $logs ($stamp + '-server-error.log')
    $serverArguments = (Quote-OurChatArgument $nextCli) + ' start --hostname 127.0.0.1 --port 3000'
    if (-not (Test-OurChatPort -Port 3000)) { throw 'Another program started using port 3000. Close it and try again.' }
    $server = Start-Process -FilePath $nodePath -ArgumentList $serverArguments -WorkingDirectory $appPath -NoNewWindow -PassThru -RedirectStandardOutput $serverOut -RedirectStandardError $serverError
    [void]$server.Handle
    $ready = $false
    $deadline = [DateTime]::UtcNow.AddSeconds(60)
    Write-Host 'Starting OurChat at http://localhost:3000 ...'
    while ([DateTime]::UtcNow -lt $deadline) {
        $server.Refresh()
        if ($server.HasExited) { throw ('The local server stopped. See the private logs in: ' + $logs) }
        try {
            $response = Invoke-WebRequest -Uri 'http://127.0.0.1:3000/login' -UseBasicParsing -TimeoutSec 2
            $server.Refresh()
            if ($response.StatusCode -eq 200 -and -not $server.HasExited) { $ready = $true; break }
        } catch { }
        Start-Sleep -Milliseconds 500
    }
    if (-not $ready) { throw ('The app did not become ready in time. See the private logs in: ' + $logs) }
    Write-Host ''
    Write-Host 'OurChat is open in your browser. Keep this window open while using it.'
    Write-Host 'Press Q in this window to stop the local server. Supabase setup is required for real chat.'
    Write-Host 'Use the OurChat Settings shortcut later to change your connection settings.'
    Start-Process 'http://localhost:3000/login' | Out-Null
    $stopRequested = $false
    while (-not $server.WaitForExit(250)) {
        try {
            if ([System.Console]::KeyAvailable) {
                $pressed = [System.Console]::ReadKey($true)
                if ($pressed.Key -eq [System.ConsoleKey]::Q) { $stopRequested = $true; break }
            }
        } catch { }
    }
    if (-not $stopRequested -and $server.ExitCode -ne 0) { throw ('The app stopped unexpectedly. See the private logs in: ' + $logs) }
} catch {
    $failed = $true
    Write-Host ''
    Write-Host ('OurChat: ' + $_.Exception.Message) -ForegroundColor Red
} finally {
    # Never terminate by image name or by a port: only our own Process object.
    if ($null -ne $server) {
        try { $server.Refresh(); if (-not $server.HasExited) { $server.Kill(); $server.WaitForExit() } } catch { }
        $server.Dispose()
    }
    $env:NEXT_TELEMETRY_DISABLED = $oldTelemetry
    $env:PATH = $oldPath
    if ($ownsMutex -and $null -ne $mutex) { try { $mutex.ReleaseMutex() } catch { } }
    if ($null -ne $mutex) { $mutex.Dispose() }
    if ($failed) {
        try { [void](Read-Host 'Press Enter to close this window') } catch { }
    }
}
if ($failed) { exit 1 }
