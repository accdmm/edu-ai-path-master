# 智能学习平台（edu-ai-path-master）— 项目交接文档

> 本文件供后续 Claude 会话快速恢复上下文。更新代码后请同步维护本文件。

## 1. 项目位置与结构

- 项目根目录：`C:\Users\34147\Downloads\edu-ai-path-master`
- 非 git 仓库，已删除旧嵌套源码目录（`edu-ai-path-master/edu-ai-path-master`），当前结构干净：

```
edu-ai-path-master/
├── backend/                     # Spring Boot 3.2.6 + Java 17 + Maven（端口 8080）
│   └── src/main/java/com/atguigu/exam/
├── frontend/                    # Vue3 + Vite（Element Plus + Pinia + ECharts，端口 3001）
│   └── src/
├── scripts/
│   ├── init.sql                 # MySQL 建表脚本 + 种子数据
│   └── mongodb-data/            # MongoDB 8.0.6 本地数据目录（运行中，27017）
└── README.md                    # 面向用户的启动说明
```

## 2. 项目来历

- 以 day23（`exam-system-web-backup` 前端 + `backend`）为主体，整合 edu-ai-path 的 JWT 认证 + AI 智能客服（LangChain4j + 阿里云百炼）。
- 前端来源：`C:\Users\34147\Desktop\exam-system-web-backup`（已 robocopy 至 frontend/）。
- AI 客服页面：从旧 edu 前端移植（`@/api/chat` + `<Chat.vue>`）。

## 3. 已完成（已验证）

### 后端（backend/）
- **认证模块**：`JwtUtil`、`JwtAuthenticationFilter`、`CustomUserDetailsService`、`SecurityConfig`、`UserController`（login/register/info/updatePwd）。
- **AI 客服**：`DashScopeConfig`、`XiaohuAgent`（@AiService）、`ChatController`（`/api/chat`、`/api/chat/history` 等）、`MongoChatMemory`（MongoDB 持久化，collection `chat_messages`）、`FindQuestionUtil` 工具、`system` 提示词 `xiaohu_callWords.txt`。
  - **关键坑（乱码已修复）**：LangChain4j 1.0.0-beta3 的 `@SystemMessage(fromResource=…)` 内部用 `new Scanner(InputStream)`（**平台默认编码**）读资源文件，Windows 下会把 UTF-8 中文提示词读成乱码并持久化进 Mongo，导致聊天记录首条为乱码 SYSTEM、侧栏标题乱码。修复：改为 Java 常量 `XiaohuSystemPrompt.SYSTEM`（源码 UTF-8，编译期固定），绕开运行时编码；同时 `ChatController.extractTitle` 改为**跳过 SYSTEM/TOOL 取第一条 USER 文本**、`Chat.vue` 渲染历史时 `.filter(msg.type !== 'SYSTEM')`。旧乱码记录仍在 Mongo（`chat_messages`），前端删记录即可清理，不影响新对话。
- **编译**：`mvn compile -q` 通过。`mvn spring-boot:run` 启动成功（Tomcat 8080）。
- **联调验证（真实 HTTP 调用）**：
  - `POST /api/user/login`（admin/123456）→ code=200，签发 JWT，role=ADMIN。
  - 无 token 访问 `/api/user/info` → HTTP 401 + `{"code":401,...}` JSON。
  - `POST /api/chat`（带 token）→ code=200，Kimi 真实中文回复。

