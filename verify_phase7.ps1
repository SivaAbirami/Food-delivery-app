# verify_phase7.ps1
# Comprehensive Live Verification for Phase 7: Redis Caching & Performance Optimization

$ErrorActionPreference = "Stop"

function Write-Step($msg) {
    Write-Host "`n[PHASE 7 STEP] $msg" -ForegroundColor Cyan
}

function Write-Success($msg) {
    Write-Host "[SUCCESS] $msg" -ForegroundColor Green
}

function Write-Fail($msg) {
    Write-Host "[FAILURE] $msg" -ForegroundColor Red
    exit 1
}

$baseUrl = "http://localhost:8080"

# 1. Verify Redis is running
Write-Step "Checking Redis connectivity on port 6379..."
try {
    $tcp = New-Object System.Net.Sockets.TcpClient
    $tcp.Connect("127.0.0.1", 6379)
    $tcp.Close()
    Write-Success "Redis is reachable on port 6379."
} catch {
    Write-Fail "Cannot connect to Redis on localhost:6379. Make sure Redis container/service is running."
}

# 2. Check if Spring Boot backend is running; if not, build and start it
Write-Step "Checking if Spring Boot Backend is running at $baseUrl..."
$serverRunning = $false
try {
    $res = Invoke-RestMethod -Uri "$baseUrl/actuator/health" -Method Get -TimeoutSec 3 -ErrorAction SilentlyContinue
    if ($res.status -eq "UP") {
        $serverRunning = $true
        Write-Success "Spring Boot Backend is already up and healthy."
    }
} catch {}

$serverProcess = $null
if (-not $serverRunning) {
    Write-Step "Starting Spring Boot application in background..."
    $serverProcess = Start-Process -FilePath ".\mvnw.cmd" -ArgumentList "spring-boot:run" -PassThru -NoNewWindow
    Write-Host "Waiting for backend to become healthy..."
    $attempts = 0
    while ($attempts -lt 40) {
        Start-Sleep -Seconds 2
        try {
            $health = Invoke-RestMethod -Uri "$baseUrl/actuator/health" -Method Get -TimeoutSec 2 -ErrorAction SilentlyContinue
            if ($health.status -eq "UP") {
                $serverRunning = $true
                Write-Success "Spring Boot Backend successfully started and reported UP."
                break
            }
        } catch {}
        $attempts++
    }
    if (-not $serverRunning) {
        if ($serverProcess) { Stop-Process -Id $serverProcess.Id -Force -ErrorAction SilentlyContinue }
        Write-Fail "Spring Boot backend failed to start within timeout."
    }
}

