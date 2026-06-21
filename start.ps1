$ErrorActionPreference = 'Stop'

$rootDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$appName = 'jusic-serve'
$httpPort = 7888
$logDir = Join-Path $rootDir 'logs'
$pidFile = Join-Path $logDir "$appName.pid"
$bootOutLog = Join-Path $logDir 'nohup.log'
$bootErrLog = Join-Path $logDir 'nohup-error.log'
$redisBootLog = Join-Path $logDir 'redis.log'
$redisBootErrLog = Join-Path $logDir 'redis-error.log'

if (-not (Test-Path $logDir)) {
  New-Item -ItemType Directory -Path $logDir | Out-Null
}

if (-not $env:JUSIC_PROFILE) {
  $env:JUSIC_PROFILE = 'memory'
}
if (-not $env:JUSIC_STORAGE) {
  $env:JUSIC_STORAGE = 'memory'
}

function Test-TcpPort {
  param(
    [string]$HostName,
    [int]$Port,
    [int]$TimeoutMs = 1500
  )

  try {
    $client = New-Object System.Net.Sockets.TcpClient
    $result = $client.BeginConnect($HostName, $Port, $null, $null)
    if (-not $result.AsyncWaitHandle.WaitOne($TimeoutMs, $false)) {
      $client.Close()
      return $false
    }
    $client.EndConnect($result) | Out-Null
    $client.Close()
    return $true
  } catch {
    return $false
  }
}

function Get-TcpListeningPid {
  param(
    [int]$Port
  )

  try {
    $lines = & netstat -ano | Where-Object { $_ -match "[:\]]$Port\s+.*LISTENING\s+\d+\s*$" }
    foreach ($line in $lines) {
      if ($line -match '\s(\d+)\s*$') {
        return [int]$matches[1]
      }
    }
  } catch {
  }

  return $null
}

function Wait-Http {
  param(
    [string]$Url,
    [int]$TimeoutSec = 30
  )

  for ($i = 0; $i -lt $TimeoutSec; $i++) {
    try {
      $resp = Invoke-WebRequest -UseBasicParsing -TimeoutSec 2 $Url
      if ($resp.StatusCode -ge 200 -and $resp.StatusCode -lt 500) {
        return $true
      }
    } catch {
    }
    Start-Sleep -Seconds 1
  }
  return $false
}

function Resolve-RedisServer {
  $candidate = $env:REDIS_SERVER_EXE
  if ($candidate -and (Test-Path $candidate)) {
    return $candidate
  }
  try {
    $found = (Get-Command redis-server.exe -ErrorAction SilentlyContinue | Select-Object -First 1).Source
    if ($found) {
      return $found
    }
  } catch {
  }
  return $null
}

function Open-Url {
  param([string]$Url)

  try {
    Start-Process $Url | Out-Null
  } catch {
  }
}

function Stop-AppProcesses {
  param(
    [int]$TimeoutSec = 15
  )

  if (Test-TcpPort -HostName '127.0.0.1' -Port $httpPort -TimeoutMs 800) {
    Write-Host "Port $httpPort is already in use, reusing the existing service."
    $script:ExistingServicePid = Get-TcpListeningPid -Port $httpPort
    return
  }

  if (Test-Path $pidFile) {
    $oldPid = (Get-Content $pidFile -ErrorAction SilentlyContinue | Select-Object -First 1).Trim()
    if ($oldPid -match '^\d+$') {
      $oldPidValue = [int]$oldPid
      Write-Host "Stopping existing $appName (PID=$oldPidValue)..."
      $stopped = $false
      try {
        & taskkill /PID $oldPidValue /T /F | Out-Null
        if ($LASTEXITCODE -eq 0) {
          $stopped = $true
        }
      } catch {
      }
      if (-not $stopped) {
        Write-Host "Failed to stop PID=$oldPidValue cleanly, checking whether the port is already free..."
        for ($i = 0; $i -lt $TimeoutSec; $i++) {
          $alive = Get-Process -Id $oldPidValue -ErrorAction SilentlyContinue
          if (-not $alive -and -not (Test-TcpPort -HostName '127.0.0.1' -Port $httpPort -TimeoutMs 500)) {
            $stopped = $true
            break
          }
          Start-Sleep -Seconds 1
        }
      }

      if (-not $stopped) {
        Write-Host "Existing process could not be confirmed stopped, but startup will continue."
      }
    }
  }

  Remove-Item $pidFile -Force -ErrorAction SilentlyContinue
}

