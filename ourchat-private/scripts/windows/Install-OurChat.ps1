param([switch]$LibraryOnly)

$ErrorActionPreference = 'Stop'
$ArchiveSha256 = '__ARCHIVE_SHA256__'
$NodeVersion = '24.22.0'
$NodeChecksums = @{
    x64 = 'f9683a19877e12a9b167305c8db71040d812876359de21f0d2e97c3f321f80c3'
    arm64 = '3a5c209a5bc59e65b3bc0fa13ed5e166a26bd3d55332847f498b76b037408d61'
}

function Assert-FileHash([string]$Path, [string]$Expected) {
    if ((Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash -ne $Expected) {
        throw 'File verification failed. Delete the installer and download it again.'
    }
}

function Expand-SafeZip([string]$Path, [string]$Destination) {
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $prefix = [IO.Path]::GetFullPath($Destination).TrimEnd([IO.Path]::DirectorySeparatorChar) + [IO.Path]::DirectorySeparatorChar
    $zip = [IO.Compression.ZipFile]::OpenRead($Path)
    try {
        foreach ($entry in $zip.Entries) {
            if ($entry.FullName.Contains(':') -or $entry.FullName.StartsWith('/') -or $entry.FullName.StartsWith('\') -or
                (($entry.ExternalAttributes -shr 16) -band 61440) -eq 40960) {
                throw 'The archive contains an unsupported path or symbolic link.'
            }
            $relative = $entry.FullName.Replace('\', [string][IO.Path]::DirectorySeparatorChar).Replace('/', [string][IO.Path]::DirectorySeparatorChar)
            $target = [IO.Path]::GetFullPath((Join-Path $Destination $relative))
            if (-not $target.StartsWith($prefix, [StringComparison]::OrdinalIgnoreCase)) {
                throw 'The archive contains a path outside the installation folder.'
            }
        }
    } finally { $zip.Dispose() }
    [IO.Compression.ZipFile]::ExtractToDirectory($Path, $Destination)
}

function Get-WindowsArchitecture {
    $architecture = $env:PROCESSOR_ARCHITEW6432
    if (-not $architecture) { $architecture = $env:PROCESSOR_ARCHITECTURE }
    switch ($architecture.ToUpperInvariant()) {
        'AMD64' { return 'x64' }
        'ARM64' { return 'arm64' }
        default { throw 'OurChat requires 64-bit Windows 10 or Windows 11 (x64 or ARM64).' }
    }
}

function Set-PrivateDirectory([string]$Path) {
    New-Item -ItemType Directory -Force -Path $Path | Out-Null
    $sid = [Security.Principal.WindowsIdentity]::GetCurrent().User.Value
    & "$env:SystemRoot\System32\icacls.exe" $Path '/inheritance:r' '/grant:r' "*${sid}:(OI)(CI)F" '*S-1-5-18:(OI)(CI)F' | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Could not protect the local installation folder permissions.' }
}

function Invoke-OurChatInstallation {
    $installMutex = $null
    $mutexOwned = $false
    $temporary = $null
    try {
        if ($env:OS -ne 'Windows_NT') { throw 'This installer is for Windows only.' }
        $sid = [Security.Principal.WindowsIdentity]::GetCurrent().User.Value
        $installMutex = New-Object Threading.Mutex($false, "Local\OurChat-Install-$sid")
        try { $mutexOwned = $installMutex.WaitOne(0) } catch [Threading.AbandonedMutexException] { $mutexOwned = $true }
        if (-not $mutexOwned) { throw 'Another OurChat installer is already running. Please wait for it to finish.' }
        $root = Join-Path $env:LOCALAPPDATA 'OurChat'
        Set-PrivateDirectory $root
        $statePath = Join-Path $root 'installation.json'
        $runningMutex = New-Object Threading.Mutex($false, "Local\OurChat-Run-$sid")
        $runningOwned = $false
        try {
            try { $runningOwned = $runningMutex.WaitOne(0) } catch [Threading.AbandonedMutexException] { $runningOwned = $true }
            if (-not $runningOwned) { throw 'Close the existing OurChat server window before installing or updating.' }
            $architecture = Get-WindowsArchitecture
            [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
            $temporary = Join-Path $root ('setup-' + [Guid]::NewGuid().ToString('N'))
            New-Item -ItemType Directory -Path $temporary | Out-Null
            $installer = [IO.File]::ReadAllText($env:OURCHAT_SETUP_FILE, [Text.Encoding]::UTF8)
            $marker = "`r`n:OURCHAT_ARCHIVE`r`n"
            $markerIndex = $installer.IndexOf($marker, [StringComparison]::Ordinal)
            if ($markerIndex -lt 0) { throw 'The installer archive is missing.' }
            $archivePath = Join-Path $temporary 'OurChat.zip'
            [IO.File]::WriteAllBytes($archivePath, [Convert]::FromBase64String($installer.Substring($markerIndex + $marker.Length).Trim()))
            Assert-FileHash $archivePath $ArchiveSha256
            $release = Join-Path (Join-Path $root 'releases') $ArchiveSha256.Substring(0, 16)
            $app = Join-Path $release 'ourchat-private'
            $readyPath = Join-Path $release 'dependencies-ready'
            if (-not (Test-Path -LiteralPath (Join-Path $app 'package-lock.json'))) {
                if (Test-Path -LiteralPath $release) { Remove-Item -LiteralPath $release -Recurse -Force }
                New-Item -ItemType Directory -Force -Path (Split-Path $release) | Out-Null
                Write-Host 'Extracting verified OurChat application...'
                Expand-SafeZip $archivePath $release
            }

            $runtimeRoot = Join-Path $root ('runtime\node-v' + $NodeVersion + '-win-' + $architecture)
            $node = Join-Path $runtimeRoot 'node.exe'
            $nodeReady = Join-Path $runtimeRoot 'ourchat-runtime-ready'
            if (-not (Test-Path -LiteralPath $nodeReady) -or -not (Test-Path -LiteralPath $node)) {
                Write-Host 'Downloading the official Node.js runtime (no administrator access needed)...'
                $runtimeArchive = Join-Path $temporary 'node.zip'
                $downloadUrl = 'https://nodejs.org/dist/v' + $NodeVersion + '/node-v' + $NodeVersion + '-win-' + $architecture + '.zip'
                Invoke-WebRequest -Uri $downloadUrl -OutFile $runtimeArchive -UseBasicParsing -TimeoutSec 300
                Assert-FileHash $runtimeArchive $NodeChecksums[$architecture]
                if (Test-Path -LiteralPath $runtimeRoot) { Remove-Item -LiteralPath $runtimeRoot -Recurse -Force }
                $runtimeStage = Join-Path $temporary 'runtime'
                Expand-SafeZip $runtimeArchive $runtimeStage
                New-Item -ItemType Directory -Force -Path (Split-Path $runtimeRoot) | Out-Null
                Move-Item -LiteralPath (Join-Path $runtimeStage ('node-v' + $NodeVersion + '-win-' + $architecture)) -Destination $runtimeRoot
                [IO.File]::WriteAllText($nodeReady, $NodeChecksums[$architecture])
            }
            $env:PATH = $runtimeRoot + ';' + $env:PATH
            $env:NEXT_TELEMETRY_DISABLED = '1'
            $npmCli = Join-Path $runtimeRoot 'node_modules\npm\bin\npm-cli.js'
            Push-Location $app
            try {
                if (-not (Test-Path -LiteralPath $readyPath)) {
                    Write-Host 'Installing locked application dependencies. This can take several minutes...'
                    & $node $npmCli ci '--no-audit' '--no-fund'
                    if ($LASTEXITCODE -ne 0) { throw 'Dependency installation failed. Check your internet connection and run the installer again.' }
                    [IO.File]::WriteAllText($readyPath, 'ready')
                }
                $configFolder = Join-Path $root 'config'
                Set-PrivateDirectory $configFolder
                $config = Join-Path $configFolder '.env.local'
                if (-not (Test-Path -LiteralPath $config)) {
                    Copy-Item -LiteralPath (Join-Path $app '.env.example') -Destination $config
                }
                # The child writes keys directly to the private config file, never stdout.
                $env:OURCHAT_CONFIG_PATH = $config
                & $node (Join-Path $app 'scripts\windows\initialize-config.mjs')
                if ($LASTEXITCODE -ne 0) { throw 'Could not initialize the private application settings.' }
                Remove-Item Env:\OURCHAT_CONFIG_PATH
            } finally { Pop-Location }

            foreach ($name in @('Start-OurChat.ps1', 'Configure-OurChat.ps1')) {
                Copy-Item -LiteralPath (Join-Path $app ('scripts\windows\' + $name)) -Destination (Join-Path $root $name) -Force
            }
            $previousHash = ''
            if (Test-Path -LiteralPath $statePath) {
                $old = Get-Content -LiteralPath $statePath -Raw | ConvertFrom-Json
                if ($old.appPath -eq $app) { $previousHash = $old.buildConfigHash }
            }
            $state = @{ appPath = $app; nodePath = $node; configPath = $config; port = 3000; buildConfigHash = $previousHash }
            [IO.File]::WriteAllText($statePath, ($state | ConvertTo-Json), (New-Object Text.UTF8Encoding($false)))
            $shell = New-Object -ComObject WScript.Shell
            $desktop = [Environment]::GetFolderPath('Desktop')
            $powershell = Join-Path $env:SystemRoot 'System32\WindowsPowerShell\v1.0\powershell.exe'
            foreach ($shortcutSpec in @(@('OurChat.lnk', ''), @('OurChat Settings.lnk', ' -Configure'))) {
                $shortcut = $shell.CreateShortcut((Join-Path $desktop $shortcutSpec[0]))
                $shortcut.TargetPath = $powershell
                $shortcut.Arguments = '-NoLogo -NoProfile -ExecutionPolicy Bypass -File "' + (Join-Path $root 'Start-OurChat.ps1') + '"' + $shortcutSpec[1]
                $shortcut.WorkingDirectory = $root
                $shortcut.Description = 'OurChat local application. Keep its server window open while using it.'
                $shortcut.Save()
            }
            Write-Host 'PC installation complete. Your existing settings and notification keys have been preserved.'
            Write-Host 'Supabase database and two account setup are required for real chat. See the setup guide.'
        } finally {
            if ($runningOwned) { $runningMutex.ReleaseMutex() }
            $runningMutex.Dispose()
        }
        if ($temporary -and (Test-Path -LiteralPath $temporary)) { Remove-Item -LiteralPath $temporary -Recurse -Force; $temporary = $null }
        # Start the normal launcher, including its connection-settings dialog on first use.
        & (Join-Path $root 'Start-OurChat.ps1') -InstallRoot $root -Configure
        if ($LASTEXITCODE -ne 0) { exit 1 }
    } catch {
        Write-Host ''
        Write-Host ('Installation did not finish: ' + $_.Exception.Message) -ForegroundColor Red
        Write-Host 'Check your connection and run the same installer again. Existing settings are kept.'
        Read-Host 'Press Enter to close' | Out-Null
        exit 1
    } finally {
        if ($temporary -and (Test-Path -LiteralPath $temporary)) { Remove-Item -LiteralPath $temporary -Recurse -Force -ErrorAction SilentlyContinue }
        if ($mutexOwned) { $installMutex.ReleaseMutex() }
        if ($installMutex) { $installMutex.Dispose() }
    }
}

if (-not $LibraryOnly) { Invoke-OurChatInstallation }