### AI 聊天生成试卷（已在客服中打通）
- **能力**：对客服说「帮我生成一套 JVM 试卷」→ LLM 真实出题 → 事务落库（questions/choices/answers + paper[状态DRAFT] + paper_question + user_paper）→ 绑定当前登录用户 → 聊天回复含可点击链接「点击开始考试：/exam/start/{id}」。用户可去「我的AI试卷」页查看并开考。
- **关键坑（已被迫改架构）**：LangChain4j 1.0.0-beta3 的 AiService 工具回传存在序列化缺陷——`InternalOpenAiHelper` 把 `ToolExecutionResultMessage` 转成 `ToolMessage.from(toolCallId, text)`，**TOOL 消息不带 `name` 字段**，DashScope 的 Kimi K3 会报 `tool messages need a resolvable tool name`（InvalidParameter，会话级崩溃）。无法配置解决 → **`XiaohuAgent` 已移除全部 tools**，改由 `ChatController.chat` 手动意图识别：命中「生成一套/出一套/生成试卷/帮我生成/帮我出」+ 含「试卷/题目/题」→ 调 `AiGeneratedPaperService`；否则普通问答。提示词 `XiaohuSystemPrompt` 选项 B 同步改为「引导用户直接说【帮我生成一套 XX 试卷】，系统自动识别」（勿提工具）。
- **出题模型**：`DashScopeConfig` 新增 `paperChatModel` bean（配置 `dashscope.paper-model`，为空回退主模型 kimi-k3），统一走 `buildOpenAiChatModel()`（**timeout 8 分钟 + maxRetries(1)**）。qwen-plus 报 `AllocationQuota.FreeTierOnly`（免费额度用尽），kimi-k3 可用、用于出题。
- **落库数据规范**（已验证）：CHOICE 答案 `question_answers.answer` 存选项字母（多选 A,B,D 逗号分隔，由 is_correct 推导）；JUDGE 存 TRUE/FALSE；TEXT 存文本；选项字母由 sort 推导（表无 choice_character 列）。
- **越权漏洞已修复**：原本 `UserContextUtil.getUserId()` 无 token 回退 `1L`（答辩演示友好），而 admin 的 userId 恰为 1 → 无 token 也能看「我的AI试卷」和 DRAFT 私有卷详情。修复：`UserContextUtil` 新增 `isAuthenticated()`；`PaperController` 的 `/my-ai-papers` 需登录（否则 code=401），`GET /{id}` 对未登录访问者传 `userId=-1L`（永不命中归属校验），PUBLISHED 公开卷仍可匿名访问。`ExamServiceImpl.startExam` 对 DRAFT 卷做归属校验并写 `exam_records.user_id`。
- **遗留**：`exam_records`/`paper` 含联调产生的测试数据（paper id=2、3 为 kimi-k3 生成的 AI 卷，可留作演示；exam_records id=1 为测试记录）。

