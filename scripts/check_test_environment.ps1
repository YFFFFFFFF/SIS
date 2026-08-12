[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$failures = [System.Collections.Generic.List[string]]::new()
$warnings = [System.Collections.Generic.List[string]]::new()

function Write-CheckResult {
    param(
        [string]$Name,
        [bool]$Passed,
        [string]$Detail
    )

    $label = if ($Passed) { '[OK]' } else { '[FAIL]' }
    $color = if ($Passed) { 'Green' } else { 'Red' }
    Write-Host "$label $Name - $Detail" -ForegroundColor $color
    if (-not $Passed) {
        $failures.Add("$Name : $Detail")
    }
}

function Get-MajorVersion {
    param([string]$Text)
    $match = [regex]::Match($Text, '(?<!\d)(\d+)(?:\.\d+)*')
    if ($match.Success) { return [int]$match.Groups[1].Value }
    return 0
}

function Test-CommandVersion {
    param(
        [string]$Name,
        [string]$Command,
        [string[]]$Arguments,
        [int]$MinimumMajor
    )

    if (-not (Get-Command $Command -ErrorAction SilentlyContinue)) {
        Write-CheckResult $Name $false "Command not found: $Command"
        return
    }

    $previousErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        $output = (& $Command @Arguments 2>&1 | ForEach-Object { "$_" } | Out-String).Trim()
    } finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }
    $major = Get-MajorVersion $output
    Write-CheckResult $Name ($major -ge $MinimumMajor) "$output (required major >= $MinimumMajor)"
}

$repoRoot = Split-Path -Parent $PSScriptRoot
Write-Host "Checking repository: $repoRoot" -ForegroundColor Cyan

Test-CommandVersion 'Java' 'java' @('-version') 17
Test-CommandVersion 'Maven' 'mvn' @('-version') 3
Test-CommandVersion 'Node.js' 'node' @('--version') 22
Test-CommandVersion 'npm' 'npm' @('--version') 10

foreach ($relativePath in @('backend/pom.xml', 'frontend/package.json', 'frontend/package-lock.json', 'scripts/initialize_demo_data.ps1')) {
    $fullPath = Join-Path $repoRoot $relativePath
    Write-CheckResult $relativePath (Test-Path -LiteralPath $fullPath) $fullPath
}

foreach ($port in @(8080, 5173)) {
    $listener = Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue
    if ($listener) {
        $warnings.Add("Port $port is already in use. Stop the process first unless the SIS service is already running.")
        Write-Host "[WARN] Port $port - already listening" -ForegroundColor Yellow
    } else {
        Write-Host "[OK] Port $port - available" -ForegroundColor Green
    }
}

$chromeCandidates = @(@(
    "$env:ProgramFiles\Google\Chrome\Application\chrome.exe",
    "${env:ProgramFiles(x86)}\Google\Chrome\Application\chrome.exe",
    "$env:LOCALAPPDATA\Google\Chrome\Application\chrome.exe"
) | Where-Object { $_ -and (Test-Path -LiteralPath $_) })
if ($chromeCandidates.Count -eq 0) {
    $warnings.Add('Google Chrome was not found. It is required only for Playwright E2E tests.')
    Write-Host '[WARN] Google Chrome - not found; E2E tests are unavailable' -ForegroundColor Yellow
} else {
    Write-Host "[OK] Google Chrome - $($chromeCandidates[0])" -ForegroundColor Green
}

if ($warnings.Count -gt 0) {
    Write-Host "`nWarnings:" -ForegroundColor Yellow
    $warnings | ForEach-Object { Write-Host "- $_" -ForegroundColor Yellow }
}

if ($failures.Count -gt 0) {
    Write-Host "`nEnvironment check failed:" -ForegroundColor Red
    $failures | ForEach-Object { Write-Host "- $_" -ForegroundColor Red }
    exit 1
}

Write-Host "`nEnvironment check passed. See docs/test_environment.md for startup and validation steps." -ForegroundColor Green
