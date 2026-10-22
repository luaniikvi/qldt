param(
    [string]$MySqlBase = 'C:\Program Files\MySQL\MySQL Server 8.0',
    [int]$Port = 3307,
    [string]$Database = 'qldt',
    [string]$User = 'qldt_app',
    [string]$Password = 'QldtApp@2026'
)

$mysql = Join-Path $MySqlBase 'bin\mysql.exe'
$uploadDir = Join-Path (Split-Path $PSScriptRoot -Parent) 'uploads'

if (-not (Test-Path $mysql)) {
    Write-Host "Khong tim thay mysql client: $mysql"
    exit 1
}
if (-not (Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue)) {
    Write-Host "MySQL instance ($Port) chua chay. Hay chay scripts\start-db.ps1 truoc."
    exit 1
}

$env:MYSQL_PWD = $Password
& $mysql -h 127.0.0.1 -P $Port --protocol=TCP -u $User -e "DROP DATABASE IF EXISTS $Database; CREATE DATABASE $Database CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
$code = $LASTEXITCODE
Remove-Item Env:\MYSQL_PWD -ErrorAction SilentlyContinue

if ($code -ne 0) {
    Write-Host "Reset database that bai (ma loi $code)"
    exit 1
}

if (Test-Path $uploadDir) {
    Get-ChildItem $uploadDir -File -ErrorAction SilentlyContinue | Remove-Item -Force
    Write-Host "Da xoa cac tep bao cao trong uploads"
}

Write-Host "Da reset database '$Database'"
Write-Host "Chay lai ung dung de tao bang va du lieu mau."