### AI 学习分析（已开发并验证）
- **能力**：补齐 Home「AI分析」入口死链 → 完整实现智能学习报告。`AnalysisController`（`/api/analysis/learning-report`、`/api/analysis/ai-suggest`，**均需登录**）+ `LearningAnalysisService(Impl)` + `LearningReportVo`（overview 概览 / scoreTrend 成绩趋势 / categoryMastery 知识点掌握 / radar 能力雷达 Top6）。
- **数据源**（全部走既有 Mapper，代码层聚合，无新表）：`exam_records`(score 非 null) JOIN `paper.totalScore` → 趋势；`answer_record`→`questions.categoryId`→`categories.name` → 知识点得分率（按分值算）；`user_paper` → AI 卷数；`mock_interview` → 面试次数。空数据返回空集合不报错。
- **AI 建议走 DashScope kimi-k3**：`generateAiSuggest` 注入 `@Qualifier("paperChatModel") ChatLanguageModel`，用 `chat()`（**注意 LangChain4j 1.0.0-beta3 无 `generate()`，统一用 `chat(String)`**）+ kimi.api-key 的 Moonshot `KimiAiService` 仅用于简答题批阅/出题，不做 AI 建议）。解析 `{"suggestions":[...]}` JSON（剥离 ```json 包裹），解析失败/调用失败**降级为规则型建议**。已验证返回 5 条真实建议，正确引用 interviewCount=3。
- **前端**：`views/Analysis.vue`（概览统计卡 + echarts 折线/柱状/雷达 + AI 建议按钮，`el-empty` 空态）、`api/analysis.js`、`router` 加 `/analysis`（requiresAuth）。`npm run build` 通过。
- 后端重启需带 `DASHSCOPE_API_KEY`（`cmd /c set KEY=... && mvn spring-boot:run`）。

### 数据库
- `scripts/init.sql` 已执行（MySQL 8.0，库 `exam_system_0625`，账号 root/root）：**16 张核心表 + 11 张商业化闭环表 = 27 张表** + 种子数据。
  - 默认用户：admin/teacher/student，密码均 123456（BCrypt）。
  - 商业化种子：4 家企业（阿里/字节/腾讯/美团）、12 道真题、3 个邀请码（`EDU2024DEMO` 已被 user3 激活、`EDU2024VIP0`、`EDU2024ENTR`），user3 初始积分 100/100。
  - 注意：`mysql` 命令在 PowerShell 中无法用 `<` 重定向，需用 `mysql -e "source 路径"`，且 PowerShell 控制台中文会乱码（数据本身 UTF-8 正常）。重跑 init.sql 加 `--default-character-set=utf8mb4`。
- **MongoDB 8.0.6** 手动部署（winget 安装失败/超时后改 zip）：
  - 二进制：`C:\Users\34147\AppData\Local\Temp\opencode\mongodb\mongodb-win32-x86_64-windows-8.0.6\bin`
  - 数据目录：`scripts/mongodb-data`（mongod 运行于 27017）。

### 前端（frontend/）
- `npm install` + `npm run build`（vite build）通过。
- 新增/改造：
  - `stores/user.js`（pinia：token + userInfo 持久化）
  - `api/user.js`、`api/chat.js`
  - `utils/request.js`：baseURL=`http://localhost:8080`（绝对地址），自动注入 `Authorization: Bearer`，`/chat` 接口 60s 超时，401 自动 logout 并跳 `/login`。
  - `views/Login.vue`、`Register.vue`、`Chat.vue`（Chat 从旧前端整页拷贝）
  - `router/index.js`：新增 `/login`、`/register`、`/chat`（requiresAuth 守卫，带 redirect 回跳）
  - `views/Home.vue`：导航栏加 AI 客服入口、用户下拉退出、管理员登录改走真实 `/api/user/login` 并校验 role=ADMIN 后存 token。
  - `views/InterviewCodes.vue`：原为空文件导致 build 失败，改为占位页。
  - `api/interviewQuestion.js`：补 `getRelatedQuestions` / `toggleFavorite` / `submitEvaluation`。
  - `views/InterviewResult.vue`：图标 `Lightbulb` 不存在，改 `Aim`。
  - `views/Chat.vue`：消息气泡改 `v-html` + `renderContent`（escapeHtml 防 XSS + `/exam/start/(\d+)` 转可点击链接 + `\n` 转 `<br/>`）。
  - `views/MyPapers.vue` 新建：我的AI试卷列表（卡片 + 开始考试/看详情/空态/loading），路由 `/my-papers`（requiresAuth），Home 导航栏加「我的AI试卷」按钮，`api/paper.js` 补 `getMyAiPapers()`。
  - `views/Analysis.vue` 新建：AI学习分析页（概览卡 + echarts 折线/柱状/雷达 + AI建议按钮），路由 `/analysis`（requiresAuth），Home/Analysis 入口已打通。
- `package.json` 添加 `sass`（Chat.vue 用 scss）。