Stop-AppProcesses

if ($script:ExistingServicePid) {
  [System.IO.File]::WriteAllText($pidFile, [string]$script:ExistingServicePid, [System.Text.Encoding]::ASCII)
  if (Wait-Http -Url "http://localhost:$httpPort/" -TimeoutSec 5) {
    Open-Url "http://localhost:$httpPort/"
  }

  Write-Host "$appName is already running, PID=$script:ExistingServicePid"
  Write-Host "main log: $logDir\common-all.log"
  Write-Host "error log: $logDir\common-error.log"
  Write-Host "boot log: $bootOutLog"
  Write-Host "boot error log: $bootErrLog"
  Write-Host "redis log: $redisBootLog"
  Write-Host "redis error log: $redisBootErrLog"
  return
}

if ($env:JUSIC_PROFILE -eq 'redis' -or $env:JUSIC_STORAGE -eq 'redis') {
  if (-not (Test-TcpPort -HostName '127.0.0.1' -Port 6379 -TimeoutMs 1000)) {
    $redisExe = Resolve-RedisServer
    if (-not $redisExe) {
      throw 'Redis is not available on 127.0.0.1:6379 and redis-server.exe was not found.'
    }

    Write-Host 'Redis not detected, trying to start it...'
    if (Test-Path $redisBootLog) { Remove-Item $redisBootLog -Force -ErrorAction SilentlyContinue }
    if (Test-Path $redisBootErrLog) { Remove-Item $redisBootErrLog -Force -ErrorAction SilentlyContinue }

    $redisArgs = @()
    if ($env:REDIS_CONF -and (Test-Path $env:REDIS_CONF)) {
      $redisArgs += $env:REDIS_CONF
    }

    Start-Process -FilePath $redisExe -ArgumentList $redisArgs -WorkingDirectory $rootDir -WindowStyle Hidden -RedirectStandardOutput $redisBootLog -RedirectStandardError $redisBootErrLog | Out-Null

    if (-not (Test-TcpPort -HostName '127.0.0.1' -Port 6379 -TimeoutMs 1000)) {
      Start-Sleep -Seconds 2
    }
    if (-not (Test-TcpPort -HostName '127.0.0.1' -Port 6379 -TimeoutMs 1000)) {
      throw 'Redis startup failed, port 6379 is still unavailable.'
    }
    Write-Host 'Redis started successfully.'
  }
} else {
  Write-Host 'Using memory mode, Redis is not required.'
}

if (Test-Path $bootOutLog) { Remove-Item $bootOutLog -Force -ErrorAction SilentlyContinue }
if (Test-Path $bootErrLog) { Remove-Item $bootErrLog -Force -ErrorAction SilentlyContinue }

$runArgs = @('-q', '-DskipTests', 'spring-boot:run')
$javaProc = Start-Process -FilePath (Join-Path $rootDir 'mvnw.cmd') -ArgumentList $runArgs -WorkingDirectory $rootDir -RedirectStandardOutput $bootOutLog -RedirectStandardError $bootErrLog -WindowStyle Hidden -PassThru
Start-Sleep -Seconds 5
if (-not (Get-Process -Id $javaProc.Id -ErrorAction SilentlyContinue)) {
  throw 'Application process exited too early.'
}

[System.IO.File]::WriteAllText($pidFile, [string]$javaProc.Id, [System.Text.Encoding]::ASCII)

if (Wait-Http -Url "http://localhost:$httpPort/" -TimeoutSec 60) {
  Open-Url "http://localhost:$httpPort/"
} else {
  Write-Host "Service started, but http://localhost:$httpPort/ is not responding yet."
}

Write-Host "$appName started successfully, PID=$($javaProc.Id)"
Write-Host "main log: $logDir\common-all.log"
Write-Host "error log: $logDir\common-error.log"
Write-Host "boot log: $bootOutLog"
Write-Host "boot error log: $bootErrLog"
Write-Host "redis log: $redisBootLog"
Write-Host "redis error log: $redisBootErrLog"
Write-Host "stop: taskkill /PID $($javaProc.Id) /T /F"
