param(
    [string]$ProjectRoot = (Split-Path -Parent $PSScriptRoot)
)

$ErrorActionPreference = 'Stop'
Push-Location $ProjectRoot
try {
    mvn -pl framework -am clean package
    $jar = Join-Path $ProjectRoot 'framework\target\sprint-framework-0.1.0-SNAPSHOT.jar'
    $duplicate = Join-Path $ProjectRoot 'framework\target\sprint-framework-copy.jar'
    $lib = Join-Path $ProjectRoot 'test-app\lib'
    New-Item -ItemType Directory -Force -Path $lib | Out-Null
    Copy-Item $jar $duplicate -Force
    Copy-Item $jar (Join-Path $lib 'sprint-framework.jar') -Force

    $userPath = [Environment]::GetEnvironmentVariable('Path', 'User')
    $pathEntries = @($userPath -split ';' | Where-Object { $_ })
    $jarDirectory = Split-Path $jar
    if ($pathEntries -notcontains $jarDirectory) {
        [Environment]::SetEnvironmentVariable('Path', (($pathEntries + $jarDirectory) -join ';'), 'User')
    }
    Write-Host "JAR genere : $jar"
    Write-Host "Copie      : $duplicate"
    Write-Host "Application: $(Join-Path $lib 'sprint-framework.jar')"
    Write-Host 'Le PATH utilisateur a ete mis a jour. Ouvrir un nouveau terminal pour le relire.'
}
finally {
    Pop-Location
}