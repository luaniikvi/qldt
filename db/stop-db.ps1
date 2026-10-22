# Dung instance MySQL rieng cua du an (port 3307)
Get-NetTCPConnection -LocalPort 3307 -State Listen -ErrorAction SilentlyContinue | ForEach-Object {
    Stop-Process -Id $_.OwningProcess -Force
    Write-Host 'Da dung MySQL instance (3307)'
}
