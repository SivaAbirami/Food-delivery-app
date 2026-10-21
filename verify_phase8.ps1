# verify_phase8.ps1
# Comprehensive Live Verification for Phase 8: Delivery & Dispatch Management

$ErrorActionPreference = "Stop"

function Write-Step($msg) {
    Write-Host "`n[PHASE 8 STEP] $msg" -ForegroundColor Cyan
}

function Write-Success($msg) {
    Write-Host "[SUCCESS] $msg" -ForegroundColor Green
}

function Write-Fail($msg) {
    Write-Host "[FAILURE] $msg" -ForegroundColor Red
    exit 1
}

$baseUrl = "http://localhost:8080"

# 1. Verify Redis connectivity
Write-Step "Checking Redis connectivity on port 6379..."
try {
    $tcp = New-Object System.Net.Sockets.TcpClient
    $tcp.Connect("127.0.0.1", 6379)
    $tcp.Close()
    Write-Success "Redis is reachable on port 6379."
} catch {
    Write-Fail "Cannot connect to Redis on localhost:6379. Make sure Redis is running."
}

# 2. Check if Spring Boot backend is running; if not, start it
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

    # 3. Authenticate Users & Seed if needed
    Write-Step "Authenticating Users (Owner, Drivers, Customers)..."
    function Get-JwtToken($email, $password) {
        $body = @{ email = $email; password = $password } | ConvertTo-Json
        $res = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $body -ContentType "application/json"
        return $res.accessToken
    }

    # Ensure Owner 1 & Owner 2 exist
    docker exec -i food-delivery-mysql mysql -u food_user -pfood_password -D food_delivery_db -e "INSERT IGNORE INTO users (name, email, password, phone, role, created_at, updated_at) VALUES ('Chef Luigi', 'owner1@example.com', '`$2a`$10`$otI4tBiF6Jexipx09p.nyuhiKKneBuzxt3AoYb161azfdI/XFF5qi', '9876543210', 'RESTAURANT_OWNER', NOW(), NOW());"
    docker exec -i food-delivery-mysql mysql -u food_user -pfood_password -D food_delivery_db -e "INSERT IGNORE INTO users (name, email, password, phone, role, created_at, updated_at) VALUES ('Chef Mario', 'owner2@example.com', '`$2a`$10`$otI4tBiF6Jexipx09p.nyuhiKKneBuzxt3AoYb161azfdI/XFF5qi', '9876543211', 'RESTAURANT_OWNER', NOW(), NOW());"

    # Ensure Driver 1 & Driver 2 exist
    docker exec -i food-delivery-mysql mysql -u food_user -pfood_password -D food_delivery_db -e "INSERT IGNORE INTO users (name, email, password, phone, role, created_at, updated_at) VALUES ('Driver Dan', 'driver1@example.com', '`$2a`$10`$otI4tBiF6Jexipx09p.nyuhiKKneBuzxt3AoYb161azfdI/XFF5qi', '9988776655', 'DELIVERY_PARTNER', NOW(), NOW());"
    docker exec -i food-delivery-mysql mysql -u food_user -pfood_password -D food_delivery_db -e "INSERT IGNORE INTO users (name, email, password, phone, role, created_at, updated_at) VALUES ('Driver Dave', 'driver2@example.com', '`$2a`$10`$otI4tBiF6Jexipx09p.nyuhiKKneBuzxt3AoYb161azfdI/XFF5qi', '9988776644', 'DELIVERY_PARTNER', NOW(), NOW());"
    docker exec -i food-delivery-mysql mysql -u food_user -pfood_password -D food_delivery_db -e "UPDATE users SET password = '`$2a`$10`$otI4tBiF6Jexipx09p.nyuhiKKneBuzxt3AoYb161azfdI/XFF5qi' WHERE email IN ('owner1@example.com', 'owner2@example.com', 'driver1@example.com', 'driver2@example.com');"

    $owner1Token = Get-JwtToken "owner1@example.com" "bobPass123"
    $owner2Token = Get-JwtToken "owner2@example.com" "bobPass123"
    $driver1Token = Get-JwtToken "driver1@example.com" "bobPass123"
    $driver2Token = Get-JwtToken "driver2@example.com" "bobPass123"

    # Register Customer 1 & Customer 2
    $cust1Email = "customer1_$ts@example.com"
    $cust2Email = "customer2_$ts@example.com"
    Invoke-RestMethod -Uri "$baseUrl/api/auth/register" -Method Post -ContentType "application/json" -Body (@{
        name = "Customer Alice"
        email = $cust1Email
        password = "Password123!"
        phone = "9112233445"
    } | ConvertTo-Json) | Out-Null
    $cust1Token = Get-JwtToken $cust1Email "Password123!"

    Invoke-RestMethod -Uri "$baseUrl/api/auth/register" -Method Post -ContentType "application/json" -Body (@{
        name = "Customer Bob"
        email = $cust2Email
        password = "Password123!"
        phone = "9223344556"
    } | ConvertTo-Json) | Out-Null
    $cust2Token = Get-JwtToken $cust2Email "Password123!"

    Write-Success "Tokens acquired for Owner 1, Owner 2, Driver 1, Driver 2, Customer 1, and Customer 2."

    $owner1Header = @{ Authorization = "Bearer $owner1Token" }
    $owner2Header = @{ Authorization = "Bearer $owner2Token" }
    $driver1Header = @{ Authorization = "Bearer $driver1Token" }
    $driver2Header = @{ Authorization = "Bearer $driver2Token" }
    $cust1Header = @{ Authorization = "Bearer $cust1Token" }
    $cust2Header = @{ Authorization = "Bearer $cust2Token" }

    # 4. Create Restaurant and MenuItem
    Write-Step "Setting up Restaurant and Menu Items for Owner 1..."
    $restReq = @{
        name = "Dispatch Trattoria $ts"
        description = "Quick delivery specials"
        address = "123 Dispatch Way"
        phone = "5551234567"
    } | ConvertTo-Json
    $rest = Invoke-RestMethod -Uri "$baseUrl/api/restaurants" -Method Post -Headers $owner1Header -ContentType "application/json" -Body $restReq
    $restId = $rest.id

    $menuReq = @{
        name = "Dispatch Pizza $ts"
        description = "Freshly baked pizza"
        price = 350.00
        category = "Main Course"
        imageUrl = "http://example.com/pizza.jpg"
    } | ConvertTo-Json
    $item = Invoke-RestMethod -Uri "$baseUrl/api/restaurants/$restId/menu" -Method Post -Headers $owner1Header -ContentType "application/json" -Body $menuReq
    $itemId = $item.id
    Write-Success "Created Restaurant ID: $restId, MenuItem ID: $itemId"

    # 5. Customer places order
    Write-Step "Customer 1 placing order..."
    Invoke-RestMethod -Uri "$baseUrl/api/cart/items" -Method Post -Headers $cust1Header -ContentType "application/json" -Body (@{
        menuItemId = $itemId
        quantity = 2
    } | ConvertTo-Json) | Out-Null

    $checkoutReq = @{
        deliveryAddress = "Apartment 5B, 888 Skyline Road"
        specialInstructions = "Call when arriving"
    } | ConvertTo-Json
    $order = Invoke-RestMethod -Uri "$baseUrl/api/orders/checkout" -Method Post -Headers $cust1Header -ContentType "application/json" -Body $checkoutReq
    $orderId = $order.id
    Write-Success "Order created: ID $orderId (OrderNumber: $($order.orderNumber), Status: $($order.status))"

    # 6. Restaurant Owner progresses order to READY_FOR_PICKUP
    Write-Step "Owner 1 progressing order to READY_FOR_PICKUP..."
    Invoke-RestMethod -Uri "$baseUrl/api/owner/orders/$orderId/status" -Method Patch -Headers $owner1Header -ContentType "application/json" -Body (@{ status = "ACCEPTED" } | ConvertTo-Json) | Out-Null
    Invoke-RestMethod -Uri "$baseUrl/api/owner/orders/$orderId/status" -Method Patch -Headers $owner1Header -ContentType "application/json" -Body (@{ status = "PREPARING" } | ConvertTo-Json) | Out-Null
    $orderReady = Invoke-RestMethod -Uri "$baseUrl/api/owner/orders/$orderId/status" -Method Patch -Headers $owner1Header -ContentType "application/json" -Body (@{ status = "READY_FOR_PICKUP" } | ConvertTo-Json)
    if ($orderReady.status -ne "READY_FOR_PICKUP") {
        Write-Fail "Order status is not READY_FOR_PICKUP!"
    }
    Write-Success "Order is in READY_FOR_PICKUP status."

    # 7. Available Deliveries Discovery (Phase 8 API)
    Write-Step "Driver 1 discovering available deliveries via GET /api/deliveries/available..."
    $avail = Invoke-RestMethod -Uri "$baseUrl/api/deliveries/available" -Method Get -Headers $driver1Header
    $targetAvailable = $avail.content | Where-Object { $_.orderId -eq $orderId }
    if ($null -eq $targetAvailable) {
        Write-Fail "Order ID $orderId not found in available deliveries list!"
    }
    Write-Success "Available delivery discovered: Order ID $($targetAvailable.orderId), Restaurant: $($targetAvailable.restaurantName)"

    # 8. Claiming & Concurrency Protection
    Write-Step "Driver 1 claiming order via POST /api/deliveries/{id}/claim..."
    $claimResp = Invoke-RestMethod -Uri "$baseUrl/api/deliveries/$orderId/claim" -Method Post -Headers $driver1Header
    if ($claimResp.orderId -ne $orderId) {
        Write-Fail "Claim response orderId mismatch!"
    }
    Write-Success "Driver 1 successfully claimed order."

    Write-Step "Testing Concurrency Conflict: Driver 2 attempting to claim the same order..."
    try {
        Invoke-RestMethod -Uri "$baseUrl/api/deliveries/$orderId/claim" -Method Post -Headers $driver2Header
        Write-Fail "Expected 409 Conflict for Driver 2 claim, but request succeeded!"
    } catch {
        if ($_.Exception.Response.StatusCode.value__ -eq 409) {
            Write-Success "Driver 2 claim safely rejected with 409 Conflict (Concurrency protected)."
        } else {
            Write-Fail "Expected status 409 but received $($_.Exception.Response.StatusCode.value__)"
        }
    }

    # 9. View My Deliveries
    Write-Step "Driver 1 fetching assigned deliveries via GET /api/deliveries/my..."
    $myDeliv = Invoke-RestMethod -Uri "$baseUrl/api/deliveries/my" -Method Get -Headers $driver1Header
    $myOrder = $myDeliv.content | Where-Object { $_.orderId -eq $orderId }
    if ($null -eq $myOrder) {
        Write-Fail "Claimed order not found in Driver 1's /my deliveries!"
    }
    Write-Success "Claimed order verified in Driver 1's assigned deliveries list."

    # 10. Pickup Transition (READY_FOR_PICKUP -> OUT_FOR_DELIVERY)
    Write-Step "Driver 1 marking delivery picked up via PATCH /api/deliveries/{id}/pickup..."
    $pickupResp = Invoke-RestMethod -Uri "$baseUrl/api/deliveries/$orderId/pickup" -Method Patch -Headers $driver1Header
    if ($pickupResp.status -ne "OUT_FOR_DELIVERY" -or $pickupResp.deliveryPartnerAssigned -ne $true -or $pickupResp.deliveryPartnerDisplayName -ne "Driver Dan") {
        Write-Fail "Pickup response invalid: Status=$($pickupResp.status), Driver=$($pickupResp.deliveryPartnerDisplayName)"
    }
    Write-Success "Delivery successfully transitioned to OUT_FOR_DELIVERY."

    # 11. Complete Delivery (OUT_FOR_DELIVERY -> DELIVERED)
    Write-Step "Driver 1 marking delivery complete via PATCH /api/deliveries/{id}/deliver..."
    $deliverResp = Invoke-RestMethod -Uri "$baseUrl/api/deliveries/$orderId/deliver" -Method Patch -Headers $driver1Header
    if ($deliverResp.status -ne "DELIVERED") {
        Write-Fail "Delivery completion failed! Status is $($deliverResp.status)"
    }
    Write-Success "Delivery successfully completed and transitioned to DELIVERED."

    # 12. Customer Delivery Tracking & Security Isolation
    Write-Step "Customer 1 tracking delivery via GET /api/orders/{id}/delivery..."
    $custTracking = Invoke-RestMethod -Uri "$baseUrl/api/orders/$orderId/delivery" -Method Get -Headers $cust1Header
    if ($custTracking.status -ne "DELIVERED" -or $custTracking.deliveryPartnerDisplayName -ne "Driver Dan") {
        Write-Fail "Customer delivery tracking data mismatch!"
    }
    Write-Success "Customer 1 successfully tracked delivery status ($($custTracking.status))."

    Write-Step "Verifying Customer Security Isolation: Customer 2 tracking Customer 1's order..."
    try {
        Invoke-RestMethod -Uri "$baseUrl/api/orders/$orderId/delivery" -Method Get -Headers $cust2Header
        Write-Fail "Expected 403 Forbidden for Customer 2 tracking Customer 1's order!"
    } catch {
        if ($_.Exception.Response.StatusCode.value__ -eq 403) {
            Write-Success "Customer 2 unauthorized tracking correctly rejected with 403 Forbidden."
        } else {
            Write-Fail "Expected status 403 but got $($_.Exception.Response.StatusCode.value__)"
        }
    }

    # 13. Restaurant Owner Delivery Tracking & Security Isolation
    Write-Step "Owner 1 tracking delivery via GET /api/owner/orders/{id}/delivery..."
    $ownerTracking = Invoke-RestMethod -Uri "$baseUrl/api/owner/orders/$orderId/delivery" -Method Get -Headers $owner1Header
    if ($ownerTracking.status -ne "DELIVERED" -or $ownerTracking.restaurantName -ne "Dispatch Trattoria $ts") {
        Write-Fail "Owner delivery tracking data mismatch!"
    }
    Write-Success "Owner 1 successfully tracked restaurant delivery status."

    Write-Step "Verifying Owner Security Isolation: Owner 2 tracking Owner 1's restaurant order..."
    try {
        Invoke-RestMethod -Uri "$baseUrl/api/owner/orders/$orderId/delivery" -Method Get -Headers $owner2Header
        Write-Fail "Expected 403 Forbidden for Owner 2 tracking Owner 1's restaurant order!"
    } catch {
        if ($_.Exception.Response.StatusCode.value__ -eq 403) {
            Write-Success "Owner 2 unauthorized tracking correctly rejected with 403 Forbidden."
        } else {
            Write-Fail "Expected status 403 but got $($_.Exception.Response.StatusCode.value__)"
        }
    }

    # 14. Role Boundary Security Verification
    Write-Step "Verifying Role Boundary: Customer and Owner attempting delivery partner endpoints..."
    try {
        Invoke-RestMethod -Uri "$baseUrl/api/deliveries/available" -Method Get -Headers $cust1Header
        Write-Fail "Expected 403 Forbidden when Customer accesses /api/deliveries/available!"
    } catch {
        if ($_.Exception.Response.StatusCode.value__ -eq 403) {
            Write-Success "Customer rejected from driver endpoint with 403 Forbidden."
        }
    }

    try {
        Invoke-RestMethod -Uri "$baseUrl/api/deliveries/available" -Method Get -Headers $owner1Header
        Write-Fail "Expected 403 Forbidden when Owner accesses /api/deliveries/available!"
    } catch {
        if ($_.Exception.Response.StatusCode.value__ -eq 403) {
            Write-Success "Owner rejected from driver endpoint with 403 Forbidden."
        }
    }

    # 15. Backward Compatibility (Phase 6 API /api/delivery/orders/...)
    Write-Step "Verifying Backward Compatibility of Phase 6 delivery endpoints..."
    $legacyAvail = Invoke-RestMethod -Uri "$baseUrl/api/delivery/orders/available" -Method Get -Headers $driver1Header
    if ($null -eq $legacyAvail.content) {
        Write-Fail "Legacy endpoint /api/delivery/orders/available did not return PagedResponse!"
    }
    Write-Success "Phase 6 legacy delivery endpoints verified working seamlessly."

    Write-Host "`n========================================================" -ForegroundColor Green
    Write-Host "PHASE 8 LIVE DELIVERY & DISPATCH VERIFICATION PASSED!" -ForegroundColor Green
    Write-Host "========================================================`n" -ForegroundColor Green
} finally {
    if ($serverProcess) {
        Write-Host "Stopping background Spring Boot process..."
        Stop-Process -Id $serverProcess.Id -Force -ErrorAction SilentlyContinue
    }
}