### 商业化闭环后端（已开发并验证，详见 PLAN-商业化闭环.md）
- **新增 11 张表**（脚本追加在 init.sql，库 `exam_system_0625`）：`interview_question`、`interview_company`、`interview_question_category`、`mock_interview`、`mock_interview_answer`、`invite_code`、`user_credit`、`credit_record`、`user_contribution`、`interview_favorite`、`interview_evaluation`。
- **企业真题用户端**：`InterviewQuestionService`/`Controller`（list/detail/hot/latest/related/view/favorite/stats）。注意 `interview_question` 无 `question_type` 列；`interview_favorite` 无 `update_time`；`interview_evaluation` 是用户星级评价表（仅 rating+comment，不存 AI 评分）。
- **AI 模拟面试**：`MockInterviewAiService`（复用 `KimiAiService.callKimiAi`，AI 评分 gradeAnswer + AI 汇总 summarizeInterview，JSON 剥离 + 分数 clamp + 兜底）；`MockInterviewController`（/api/mock-interview/start、/submit-answer 双模式、/{id}/complete、/user/{userId}/records）。
- **邀请码**：`InviteCodeController`（/interview/codes 下：list/generate/activate/invitees/delete/request），激活发放积分（normal/vip=100，enterprise=300）。
- **积分**：`UserCreditController`（/api/user-interview-credits/*、/interview/credits/history）。
- **用户贡献真题**：`UserContributionController`（/api/user-contributions/upload 兼容 /api/pending-questions，审核 /review）。
- **管理端**：`AdminContentController`（/api/admin/interview-questions 增删查、/api/companies/enabled|list、/api/company-question-categories/enabled|tree）；`CommonUploadController`（/api/upload 本地降级存 ./uploads，MinIO 不可达时视频/图片上传仍可用）。
- **模拟面试门户**：`InterviewPortalController`（/interview/mock-interview/{id}、result、history、statistics、share）。
- **前端改动**：`MockInterview.vue` 补 `interviewResult.id = response.data.id`（此前 submit/complete 用 undefined）；`InterviewQuestionManage.vue` 由 mock 改为调真实接口；router 补 `/company-detail/:id`。
- **验证结果**：`mvn compile -q` 通过；Python 全链路断言 30/30 通过（登录→激活邀请码+100分→开始面试→AI 评分→完成→详情→记录→真题/收藏/统计→管理端列表）；`npm run build` 通过。UI 级浏览器验证未执行（计划用 gstack browse，缺 Playwright 内核 1208，用户选择自行验证）。

## 4. AI 模型配置（backend/src/main/resources/application.yml）

```yaml
dashscope:
  api-key: sk-ws-...（用户提供）
  chat-model: kimi-k3        # 阿里云百炼，走 OpenAI 兼容接口
  embedding-model: text-embedding-v3
pinecone:
  api-key: ""                # 未配置 → 降级内存存储
```

- **关键坑**：`QwenChatModel` 走 DashScope 原生接口会报 `url error`，必须用 `OpenAiChatModel` + baseUrl `https://dashscope.aliyuncs.com/compatible-mode/v1`（DashScopeConfig 已实现）。
- **降级策略**（无 key 时系统仍可启动，仅客服降级）：
  - ChatModel：DashScope key 缺失 → 回退 Kimi/Moonshot（kimi.api-key / `https://api.moonshot.cn/v1`）。
  - EmbeddingModel：DashScope key 缺失 → 本地 BGE 量化模型（仅维度占位）。
  - 向量存储：Pinecone key 缺失 → `InMemoryEmbeddingStore`（知识库检索不可用，客服仅通用问答）。

## 5. 安全策略（渐进式）

- Spring Security 放行大多数接口，仅 `/api/chat/**`、`/api/user/info`、`/api/user/updatePwd` 需认证。
- 未认证访问受保护接口 → `authenticationEntryPoint` 返回 `Result{code:401}` JSON（非默认 403）。
- **私有资源业务级鉴权**（SecurityConfig permitAll 场景下在 service/controller 层强制）：`/api/papers/{id}`（DRAFT 卷仅归属用户，匿名传 -1L）、`/api/papers/my-ai-papers`（需登录）、`/api/exams/start`（DRAFT 卷归属校验 + 写 user_id）、`/api/analysis/**`（SecurityConfig authenticated）。`UserContextUtil` 提供 `isAuthenticated()`，切勿依赖 `getUserId()` 的 1L 回退判断登录态。
- 管理端写接口暂未强制鉴权，后续可在前端全链路 token 化后收紧。

## 6. 还需做（待办/阻塞项）

### 阻塞项（等待用户提供）
- **Redis**：已解决。本机已有 Redis（`D:\Redis\Redis-x64-5.0.14.1\redis-server.exe`，127.0.0.1:6379，无需密码）。`application.yml` host 已从不可达的 `47.94.86.115` 改为 `127.0.0.1`。注意热门题目（`/api/questions/popular`）和答题计数强依赖 Redis（`RedisUtils` zset），此前报"连接不到 redis"即此因；另修复了 `customFindPopularQuestions` 在 Redis 空榜时 `NOT IN ()` 非法 SQL 的 bug（`idsSet.isEmpty()` 时跳过 notIn）。
- **MinIO 地址**：`minio.endpoint` 指向 `47.94.86.115:9000`（不可达）。视频/图片上传与展示依赖 MinIO，未配置则视频模块不可用（`CommonUploadController` 已做本地降级存 `./uploads`）。
- **Pinecone key**：未提供；配置后需在 Pinecone 建索引并灌入知识库数据（`EmbeddingStoreConfig` createIndex dimension 由 embeddingModel 决定）。

### 待办（可自主推进）
1. **管理端前端鉴权**：`views/AdminLayout.vue` 等管理页面目前无 token 校验；完成"管理员登录存 token → 管理端路由守卫 → 后端管理接口鉴权"闭环。
2. **Chat.vue + 模拟面试/学习分析 UI 浏览器验证**：前后端已就绪，用户自行在浏览器走一遍登录→聊天→模拟面试→AI学习分析流程（未执行 UI 级测试）。其中「AI 聊天生成试卷」「学习报告/AI建议」链路已做 HTTP 级验证，但「点击聊天链接跳开考」「我的AI试卷页开考」「AI分析页图表渲染」等 UI 交互待用户浏览器确认。
3. **`.gitignore`**：非 git 仓库，若未来 git init 需忽略 `scripts/mongodb-data/`、`frontend/node_modules/`、`backend/target/`、`backend/uploads/`。
4. **中文控制台乱码**：后端日志/接口在 PowerShell 输出中文乱码（实际 UTF-8 数据正常），联调建议用 Python 或文件方式断言，勿信终端显示。

## 7. 常用命令速查

```bash
# 后端编译/启动（PowerShell）
cd backend; mvn compile -q; mvn spring-boot:run        # 8080
# 前端启动/构建
cd frontend; npm run dev                                # 3001
npm run build
# MySQL 初始化（PowerShell 不能用 < 重定向）
mysql -uroot -proot -e "source C:/Users/.../scripts/init.sql"
# MongoDB 启动（若未运行）
& "C:\Users\34147\AppData\Local\Temp\opencode\mongodb\mongodb-win32-x86_64-windows-8.0.6\bin\mongod.exe" --dbpath "scripts/mongodb-data" --port 27017 --bind_ip 127.0.0.1
```

## 8. 技术栈速览

- 后端：Spring Boot 3.2.6、MyBatis-Plus 3.5.11、Spring Security + JWT、LangChain4j 1.0.0-beta3（community-dashscope / open-ai / pinecone / easy-rag / pdfbox）、MongoDB、MySQL 8、minio、Swagger。
- 前端：Vue3（组合式 API + `<script setup>`）、Vite 4、vue-router 4、pinia、Element Plus、axios、echarts、sass。
- 环境：JDK 17.0.9、Maven 3.6.3（仓库 `D:\repository`）、Node v24.14.0、MySQL 8.4.5（`D:\mysql-8.4.5-winx64`）。