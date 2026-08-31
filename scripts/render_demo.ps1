param(
    [string]$CoreVideo = "artifacts\submission\replaylead-demo-raw.mp4",
    [string]$PaywallVideo = "artifacts\submission\replaylead-paywall-raw.mp4",
    [string]$Narration = "artifacts\submission\replaylead-narration.wav",
    [string]$Captions = "artifacts\submission\replaylead-demo-captions.srt",
    [string]$Output = "artifacts\submission\replaylead-demo-final.mp4"
)

$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $PSScriptRoot
$ffmpeg = Join-Path $projectRoot ".tooling\python-packages\imageio_ffmpeg\binaries\ffmpeg-win-x86_64-v7.1.exe"

if (-not (Test-Path -LiteralPath $ffmpeg)) {
    throw "FFmpeg was not found. Install imageio-ffmpeg into .tooling\python-packages first."
}

$corePath = Join-Path $projectRoot $CoreVideo
$paywallPath = Join-Path $projectRoot $PaywallVideo
$narrationPath = Join-Path $projectRoot $Narration
$captionFile = Join-Path $projectRoot $Captions
$captionPath = $Captions.Replace("\", "/")
$outputPath = Join-Path $projectRoot $Output

@($corePath, $paywallPath, $narrationPath, $captionFile) | ForEach-Object {
    if (-not (Test-Path -LiteralPath $_)) {
        throw "Required demo source is missing: $_"
    }
}

$filter = @"
[0:v]trim=start=0:end=134,setpts=(PTS-STARTPTS)/1.80,scale=864:1920,pad=1080:1920:108:0:color=0x0D2B3C[core];
[1:v]trim=start=0:end=32.5,setpts=PTS-STARTPTS,scale=864:1920,pad=1080:1920:108:0:color=0x0D2B3C[paywall];
[core][paywall]concat=n=2:v=1:a=0,tpad=stop_mode=clone:stop_duration=20,subtitles='$captionPath':force_style='FontName=Arial,FontSize=9,PrimaryColour=&H00FFFFFF,OutlineColour=&HCC0D2B3C,BackColour=&HC00D2B3C,BorderStyle=3,Outline=0.5,Shadow=0,MarginV=16,Alignment=2',fade=t=in:st=0:d=0.5,fade=t=out:st=106.7:d=0.5[v];
[2:a]adelay=1000,apad=pad_dur=3[a]
"@ -replace "`r?`n", ""

Push-Location $projectRoot
try {
    & $ffmpeg -hide_banner -loglevel warning -y `
        -i $corePath `
        -i $paywallPath `
        -i $narrationPath `
        -filter_complex $filter `
        -map "[v]" -map "[a]" `
        -t 107.2 -r 30 `
        -c:v libx264 -preset medium -crf 20 -pix_fmt yuv420p `
        -c:a aac -b:a 192k `
        -movflags +faststart `
        $outputPath
}
finally {
    Pop-Location
}

if ($LASTEXITCODE -ne 0) {
    throw "FFmpeg failed with exit code $LASTEXITCODE"
}

Get-Item -LiteralPath $outputPath | Select-Object FullName, Length, LastWriteTime
