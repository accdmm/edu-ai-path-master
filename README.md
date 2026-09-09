# 智能学习平台（edu-ai-path-master）

前后端分离的智能学习平台整合仓库。以 day23 后端为主体，移植了 edu-ai-path 的 JWT 认证与 AI 智能客服（LangChain4j + 阿里云百炼 DashScope / Kimi）。

## 目录结构

```
edu-ai-path-master/
├── backend/                 # Spring Boot 3.2.6 后端（Java 17, Maven）
├── frontend/                # Vue3 + Vite 前端（Element Plus + Pinia）
├── scripts/
│   ├── init.sql             # MySQL 建表脚本 + 种子数据（管理员 admin/123456）
│   └── mongodb-data/        # MongoDB 本地数据目录（自动生成）
└── README.md
```

## 核心功能

- **在线考试**：试卷/题库管理、在线作答、自动批改、AI 简答题批改、考试排行榜
- **视频学习**：视频分类、上传审核、观看/点赞统计（需 MinIO 对象存储）
- **AI 智能客服**：LangChain4j + Kimi（阿里云百炼），记忆存储于 MongoDB，支持知识库检索（Pinecone，可选）
- **用户认证**：JWT 登录/注册/修改密码，管理员后台（admin/123456）

## 快速启动

### 1. 环境要求

| 组件 | 版本/说明 |
|------|-----------|
| JDK | 17+ |
| Maven | 3.6+ |
| Node.js | 18+（推荐 20+） |
| MySQL | 8.x，端口 3306，账号 root/root |
| MongoDB | 本地 27017（聊天记忆） |
| MinIO（可选） | 视频/图片对象存储，未配置则视频模块不可用 |
| DashScope API Key（可选） | AI 客服模型，见下文 |

### 2. 初始化数据库

```bash
mysql -uroot -proot --default-character-set=utf8mb4 -e "source scripts/init.sql"
```

导入后默认用户：

| 用户名 | 密码 | 角色 |
|--------|------|------|
| admin | 123456 | ADMIN（管理员后台） |
| teacher | 123456 | TEACHER |
| student | 123456 | STUDENT |

### 3. 启动后端

```bash
cd backend
mvn spring-boot:run
```

后端默认端口 **8080**。

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端默认端口 **3001**，访问 http://localhost:3001

### 5. AI 客服配置（可选）

后端 `application.yml` 已内置 DashScope key。如需切换模型或使用自己的 key：

```yaml
dashscope:
  api-key: sk-xxx
  chat-model: kimi-k3      # 阿里云百炼 DashScope 模型（走 OpenAI 兼容接口）
  embedding-model: text-embedding-v3
```

**降级策略**（key 缺失时仍可启动系统，仅客服降级）：

| 组件 | 有 key | 无 key 降级 |
|------|--------|-------------|
| ChatModel | 阿里云百炼 DashScope（OpenAI 兼容接口） | Kimi/Moonshot（需 kimi.api-key） |
| EmbeddingModel | Qwen text-embedding-v3 | 本地 BGE 模型（仅维度占位） |
| 向量存储 | Pinecone | 内存存储（知识库检索不可用，客服仅通用问答） |

MongoDB 用于持久化客服对话记忆（集合 `chat_messages`），未安装时客服接口会报错。

## 关键配置点

- **后端状态码**：`Result<T>` 中 `200` 成功，`401` 未登录（未带 token 访问受保护接口也返回 401 JSON，前端据此跳登录）。
- **需要登录的接口**：`/api/chat/**`、`/api/user/info`、`/api/user/updatePwd`。其余接口渐进式放行（后续可收紧）。
- **前端请求**：`frontend/src/utils/request.js` 使用绝对地址 `http://localhost:8080`，自动附加 `Authorization: Bearer <token>`，`/chat` 接口超时 60s，401 自动跳转 `/login`。
- **聊天历史**：`ChatController` 提供 `/api/chat/history`（按用户分组），删除/清空记录由管理员接口完成。

## 已知限制

- 视频上传/预览依赖 MinIO，未配置时视频模块不可用。
- 邀请码管理（前端 `/interview-codes`）为占位页，后端接口未实现。
- 企业真题/模拟面试为前端保留页面（mock/待补后端）。