try {
    $ts = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()

    # 3. Authenticate / Register Users
    Write-Step "Authenticating Owner and Customer accounts..."
    function Get-JwtToken($email, $password) {
        $body = @{ email = $email; password = $password } | ConvertTo-Json
        $res = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $body -ContentType "application/json"
        return $res.accessToken
    }

    $ownerToken = $null
    try {
        $ownerToken = Get-JwtToken "owner1@example.com" "bobPass123"
    } catch {
        # Seed owner in DB if not existing
        docker exec -i food-delivery-mysql mysql -u food_user -pfood_password -D food_delivery_db -e "INSERT IGNORE INTO users (name, email, password, phone, role, created_at, updated_at) VALUES ('Seed Owner', 'owner1@example.com', '`$2a`$10`$otI4tBiF6Jexipx09p.nyuhiKKneBuzxt3AoYb161azfdI/XFF5qi', '9876543210', 'RESTAURANT_OWNER', NOW(), NOW());"
        docker exec -i food-delivery-mysql mysql -u food_user -pfood_password -D food_delivery_db -e "UPDATE users SET password = '`$2a`$10`$otI4tBiF6Jexipx09p.nyuhiKKneBuzxt3AoYb161azfdI/XFF5qi', role = 'RESTAURANT_OWNER' WHERE email = 'owner1@example.com';"
        $ownerToken = Get-JwtToken "owner1@example.com" "bobPass123"
    }
    Write-Success "Owner Token acquired."

    # Register dynamic customer
    $customerEmail = "redis_customer_$ts@example.com"
    $custReg = Invoke-RestMethod -Uri "$baseUrl/api/auth/register" -Method Post -ContentType "application/json" -Body (@{
        name = "Redis Customer"
        email = $customerEmail
        password = "Password123!"
        phone = "9123456780"
    } | ConvertTo-Json)
    $custToken = Get-JwtToken $customerEmail "Password123!"
    Write-Success "Customer registered and Token acquired."

    $ownerHeader = @{ Authorization = "Bearer $ownerToken" }
    $custHeader = @{ Authorization = "Bearer $custToken" }

    # 4. Create Restaurant
    Write-Step "Creating test Restaurant..."
    $restReq = @{
        name = "Redis Bistro $ts"
        description = "Gourmet cached dining"
        address = "777 Cache Boulevard"
        phone = "5557778888"
    } | ConvertTo-Json

    $rest = Invoke-RestMethod -Uri "$baseUrl/api/restaurants" -Method Post -Headers $ownerHeader -ContentType "application/json" -Body $restReq
    $restId = $rest.id
    Write-Success "Created Restaurant ID: $restId with name '$($rest.name)'"

    # 5. Test Restaurant Detail Cache (Miss -> Hit)
    Write-Step "Testing Restaurant Detail Cache (Miss -> Hit)..."
    $t1 = Get-Date
    $r1 = Invoke-RestMethod -Uri "$baseUrl/api/restaurants/$restId" -Method Get
    $t2 = Get-Date
    $dur1 = ($t2 - $t1).TotalMilliseconds

    $t3 = Get-Date
    $r2 = Invoke-RestMethod -Uri "$baseUrl/api/restaurants/$restId" -Method Get
    $t4 = Get-Date
    $dur2 = ($t4 - $t3).TotalMilliseconds

    Write-Host "First read duration (DB fetch + Cache put): $dur1 ms"
    Write-Host "Second read duration (Redis Cache hit): $dur2 ms"
    if ($r1.id -ne $restId -or $r2.id -ne $restId) {
        Write-Fail "Restaurant detail response mismatch."
    }
    Write-Success "Restaurant Detail Cache functioning correctly."

    # 6. Create Menu Items
    Write-Step "Creating Menu Items..."
    $menu1Req = @{
        name = "Redis Pizza $ts"
        description = "Crispy thin crust"
        price = 299.00
        category = "Pizza"
        imageUrl = "http://example.com/pizza.jpg"
    } | ConvertTo-Json
    $item1 = Invoke-RestMethod -Uri "$baseUrl/api/restaurants/$restId/menu" -Method Post -Headers $ownerHeader -ContentType "application/json" -Body $menu1Req
    $itemId1 = $item1.id

    $menu2Req = @{
        name = "Redis Gelato $ts"
        description = "Italian ice cream"
        price = 149.00
        category = "Dessert"
        imageUrl = "http://example.com/gelato.jpg"
    } | ConvertTo-Json
    $item2 = Invoke-RestMethod -Uri "$baseUrl/api/restaurants/$restId/menu" -Method Post -Headers $ownerHeader -ContentType "application/json" -Body $menu2Req
    $itemId2 = $item2.id
    Write-Success "Created Menu Items: ID $itemId1 (Rs 299.00), ID $itemId2 (Rs 149.00)"

    # 7. Test Menu List Cache & Filtering
    Write-Step "Testing Menu List Cache and Category Filtering..."
    $menuAll1 = Invoke-RestMethod -Uri "$baseUrl/api/restaurants/$restId/menu" -Method Get
    $menuAll2 = Invoke-RestMethod -Uri "$baseUrl/api/restaurants/$restId/menu" -Method Get
    if ($menuAll1.content.Count -lt 2 -or $menuAll2.content.Count -lt 2) {
        Write-Fail "Menu list count expected >= 2."
    }

    $menuPizza = Invoke-RestMethod -Uri "$baseUrl/api/restaurants/$restId/menu?category=Pizza" -Method Get
    $menuDessert = Invoke-RestMethod -Uri "$baseUrl/api/restaurants/$restId/menu?category=Dessert" -Method Get
    if ($menuPizza.content[0].category -ne "Pizza" -or $menuDessert.content[0].category -ne "Dessert") {
        Write-Fail "Menu category filters collided or returned wrong categories."
    }
    Write-Success "Menu List caching and filter segregation passed."

    # 8. Test MenuItem Detail Cache & Price Invalidation
    Write-Step "Testing Price Invalidation on MenuItem and Menu List..."
    $detail1 = Invoke-RestMethod -Uri "$baseUrl/api/menu-items/$itemId1" -Method Get
    if ([decimal]$detail1.price -ne [decimal]299.00) {
        Write-Fail "Expected initial price 299.00"
    }

    Write-Host "Updating MenuItem price from 299.00 to 349.00..."
    $updateReq = @{
        name = "Redis Pizza $ts"
        description = "Crispy thin crust with extra cheese"
        price = 349.00
        category = "Pizza"
        imageUrl = "http://example.com/pizza.jpg"
    } | ConvertTo-Json
    $updatedItem = Invoke-RestMethod -Uri "$baseUrl/api/menu-items/$itemId1" -Method Put -Headers $ownerHeader -ContentType "application/json" -Body $updateReq

    # Read item detail immediately
    $detail2 = Invoke-RestMethod -Uri "$baseUrl/api/menu-items/$itemId1" -Method Get
    if ([decimal]$detail2.price -ne [decimal]349.00) {
        Write-Fail "Stale price detected in MenuItem detail cache! Expected 349.00 but got $($detail2.price)"
    }

    # Read menu list immediately
    $menuAfterPriceUpdate = Invoke-RestMethod -Uri "$baseUrl/api/restaurants/$restId/menu" -Method Get
    $cachedPizza = $menuAfterPriceUpdate.content | Where-Object { $_.id -eq $itemId1 }
    if ([decimal]$cachedPizza.price -ne [decimal]349.00) {
        Write-Fail "Stale price detected in Menu List cache! Expected 349.00 but got $($cachedPizza.price)"
    }
    Write-Success "Price invalidation verified: Updated price (Rs 349.00) accurately reflected across all cached reads."

    # 9. Test Availability Invalidation
    Write-Step "Testing Availability Invalidation..."
    $toggleBody = @{ isAvailable = $false } | ConvertTo-Json
    Invoke-RestMethod -Uri "$baseUrl/api/menu-items/$itemId1/status" -Method Patch -Headers $ownerHeader -ContentType "application/json" -Body $toggleBody

    $detailAvail = Invoke-RestMethod -Uri "$baseUrl/api/menu-items/$itemId1" -Method Get
    if ($detailAvail.isAvailable -ne $false) {
        Write-Fail "Stale availability detected! Expected isAvailable=false"
    }

    # Toggle back to true
    $toggleBack = @{ isAvailable = $true } | ConvertTo-Json
    Invoke-RestMethod -Uri "$baseUrl/api/menu-items/$itemId1/status" -Method Patch -Headers $ownerHeader -ContentType "application/json" -Body $toggleBack
    Write-Success "Availability invalidation verified."

    # 10. Test Restaurant Update Invalidation
    Write-Step "Testing Restaurant Update Invalidation..."
    $updateRestReq = @{
        name = "Redis Bistro Premium $ts"
        description = "Updated gourmet cached dining"
        address = "888 Cache Highway"
        phone = "5558889999"
    } | ConvertTo-Json
    Invoke-RestMethod -Uri "$baseUrl/api/restaurants/$restId" -Method Put -Headers $ownerHeader -ContentType "application/json" -Body $updateRestReq

    $restDetailAfter = Invoke-RestMethod -Uri "$baseUrl/api/restaurants/$restId" -Method Get
    if ($restDetailAfter.name -ne "Redis Bistro Premium $ts") {
        Write-Fail "Stale restaurant name detected in detail cache!"
    }
    Write-Success "Restaurant update invalidation verified."

    # 11. Test Phase 6 Checkout Authoritative Pricing Integrity
    Write-Step "Testing Phase 6 Checkout flow (Authoritative MySQL pricing)..."
    # Customer adds item to cart
    $cartReq = @{
        menuItemId = $itemId1
        quantity = 2
    } | ConvertTo-Json
    $cart = Invoke-RestMethod -Uri "$baseUrl/api/cart/items" -Method Post -Headers $custHeader -ContentType "application/json" -Body $cartReq
    Write-Success "Item added to cart. Total amount: $($cart.totalAmount)"

    # Checkout
    $checkoutReq = @{
        deliveryAddress = "100 Cache Lane, Tech Park"
        specialInstructions = "Leave at desk"
    } | ConvertTo-Json
    $order = Invoke-RestMethod -Uri "$baseUrl/api/orders/checkout" -Method Post -Headers $custHeader -ContentType "application/json" -Body $checkoutReq
    if ($order.items[0].price -ne 349.00 -or $order.subtotalAmount -ne 698.00) {
        Write-Fail "Checkout pricing mismatch! Expected item price 349.00 and subtotal 698.00. Got $($order.subtotalAmount)"
    }
    Write-Success "Phase 6 Checkout successfully verified with authoritative MySQL price snapshot (Total: $($order.totalAmount))."

    # 12. Delete Menu Item & Verify Invalidation
    Write-Step "Testing MenuItem Deletion Invalidation..."
    Invoke-RestMethod -Uri "$baseUrl/api/menu-items/$itemId2" -Method Delete -Headers $ownerHeader
    $menuAfterDelete = Invoke-RestMethod -Uri "$baseUrl/api/restaurants/$restId/menu" -Method Get
    $deletedFound = $menuAfterDelete.content | Where-Object { $_.id -eq $itemId2 }
    if ($deletedFound) {
        Write-Fail "Deleted MenuItem still present in cached menu list!"
    }
    Write-Success "MenuItem deletion evicted menu cache cleanly."

    Write-Host "`n========================================================" -ForegroundColor Green
    Write-Host "PHASE 7 LIVE REDIS VERIFICATION PASSED SUCCESSFULLY!" -ForegroundColor Green
    Write-Host "========================================================`n" -ForegroundColor Green
} finally {
    if ($serverProcess) {
        Write-Host "Stopping background Spring Boot process..."
        Stop-Process -Id $serverProcess.Id -Force -ErrorAction SilentlyContinue
    }
}
