# =============================================================================
# Swiggy Food Delivery — Automated 1-Click Launch Script (Windows PowerShell)
#
# Clones / launches the full system:
# 1. Starts MySQL 8.0 & Redis 7 Docker containers (no observability bloat)
# 2. Waits for MySQL readiness & idempotently seeds 12 restaurants, users, offers
# 3. Starts Spring Boot 3 backend (port 8080)
# 4. Starts Vite React frontend (port 5173)
# 5. Opens http://localhost:5173 in your default browser
# =============================================================================

$ErrorActionPreference = "Continue"

Write-Host ""
Write-Host "======================================================================" -ForegroundColor Cyan
Write-Host "       🍔  SWIGGY FOOD DELIVERY ECOSYSTEM — AUTOMATED STARTUP  🛵      " -ForegroundColor Yellow
Write-Host "======================================================================" -ForegroundColor Cyan
Write-Host ""

# 1. Check Docker status
Write-Host "▶ Step 1/5: Checking Docker engine status..." -ForegroundColor White
docker info > $null 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Error: Docker is not running. Please start Docker Desktop and run this script again." -ForegroundColor Red
    Pause
    exit 1
}
Write-Host "   ✔ Docker engine is active." -ForegroundColor Green

# 2. Start MySQL and Redis containers
Write-Host "`n▶ Step 2/5: Starting database & cache containers (MySQL + Redis)..." -ForegroundColor White
docker compose up -d
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Failed to start docker containers via docker compose." -ForegroundColor Red
    Pause
    exit 1
}

# 3. Wait for MySQL to become ready
Write-Host "   Waiting for MySQL to accept connections..." -ForegroundColor Gray
$retryCount = 0
$maxRetries = 25
$mysqlReady = $false

while (-not $mysqlReady -and ($retryCount -lt $maxRetries)) {
    Start-Sleep -Seconds 2
    $retryCount++
    docker exec food-delivery-mysql mysqladmin ping -h localhost -u root -proot_password > $null 2>&1
    if ($LASTEXITCODE -eq 0) {
        $mysqlReady = $true
        break
    }
    Write-Host "   ... waiting for database ($retryCount/$maxRetries)" -ForegroundColor DarkGray
}

if (-not $mysqlReady) {
    Write-Host "❌ MySQL took too long to start. Please check 'docker logs food-delivery-mysql'." -ForegroundColor Red
    Pause
    exit 1
}
Write-Host "   ✔ MySQL and Redis are healthy and ready!" -ForegroundColor Green

# 4. Seed database
Write-Host "`n▶ Step 3/5: Seeding 12 Restaurants, 80+ Dishes, Customers, Riders & Coupons..." -ForegroundColor White
if (Test-Path "seed_swiggy_db.sql") {
    Get-Content "seed_swiggy_db.sql" -Raw | docker exec -i food-delivery-mysql mysql -u root -proot_password food_delivery_db
    if ($LASTEXITCODE -eq 0) {
        Write-Host "   ✔ Database seeded successfully!" -ForegroundColor Green
    } else {
        Write-Host "   ⚠ Database seed reported notices (data already present or created)." -ForegroundColor Yellow
    }
} else {
    Write-Host "   ⚠ seed_swiggy_db.sql not found in root directory; skipping manual seed." -ForegroundColor Yellow
}

# 5. Check frontend dependencies
Write-Host "`n▶ Step 4/5: Verifying frontend node dependencies..." -ForegroundColor White
if (-not (Test-Path "frontend/node_modules")) {
    Write-Host "   Installing frontend node packages (one-time setup)..." -ForegroundColor Cyan
    npm install --prefix frontend
} else {
    Write-Host "   ✔ Frontend dependencies are ready." -ForegroundColor Green
}

# 6. Start Backend & Frontend in dedicated windows
Write-Host "`n▶ Step 5/5: Launching Spring Boot backend and React frontend..." -ForegroundColor White

# Launch Spring Boot in a new console window
$backendCommand = "Write-Host 'Starting Spring Boot Backend on http://localhost:8080...' -ForegroundColor Cyan; .\mvnw.cmd spring-boot:run"
Start-Process powershell -ArgumentList "-NoExit", "-Command", $backendCommand

# Launch Frontend in a new console window
$frontendCommand = "Write-Host 'Starting Vite Frontend on http://localhost:5173...' -ForegroundColor Green; npm run dev --prefix frontend"
Start-Process powershell -ArgumentList "-NoExit", "-Command", $frontendCommand

Write-Host "   ✔ Backend and Frontend processes launched in dedicated windows." -ForegroundColor Green

# 7. Wait briefly and open browser
Write-Host "`nWaiting for web server to initialize (5s)..." -ForegroundColor DarkGray
Start-Sleep -Seconds 5

Start-Process "http://localhost:5173"

Write-Host ""
Write-Host "======================================================================" -ForegroundColor Green
Write-Host "       🎉  ALL SERVICES STARTED & BROWSER OPENED!  🎉                " -ForegroundColor Yellow
Write-Host "======================================================================" -ForegroundColor Green
Write-Host ""
Write-Host " 🌐 Customer Portal / Gateway:  http://localhost:5173" -ForegroundColor White
Write-Host " 🍳 Restaurant Partner Tab:     http://localhost:5173/?portal=partner" -ForegroundColor White
Write-Host " 🛵 Delivery Fleet Rider Tab:   http://localhost:5173/?portal=rider" -ForegroundColor White
Write-Host " ⚡ Spring Boot API Health:     http://localhost:8080/api/health" -ForegroundColor White
Write-Host ""
Write-Host " 🔑 Default Demo Password:      bobPass123" -ForegroundColor Yellow
Write-Host "    - Customer:                 customer@swiggy.in (Rahul Sharma)" -ForegroundColor Gray
Write-Host "    - Restaurant:               owner@meghana.in (Meghana Foods)" -ForegroundColor Gray
Write-Host "    - Delivery Rider:           rider1@swiggy.in (Ramesh Rider)" -ForegroundColor Gray
Write-Host ""
Write-Host " To stop all services later, run .\stop.bat or .\stop.ps1" -ForegroundColor DarkGray
Write-Host ""

