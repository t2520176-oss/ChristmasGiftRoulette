# Helper-only validation. Does not run Windows installation, ACL changes, or the GUI.
param([string]$NodePath = '')
$ErrorActionPreference = 'Stop'
$repo = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
. (Join-Path $repo 'scripts/windows/Install-OurChat.ps1') -LibraryOnly
if (-not $NodePath) { $NodePath = (Get-Command node -ErrorAction Stop).Source }
$testRoot = Join-Path ([IO.Path]::GetTempPath()) ('ourchat-installer-tests-' + [Guid]::NewGuid().ToString('N'))
$checks = 0
function Assert-Test([bool]$Condition, [string]$Message) {
    if (-not $Condition) { throw $Message }
    $script:checks++
}
function Assert-Rejected([scriptblock]$Action, [string]$Message) {
    $rejected = $false
    try { & $Action } catch { $rejected = $true }
    Assert-Test $rejected $Message
}
function New-TestZip([string]$Name, [string]$EntryName, [switch]$SymbolicLink) {
    $path = Join-Path $testRoot ($Name + '.zip')
    $archive = [IO.Compression.ZipFile]::Open($path, [IO.Compression.ZipArchiveMode]::Create)
    try {
        $entry = $archive.CreateEntry($EntryName)
        if ($SymbolicLink) { $entry.ExternalAttributes = [Convert]::ToInt32('A1FF0000', 16) }
        $writer = New-Object IO.StreamWriter($entry.Open())
        try { $writer.Write('valid content') } finally { $writer.Dispose() }
    } finally { $archive.Dispose() }
    return $path
}
New-Item -ItemType Directory -Path $testRoot | Out-Null
try {
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $valid = New-TestZip 'valid' 'ourchat-private/test.txt'
    $validOutput = Join-Path $testRoot 'valid-output'
    Expand-SafeZip $valid $validOutput
    Assert-Test ((Get-Content -LiteralPath (Join-Path $validOutput 'ourchat-private/test.txt') -Raw) -eq 'valid content') 'Valid ZIP did not extract correctly'
    $fixtures = @(
        @('dotdot', '../escaped.txt'),
        @('backslash', '..\escaped.txt'),
        @('absolute', '/absolute/escaped.txt'),
        @('backslash-absolute', '\Windows\escaped.txt'),
        @('drive', 'C:\escaped.txt')
    )
    foreach ($fixture in $fixtures) {
        $archivePath = New-TestZip $fixture[0] $fixture[1]
        $destination = Join-Path $testRoot ($fixture[0] + '-output')
        Assert-Rejected { Expand-SafeZip $archivePath $destination } ('Unsafe ZIP path was accepted: ' + $fixture[0])
        Assert-Test (-not (Test-Path -LiteralPath $destination)) ('Rejected ZIP created an output directory: ' + $fixture[0])
    }
    $linkArchive = New-TestZip 'symlink' 'ourchat-private/link' -SymbolicLink
    $linkDestination = Join-Path $testRoot 'symlink-output'
    Assert-Rejected { Expand-SafeZip $linkArchive $linkDestination } 'Symbolic-link ZIP entry was accepted'
    Assert-Test (-not (Test-Path -LiteralPath $linkDestination)) 'Rejected symbolic-link ZIP created an output directory'
    Assert-FileHash $valid ((Get-FileHash -LiteralPath $valid -Algorithm SHA256).Hash)
    $checks++
    Assert-Rejected { Assert-FileHash $valid ('0' * 64) } 'SHA256 mismatch was accepted'

    $oldArch = $env:PROCESSOR_ARCHITECTURE
    $oldWow = $env:PROCESSOR_ARCHITEW6432
    try {
        $env:PROCESSOR_ARCHITEW6432 = ''
        $env:PROCESSOR_ARCHITECTURE = 'AMD64'
        Assert-Test ((Get-WindowsArchitecture) -eq 'x64') 'x64 selection failed'
        $env:PROCESSOR_ARCHITECTURE = 'ARM64'
        Assert-Test ((Get-WindowsArchitecture) -eq 'arm64') 'ARM64 selection failed'
        $env:PROCESSOR_ARCHITECTURE = 'x86'
        $env:PROCESSOR_ARCHITEW6432 = 'AMD64'
        Assert-Test ((Get-WindowsArchitecture) -eq 'x64') 'WOW64 native selection failed'
        $env:PROCESSOR_ARCHITEW6432 = 'ARM64'
        Assert-Test ((Get-WindowsArchitecture) -eq 'arm64') 'WOW ARM64 native selection failed'
        $env:PROCESSOR_ARCHITEW6432 = ''
        Assert-Rejected { Get-WindowsArchitecture } 'Unsupported architecture was accepted'
    } finally {
        $env:PROCESSOR_ARCHITECTURE = $oldArch
        $env:PROCESSOR_ARCHITEW6432 = $oldWow
    }
    Write-Output ('PASS: {0} installer helper assertions' -f $checks)

    # Child processes receive private fixture paths only. No generated key is printed.
    $nodeTests = @'
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const { spawnSync } = require('node:child_process');
const [initializer, root] = process.argv.slice(1);
let checks = 0;
const check = (condition, message) => { assert.ok(condition, message); checks += 1; };
const run = (file) => spawnSync(process.execPath, [initializer], {
  env: { ...process.env, OURCHAT_CONFIG_PATH: file }, encoding: 'utf8'
});
const file = path.join(root, 'first.env');
const initial = '# keep this comment\nNEXT_PUBLIC_VAPID_PUBLIC_KEY=\nVAPID_PRIVATE_KEY=\nNEXT_PUBLIC_SITE_URL=\nOTHER_SETTING=keep\n';
fs.writeFileSync(file, initial, {mode: 0o600});
let result = run(file);
check(result.status === 0, 'First initialization failed');
check(result.stdout === '' && result.stderr === '', 'Initialization wrote unexpected output');
const generated = fs.readFileSync(file, 'utf8');
const read = (text, name) => text.match(new RegExp(`^${name}=(.*)$`, 'm'))?.[1].trim();
const publicKey = read(generated, 'NEXT_PUBLIC_VAPID_PUBLIC_KEY');
const privateKey = read(generated, 'VAPID_PRIVATE_KEY');
check(Buffer.from(publicKey, 'base64url').length === 65, 'Generated public key is invalid');
check(Buffer.from(privateKey, 'base64url').length === 32, 'Generated private key is invalid');
const ecdh = crypto.createECDH('prime256v1');
ecdh.setPrivateKey(Buffer.from(privateKey, 'base64url'));
check(ecdh.getPublicKey().equals(Buffer.from(publicKey, 'base64url')), 'Generated VAPID keys do not match');
check(read(generated, 'NEXT_PUBLIC_SITE_URL') === 'http://localhost:3000', 'Default local URL is missing');
check(generated.includes('# keep this comment') && generated.includes('OTHER_SETTING=keep'), 'Unrelated settings were lost');
result = run(file);
check(result.status === 0, 'Repeated initialization failed');
check(fs.readFileSync(file, 'utf8') === generated, 'Repeated initialization rotated keys or changed settings');
check(result.stdout === '' && result.stderr === '', 'Repeated initialization wrote unexpected output');
const customUrl = generated.replace('http://localhost:3000', 'https://existing.example');
fs.writeFileSync(file, customUrl);
result = run(file);
check(result.status === 0 && fs.readFileSync(file, 'utf8') === customUrl, 'Existing URL or keys were overwritten');
for (const key of ['NEXT_PUBLIC_VAPID_PUBLIC_KEY', 'VAPID_PRIVATE_KEY']) {
  const partial = initial.replace(`${key}=`, `${key}=existing-value`);
  const partialFile = path.join(root, `${key}.env`);
  fs.writeFileSync(partialFile, partial, {mode: 0o600});
  result = run(partialFile);
  check(result.status !== 0, 'Incomplete VAPID pair was accepted');
  check(fs.readFileSync(partialFile, 'utf8') === partial, 'Incomplete VAPID pair changed the file');
  check(!result.stdout.includes('existing-value') && !result.stderr.includes('existing-value'), 'A private fixture value leaked into output');
}
console.log(`PASS: ${checks} config initialization assertions`);
'@
    & $NodePath -e $nodeTests (Join-Path $repo 'scripts/windows/initialize-config.mjs') $testRoot
    if ($LASTEXITCODE -ne 0) { throw 'Config initialization checks failed' }
} finally {
    if (Test-Path -LiteralPath $testRoot) { Remove-Item -LiteralPath $testRoot -Recurse -Force }
}
