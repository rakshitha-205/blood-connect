param([switch]$Test)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$tools = Join-Path $env:LOCALAPPDATA "bloodconnect-tools"

$jdk = Get-ChildItem $tools -Directory -Filter "jdk-*" -ErrorAction SilentlyContinue | Select-Object -First 1
$maven = Get-ChildItem $tools -Directory -Filter "apache-maven-*" -ErrorAction SilentlyContinue | Select-Object -First 1
if ($jdk) { $env:JAVA_HOME = $jdk.FullName; $env:PATH = "$($jdk.FullName)\bin;$env:PATH" }
if ($maven) { $env:PATH = "$($maven.FullName)\bin;$env:PATH" }

$pom = Join-Path $root "backend\pom.xml"
Set-Location $root
if ($Test) {
  $env:MAVEN_OPTS = "-Xmx384m"
  mvn -q -f $pom test
} else {
  $env:MAVEN_OPTS = "-Xmx256m"
  mvn -f $pom "-Dspring-boot.run.profiles=local" spring-boot:run
}
exit $LASTEXITCODE
