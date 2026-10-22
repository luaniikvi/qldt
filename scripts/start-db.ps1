param(
    [string]$MySqlBase = 'C:\Program Files\MySQL\MySQL Server 8.0',
    [int]$Port = 3307
)

$mysqld = Join-Path $MySqlBase 'bin\mysqld.exe'
$dataDir = Join-Path $env:LOCALAPPDATA 'qldt-mysql\data'
$logDir = Join-Path $env:LOCALAPPDATA 'qldt-mysql\logs'

if (-not (Test-Path $mysqld)) {
    Write-Host "Khong tim thay mysqld: $mysqld"
    exit 1
}
if (-not (Test-Path $dataDir)) {
    Write-Host "Khong tim thay thu muc du lieu: $dataDir"
    exit 1
}
if (Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue) {
    Write-Host "MySQL instance ($Port) dang chay"
    exit 0
}

New-Item -ItemType Directory -Force -Path $logDir | Out-Null
Start-Process -FilePath $mysqld -ArgumentList "--datadir=$dataDir", "--port=$Port", '--mysqlx=0',
    "--log-error=$(Join-Path $logDir 'error.log')", "--pid-file=$(Join-Path $logDir 'mysqld.pid')" -WindowStyle Hidden

for ($i = 0; $i -lt 40; $i++) {
    Start-Sleep -Seconds 1
    if (Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue) {
        Write-Host "MySQL instance ($Port) da san sang"
        exit 0
    }
}

Write-Host "Khong khoi dong duoc MySQL, xem log tai $logDir\error.log"
exit 1
