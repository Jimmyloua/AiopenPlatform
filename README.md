# AI开放平台 (AI Open Platform)

一个支持多租户的AI助手平台，提供助手管理、对话管理、技能配置等功能。

## 技术栈

### 前端
- React 18 + TypeScript
- Vite (构建工具)
- Zustand (状态管理)
- Ant Design (UI组件库)
- React Router (路由)
- Axios (HTTP客户端)
- React Markdown (Markdown渲染)

### 后端
- Spring Boot 3.2
- Java 17
- MyBatis-Plus (ORM)
- Spring Security + JWT (认证授权)
- Spring Kafka (消息队列)
- Redis (缓存)
- MySQL 8.0 (数据库)

### 基础设施
- Docker & Docker Compose
- Kafka (消息队列)
- Redis (缓存/会话)
- MySQL (持久化存储)

## 项目结构

```
ai-open-platform/
├── frontend/                    # React前端
│   ├── src/
│   │   ├── components/         # 组件
│   │   │   ├── chat/           # 对话组件
│   │   │   ├── assistant/      # 助手组件
│   │   │   └── common/         # 通用组件
│   │   ├── pages/              # 页面
│   │   ├── services/           # API服务
│   │   ├── stores/             # Zustand状态管理
│   │   ├── hooks/              # 自定义Hooks
│   │   ├── types/              # TypeScript类型
│   │   └── utils/              # 工具函数
│   ├── package.json
│   └── vite.config.ts
├── backend/                     # Spring Boot后端
│   ├── platform-service/       # 平台核心服务
│   ├── gateway-service/        # 网关服务
│   └── connection-service/     # 连接服务
├── database/                    # 数据库脚本
│   └── schema.sql
├── docker/                      # Docker配置
│   └── docker-compose.yml
└── docs/                        # 文档
```

## 功能特性

### 用户侧
- **AI CUI对话界面** - 支持Markdown渲染、流式响应、思考过程展示
- **助手广场** - 浏览和使用公开的助手
- **多设备同步** - 会话数据实时同步

### 平台侧
- **用户管理** - 注册、登录、权限控制(RBAC)
- **助手管理** - 创建、编辑、发布助手到广场
- **技能管理** - 定义和配置AI技能
- **会话管理** - 会话创建、历史记录、上下文维护
- **审核系统** - 助手发布审核流程

### 连接侧
- **统一网关** - 路由、协议转换、限流熔断
- **本地Agent连接器** - OpenCode兼容的本地沙箱
- **远端模型连接器** - OpenAI、Claude等API适配

## 快速开始

### 环境要求
- JDK 17+
- Node.js 18+
- Docker & Docker Compose
- Maven 3.8+

### 1. 启动基础设施

```bash
cd docker
docker-compose up -d
```

等待MySQL、Redis、Kafka启动完成。

### 2. 初始化数据库

```bash
mysql -h localhost -u root -proot123 < database/schema.sql
```

### 3. 启动后端服务

```bash
cd backend

# 编译所有模块
mvn clean install

# 启动平台服务
cd platform-service
mvn spring-boot:run

# 在新终端启动网关服务
cd gateway-service
mvn spring-boot:run

# 在新终端启动连接服务
cd connection-service
mvn spring-boot:run
```

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```

访问 http://localhost:3000

### 默认账号

- 用户名: `admin`
- 密码: `admin123`

## API文档

启动服务后访问:
- Platform Service Swagger: http://localhost:8080/swagger-ui.html
- Gateway Service Swagger: http://localhost:8000/swagger-ui.html
- Connection Service Swagger: http://localhost:8081/swagger-ui.html

### OpenAI兼容API

```bash
# 聊天补全
curl -X POST http://localhost:8000/v1/chat/completions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_API_KEY" \
  -d '{
    "model": "gpt-4",
    "messages": [{"role": "user", "content": "Hello!"}],
    "stream": true
  }'

# 列出模型
curl http://localhost:8000/v1/models \
  -H "Authorization: Bearer YOUR_API_KEY"
```

## 配置说明

### 后端配置

每个服务的配置文件位于 `src/main/resources/application.yml`:

- **platform-service**: 端口8080
- **gateway-service**: 端口8000
- **connection-service**: 端口8081

### 前端配置

编辑 `frontend/.env`:

```env
VITE_API_BASE_URL=http://localhost:8000
```

## 架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                        用户侧 (User Layer)                       │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────────────┐ │
│  │  AI CUI  │  │ IM集成   │  │ 会议集成  │  │    助手广场      │ │
│  └──────────┘  └──────────┘  └──────────┘  └──────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      平台侧 (Platform Layer)                     │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────────────┐ │
│  │ 场景适配 │  │ 会话管理 │  │Session管理│  │  凭证与连接控制  │ │
│  └──────────┘  └──────────┘  └──────────┘  └──────────────────┘ │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────────────┐ │
│  │ 助手管理 │  │ 技能管理 │  │ 用户管理  │  │   权限控制       │ │
│  └──────────┘  └──────────┘  └──────────┘  └──────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    消息总线层 (Kafka)                            │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────────────┐ │
│  │ 生产者   │  │ 消费者   │  │ 事件流   │  │   异步任务队列   │ │
│  └──────────┘  └──────────┘  └──────────┘  └──────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    连接侧 (Connection Layer)                     │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │                    统一网关 (Gateway)                        ││
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐                  ││
│  │  │协议转换  │  │ 负载均衡 │  │ 限流熔断 │                  ││
│  │  └──────────┘  └──────────┘  └──────────┘                  ││
│  └─────────────────────────────────────────────────────────────┘│
│  ┌──────────────────┐  ┌────────────────────────────────────────┐│
│  │ 本地沙箱Agent   │  │         远端模型API连接器               ││
│  │ (OpenCode兼容)  │  │  (OpenAI, Claude, Gemini, etc.)        ││
│  └──────────────────┘  └────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────┘
```

## 开发指南

### 添加新的API端点

1. 在对应的Controller中添加新方法
2. 在Service层实现业务逻辑
3. 编写单元测试

### 添加新的前端页面

1. 在 `frontend/src/pages/` 创建页面组件
2. 在 `frontend/src/App.tsx` 添加路由
3. 在侧边栏菜单中添加入口

### 数据库迁移

1. 修改 `database/schema.sql`
2. 手动执行SQL或重启Docker容器

## 测试

### 后端测试

```bash
cd backend
mvn test
```

### 前端测试

```bash
cd frontend
npm run test
```

## 部署

### Docker部署

```bash
# 构建镜像
docker build -t ai-platform-frontend ./frontend
docker build -t ai-platform-backend ./backend

# 运行容器
docker-compose -f docker-compose.prod.yml up -d
```

## 许可证

MIT License