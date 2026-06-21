$ErrorActionPreference = 'Stop'

$rootDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$logDir = Join-Path $rootDir 'logs'
$files = @(
  (Join-Path $logDir 'common-all.log'),
  (Join-Path $logDir 'common-error.log'),
  (Join-Path $logDir 'nohup.log'),
  (Join-Path $logDir 'nohup-error.log')
)

Write-Host ''
Write-Host '=== Jusic log tail ==='
Write-Host '按 Ctrl+C 关闭窗口'
Write-Host ''

$positions = @{}

while ($true) {
  foreach ($file in $files) {
    if (-not (Test-Path $file)) {
      continue
    }

    $stream = [System.IO.File]::Open($file, [System.IO.FileMode]::Open, [System.IO.FileAccess]::Read, [System.IO.FileShare]::ReadWrite)
    try {
      $length = $stream.Length
      if (-not $positions.ContainsKey($file)) {
        $positions[$file] = [int64]0
      } elseif ($positions[$file] -gt $length) {
        $positions[$file] = [int64]0
      }

      if ($length -gt $positions[$file]) {
        $stream.Seek($positions[$file], [System.IO.SeekOrigin]::Begin) | Out-Null
        $reader = New-Object System.IO.StreamReader($stream, [System.Text.Encoding]::UTF8, $true, 4096, $true)
        try {
          if ($positions[$file] -eq 0) {
            Write-Host "----- $file -----"
          }
          while (-not $reader.EndOfStream) {
            $line = $reader.ReadLine()
            if ($null -ne $line) {
              Write-Host $line
            }
          }
        } finally {
          $reader.Dispose()
        }
        $positions[$file] = $stream.Position
      }
    } finally {
      $stream.Dispose()
    }
  }

  Start-Sleep -Seconds 2
}
