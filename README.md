# APIX — AI Agent Collaboration Platform

<p align="center">
  <b>🤖 一款兼容多引擎的 AI Agent 协作平台</b><br>
  支持网页制作、代码编写、文档处理、海报设计等复杂任务的智能协作系统
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-17+-blue?logo=openjdk" alt="Java">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.3.5-brightgreen?logo=springboot" alt="Spring Boot">
  <img src="https://img.shields.io/badge/React-18+-61DAFB?logo=react" alt="React">
  <img src="https://img.shields.io/badge/MySQL-8.0-4479A1?logo=mysql" alt="MySQL">
  <img src="https://img.shields.io/badge/Redis-7.0-DC382D?logo=redis" alt="Redis">
  <img src="https://img.shields.io/badge/license-Apache%202.0-blue" alt="License">
  <img src="https://img.shields.io/badge/status-developing-yellow" alt="Status">
</p>

---

## 📋 目录

- [项目介绍](#-项目介绍)
- [系统架构](#-系统架构)
- [核心特性](#-核心特性)
- [技术栈](#-技术栈)
- [项目结构](#-项目结构)
- [快速启动](#-快速启动)
- [配置指南](#-配置指南)
- [API 概览](#-api-概览)
- [开发计划](#-开发计划)
- [许可证](#-许可证)

---

## 🎯 项目介绍

APIX 是一个 **多引擎 AI Agent 协作平台**，提供了一套完整的智能体运行时环境。它支持对接多种大语言模型（LLM），通过有向状态图引擎驱动 Agent 执行复杂任务，并内置了丰富的工具集（文件操作、代码执行、网络搜索、知识库检索等）。

### 核心设计理念

- **🔄 多引擎兼容** — 支持 OpenAI、DeepSeek、Moonshot、Ollama、Google、千问、千帆等主流 LLM
- **🧩 模块化 Agent 流水线** — 基于有向图的状态机引擎，可编排上下文准备 → LLM 调用 → 工具执行 → 消息持久化
- **🔧 可插拔工具系统** — 内置 20+ 工具，支持 MCP（Model Context Protocol）扩展
- **👥 多 Agent 协作** — 支持主 Agent + 子 Agent 的层级协作模式
- **💾 记忆系统** — 短期/长期记忆管理，支持对话历史持久化

---

## 🏗️ 系统架构

```
┌──────────────────────────────────────────────────────────────────┐
│                      apix-web (React 前端)                        │
│              Port: 5173 | Vite Dev Server                        │
│    React 18 + TypeScript + Zustand + TailwindCSS + React Router  │
└──────────┬──────────┬──────────┬──────────┬──────────┬──────────┘
           │          │          │          │          │
     ┌─────┘   ┌──────┘   ┌─────┘   ┌─────┘    ┌────┘
     ▼         ▼          ▼         ▼          ▼
┌─────────┐ ┌─────────┐ ┌────────┐ ┌────────┐ ┌─────────┐
│ apix-   │ │ apix-   │ │ apix-  │ │ apix-  │ │ apix-   │
│ agent   │ │ memory  │ │ file   │ │ task   │ │ common  │
│ :5091   │ │ :5093   │ │ :5094  │ │ :5090  │ │ (公共)  │
└────┬────┘ └────┬────┘ └───┬────┘ └───┬────┘ └─────────┘
     │           │          │          │
     └───────────┼──────────┼──────────┘
                 ▼          ▼
          ┌──────────┐ ┌──────────┐
          │  MySQL   │ │  Redis   │
          │:3306     │ │:6379     │
          └──────────┘ └──────────┘
```

### Agent 图执行流程

```
START
  │
  ▼
context_prepare ──── 上下文准备（拉取历史消息 + 记忆）
  │
  ▼
context_summary ──── 上下文压缩（截断 / 摘要）
  │
  ▼
llm_call ──────────── LLM 调用（流式 / 非流式）
  │
  ├──→ tool_execution ──→ llm_call (有 tool_calls 时循环)
  │
  └──→ messages_persist ──→ END (无 tool_calls)

```

---

## ✨ 核心特性

### 🤖 AI Agent 引擎
- 有向状态图驱动的 Agent 运行时（对标 LangGraph）
- 支持流式输出（SSE / WebSocket 打字机效果）
- 多 Agent 层级协作（主 Agent → 子 Agent）
- 角色卡系统（Role Schema）
- Docker 沙箱隔离执行

### 🔌 多 LLM 支持
| 供应商 | 支持情况 |
|--------|----------|
| OpenAI / 兼容接口 | ✅ |
| DeepSeek | ✅ |
| Moonshot (月之暗面) | ✅ |
| Ollama (本地部署) | ✅ |
| Google Gemini | ✅ |
| 阿里千问 | ✅ |
| 百度千帆 | ✅ |
| 小米 | ✅ |
| 自定义供应商 | ✅ (`custom-*`) |

### 🛠️ 工具集
| 类别 | 工具 |
|------|------|
| 📄 文件操作 | 读取文件、写入文件、列出文件、移动文件、删除文件 |
| 🌐 网络搜索 | 关键词搜索、URL 内容抓取 |
| 💡 知识库 | RAG 知识库检索 |
| ⌨️ 命令执行 | 终端命令执行、Python 代码运行 |
| 📋 任务管理 | 待办事项、任务流更新 |
| 👥 Agent 协作 | 子 Agent 分配、查询、停止 |
| 🧩 扩展 | MCP 客户端 (SSE / Stdio / HTTP / WS) |

### 💬 对话与记忆
- 多会话管理（历史会话列表）
- 树形消息结构（支持编辑分支）
- 短期记忆 + 长期记忆
- 上下文压缩（Token 超限时自动摘要）

### 📁 文件管理
- 文件上传 / 下载
- 多文件支持（单文件上限 100MB）
- SHA256 文件校验

---

## 🛠️ 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| **后端语言** | Java | **17+** |
| **主框架** | Spring Boot | **3.3.5** |
| **微服务** | Spring Cloud | 2023.0.3 |
| **ORM** | MyBatis-Plus | 3.5.13 (spring-boot3-starter) |
| **数据库** | MySQL | 8.0+ |
| **缓存** | Redis + Redisson | 3.37.0 |
| **JWT** | jjwt | 0.12.6 |
| **HTTP 客户端** | OkHttp | 4.12.0 |
| **HTML 解析** | Jsoup | 1.18.3 |
| **密码加密** | BCrypt (spring-security-crypto) | — |
| **工具库** | Hutool, FastJSON, Lombok | — |
| **前端框架** | React 18 + TypeScript | — |
| **构建工具** | Vite 6 | — |
| **路由** | React Router v6 | 6.28.0 |
| **状态管理** | Zustand 5 | — |
| **样式** | TailwindCSS 3 | — |
| **后端构建** | Maven | — |
| **前端包管理** | pnpm | — |

---

## 📁 项目结构

```
apix-java/
├── pom.xml                          # 父 POM (多模块聚合)
├── apix-common/                     # 🧩 公共模块
│   └── src/main/java/com/apix/common/
│       ├── constant/                 # 常量 (LlmProvider, AgentEvent)
│       ├── exception/                # 统一异常体系
│       ├── model/                    # 数据模型 (R<T>, AgentConfig, RoleSchema...)
│       └── util/                     # 工具类
│
├── apix-agent/                      # 🤖 Agent 核心引擎 (:5091)
│   └── src/main/java/com/apix/agent/
│       ├── AgentApplication.java     # 启动入口
│       ├── web/                      # REST API + WebSocket
│       │   ├── AgentController.java
│       │   ├── AgentWebSocketHandler.java
│       │   └── WebSocketConfig.java
│       ├── core/                     # 核心引擎
│       │   ├── AgentRuntime.java      # 运行时管理器
│       │   ├── AgentCreator.java      # 图工厂
│       │   ├── GenerationManager.java # 生成生命周期
│       │   ├── graph/                 # 状态图引擎
│       │   │   ├── AgentGraph.java    # 有向图引擎
│       │   │   ├── AgentNode.java     # 节点接口
│       │   │   └── node/              # 图节点实现
│       │   ├── llm/                   # LLM 适配器
│       │   ├── mcp/                   # MCP 客户端
│       │   ├── pipeline/              # 流式事件处理
│       │   ├── sandbox/               # Docker 沙箱
│       │   └── tools/                 # 工具系统
│       ├── client/                    # HTTP 客户端
│       └── config/                    # 配置
│
├── apix-memory/                     # 💾 记忆/对话/用户服务 (:5093)
│   └── src/main/java/com/apix/memory/
│       ├── MemoryApplication.java    # 启动入口
│       ├── controller/               # AuthController, MemoryController, UserController...
│       ├── entity/                   # 数据库实体 (User, Conversation, Message, FileStore)
│       ├── mapper/                   # MyBatis-Plus Mapper
│       ├── service/                  # 业务逻辑
│       └── config/                   # JWT 配置、安全过滤
│
├── apix-file/                       # 📁 文件管理服务 (:5094)
│   └── src/main/java/com/apix/file/
│       ├── FileApplication.java      # 启动入口
│       ├── controller/               # FileController
│       ├── entity/                   # 文件实体
│       ├── mapper/                   # Mapper
│       └── service/                  # 文件业务
│
├── apix-task/                       # 📋 任务管理服务 (:5090)
│   └── src/main/java/com/apix/task/
│       └── TaskApplication.java      # 启动入口（待开发）
│
└── apix-web/                        # 🖥️ React 前端 (:5173)
    ├── package.json
    ├── vite.config.ts                # Vite 配置 + 代理
    ├── tailwind.config.js
    └── src/
        ├── main.tsx                  # 入口
        ├── pages/                    # 页面
        │   ├── LoginPage.tsx         # 登录/注册
        │   ├── AssistPage.tsx        # 智能体对话（主页面）
        │   ├── DataPage.tsx          # 数据中心
        │   ├── TaskPage.tsx          # 任务管理
        │   ├── TaskFlowPage.tsx      # 工作流
        │   ├── ReportPage.tsx        # 报告
        │   ├── ServerPage.tsx        # 服务状态
        │   └── SettingPage.tsx       # 设置
        ├── components/               # 公共组件
        ├── store/                    # Zustand 状态
        │   ├── authStore.ts          # 认证
        │   └── chatStore.ts          # 聊天
        └── lib/
            ├── api.ts                # API 封装
            └── types.ts              # 类型定义
```

---

## � 项目完成度

| 模块 | 完成度 | 说明 |
|------|--------|------|
| apix-common | ✅ 90% | 公共模型、常量、异常体系 |
| apix-agent | ✅ **88%** | Agent 图引擎、LLM 适配、20+ 工具、MCP、子 Agent 调度 |
| apix-memory | ✅ **90%** | 对话/记忆/用户管理、JWT、BCrypt |
| apix-file | ✅ **75%** | 文件上传下载、MySQL 持久化、RAG 端点 |
| apix-task | ⚠️ **25%** | 基础 Controller 端点，待完整任务流引擎 |
| apix-web | ✅ **85%** | 8 个页面、深色主题、服务监控、代码编辑器 |

---

## �🚀 快速启动

### 环境要求

| 依赖 | 版本要求 | 用途 |
|------|----------|------|
| JDK | **17+** | 后端运行 |
| Maven | 3.6+ | 后端构建 |
| Node.js | 18+ | 前端构建 |
| pnpm | 8+ | 前端包管理 |
| MySQL | 8.0+ | 数据存储 |
| Redis | 7.0+ | 缓存 & 会话 |
| Docker (可选) | 最新版 | 代码沙箱执行 |

### 1️⃣ 克隆项目

```bash
git clone https://github.com/zjbTinyer/APIX-JAVA.git
cd apix-java
```

### 2️⃣ 初始化数据库

```sql
CREATE DATABASE IF NOT EXISTS apix_database
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

### 3️⃣ 配置后端

编辑 `apix-memory/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/apix_database?useSSL=false&serverTimezone=Asia/Shanghai
    username: root          # 改为你的 MySQL 用户名
    password: your-password # 改为你的 MySQL 密码
  redis:
    host: localhost
    port: 6379
    database: 0
```

### 4️⃣ 启动后端服务

按依赖顺序启动（推荐使用终端多标签页或 IDE 分批启动）：

```bash
# 编译所有模块
mvn clean package -DskipTests

# 启动记忆服务 (端口 5093)
mvn spring-boot:run -pl apix-memory

# 启动文件服务 (端口 5094)
mvn spring-boot:run -pl apix-file

# 启动 Agent 服务 (端口 5091)
mvn spring-boot:run -pl apix-agent

# 启动任务服务 (端口 5090) — 可选，目前为骨架
mvn spring-boot:run -pl apix-task
```

> 💡 **提示**：也可以在 IDE（IntelliJ IDEA）中直接运行各模块的 `*Application.java` 主类。

### 5️⃣ 启动前端

```bash
cd apix-web

# 安装依赖
pnpm install

# 启动开发服务器 (端口 5173)
pnpm dev
```

### 6️⃣ 访问系统

打开浏览器访问 **http://localhost:5173**

1. 进入登录页面，注册一个新账号
2. 登录后进入智能体对话页面
3. 在设置页面配置 LLM 供应商（如 OpenAI、DeepSeek 等）
4. 开始与 AI Agent 对话！

---

## ⚙️ 配置指南

### LLM 供应商配置

通过前端 `/settings` 页面配置，数据保存在浏览器 `localStorage` 中（键：`apix_providers`）。

支持的供应商格式：

```json
{
  "openai": {
    "apiKey": "sk-xxx",
    "baseUrl": "https://api.openai.com/v1",
    "model": "gpt-4o"
  },
  "deepseek": {
    "apiKey": "sk-xxx",
    "baseUrl": "https://api.deepseek.com",
    "model": "deepseek-chat"
  },
  "ollama": {
    "baseUrl": "http://localhost:11434",
    "model": "qwen2.5:7b"
  }
}
```

### 服务端口一览

| 服务 | 端口 | 说明 |
|------|------|------|
| apix-agent | 5091 | Agent 核心引擎 |
| apix-memory | 5093 | 记忆/对话/用户认证 |
| apix-file | 5094 | 文件管理 |
| apix-task | 5090 | 任务管理（待开发） |
| apix-web (Vite) | 5173 | 前端开发服务器 |

### 前端代理配置

`apix-web/vite.config.ts` 中已配置代理规则：

| 前端路径 | 代理目标 | 说明 |
|----------|----------|------|
| `/api/*` | `http://localhost:5091` | Agent API |
| `/ws` | `ws://localhost:5091` | WebSocket |
| `/auth/*` | `http://localhost:5093` | 认证 |
| `/memory/*` | `http://localhost:5093` | 记忆 |
| `/user/*` | `http://localhost:5093` | 用户 |
| `/file/*` | `http://localhost:5094` | 文件 |

---

## 📖 API 概览

### Agent 服务 (`:5091`)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/health` | 健康检查 |
| POST | `/api/v1/chat` | HTTP 聊天（同步） |
| POST | `/api/v1/get_models_list` | 获取模型列表（自动调用供应商 API） |
| GET | `/api/v1/get_sub_agent_task_list` | 子 Agent 任务列表 |
| GET | `/api/v1/clear_finished_tasks` | 清除已完成任务 |
| WS | `/ws/{platform}/{clientId}` | Agent 对话 WebSocket |

### 记忆服务 (`:5093`)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/health` | 健康检查 |
| POST | `/auth/login` | 用户登录 (AES 解密) |
| POST | `/auth/register` | 用户注册 |
| POST | `/auth/ensure_user` | 验证用户存在 |
| POST | `/memory/memory/append_message` | 追加消息 |
| POST | `/memory/memory/get_messages` | 获取消息列表 |
| POST | `/memory/conversation/get_list` | 会话列表 |
| POST | `/memory/conversation/create` | 创建会话 |
| GET | `/user/info` | 获取用户信息 |

### 文件服务 (`:5094`)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/file/health` | 健康检查 |
| POST | `/file/file/insert_file` | 文件上传（多文件） |
| POST | `/file/file/get_recent_files` | 最近文件列表 |
| POST | `/file/file/update_file` | 更新文件（软删除） |
| POST | `/rag/retrieval/search` | 知识库搜索 |
| GET | `/rag/documents` | 文档列表 |
| DELETE | `/rag/documents/{docId}` | 删除文档 |

### 任务服务 (`:5090`)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/health` | 健康检查 |
| GET | `/api/v1/tasks` | 任务列表 |
| GET | `/api/v1/tasks/stats` | 任务统计 |
| POST | `/api/v1/tasks/{taskId}/stop` | 终止任务 |

---

## 🗺️ 开发计划

- [x] **v1.0 基础版** ✅
  - [x] Agent 图引擎（上下文准备 → LLM 调用 → 工具执行 → 持久化）
  - [x] 多 LLM 供应商支持 + 模型列表自动获取
  - [x] Function Calling 工具系统（20+ 工具 + 权限控制）
  - [x] MCP 客户端（SSE / Stdio / HTTP / WebSocket）
  - [x] 子 Agent 后台调度
  - [x] WebSocket 流式通信（打字机效果）
  - [x] 对话记忆管理（短期 + 长期）
  - [x] 用户认证系统（JWT + BCrypt）
  - [x] JDK 17 + Spring Boot 3.3.5 升级
  - [x] 文件管理（上传/下载/SHA256/MySQL持久化）
  - [x] 前端深色主题 + 服务监控 + 代码编辑器
- [ ] **v1.1 增强版**
  - [ ] RAG 知识库（向量检索，集成 Chroma/FAISS）
  - [ ] Docker 沙箱完善
  - [ ] 任务流引擎（apix-task 模块）
  - [ ] MCP 工具扩展市场
  - [ ] 单元测试 + 集成测试覆盖
- [ ] **v2.0 协作版**
  - [ ] 多 Agent 团队协作
  - [ ] 可视化工作流编排（TaskFlowPage）
  - [ ] 插件系统
  - [ ] 企业级权限管理

---

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/amazing-feature`)
3. 提交更改 (`git commit -m 'feat: add some amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 开启 Pull Request

### 开发规范

- **代码风格**：遵循阿里巴巴 Java 开发手册
- **提交信息**：使用 Conventional Commits 规范
- **分支策略**：Git Flow

---

## 📄 许可证

本项目基于 **Apache License 2.0** 开源协议发布。

```
Copyright 2026 APIX Contributors

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

---

<p align="center">
  Made with ❤️ by APIX Contributors
</p>
