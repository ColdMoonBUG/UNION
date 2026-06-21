$ErrorActionPreference = 'Stop'

$rootDir = Split-Path -Parent $MyInvocation.MyCommand.Path

Set-Location $rootDir
& "$rootDir\start.ps1"
& "$rootDir\watch-logs.ps1"
