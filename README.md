# FlashFlow 高并发闪购订单平台

> 基于 Spring Cloud Alibaba 的企业级高并发闪购订单平台，支撑 **5000+ QPS** 秒杀场景。

## 技术栈

| 层 | 技术 | 版本 |
|----|------|------|
| 语言 | Java | 17 (Temurin-17.0.19) |
| 框架 | Spring Boot / Spring Cloud | 3.2.12 / 2023.0.4 |
| 微服务 | Spring Cloud Alibaba | 2023.0.3.2 |
| ORM | MyBatis-Plus | 3.5.7 |
| 数据库 | MySQL | 5.7 / 8.0 兼容 |
| 缓存 | Redis + Redisson | 3.2+ / 3.40.2 |
| 消息 | RabbitMQ | 3.12+ |
| 注册中心 | Nacos | 2.4.3 |
| 网关 | Spring Cloud Gateway | 4.x |
| 限流熔断 | Sentinel | 1.8.8 |
| 鉴权 | Spring Security + JWT | 6.x / JJWT 0.12 |
| 前端 | Vue 3 + Element Plus + Vite + TypeScript | 3.4 / 2.5 / 5 / 5.x |

## 项目结构

```
flashflow/
├── flashflow-common/        # 公共模块：R<T>, ErrorCode, 全局异常, Redisson, MyBatis-Plus
├── flashflow-auth/          # 认证授权：Spring Security + JWT, RBAC, C端用户
├── flashflow-inventory/     # ⭐ 库存服务：分片策略, Redisson锁, Lua原子扣减, Fallback
├── flashflow-order/         # 订单服务：状态机, Event Sourcing, Saga事务表
├── flashflow-promotion/     # 营销引擎：秒杀/预售/拼团, Redis预热, 限购校验
├── flashflow-payment/       # 支付服务：支付宝沙箱对接, 模拟支付, 退款
├── flashflow-gateway/       # API网关：路由转发, JWT统一鉴权, Sentinel限流
├── flashflow-frontend/      # Vue 3 前端管理后台（含秒杀页面）
├── sql/                     # 数据库初始化 + 演示数据
├── deploy/docker/           # Docker 容器化配置
└── DEPLOY.md                # 部署文档
```

## 核心架构亮点

### 1. 库存分片扣减（防超卖）
```
用户请求 → userId % 16 → 选择分片 → Redisson RLock →
  Lua 脚本原子扣减 → 成功/失败 → Fallback 其他分片
```
- 16 片 Redis 分片减少锁竞争
- Lua 脚本保证原子性
- 分片 Fallback 提高利用率

### 2. 订单状态机
```
PENDING → PAID → SHIPPED → DELIVERED → COMPLETED
    ↘        ↘
    CANCELLED  REFUNDING → REFUNDED
```
- Event Sourcing 记录每次状态变更
- 非法转换被状态机拦截

### 3. 秒杀全链路
```
创建活动 → 添加商品 → 发布活动 → Redis预热 →
    用户抢购 → Lua扣库存 → 创建订单 → 支付回调
```

## 快速启动

详见 [DEPLOY.md](DEPLOY.md)

```bash
# 1. 启动基础设施（MySQL, Redis, RabbitMQ, Nacos）
# 2. 初始化数据库
mysql -uroot -p < sql/init-databases.sql
# 3. 在 IDEA 中依次启动各服务（Gateway → Auth → 其他）
# 4. 启动前端
cd flashflow-frontend && npm install && npm run dev
# 5. 访问 http://localhost:3000
```

## 演示账号

| 角色 | 账号 | 密码 |
|------|------|------|
| 管理员 | admin | 见 SQL 初始化脚本 |
| C端用户 | 13800138001 | 见 SQL 初始化脚本 |

## 测试

```bash
# 运行全部 94+ 测试
mvn test
# 运行并发压测（需 Redis）
mvn test -pl flashflow-inventory -Dtest=InventoryConcurrentTest
```

## 面试要点

- **库存扣减**：为什么用分片？为什么用 Lua？Fallback 策略？
- **高并发**：Sentinel 限流、分片锁、Redis 预热
- **一致性**：Saga 事务表、状态机、Event Sourcing
- **安全**：JWT 网关统一鉴权、BCrypt 密码、参数化查询
