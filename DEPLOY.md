# FlashFlow 部署文档

> 高并发闪购订单平台 - 部署与演示指南

---

## 系统架构

```
┌─────────────────────────────────────────────────────────────┐
│                        Frontend (Vue 3)                      │
│                      http://localhost:3000                    │
└──────────────────────────┬──────────────────────────────────┘
                           │ HTTP
┌──────────────────────────▼──────────────────────────────────┐
│              Gateway (Spring Cloud Gateway)                   │
│                     http://localhost:5050                      │
│          JWT鉴权 / 路由转发 / Sentinel 限流熔断                │
└────┬──────┬──────┬──────┬──────┬──────┬──────────────────────┘
     │      │      │      │      │      │
┌────▼──┐┌─▼────┐┌─▼────┐┌─▼────┐┌─▼────┐┌─▼──────────────┐
│ Auth  ││Pro-  ││Order ││Inven-││Pay-  ││  Nacos 注册中心 │
│ :8080 ││motion││:8100 ││tory  ││ment  ││  :8848          │
│       ││:8090 ││      ││:8110 ││:8120 │└─────────────────┘
└──┬────┘└──┬───┘└──┬───┘└──┬───┘└──┬───┘
   │        │       │       │       │
┌──▼────────▼───────▼───────▼───────▼───────────────────────┐
│                   MySQL 5.7 / Redis 3.2                    │
│             RabbitMQ 3.12 (Saga 分布式事务)                 │
└───────────────────────────────────────────────────────────┘
```

---

## 环境要求

| 组件 | 版本 | 用途 |
|------|------|------|
| JDK | 17+ | 编译运行 Java 服务 |
| Maven | 3.6.3+ | 构建项目（3.6.1 不兼容） |
| MySQL | 5.7+ | 数据持久化 |
| Redis | 3.2+ | 缓存 + 分布式锁 + 库存扣减 |
| RabbitMQ | 3.12+ | Saga 分布式事务消息 |
| Nacos | 2.4.3 | 服务注册与发现 |
| Node.js | 18+ | 前端构建 |
| npm | 9+ | 前端依赖管理 |

---

## 快速启动（本地开发）

### 1. 启动基础设施

#### 1.1 MySQL
```bash
# Windows 启动
net start mysql
# 或
"D:/MySQL/mysql-5.7.19-winx64/bin/mysqld" --console

# 确认连接
mysql -uroot -p -h127.0.0.1
```

#### 1.2 Redis
```bash
# Windows 启动
redis-server
# 默认端口 6379，无密码
```

#### 1.3 RabbitMQ
```bash
# 启动服务
rabbitmq-server start
# 管理端: http://localhost:15672  (guest/guest)
```

#### 1.4 Nacos
```bash
cd D:\soft\nacos-server-2.4.3\bin

# Windows 启动（standalone + embedded storage）
startup.cmd -m standalone -DembeddedStorage=true

# 管理端: http://localhost:8848/nacos  (nacos/nacos)
# 创建 namespace: dev (ID=dev)
```

### 2. 初始化数据库

```bash
# 执行建库脚本
mysql -uroot -p < sql/init-databases.sql

# 执行各模块表结构
mysql -uroot -p flashflow_auth < sql/init-auth.sql
mysql -uroot -p flashflow_inventory < sql/init-inventory.sql
mysql -uroot -p flashflow_order < sql/init-order.sql
mysql -uroot -p flashflow_payment < sql/init-payment.sql
mysql -uroot -p flashflow_promotion < sql/init-promotion.sql

# 导入演示数据（可选）
mysql -uroot -p < sql/init-demo-data.sql
```

### 3. 启动后端服务

按以下顺序启动（IDEA 中运行 main 方法）：

| 顺序 | 服务 | 模块 | 端口 |
|------|------|------|------|
| 1 | Nacos | - | 8848 |
| 2 | Gateway | flashflow-gateway | 5050 |
| 3 | Auth | flashflow-auth | 8080 |
| 4 | Promotion | flashflow-promotion | 8090 |
| 5 | Inventory | flashflow-inventory | 8110 |
| 6 | Order | flashflow-order | 8100 |
| 7 | Payment | flashflow-payment | 8120 |

**IDEA 配置：**
- JDK: `D:\soft\jdk\jdk-17.0.19+10`
- 每个服务 main class：`com.flashflow.{module}.Flashflow{Module}Application`
- VM options：`-Dspring.profiles.active=dev`

### 4. 启动前端

```bash
cd flashflow-frontend
npm install
npm run dev
# 访问 http://localhost:3000
```

### 5. Redis 预热（重要）

