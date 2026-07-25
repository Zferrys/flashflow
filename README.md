<p align="center">
  <h1 align="center">FlashFlow</h1>
  <p align="center">高并发智能闪购平台 · 1600+ QPS 秒杀引擎</p>
</p>

---

## 项目简介

FlashFlow 是一个基于 Spring Cloud Alibaba 微服务架构的高并发闪购平台，拆分 **7 个微服务模块**，在高并发秒杀场景下稳定支撑 **1600+ QPS**，实现**零超卖**。

平台内置 **RAG（检索增强生成）智能 AI 助手**，基于 LangChain4j 接入 DeepSeek 大模型，结合本地 BGE-small-zh 中文向量模型，为用户提供秒杀流程、优惠券规则、退款政策等自然语言智能问答能力。

## 技术栈

| 分类 | 技术 | 版本 |
|------|------|------|
| 语言 | Java | 17 |
| 框架 | Spring Boot / Spring Cloud | 3.2.12 / 2023.0.4 |
| 微服务 | Spring Cloud Alibaba（Nacos / Sentinel） | 2023.0.3.2 |
| ORM | MyBatis-Plus | 3.5.7 |
| 数据库 | MySQL | 5.7 / 8.0 |
| 缓存 | Redis + Redisson | 3.2+ / 3.40.2 |
| 消息队列 | RabbitMQ | 3.12+ |
| API 网关 | Spring Cloud Gateway | 4.x |
| 鉴权 | Spring Security + JWT（JJWT 0.12） | 6.x |
| 连接池 | Druid / Apache HttpClient 5 | 1.2.24 |
| 本地缓存 | Caffeine | — |
| AI 框架 | LangChain4j + DeepSeek + BGE-small-zh | 1.18.0 |
| 前端 | Vue 3 + Element Plus + Vite + TypeScript | 3.4 / 2.5 / 5.x |
| 部署 | Docker / Docker Compose | — |

## 项目结构

```
flashflow/
├── flashflow-common/         公共模块：统一响应、全局异常、Redisson、Jackson、AI 引擎
├── flashflow-auth/           认证授权：Spring Security + JWT、RBAC、C 端用户
├── flashflow-gateway/        API 网关：路由转发、JWT 统一鉴权、Sentinel 限流
├── flashflow-inventory/      库存服务：16 分片、Redisson 锁、Lua 原子扣减、Fallback
├── flashflow-order/          订单服务：状态机、Event Sourcing、Saga 事务补偿
├── flashflow-promotion/      营销引擎：秒杀管理、Redis 预热、限购校验、熔断降级
├── flashflow-payment/        支付服务：支付宝沙箱、异步回调、退款
├── flashflow-frontend/       Vue 3 前端管理后台
├── sql/                      数据库初始化 + 演示数据
├── deploy/                   Docker / Nginx / Prometheus 部署配置
└── docker-compose.yml        一键部署编排
```

## 核心特性

### 高性能秒杀引擎

- **Redis Lua 原子扣减**：库存扣减与限购检查合并为单次原子操作，消除并发竞态
- **16 库存分片**：按 `userId % 16` 哈希路由，降低锁竞争，提升热点库存写入性能
- **Redisson 分布式锁** + 分片 Fallback 策略：单分片库存不足时自动尝试其他分片
- **Caffeine 两级缓存**：本地缓存 + Redis 防击穿
- **自研熔断器**：30s 滑动窗口 / 50% 失败阈值，保护下游订单服务

### 分布式事务与异步补偿

- 基于 RabbitMQ 实现 **Saga 异步补偿事务**
- 订单/支付失败时自动释放库存、恢复限购、退回优惠券
- **Redis Key 幂等去重**、3 次重试 + 死信队列兜底

### 多层安全防护

- **JWT 双 Token 机制**：Access Token（30min）+ Refresh Token（7d），无感刷新
- **登出黑名单**：Redis 存储已吊销 Token，主动失效
- **IP 维度限流**：登录/注册防暴力破解
- **ThreadLocal 用户上下文** + 接口层资源归属校验，杜绝水平越权

### RAG 智能 AI 助手

- 基于 **LangChain4j** 接入 **DeepSeek** 大模型（OpenAI 兼容协议）
- 本地部署 **BGE-small-zh** 中文向量模型，离线 Embedding，零成本
- 知识库覆盖秒杀流程、优惠券规则、退款政策等平台业务文档
- 用户自然语言提问 → 语义向量检索 → 知识注入 Prompt → 生成精准回答并标注引用来源
- 支持多轮对话记忆，会话级上下文隔离

## 快速开始

### 环境要求

- JDK 17+
- MySQL 5.7 / 8.0
- Redis 3.2+
- RabbitMQ 3.12+
- Nacos 2.4+
- Node.js 18+（前端）

### 本地启动

```bash
# 1. 启动中间件（MySQL、Redis、RabbitMQ、Nacos）
# 2. 初始化数据库
mysql -u root -p < sql/00-init-databases.sql

# 3. 导入演示数据（可选）
mysql -u root -p < sql/zz-init-demo-data.sql

# 4. 设置环境变量
export MYSQL_PASSWORD=your_password
export JWT_SECRET=your_jwt_secret_at_least_32_chars
export DEEPSEEK_API_KEY=sk-your-deepseek-key    # AI 助手需要

# 5. 启动后端（按顺序）
# Gateway → Auth → Inventory → Order → Promotion → Payment

# 6. 启动前端
cd flashflow-frontend && npm install && npm run dev

# 7. 访问 http://localhost:3000
```

### Docker 部署

```bash
# 设置环境变量后一键启动
export MYSQL_ROOT_PASSWORD=your_password
export JWT_SECRET=your_jwt_secret
docker-compose up -d
```

## API 文档

启动任一服务后访问 Knife4j 文档：

- Auth 服务：http://localhost:8090/doc.html
- Promotion 服务：http://localhost:8100/doc.html

## 测试

```bash
# 运行全部 94+ 单元测试
mvn test

# 并发压测（需 Redis）
mvn test -pl flashflow-inventory -Dtest=InventoryConcurrentTest
```

## 性能基准

| 指标 | 数值 |
|------|------|
| 秒杀 QPS | 1600+ |
| 库存扣减响应 | 毫秒级 |
| RestTemplate 连接池优化 | QPS 8.7 → 1661（×191） |
| 超卖次数 | 0 |

## 演示账号

> 密码均为初始化脚本中的默认值，生产环境请务必修改。

| 角色 | 账号 |
|------|------|
| 管理员 | admin |
| C 端用户 | 13800138001 |

## 许可证

本项目仅供学习和研究使用。
