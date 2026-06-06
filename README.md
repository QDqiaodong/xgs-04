# 个人书单互换借阅平台

聚焦同城书友纸质图书线下流转的借阅平台。用户可登记自有藏书并开放出借权限，线上发起借阅申请、登记借还记录，实现图书资源共享与知识传递。

## 目录

- [项目简介](#项目简介)
- [功能特性](#功能特性)
- [技术栈](#技术栈)
- [项目结构](#项目结构)
- [快速开始](#快速开始)
- [端口配置](#端口配置)
- [环境变量](#环境变量)
- [功能模块](#功能模块)
- [性能优化](#性能优化)
- [部署说明](#部署说明)
- [常见问题](#常见问题)

## 功能特性

- **自有图书管理**：录入书名、作者、ISBN、分类、成色、简介，设置是否对外开放出借
- **借阅申请流程**：选中可借图书填写借用周期，所有者审核申请（同意/拒绝）
- **借还状态流转**：双方确认借出/归还后变更单据状态，全流程可追溯
- **多维度筛选**：按所在城市、图书分类、可出借状态、关键词组合筛选
- **筛选条件缓存**：用户筛选条件本地持久化，下次访问自动恢复
- **Redis 缓存加速**：在借图书信息、用户筛选条件缓存，提升访问性能

## 技术栈

### 前端

- **框架**：Vue 3.4 + Vue Router 4.2
- **构建工具**：Vite 5.0
- **UI 组件库**：Element Plus 2.4
- **HTTP 客户端**：Axios 1.6
- **部署**：Nginx + Docker

### 后端

- **框架**：Spring Boot 3.3
- **JDK**：Java 17
- **ORM**：Spring Data JPA
- **缓存**：Spring Data Redis
- **数据库**：MySQL 8.0
- **构建工具**：Maven 3.9

### 基础设施

- **容器化**：Docker + Docker Compose
- **数据库连接池**：HikariCP
- **镜像源加速**：DaoCloud / 阿里云 / npmmirror

## 项目结构

```
.
├── backend/                     # 后端 Spring Boot 项目
│   ├── src/main/
│   │   ├── java/com/bookexchange/
│   │   │   ├── config/          # 配置类（数据初始化、Redis、Web）
│   │   │   ├── controller/      # 控制层（图书、借阅记录、分类、城市、用户）
│   │   │   ├── dto/             # 数据传输对象
│   │   │   ├── entity/          # 实体类
│   │   │   ├── repository/      # 数据访问层
│   │   │   └── service/         # 业务逻辑层
│   │   └── resources/
│   │       └── application.yml  # 应用配置
│   ├── Dockerfile               # 后端 Docker 构建文件
│   ├── pom.xml                  # Maven 依赖配置
│   └── settings.xml             # Maven 镜像源配置
├── frontend/                    # 前端 Vue 项目
│   ├── src/
│   │   ├── api/                 # API 接口封装
│   │   ├── components/          # 公共组件
│   │   ├── router/              # 路由配置
│   │   ├── views/               # 页面视图
│   │   ├── App.vue              # 根组件
│   │   ├── main.js              # 入口文件
│   │   └── style.css            # 全局样式
│   ├── Dockerfile               # 前端 Docker 构建文件
│   ├── nginx.conf.template      # Nginx 配置模板
│   ├── vite.config.js           # Vite 构建配置
│   └── package.json             # npm 依赖配置
├── database/
│   └── init/                    # 数据库初始化脚本
├── .env                         # 全局环境变量配置
├── docker-compose.yml           # Docker Compose 编排文件
├── start.sh                     # 一键启动脚本
└── README.md                    # 项目说明文档
```

## 快速开始

### 方式一：一键启动脚本（推荐）

```bash
./start.sh
```

脚本自动完成构建、启动、健康检查，启动成功后输出访问地址。

### 方式二：Docker Compose 命令

```bash
# 构建并启动所有服务
docker compose up --build -d

# 仅启动（使用已有镜像）
docker compose up -d
```

启动成功后访问：

- 前端页面：http://localhost:3008
- 后端 API：http://localhost:8088/api

## 端口配置

所有端口在 `.env` 文件中统一配置，可根据实际环境修改。

| 服务   | 环境变量       | 默认值 | 说明           |
| ------ | -------------- | ------ | -------------- |
| 前端   | FRONTEND_PORT  | 3008   | Nginx 静态页面 |
| 后端   | BACKEND_PORT   | 8088   | Spring Boot API |
| MySQL  | MYSQL_PORT     | 3309   | 数据库服务     |
| Redis  | REDIS_PORT     | 6380   | 缓存服务       |

> 所有端口均已避开常用默认端口（80、443、8080、3306、6379 等），避免端口冲突。

## 环境变量

项目根目录 `.env` 文件包含所有可配置项：

```bash
# 项目名称
PROJECT_NAME=book-exchange-platform

# Docker 镜像仓库（统一使用国内镜像源）
DOCKER_REGISTRY=docker.m.daocloud.io

# 端口配置
FRONTEND_PORT=3008
BACKEND_PORT=8088
MYSQL_PORT=3309
REDIS_PORT=6380

# MySQL 配置
MYSQL_ROOT_PASSWORD=bookexchange2024
MYSQL_DATABASE=book_exchange
MYSQL_USER=bookadmin
MYSQL_PASSWORD=bookadmin2024

# Redis 配置
REDIS_PASSWORD=redis2024

# 时区
TZ=Asia/Shanghai
```

## 功能模块

### 图书广场

- 以卡片网格形式展示所有可借阅图书
- 支持按城市、分类、可借状态筛选，关键词搜索书名/作者
- 图书卡片展示书名、作者、分类、成色、城市、所有者、状态
- 点击申请借阅弹出对话框，填写借用周期和备注

### 我的藏书

- 展示当前用户录入的所有图书列表
- 支持添加新图书、编辑已有图书、删除图书
- 图书信息包含书名、作者、ISBN、分类、成色、描述、可借状态

### 借阅申请

- **收到的申请**：作为图书所有者查看他人发起的借阅申请，支持同意/拒绝
- **我的申请**：作为借阅者查看自己发起的所有申请及状态
- 状态流转：待审核 → 已同意 → 借阅中 → 已归还 / 已拒绝
- 支持确认借出、确认归还操作

### 借阅记录

- 以表格形式展示借阅历史记录
- 包含图书名称、借阅人、所有者、起止日期、借出/归还时间、状态、备注

## 性能优化

### 前端优化

- **组件封装**：图书筛选联动组件独立封装，复用性强
- **本地缓存**：多条件筛选参数 localStorage 持久化
- **代码分割**：构建产物代码分割（vue-vendor、element-plus 独立 chunk）
- **Gzip 压缩**：Nginx 开启 Gzip 压缩传输

### 后端优化

- **Redis 缓存**：在借图书基础信息缓存（1 小时过期），用户筛选条件缓存（7 天过期）
- **初始化数据**：城市、图书分类数据内置启动时自动初始化
- **连接池**：Hikari 数据库连接池优化配置
- **动态查询**：JPA Specification 动态条件组合查询

### Docker 优化

- **多阶段构建**：前端和后端均采用多阶段构建，减小最终镜像体积
- **分层缓存**：依赖层与源码层分离，最大化缓存复用
- **国内镜像源**：npm、Maven、Docker 基础镜像均使用国内镜像源加速
- **数据库优化**：MySQL 关闭二进制日志减少磁盘 IO

## 部署说明

### 镜像源配置

项目已预置国内镜像源，无需 VPN 即可正常构建。

| 层面   | 配置文件           | 镜像源              |
| ------ | ------------------ | ------------------- |
| 前端   | `frontend/.npmrc`  | npmmirror.com       |
| 后端   | `backend/settings.xml` | 阿里云 Maven 仓库 |
| Docker | `.env`             | docker.m.daocloud.io |

### 构建缓存验证

```bash
# 首次全量构建
docker compose build --no-cache

# 后续增量构建（观察输出中 "Using cache" 字样确认缓存生效）
docker compose build
```

### 常用命令

```bash
# 启动服务
docker compose up -d

# 重新构建并启动
docker compose up --build -d

# 停止服务
docker compose down

# 停止并删除数据卷（⚠️ 会清空数据库）
docker compose down -v

# 查看服务状态
docker compose ps

# 查看所有服务日志
docker compose logs -f

# 查看指定服务日志
docker compose logs -f backend
docker compose logs -f frontend

# 重启某个服务
docker compose restart backend
```

## 常见问题

### 端口冲突

启动时提示端口被占用，修改 `.env` 文件中对应端口号即可。

```bash
# 检查端口占用
lsof -i :3008
lsof -i :8088
```

### 镜像拉取失败

Docker 镜像拉取失败，可尝试更换 `.env` 中的 `DOCKER_REGISTRY`：

```bash
# 可选镜像源
DOCKER_REGISTRY=docker.m.daocloud.io
DOCKER_REGISTRY=hub-mirror.c.163.com
DOCKER_REGISTRY=mirror.baidubce.com
```

### 依赖下载失败

npm 或 Maven 依赖下载失败，检查网络连接，或更换对应配置文件中的镜像源地址。

### 查看容器日志

```bash
# 查看所有服务日志
docker compose logs -f

# 查看后端日志
docker compose logs -f backend

# 查看前端日志
docker compose logs -f frontend
```

## License

MIT