导入演示数据后，需执行 Redis 预热：
- **方法1（前端）**：登录 → 活动管理 → 点击活动1 "发布" 按钮
- **方法2（API）**：
  ```bash
  # 管理员登录获取 token
  curl -X POST http://localhost:5050/api/flashflow/auth/login \
    -H "Content-Type: application/json" \
    -d '{"username":"admin","password":"<your-admin-password>"}'

  # 用返回的 token 发布活动
  curl -X POST http://localhost:5050/api/flashflow/promotion/activity/1/publish \
    -H "Authorization: Bearer {token}"
  ```

---

## 演示账号

| 角色 | 账号 | 密码 |
|------|------|------|
| 管理员 | admin | <your-admin-password> |
| C 端用户 | 13800138001 | <your-user-password> |
| C 端用户 | 13800138002 | <your-user-password> |
| C 端用户 | 13800138003 | <your-user-password> |

---

## 端到端演示流程

```
                   管理员                          C 端用户
                   ──────                        ────────
  1. 登录 admin/<your-admin-password>
  2. 进入活动管理 → 发布活动（预热 Redis）
  3. 进入活动 → 查看商品列表
                                      4. 注册账号 13800138001 / <your-user-password>
                                         （或直接使用演示账号）
                                      5. 进入活动管理 → 点击 "去秒杀"
                                      6. 查看秒杀详情 → 倒计时 → 抢购
                                      7. 支付页面 → 确认支付
                                      8. 订单管理 → 查看订单状态
```

---

## 关键 API 接口

### 认证
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/flashflow/auth/login` | 管理员登录 |
| POST | `/api/flashflow/auth/user/register` | C 端注册 |
| POST | `/api/flashflow/auth/user/login` | C 端登录 |

### 活动管理
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/flashflow/promotion/activity/page` | 活动分页 |
| POST | `/api/flashflow/promotion/activity` | 创建活动 |
| POST | `/api/flashflow/promotion/activity/{id}/publish` | 发布（预热 Redis） |
| GET | `/api/flashflow/promotion/activity/{id}/sku` | 活动商品列表 |

### 秒杀核心
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/flashflow/promotion/flash/sale` | 秒杀抢购 |

### 订单
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/flashflow/order` | 创建订单 |
| GET | `/api/flashflow/order/{id}` | 订单详情 |
| POST | `/api/flashflow/order/{orderSn}/pay-success` | 支付回调 |

### 支付
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/flashflow/payment/mock-pay` | 模拟支付 |
| GET | `/api/flashflow/payment/{orderSn}/status` | 支付状态 |

---

## Docker 部署

> ⚠️ Docker 部署需要至少 8GB 内存

```bash
# 构建所有服务镜像
mvn clean package -DskipTests

# 启动所有容器
docker-compose up -d

# 查看日志
docker-compose logs -f

# 确认所有服务健康
docker-compose ps
```

访问：
- 前端：http://localhost:3000
- Nacos：http://localhost:8848/nacos (nacos/nacos)
- RabbitMQ：http://localhost:15672 (guest/guest)
- Prometheus：http://localhost:9090
- Grafana：http://localhost:3001 (admin/admin)

---

## 常见问题

### 1. Nacos 启动失败
```bash
# 确保使用 standalone 模式
startup.cmd -m standalone -DembeddedStorage=true
```

### 2. 服务注册不上 Nacos
检查 `application-dev.yml` 中的 Nacos 配置：
```yaml
spring.cloud.nacos.discovery:
  server-addr: 127.0.0.1:8848
  namespace: dev
  ip: 127.0.0.1
```
确认 Nacos 中已创建 `dev` namespace（ID 也是 `dev`）。

### 3. Gateway 报 503
```bash
# Gateway 需要 loadbalancer 依赖
# 确认 pom.xml 包含 spring-cloud-starter-loadbalancer
```

### 4. API 返回 401
```bash
# 确认 JWT 没有过期（默认 7 天）
# 重新登录获取新 token
```

### 5. 秒杀库存不足
```bash
# 确认 Redis 已执行预热
# 检查 Redis 中是否有 stock:{skuId}:{shard} 的 key
```

---

## 项目结构

```
flashflow/
├── flashflow-gateway/        # API 网关（5050）
├── flashflow-auth/           # 认证服务（8080）
├── flashflow-promotion/      # 营销服务（8090）
├── flashflow-inventory/      # 库存服务（8110）
├── flashflow-order/          # 订单服务（8100）
├── flashflow-payment/        # 支付服务（8120）
├── flashflow-common/         # 公共模块
├── flashflow-frontend/       # Vue 3 前端（3000）
├── sql/                      # 数据库初始化脚本
├── deploy/docker/            # Docker 配置
└── docker-compose.yml        # Docker Compose
```
