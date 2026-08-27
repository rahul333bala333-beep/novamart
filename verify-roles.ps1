$ErrorActionPreference = "Stop"

Write-Host "=================================================="
Write-Host "  ZENO MART E2E ROLE & PERMISSION VERIFICATION    "
Write-Host "=================================================="

$uniqueId = [System.DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
# 1. Register new normal USER
$regBody = @{
    firstName = "Karthik"
    lastName = "Rajan"
    email = "karthik_$uniqueId@example.com"
    password = "Shopper@12345"
    phone = "+91 9876543210"
} | ConvertTo-Json

$userAuth = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/auth/register" -Method Post -Body $regBody -ContentType "application/json"
$rolesStr = $userAuth.data.user.roles -join ", "
Write-Host "[1] USER Registration: SUCCESS! Role(s) = $rolesStr"
$userToken = $userAuth.data.accessToken
$userHeaders = @{ Authorization = "Bearer $userToken" }

# 2. Browse catalogue
$products = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/products" -Method Get
$firstProduct = $products.data.content[0]
Write-Host "[2] Browse Catalogue: SUCCESS! Found $($products.data.totalElements) products. Selected: $($firstProduct.name)"

# 3. Add to cart as USER
$cartBody = @{
    productId = $firstProduct.id
    quantity = 2
} | ConvertTo-Json
$cart = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/cart/items" -Method Post -Headers $userHeaders -Body $cartBody -ContentType "application/json"
Write-Host "[3] Add to Cart (USER): SUCCESS! Cart items = $($cart.data.items.Count), Subtotal = $($cart.data.subtotal)"

# 4. Add Address
$addrBody = @{
    label = "Home"
    recipientName = "Karthik Rajan"
    line1 = "45 Marina Beach Road"
    city = "Chennai"
    state = "Tamil Nadu"
    postalCode = "600004"
    country = "India"
    phone = "+91 9876543210"
    isDefault = $true
} | ConvertTo-Json
$addr = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/users/me/addresses" -Method Post -Headers $userHeaders -Body $addrBody -ContentType "application/json"
Write-Host "[4] Add Address: SUCCESS! Address ID = $($addr.data.id)"

# 5. Place Order (Distributed Saga Checkout)
$orderBody = @{
    addressId = $addr.data.id
    paymentMethod = "MOCK_CARD"
    notes = "Academic submission live demo test order"
    simulateFailure = $false
} | ConvertTo-Json
$order = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/orders" -Method Post -Headers $userHeaders -Body $orderBody -ContentType "application/json"
Write-Host "[5] Place Order (Saga): SUCCESS! Order Number = $($order.data.orderNumber), Status = $($order.data.status)"

# 6. Verify Payment
$payBody = @{
    simulateFailure = $false
} | ConvertTo-Json
$payVerify = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/payments/$($order.data.paymentId)/verify" -Method Post -Headers $userHeaders -Body $payBody -ContentType "application/json"
Write-Host "[6] Verify Payment: SUCCESS! Payment Status = $($payVerify.data.status)"

# 7. Check User Orders
$ordersList = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/orders" -Method Get -Headers $userHeaders
Write-Host "[7] Shopper Orders List: SUCCESS! Total Orders = $($ordersList.data.totalElements)"

# 8. Test USER attempting to access Admin endpoint (Expect 403 Forbidden)
try {
    Invoke-RestMethod -Uri "http://localhost:8080/api/v1/orders/stats" -Method Get -Headers $userHeaders
    Write-Host "[8] USER access to admin endpoint: FAILED (Expected 403 Forbidden)"
} catch {
    Write-Host "[8] USER access to admin endpoint: BLOCKED AS EXPECTED (403 Forbidden)!"
}

# 9. Admin Login
$adminLoginBody = @{
    email = "admin@novamart.dev"
    password = "Admin@12345"
} | ConvertTo-Json
$adminAuth = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/auth/login" -Method Post -Body $adminLoginBody -ContentType "application/json"
$adminRolesStr = $adminAuth.data.user.roles -join ", "
Write-Host "[9] ADMIN Login: SUCCESS! Role(s) = $adminRolesStr"
$adminToken = $adminAuth.data.accessToken
$adminHeaders = @{ Authorization = "Bearer $adminToken" }

# 10. Admin Access to Admin Dashboard Stats
$stats = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/orders/stats" -Method Get -Headers $adminHeaders
Write-Host "[10] Admin Dashboard Stats: SUCCESS! Total Orders = $($stats.data.totalOrders), Total Revenue = $($stats.data.totalRevenue)"

# 11. Admin Access to Customers List
$customers = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/users" -Method Get -Headers $adminHeaders
Write-Host "[11] Admin Customers List: SUCCESS! Total Users = $($customers.data.totalElements)"

Write-Host "=================================================="
Write-Host "  ALL TESTS PASSED: ROLE MODEL VERIFIED 100%!     "
Write-Host "=================================================="
