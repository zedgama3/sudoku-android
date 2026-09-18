param(
    [Parameter(Mandatory = $true)][string]$Adb,
    [Parameter(Mandatory = $true)][string]$Package
)

$deviceLines = (& $Adb devices) | Select-String "\tdevice$"
if (-not $deviceLines) {
    Write-Error "No connected devices found (run 'task devices' to check)."
    exit 1
}

foreach ($line in $deviceLines) {
    $serial = ($line -split "\t")[0]
    Write-Host "Launching on $serial"
    & $Adb -s $serial shell am start -n "$Package/.MainActivity"
}
