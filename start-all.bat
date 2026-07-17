@echo off
set JAVA_HOME=D:\soft\jdk\jdk-17.0.19+10
set BASE=D:\javacode\Java\Pro\resume_pro\flashflow
set LOG=%BASE%\logs
if not exist %LOG% mkdir %LOG%

echo ============================================
echo   FlashFlow 一键启动
echo ============================================
echo.

rem 按依赖顺序启动：Auth → Promotion → Inventory → Order → Payment → Gateway
rem 每个服务等待 8 秒确保 Nacos 注册完成

echo [1/6] Auth (8090)
start "Auth" "%JAVA_HOME%\bin\java" -jar %BASE%\flashflow-auth\target\flashflow-auth-1.0.0-SNAPSHOT.jar --spring.profiles.active=dev > %LOG%\auth.log 2>&1
timeout /t 8 /nobreak >nul

echo [2/6] Promotion (8100)
start "Promotion" "%JAVA_HOME%\bin\java" -jar %BASE%\flashflow-promotion\target\flashflow-promotion-1.0.0-SNAPSHOT.jar --spring.profiles.active=dev > %LOG%\promotion.log 2>&1
timeout /t 8 /nobreak >nul

echo [3/6] Inventory (8110)
start "Inventory" "%JAVA_HOME%\bin\java" -jar %BASE%\flashflow-inventory\target\flashflow-inventory-1.0.0-SNAPSHOT.jar --spring.profiles.active=dev > %LOG%\inventory.log 2>&1
timeout /t 8 /nobreak >nul

echo [4/6] Order (5050)
start "Order" "%JAVA_HOME%\bin\java" -jar %BASE%\flashflow-order\target\flashflow-order-1.0.0-SNAPSHOT.jar --spring.profiles.active=dev > %LOG%\order.log 2>&1
timeout /t 8 /nobreak >nul

echo [5/6] Payment (8120)
start "Payment" "%JAVA_HOME%\bin\java" -jar %BASE%\flashflow-payment\target\flashflow-payment-1.0.0-SNAPSHOT.jar --spring.profiles.active=dev > %LOG%\payment.log 2>&1
timeout /t 8 /nobreak >nul

echo [6/6] Gateway (8080)
start "Gateway" "%JAVA_HOME%\bin\java" -jar %BASE%\flashflow-gateway\target\flashflow-gateway-1.0.0-SNAPSHOT.jar --spring.profiles.active=dev > %LOG%\gateway.log 2>&1

echo.
echo 等待服务全部就绪...
timeout /t 20 /nobreak >nul

echo ============================================
for %%p in (8090 8100 8110 5050 8120 8080) do (
    netstat -ano | findstr ":%%p " >nul && echo   :%%p OK || echo   :%%p FAIL
)
echo ============================================
echo 管理后台: http://localhost:3000
echo Knife4j:  http://localhost:8080/doc.html
pause
