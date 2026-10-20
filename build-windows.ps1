# Compila out\tvhome.apk no Windows (equivalente ao build.sh).
# Precisa de: JDK 17 (javac, keytool no PATH) e Android SDK em C:\Android
# (platforms;android-34 e build-tools;34.0.0).
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

$sdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { "C:\Android" }
$bt  = "$sdk\build-tools\34.0.0"
$jar = "$sdk\platforms\android-34\android.jar"
if (-not (Test-Path $jar) -or -not (Test-Path $bt)) { throw "Android SDK nao encontrado em $sdk (precisa de platforms;android-34 e build-tools;34.0.0)." }
if (-not (Get-Command javac -ErrorAction SilentlyContinue)) { throw "javac nao encontrado. Instale o JDK 17 e abra um terminal novo." }

function Check($what) { if ($LASTEXITCODE -ne 0) { throw "Falhou: $what" } }

Remove-Item out -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force out\classes, out\dex, out\gen\com\ansub\tvhome | Out-Null

# Transforma o apps.txt em Apps.java
$apps = Get-Content apps.txt | Where-Object { $_ -notmatch '^\s*#' } |
        ForEach-Object { $_ -replace '\s', '' } | Where-Object { $_ -ne '' }
$lines = @("package com.ansub.tvhome;", "final class Apps { static final String[] LIST = {") +
         ($apps | ForEach-Object { '    "' + $_ + '",' }) + @("}; }")
[IO.File]::WriteAllLines("$PWD\out\gen\com\ansub\tvhome\Apps.java", $lines)

& "$bt\aapt2.exe" link -o out\base.apk -I $jar --manifest AndroidManifest.xml --min-sdk-version 21 --target-sdk-version 34
Check "aapt2"

# Lista dos .java (com barras normais, o javac usa \ como escape no arquivo de argumentos)
$src = Get-ChildItem src, out\gen -Recurse -Filter *.java |
       ForEach-Object { '"' + ($_.FullName -replace '\\', '/') + '"' }
[IO.File]::WriteAllLines("$PWD\out\sources.txt", $src)
& javac -nowarn "-Xlint:-options" -encoding UTF-8 -source 1.8 -target 1.8 `
    -bootclasspath "$jar;$bt\core-lambda-stubs.jar" -d out\classes "@out\sources.txt"
Check "javac"

$classes = Get-ChildItem out\classes -Recurse -Filter *.class | ForEach-Object { $_.FullName }
& "$bt\d8.bat" --min-api 21 --release --lib $jar --output out\dex $classes
Check "d8"

Copy-Item out\base.apk out\unsigned.apk
Push-Location out\dex
& "$bt\aapt.exe" add ..\unsigned.apk classes.dex | Out-Null
Check "aapt add"
Pop-Location

& "$bt\zipalign.exe" -f 4 out\unsigned.apk out\aligned.apk
Check "zipalign"

# A chave e criada na primeira vez. Guarde o key.jks: atualizacoes precisam da mesma chave.
$ks = "key.jks"; $pass = "tvhome123"
if (-not (Test-Path $ks)) {
    & keytool -genkeypair -keystore $ks -storepass $pass -keypass $pass -alias tvhome `
        -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=tvhome" | Out-Null
    Check "keytool"
}
& "$bt\apksigner.bat" sign --ks $ks --ks-pass "pass:$pass" --out out\tvhome.apk out\aligned.apk
Check "apksigner"

Write-Host "Built out\tvhome.apk"
