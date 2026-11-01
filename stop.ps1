# =============================================================================
# Swiggy Food Delivery — Graceful Shutdown Script (Windows PowerShell)
#
# Stops:
# 1. Frontend dev server process on port 5173
# 2. Spring Boot backend process on port 8080
# 3. MySQL and Redis docker containers
# =============================================================================

Write-Host "`nStopping Swiggy services..." -ForegroundColor Cyan

# Stop processes listening on 5173 and 8080
$ports = @(5173, 8080)
foreach ($port in $ports) {
    $conns = Get-NetTCPConnection -LocalPort $port -ErrorAction SilentlyContinue
    if ($conns) {
        $pids = $conns.OwningProcess | Select-Object -Unique
        foreach ($p in $pids) {
            try {
                Stop-Process -Id $p -Force -ErrorAction SilentlyContinue
                Write-Host "   ✔ Stopped process $p on port $port" -ForegroundColor Green
            } catch {}
        }
    }
}

# Stop Docker containers
Write-Host "   Stopping Docker containers..." -ForegroundColor White
docker compose down

Write-Host "`n✔ All Swiggy services stopped cleanly.`n" -ForegroundColor Green

