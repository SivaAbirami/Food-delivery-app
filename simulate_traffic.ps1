# =============================================================================
# Food Delivery API Traffic Simulator
# Generates realistic load so you can see graphs in Grafana:
#   - Logs    → http://localhost:3000 → Explore → Loki
#   - Metrics → http://localhost:3000 → Explore → Mimir
#   - Traces  → http://localhost:3000 → Explore → Tempo
# =============================================================================

$BASE = "http://localhost:8080/api"
$ROUND = 0
$ProgressPreference = "SilentlyContinue"

function Prop($obj, $key) {
    if ($null -ne $obj -and $null -ne $obj.$key) { return $obj.$key }
    return $null
}

function Post($path, $body, $token) {
    $h = @{ "Content-Type" = "application/json" }
    if ($token) { $h["Authorization"] = "Bearer $token" }
    try {
        $json = ($body | ConvertTo-Json -Depth 5)
        $r = Invoke-WebRequest -Uri "$BASE$path" -Method POST -Headers $h -Body $json -UseBasicParsing
        return ($r.Content | ConvertFrom-Json)
    } catch {
        $status = $_.Exception.Response.StatusCode.value__
        Write-Host "  [POST $path] -> $status" -ForegroundColor DarkGray
        return $null
    }
}

function Get-Api($path, $token) {
    $h = @{}
    if ($token) { $h["Authorization"] = "Bearer $token" }
    try {
        $r = Invoke-WebRequest -Uri "$BASE$path" -Method GET -Headers $h -UseBasicParsing
        return ($r.Content | ConvertFrom-Json)
    } catch {
        $status = $_.Exception.Response.StatusCode.value__
        Write-Host "  [GET  $path] -> $status" -ForegroundColor DarkGray
        return $null
    }
}

function Put-Api($path, $body, $token) {
    $h = @{ "Content-Type" = "application/json" }
    if ($token) { $h["Authorization"] = "Bearer $token" }
    try {
        $json = ($body | ConvertTo-Json -Depth 5)
        $r = Invoke-WebRequest -Uri "$BASE$path" -Method PUT -Headers $h -Body $json -UseBasicParsing
        return ($r.Content | ConvertFrom-Json)
    } catch {
        $status = $_.Exception.Response.StatusCode.value__
        Write-Host "  [PUT  $path] -> $status" -ForegroundColor DarkGray
        return $null
    }
}

function Patch-Api($path, $body, $token) {
    $h = @{ "Content-Type" = "application/json" }
    if ($token) { $h["Authorization"] = "Bearer $token" }
    try {
        $json = ($body | ConvertTo-Json -Depth 5)
        $r = Invoke-WebRequest -Uri "$BASE$path" -Method PATCH -Headers $h -Body $json -UseBasicParsing
        return ($r.Content | ConvertFrom-Json)
    } catch {
        $status = $_.Exception.Response.StatusCode.value__
        Write-Host "  [PATCH $path] -> $status" -ForegroundColor DarkGray
        return $null
    }
}

function Log-Msg($msg, $color = "Cyan") {
    Write-Host "[$(Get-Date -Format 'HH:mm:ss')] $msg" -ForegroundColor $color
}

function Rnd($min, $max) {
    Start-Sleep -Milliseconds (Get-Random -Minimum $min -Maximum $max)
}

function Login($email, $pass) {
    $res = Post "/auth/login" @{ email = $email; password = $pass } $null
    return Prop $res "accessToken"
}

Log-Msg "=== Setting up users and simulation environment ===" "Yellow"

# 1. Login existing seeded Restaurant Owner
$ownerEmail = "owner1@example.com"
$ownerToken = Login $ownerEmail "bobPass123"
if (-not $ownerToken) {
    Log-Msg "Warning: Could not login owner1@example.com, trying fallback password..." "Yellow"
}
Log-Msg "Owner authenticated: $ownerEmail" "Green"

# 2. Login existing seeded Delivery Partner
$delivEmail = "driver1@example.com"
$delivToken = Login $delivEmail "bobPass123"
Log-Msg "Delivery partner authenticated: $delivEmail" "Green"

# 3. Register a fresh Customer with phone number
$custNum = Get-Random -Min 1000 -Max 9999
$custEmail = "customer_${custNum}@sim.dev"
$regResult = Post "/auth/register" @{
    name = "Sim Customer $custNum"
    email = $custEmail
    password = "Password123!"
    phone = "987654$custNum"
} $null

$custToken = Login $custEmail "Password123!"
if (-not $custToken) {
    # Fallback to customer@example.com
    $custToken = Login "customer@example.com" "bobPass123"
}
Log-Msg "Customer authenticated: $custEmail" "Green"

# 4. Find or Create Restaurant
$restaurantList = Get-Api "/restaurants" $null
$restaurantId = $null
$menuIds = @()

if ($restaurantList -and $restaurantList.Count -gt 0) {
    $firstRest = $restaurantList[0]
    $restaurantId = Prop $firstRest "id"
    Log-Msg "Using existing restaurant: $(Prop $firstRest 'name') (ID: $restaurantId)" "Green"
} else {
    $restaurant = Post "/restaurants" @{
        name = "Spice Paradise $(Get-Random -Min 100 -Max 999)"
        description = "Authentic Indian cuisine"
        address = "100 MG Road, Bengaluru"
        phone = "9876543210"
        cuisineType = "INDIAN"
        openingTime = "00:00"
        closingTime = "23:59"
    } $ownerToken
    $restaurantId = Prop $restaurant "id"
    Log-Msg "Created new restaurant (ID: $restaurantId)" "Green"
}

