# Khoi dong instance MySQL rieng cua du an (port 3307, datadir db/data)
$mysqld = 'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqld.exe'
$running = Get-NetTCPConnection -LocalPort 3307 -State Listen -ErrorAction SilentlyContinue
if ($running) {
    Write-Host 'MySQL instance (3307) dang chay'
    exit 0
}
Start-Process -FilePath $mysqld -ArgumentList '--datadir=F:\ChoKhoa\qldt\db\data','--port=3307','--mysqlx=0','--log-error=F:\ChoKhoa\qldt\db\logs\error.log','--pid-file=F:\ChoKhoa\qldt\db\logs\mysqld.pid' -WindowStyle Hidden
for ($i = 0; $i -lt 40; $i++) {
    Start-Sleep -Seconds 1
    if (Get-NetTCPConnection -LocalPort 3307 -State Listen -ErrorAction SilentlyContinue) {
        Write-Host 'MySQL instance (3307) da san sang'
        exit 0
    }
}
Write-Host 'Khong khoi dong duoc, xem db/logs/error.log'
exit 1
