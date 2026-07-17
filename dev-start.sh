#!/bin/bash
# FlashFlow 一键启动脚本（开发环境）
# 启动顺序：Nacos → Redis → MySQL → 各微服务

export JAVA_HOME=/d/soft/jdk/jdk-17.0.19+10
export PATH=$JAVA_HOME/bin:$PATH
BASE=/d/javacode/Java/Pro/resume_pro/flashflow

echo "========================================"
echo "  FlashFlow 开发环境启动"
echo "========================================"

# 1. 检查 Nacos
if curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1:8848/nacos/ 2>&1 | grep -q 200; then
    echo "[OK] Nacos 已运行"
else
    echo "[START] 启动 Nacos..."
    bash /d/soft/nacos/start-nacos.sh
    sleep 10
fi

# 2. 检查 Redis
if netstat -ano 2>/dev/null | grep ":6379 " | grep -q LISTEN; then
    echo "[OK] Redis 已运行"
else
    echo "[START] 启动 Redis..."
    /d/soft/redis/redis-server.exe /d/soft/redis/redis.windows.conf --daemonize yes 2>/dev/null
    sleep 2
fi

# 3. 检查 MySQL
if netstat -ano 2>/dev/null | grep ":3306 " | grep -q LISTEN; then
    echo "[OK] MySQL 已运行"
else
    echo "[ERROR] MySQL 未运行，请手动启动"
    exit 1
fi

# 4. 编译项目
echo "[BUILD] 编译项目..."
mvn compile -q -f "$BASE/pom.xml"
echo "[OK] 编译完成"

# 5. 启动微服务（后台运行）
echo "[START] 启动微服务..."

# Auth (:8080)
java -jar $BASE/flashflow-auth/target/flashflow-auth-1.0.0-SNAPSHOT.jar \
  --spring.profiles.active=dev > $BASE/logs/auth.log 2>&1 &
echo "  Auth :8080 → PID $!"

# Inventory (:8110)
java -jar $BASE/flashflow-inventory/target/flashflow-inventory-1.0.0-SNAPSHOT.jar \
  --spring.profiles.active=dev > $BASE/logs/inventory.log 2>&1 &
echo "  Inventory :8110 → PID $!"

# Order (:8100)
java -jar $BASE/flashflow-order/target/flashflow-order-1.0.0-SNAPSHOT.jar \
  --spring.profiles.active=dev > $BASE/logs/order.log 2>&1 &
echo "  Order :8100 → PID $!"

# Promotion (:8090)
java -jar $BASE/flashflow-promotion/target/flashflow-promotion-1.0.0-SNAPSHOT.jar \
  --spring.profiles.active=dev > $BASE/logs/promotion.log 2>&1 &
echo "  Promotion :8090 → PID $!"

# Payment (:8120)
java -jar $BASE/flashflow-payment/target/flashflow-payment-1.0.0-SNAPSHOT.jar \
  --spring.profiles.active=dev > $BASE/logs/payment.log 2>&1 &
echo "  Payment :8120 → PID $!"

# Gateway (:5050)
java -jar $BASE/flashflow-gateway/target/flashflow-gateway-1.0.0-SNAPSHOT.jar \
  --spring.profiles.active=dev > $BASE/logs/gateway.log 2>&1 &
echo "  Gateway :5050 → PID $!"

echo ""
echo "========================================"
echo "  启动完成！查看状态："
echo "    Gateway: http://127.0.0.1:5050"
echo "    Nacos:   http://127.0.0.1:8848/nacos"
echo "    日志:    $BASE/logs/"
echo "========================================"
