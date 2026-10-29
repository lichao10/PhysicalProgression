$GradleArgs = @($args)
$ErrorActionPreference = 'Stop'
$conditioningRoot = $PSScriptRoot
$conditioningLocalFile = Join-Path $conditioningRoot 'gradle/local.properties'
$conditioningJvmArgs = @()
$conditioningOldJavaHome = $env:JAVA_HOME
try {
    if (Test-Path -LiteralPath $conditioningLocalFile) {
        foreach ($conditioningLine in Get-Content -LiteralPath $conditioningLocalFile) {
            if ($conditioningLine -match '^([^#=]+)=(.*)$') {
                $conditioningKey = $Matches[1].Trim()
                $conditioningValue = $Matches[2].Trim()
                if ($conditioningKey -eq 'org.gradle.java.home') {
                    $env:JAVA_HOME = $conditioningValue
                    $conditioningJvmArgs += "-Dorg.gradle.java.home=$conditioningValue"
                } elseif ($conditioningKey.StartsWith('systemProp.')) {
                    $conditioningJvmArgs += "-D$($conditioningKey.Substring(11))=$conditioningValue"
                }
            }
        }
    }
    $conditioningJava = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin/java.exe' } else { 'java' }
    & $conditioningJava @conditioningJvmArgs '-Dorg.gradle.appname=gradlew' '-classpath' (Join-Path $conditioningRoot 'gradle/wrapper/gradle-wrapper.jar') 'org.gradle.wrapper.GradleWrapperMain' @GradleArgs
    exit $LASTEXITCODE
} finally { $env:JAVA_HOME = $conditioningOldJavaHome }
