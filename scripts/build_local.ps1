$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$jdkRoot = Get-ChildItem -LiteralPath "$projectRoot\.tooling\jdk17" -Directory | Select-Object -First 1
if (-not $jdkRoot) { throw 'Portable JDK not found under .tooling/jdk17.' }

$env:JAVA_HOME = $jdkRoot.FullName
$env:ANDROID_SDK_ROOT = "$projectRoot\.tooling\android-sdk"
$env:Path = "$env:JAVA_HOME\bin;$env:ANDROID_SDK_ROOT\platform-tools;$env:Path"

& "$projectRoot\gradlew.bat" --no-daemon clean test assembleDebug
