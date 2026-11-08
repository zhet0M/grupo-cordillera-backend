#requires -Version 7.0
[CmdletBinding()]
param(
    [string]$MavenCommand,
    [switch]$Offline,
    [string]$LocalRepository,
    [ValidateSet('test', 'verify')]
    [string]$Goal = 'test'
)

$ErrorActionPreference = 'Stop'
$PSNativeCommandUseErrorActionPreference = $false
$repoRoot = Split-Path -Parent $PSScriptRoot
$modules = @('authentication', 'clientes', 'finanzas', 'ventas', 'inventario', 'alertas', 'kpis', 'reportes', 'api-gateway')
$started = [DateTimeOffset]::UtcNow
$runId = $started.ToString('yyyyMMddTHHmmssfffZ')
$runDir = Join-Path $repoRoot "evidence/encargo3/baseline/$runId"
New-Item -ItemType Directory -Path $runDir | Out-Null
$results = [System.Collections.Generic.List[object]]::new()

Push-Location $repoRoot
try {
    $revision = (& git rev-parse HEAD 2>&1 | Out-String).Trim()
    $workingTree = (& git status --short --untracked-files=all 2>&1 | Out-String).Trim()
    $javaVersion = (& java -version 2>&1 | Out-String).Trim()
    $versionCommand = if ($MavenCommand) { $MavenCommand } else { Join-Path $repoRoot 'authentication/mvnw.cmd' }
    $mavenVersion = (& $versionCommand -version 2>&1 | Out-String).Trim()
    if ($LASTEXITCODE -ne 0) { throw "No se pudo iniciar Maven: $mavenVersion" }

    foreach ($module in $modules) {
        $moduleDir = Join-Path $repoRoot $module
        $moduleOutput = Join-Path $runDir $module
        $reportDir = Join-Path $moduleOutput 'surefire'
        New-Item -ItemType Directory -Path $reportDir | Out-Null
        $command = if ($MavenCommand) { $MavenCommand } else { Join-Path $moduleDir 'mvnw.cmd' }
        # Surefire supports reportNameSuffix as a CLI property, not reportsDirectory.
        # Only copy reports carrying this run ID, so old XML cannot count as new evidence.
        $mavenArgs = @('-B', '-ntp', '-Dstyle.color=never', "-Dsurefire.reportNameSuffix=$runId", '-DfailIfNoTests=true')
        if ($Offline) { $mavenArgs += '-o' }
        if ($LocalRepository) { $mavenArgs += "-Dmaven.repo.local=$LocalRepository" }
        $mavenArgs += @('clean', $Goal)
        $clock = [Diagnostics.Stopwatch]::StartNew()
        $moduleStarted = [DateTimeOffset]::UtcNow.ToString('o')
        Write-Host "Ejecutando $module..."
        Push-Location $moduleDir
        try {
            & $command @mavenArgs > (Join-Path $moduleOutput 'maven.log') 2>&1
            $exitCode = $LASTEXITCODE
        }
        finally { Pop-Location }
        $clock.Stop()
        $sourceReports = Join-Path $moduleDir 'target/surefire-reports'
        if (Test-Path -LiteralPath $sourceReports) {
            Get-ChildItem -LiteralPath $sourceReports -Filter "*-$runId.*" |
                Copy-Item -Destination $reportDir
        }
        $tests = 0; $failures = 0; $errors = 0; $skipped = 0
        $reportCount = 0
        $invalidReports = [System.Collections.Generic.List[string]]::new()
        foreach ($file in Get-ChildItem -LiteralPath $reportDir -Filter 'TEST-*.xml') {
            try {
                [xml]$xml = Get-Content -LiteralPath $file.FullName -Raw
                $suite = $xml.testsuite
                if (-not $suite -or -not $suite.HasAttribute('tests')) { throw 'XML sin resumen de testsuite' }
                $tests += [int]$suite.tests
                $failures += [int]$suite.failures
                $errors += [int]$suite.errors
                $skipped += [int]$suite.skipped
                $reportCount++
            }
            catch { $invalidReports.Add($file.Name) }
        }
        $status = if ($failures -gt 0 -or $errors -gt 0) { 'FAILED_TESTS' }
            elseif ($exitCode -ne 0) { 'BUILD_ERROR' }
            elseif ($invalidReports.Count -gt 0 -or $reportCount -eq 0 -or $tests -eq 0) { 'NO_VALID_REPORTS' }
            elseif ($skipped -gt 0) { 'PASSED_WITH_SKIPS' }
            else { 'PASSED' }
        $result = [ordered]@{
            module = $module; status = $status; exitCode = $exitCode
            startedUtc = $moduleStarted; durationSeconds = [Math]::Round($clock.Elapsed.TotalSeconds, 2)
            tests = $tests; passed = $tests - $failures - $errors - $skipped
            failures = $failures; errors = $errors; skipped = $skipped
            reportCount = $reportCount; invalidReports = @($invalidReports.ToArray())
            command = $command; arguments = $mavenArgs
            log = "$module/maven.log"; reports = "$module/surefire"
        }
        $results.Add([pscustomobject]$result)
        $result | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath (Join-Path $moduleOutput 'result.json') -Encoding utf8
        Write-Host ("{0}: {1}; total={2}, fallos={3}, errores={4}, omitidos={5}; {6}s" -f $module, $status, $tests, $failures, $errors, $skipped, $result.durationSeconds)
    }

    $totals = [ordered]@{}
    foreach ($field in @('tests', 'passed', 'failures', 'errors', 'skipped')) {
        $totals[$field] = [int](($results | Measure-Object -Property $field -Sum).Sum)
    }
    $summary = [ordered]@{
        runId = $runId; startedUtc = $started.ToString('o'); finishedUtc = [DateTimeOffset]::UtcNow.ToString('o')
        gitRevision = $revision; workingTree = $workingTree; javaVersion = $javaVersion; mavenVersion = $mavenVersion
        offline = [bool]$Offline; localRepository = $LocalRepository
        totals = $totals; modules = @($results.ToArray())
        note = 'Una prueba aprobada puede caracterizar un defecto conocido; este resumen no certifica conformidad con el ERS.'
    }
    $summary | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath (Join-Path $runDir 'summary.json') -Encoding utf8
    $results | Select-Object module,status,exitCode,tests,passed,failures,errors,skipped,durationSeconds |
        Export-Csv -LiteralPath (Join-Path $runDir 'summary.csv') -NoTypeInformation -Encoding utf8
    $lines = @('# Línea base de pruebas del backend', '', "Ejecución UTC: $runId", '', "Revisión Git: $revision", '',
        '| Módulo | Estado | Total | Aprobadas | Fallos | Errores | Omitidas | Segundos |',
        '| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |')
    foreach ($item in $results) {
        $lines += "| $($item.module) | $($item.status) | $($item.tests) | $($item.passed) | $($item.failures) | $($item.errors) | $($item.skipped) | $($item.durationSeconds) |"
    }
    $lines += ''; $lines += "Total: $($totals.tests); aprobadas: $($totals.passed); fallos: $($totals.failures); errores: $($totals.errors); omitidas: $($totals.skipped)."
    $lines += ''; $lines += $summary.note
    $lines += ''; $lines += 'Los logs y XML pueden contener datos del entorno. Revisarlos y anonimizar antes de compartir.'
    $lines | Set-Content -LiteralPath (Join-Path $runDir 'SUMMARY.md') -Encoding utf8
    Write-Host "Evidencia: $runDir"
    if (@($results | Where-Object { $_.status -ne 'PASSED' }).Count -gt 0) { exit 1 }
}
finally { Pop-Location }
