@echo off
echo ===================================================
echo   Starting ZENO MART Microservices and Frontend
echo ===================================================

cd /d "%~dp0"
if not exist "logs" mkdir logs

set JVM_OPTS=-XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Xss256k -Xms32m -Xmx128m

echo Starting Auth Service (8081)...
start "auth-service" /b java %JVM_OPTS% -jar services\auth-service\target\auth-service.jar --spring.profiles.active=local > logs\auth-service.log 2> logs\auth-service-err.log
timeout /t 1 /nobreak >nul

echo Starting Product Service (8082)...
start "product-service" /b java %JVM_OPTS% -jar services\product-service\target\product-service.jar --spring.profiles.active=local > logs\product-service.log 2> logs\product-service-err.log
timeout /t 1 /nobreak >nul

echo Starting Inventory Service (8086)...
start "inventory-service" /b java %JVM_OPTS% -jar services\inventory-service\target\inventory-service.jar --spring.profiles.active=local > logs\inventory-service.log 2> logs\inventory-service-err.log
timeout /t 1 /nobreak >nul

echo Starting Payment Service (8085)...
start "payment-service" /b java %JVM_OPTS% -jar services\payment-service\target\payment-service.jar --spring.profiles.active=local > logs\payment-service.log 2> logs\payment-service-err.log
timeout /t 1 /nobreak >nul

echo Starting Notification Service (8087)...
start "notification-service" /b java %JVM_OPTS% -jar services\notification-service\target\notification-service.jar --spring.profiles.active=local > logs\notification-service.log 2> logs\notification-service-err.log
timeout /t 1 /nobreak >nul

echo Starting Cart Service (8083)...
start "cart-service" /b java %JVM_OPTS% -jar services\cart-service\target\cart-service.jar --spring.profiles.active=local > logs\cart-service.log 2> logs\cart-service-err.log
timeout /t 1 /nobreak >nul

echo Starting Order Service (8084)...
start "order-service" /b java %JVM_OPTS% -jar services\order-service\target\order-service.jar --spring.profiles.active=local > logs\order-service.log 2> logs\order-service-err.log
timeout /t 1 /nobreak >nul

echo Starting API Gateway (8080)...
start "api-gateway" /b java %JVM_OPTS% -jar api-gateway\target\api-gateway.jar --spring.profiles.active=local > logs\api-gateway.log 2> logs\api-gateway-err.log
timeout /t 1 /nobreak >nul

echo Starting Next.js Frontend (3000)...
cd frontend
start "frontend" /b cmd /c "npm run dev" > ..\logs\frontend.log 2> ..\logs\frontend-err.log
cd ..

echo.
echo All services launched!
echo - Storefront:   http://localhost:3000
echo - Admin Portal: http://localhost:3000/admin
echo - API Gateway:  http://localhost:8080/api/v1
echo.
