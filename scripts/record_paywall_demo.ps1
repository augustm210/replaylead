param(
    [string]$OutputPath = "artifacts\submission\replaylead-paywall-raw.mp4"
)

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$adb = Join-Path $projectRoot ".tooling\android-sdk\platform-tools\adb.exe"
$deviceXml = "/sdcard/replaylead-paywall-ui.xml"
$localXml = Join-Path $projectRoot "artifacts\submission\replaylead-paywall-ui.xml"
$deviceVideo = "/sdcard/replaylead-paywall-raw.mp4"
$resolvedOutput = Join-Path $projectRoot $OutputPath

function Invoke-TapText {
    param(
        [Parameter(Mandatory)][string]$Text,
        [int]$WaitSeconds = 1
    )

    & $adb shell uiautomator dump $deviceXml | Out-Null
    & $adb pull $deviceXml $localXml | Out-Null
    [xml]$document = Get-Content -Raw -LiteralPath $localXml
    $bounds = @(
        $document.SelectNodes('//*[@text]') |
            Where-Object {
                $_.GetAttribute("text") -eq $Text -or
                $_.GetAttribute("content-desc") -eq $Text
            } |
            ForEach-Object { $_.GetAttribute("bounds") }
    ) | Select-Object -First 1
    if ($bounds -notmatch '^\[(\d+),(\d+)\]\[(\d+),(\d+)\]$') {
        throw "Could not find visible UI text '$Text'"
    }
    $x = [int](([int]$Matches[1] + [int]$Matches[3]) / 2)
    $y = [int](([int]$Matches[2] + [int]$Matches[4]) / 2)
    & $adb shell input tap $x $y | Out-Null
    Start-Sleep -Seconds $WaitSeconds
}

New-Item -ItemType Directory -Force -Path (Split-Path -Parent $resolvedOutput) | Out-Null
& $adb shell am force-stop com.replaylead.app.debug | Out-Null
& $adb shell rm -f $deviceVideo | Out-Null
& $adb shell monkey -p com.replaylead.app.debug -c android.intent.category.LAUNCHER 1 | Out-Null
Start-Sleep -Seconds 4

$recording = Start-Process -FilePath $adb -ArgumentList @(
    "shell", "screenrecord", "--bit-rate", "8000000", "--time-limit", "55", $deviceVideo
) -WindowStyle Hidden -PassThru

try {
    Start-Sleep -Seconds 4
    Invoke-TapText -Text "Unlock deeper coaching" -WaitSeconds 8
    Invoke-TapText -Text "Restore purchases" -WaitSeconds 9
    Start-Sleep -Seconds 6
}
finally {
    & $adb shell pkill -INT screenrecord 2>$null | Out-Null
    if (-not $recording.HasExited) {
        $recording.WaitForExit(10000) | Out-Null
    }
    & $adb pull $deviceVideo $resolvedOutput | Out-Null
}

Get-Item -LiteralPath $resolvedOutput | Select-Object FullName, Length, LastWriteTime