# 5. Fetch existing menu or add items
$menuItems = Get-Api "/restaurants/$restaurantId/menu" $null
if ($menuItems -and $menuItems.Count -gt 0) {
    foreach ($m in $menuItems) {
        $mid = Prop $m "id"
        if ($mid) { $menuIds += $mid }
    }
    Log-Msg "Found $($menuIds.Count) existing menu items" "Green"
}

if ($menuIds.Count -eq 0 -and $ownerToken) {
    $items = @(
        @{ name="Butter Chicken"; description="Rich butter curry"; price=280; category="MAIN_COURSE"; preparationTimeMinutes=20 },
        @{ name="Paneer Tikka";   description="Grilled cottage cheese"; price=220; category="STARTER"; preparationTimeMinutes=15 },
        @{ name="Garlic Naan";    description="Crispy baked naan"; price=50; category="BREAD"; preparationTimeMinutes=10 },
        @{ name="Mango Lassi";    description="Chilled yogurt smoothie"; price=80; category="BEVERAGE"; preparationTimeMinutes=5 },
        @{ name="Gulab Jamun";    description="Warm syrup dumplings"; price=90; category="DESSERT"; preparationTimeMinutes=5 }
    )
    foreach ($item in $items) {
        $m = Post "/restaurants/$restaurantId/menu" $item $ownerToken
        $mid = Prop $m "id"
        if ($mid) { $menuIds += $mid }
    }
    Log-Msg "Added $($menuIds.Count) fresh menu items" "Green"
}

Write-Host ""
Log-Msg "============================================================" "Magenta"
Log-Msg " 🚀 Traffic Simulator is LIVE! Watch live graphs in Grafana:" "Magenta"
Log-Msg "   URL:     http://localhost:3000 (admin / admin)" "Magenta"
Log-Msg "   Metrics: Explore -> Mimir -> rate(http_server_requests_seconds_count[1m])" "Magenta"
Log-Msg "   Logs:    Explore -> Loki  -> {app='food-delivery-backend'}" "Magenta"
Log-Msg "   Traces:  Explore -> Tempo -> Search -> Run Query" "Magenta"
Log-Msg "   Press Ctrl+C at any time to stop the simulation." "Yellow"
Log-Msg "============================================================" "Magenta"
Write-Host ""

while ($true) {
    $ROUND++
    Log-Msg "--- Round $ROUND ---" "White"

    # 1. Browse restaurants
    Log-Msg "  [1/6] Browsing restaurants and detail..." "Cyan"
    Get-Api "/restaurants" $null | Out-Null
    if ($restaurantId) {
        Get-Api "/restaurants/$restaurantId" $null | Out-Null
        Get-Api "/restaurants/$restaurantId/menu" $null | Out-Null
    }
    Rnd 150 400

    # 2. Add items to cart
    if ($custToken -and $menuIds.Count -gt 0) {
        Log-Msg "  [2/6] Customer adding items to cart..." "Cyan"
        $pickCount = [Math]::Min(2, $menuIds.Count)
        $selectedItems = $menuIds | Get-Random -Count $pickCount
        foreach ($mid in $selectedItems) {
            Post "/cart/items" @{ menuItemId = $mid; quantity = (Get-Random -Min 1 -Max 3) } $custToken | Out-Null
            Rnd 100 250
        }

        # 3. View cart
        Log-Msg "  [3/6] Viewing cart..." "Cyan"
        Get-Api "/cart" $custToken | Out-Null
        Rnd 150 300

        # 4. Checkout order
        Log-Msg "  [4/6] Customer placing order (checkout)..." "Cyan"
        $order = Post "/orders/checkout" @{
            deliveryAddress = "Flat 402, Sunshine Towers, Bengaluru"
            specialInstructions = "Please ring the bell twice"
        } $custToken
        $orderId = Prop $order "id"
        if ($orderId) {
            Log-Msg "  -> Order #$orderId created successfully!" "Green"
            Get-Api "/orders/$orderId" $custToken | Out-Null
        }
        Rnd 200 400
    }

    # 5. Delivery partner checks available deliveries
    if ($delivToken) {
        Log-Msg "  [5/6] Delivery partner polling orders..." "Cyan"
        Get-Api "/delivery/orders/available" $delivToken | Out-Null
        Rnd 100 250
    }

    # 6. Intentional 404 / 401 calls to generate error metrics in graphs
    if ((Get-Random -Min 0 -Max 3) -eq 0) {
        Log-Msg "  [6/6] Generating random error traffic (for HTTP 4xx charts)..." "DarkYellow"
        Get-Api "/restaurants/999999" $null | Out-Null
        Post "/auth/login" @{ email="wrong@sim.dev"; password="wrongpassword" } $null | Out-Null
    }

    $wait = Get-Random -Min 1200 -Max 2500
    Log-Msg "Round $ROUND complete. Next request cycle in ${wait}ms..." "DarkGray"
    Write-Host ""
    Start-Sleep -Milliseconds $wait
}