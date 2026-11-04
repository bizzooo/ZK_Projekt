# Vzorči porabo CPU in pomnilnika backend procesa (posluša na :8080) med obremenitvenim testom.
# Uporaba: powershell -File monitor-backend.ps1 -DurationSeconds 630 -OutFile results/backend-resources.csv
param(
    [int]$DurationSeconds = 630,
    [int]$IntervalSeconds = 5,
    [string]$OutFile = "results/backend-resources.csv"
)

$backendPid = (Get-NetTCPConnection -LocalPort 8080 -State Listen).OwningProcess | Select-Object -First 1
$cores = [Environment]::ProcessorCount
"timestamp,elapsed_s,cpu_percent_of_machine,working_set_mb,private_mb,threads" | Out-File -Encoding utf8 $OutFile

$start = Get-Date
$prev = Get-Process -Id $backendPid
$prevCpu = $prev.TotalProcessorTime.TotalSeconds
$prevTime = Get-Date

while (((Get-Date) - $start).TotalSeconds -lt $DurationSeconds) {
    Start-Sleep -Seconds $IntervalSeconds
    $p = Get-Process -Id $backendPid
    $now = Get-Date
    $cpu = $p.TotalProcessorTime.TotalSeconds
    $cpuPct = (($cpu - $prevCpu) / ($now - $prevTime).TotalSeconds) / $cores * 100
    $prevCpu = $cpu; $prevTime = $now
    [string]::Format([Globalization.CultureInfo]::InvariantCulture, "{0},{1:F0},{2:F1},{3:F1},{4:F1},{5}",
        $now.ToString("HH:mm:ss"), ($now - $start).TotalSeconds, $cpuPct,
        ($p.WorkingSet64 / 1MB), ($p.PrivateMemorySize64 / 1MB), $p.Threads.Count) |
        Out-File -Append -Encoding utf8 $OutFile
}
