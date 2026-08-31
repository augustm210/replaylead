param(
    [string]$OutputPath = "artifacts\submission\replaylead-demo-raw.mp4"
)

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$adb = Join-Path $projectRoot ".tooling\android-sdk\platform-tools\adb.exe"
$deviceXml = "/sdcard/replaylead-demo-ui.xml"
$localXml = Join-Path $projectRoot "artifacts\submission\replaylead-demo-ui.xml"
$deviceVideo = "/sdcard/replaylead-demo-raw.mp4"
$resolvedOutput = Join-Path $projectRoot $OutputPath

if (-not (Test-Path -LiteralPath $adb)) {
    throw "Android Debug Bridge was not found at $adb"
}

New-Item -ItemType Directory -Force -Path (Split-Path -Parent $resolvedOutput) | Out-Null

function Get-UiNodes {
    param([Parameter(Mandatory)][string]$Text)

    & $adb shell uiautomator dump $deviceXml | Out-Null
    & $adb pull $deviceXml $localXml | Out-Null
    [xml]$document = Get-Content -Raw -LiteralPath $localXml
    return @(
        $document.SelectNodes('//*[@text]') |
            Where-Object {
                $_.GetAttribute("text") -eq $Text -or
                $_.GetAttribute("content-desc") -eq $Text
            } |
            ForEach-Object { $_.GetAttribute("bounds") }
    )
}

function Invoke-TapText {
    param(
        [Parameter(Mandatory)][string]$Text,
        [int]$Index = 0,
        [int]$WaitSeconds = 1
    )

    $boundsList = @(Get-UiNodes -Text $Text)
    if ($boundsList.Count -le $Index) {
        throw "Could not find UI text '$Text' at index $Index"
    }
    $bounds = $boundsList[$Index]
    if ($bounds -notmatch '^\[(\d+),(\d+)\]\[(\d+),(\d+)\]$') {
        throw "Could not parse bounds for '$Text': $bounds"
    }
    $x = [int](([int]$Matches[1] + [int]$Matches[3]) / 2)
    $y = [int](([int]$Matches[2] + [int]$Matches[4]) / 2)
    & $adb shell input tap $x $y | Out-Null
    Start-Sleep -Seconds $WaitSeconds
}

function Invoke-TypeAndSend {
    param(
        [Parameter(Mandatory)][string]$Text,
        [int]$WaitSeconds = 4
    )

    Invoke-TapText -Text "Say what you would really say…" -WaitSeconds 1
    $encoded = $Text -replace ' ', '%s'
    & $adb shell input text $encoded | Out-Null
    Invoke-TapText -Text "Send" -WaitSeconds $WaitSeconds
}

& $adb shell am force-stop com.replaylead.app.debug | Out-Null
& $adb shell rm -f $deviceVideo | Out-Null
& $adb shell monkey -p com.replaylead.app.debug -c android.intent.category.LAUNCHER 1 | Out-Null
Start-Sleep -Seconds 3

$recording = Start-Process -FilePath $adb -ArgumentList @(
    "shell", "screenrecord", "--bit-rate", "8000000", "--time-limit", "175", $deviceVideo
) -WindowStyle Hidden -PassThru

try {
    Start-Sleep -Seconds 4
    Invoke-TapText -Text "Practice a conversation" -WaitSeconds 3
    Invoke-TapText -Text "2" -WaitSeconds 2
    Invoke-TapText -Text "Start rehearsal" -WaitSeconds 4

    Invoke-TypeAndSend -Text "I want to discuss the missed deadlines and understand what is getting in the way" -WaitSeconds 5
    Invoke-TypeAndSend -Text "What support would help you deliver the next milestone by Friday" -WaitSeconds 5

    Invoke-TapText -Text "Rewind here" -Index 0 -WaitSeconds 3
    Invoke-TypeAndSend -Text "I noticed the last two deadlines slipped What obstacles are you facing and what support do you need" -WaitSeconds 5
    Invoke-TypeAndSend -Text "Let us agree on Friday as the next milestone and check in Wednesday" -WaitSeconds 5

    Invoke-TapText -Text "Finish and get coaching" -WaitSeconds 6
    Invoke-TapText -Text "4" -WaitSeconds 3

    & $adb shell input swipe 540 2040 540 850 900 | Out-Null
    Start-Sleep -Seconds 4
    & $adb shell input swipe 540 2040 540 850 900 | Out-Null
    Start-Sleep -Seconds 5

    Start-Sleep -Seconds 4
}
finally {
    & $adb shell pkill -INT screenrecord 2>$null | Out-Null
    if (-not $recording.HasExited) {
        $recording.WaitForExit(10000) | Out-Null
    }
    & $adb pull $deviceVideo $resolvedOutput | Out-Null
}

Get-Item -LiteralPath $resolvedOutput | Select-Object FullName, Length, LastWriteTime
