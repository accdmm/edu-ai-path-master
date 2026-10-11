# 智能学习平台（edu-ai-path-master）— 项目交接文档

> 本文件供后续 Claude 会话快速恢复上下文。更新代码后请同步维护本文件。

## 1. 项目位置与结构

- 项目根目录：`C:\Users\34147\Downloads\edu-ai-path-master`
- 已纳入 git 版本管理（分支 main，有 origin 远端；本地已多次提交），已删除旧嵌套源码目录（`edu-ai-path-master/edu-ai-path-master`），当前结构干净：

```
edu-ai-path-master/
├── backend/                     # Spring Boot 3.2.6 + Java 17 + Maven（端口 8081）
│   └── src/main/java/com/atguigu/exam/
├── frontend/                    # Vue3 + Vite（Element Plus + Pinia + ECharts，端口 3001）
│   └── src/
├── scripts/
│   ├── init.sql                 # MySQL 建表脚本 + 种子数据
│   └── init.sql             # MySQL 建表脚本 + 种子数据（MongoDB 已迁至 D:\mongodb-data）
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
- **AI 建议走 DashScope kimi-k3**：`generateAiSuggest` 注入 `@Qualifier("paperChatModel") ChatLanguageModel`，用 `chat()`（**注意 LangChain4j 1.0.0-beta3 无 `generate()`，统一用 `chat(String)`**）。解析 `{"suggestions":[...]}` JSON（剥离 ```json 包裹），解析失败/调用失败**降级为规则型建议**。已验证返回 5 条真实建议，正确引用 interviewCount=3。
- **重要架构变更（2024-09 修复交卷 500）**：`KimiAiServiceImpl.callKimiAi` **已从 Moonshot WebClient 切换为 DashScope `paperChatModel`（kimi-k3）**。原因：`kimi.api-key`（${KIMI_API_KEY:}，无环境变量）下 Moonshot 必失败 → 考试简答题评分/总评/教师 AI 出题/模拟面试全挂，交卷报「已经重试3次！依然失败！」。切换后全部走已验证可用的 kimi-k3；`callKimiAi` 返回前增加 `stripJsonFence`（剥离 ```json 围栏）。`ExamServiceImpl.gradeExam` 的 `buildSummary` 加 try-catch → 失败降级规则型总评，**交卷永不 500**。WebClient/Moonshot 配置保留未动（降级路径仍在，仅不启用）。
- **异步判卷（Redis Stream，已上线）**：`customSubmitAnswer` 不再同步判卷——保存答案后置状态 `判卷中` 并 `XADD exam:grade`（Stream：GROUP `grader` / CONSUMER `worker1`），立即返回；`ExamGradingWorker`（ApplicationRunner 后台线程）`XREADGROUP` 消费 → `gradeExam` → `XACK`。失败不 ACK 留 PEL，每 60s `XPENDING`+`XCLAIM`（idle>5min）重投，同一任务重试 ≥3 次强制降级结算（status `已批阅`，保留已有分数，评语降级）。Redis 不可用时 catch 降级为同步判卷。注意：Spring Data Redis 3.2.6 的签名是 `pending(key, group, Range.unbounded(), limit)`（返回 PendingMessages）、`PendingMessage.getElapsedTimeSinceLastDelivery()`（无 getIdleTimeMs）、StringRedisTemplate 的 stream 泛型为 `MapRecord<String,Object,Object>`。前端 `Exam.vue` 交卷成功后**直接跳 `/exam-result/{id}`**（已去掉 3–5s 假动画），`ExamResult.vue` 在 status=`判卷中` 时每 3s 静默轮询（上限 40 次，含已等待秒数计时 + 判卷步骤打钩动画面板）；`/submit` axios timeout 放宽至 200s。客观题本地 `equalsIgnoreCase` 精确比对，仅简答题 AI 评分 + 总评 AI；**纯客观题试卷跳过 AI 总评用规则评语**（`gradeExam` 判断 `hasTextQuestion`），判卷秒级完成。已实测：纯选择卷提交→已批阅 1.1s，含简答题 AI 卷约 40s。
- **前端**：`views/Analysis.vue`（概览统计卡 + echarts 折线/柱状/雷达 + AI 建议按钮，`el-empty` 空态）、`api/analysis.js`、`router` 加 `/analysis`（requiresAuth）。`npm run build` 通过。
- 后端重启需带 `DASHSCOPE_API_KEY`（`cmd /c set KEY=... && mvn spring-boot:run`）。

### AI 解析题目（消耗积分/免费额度，已开发并验证）
- **能力**：企业真题详情页（`InterviewQuestionDetail.vue`）新增「AI 解析本题」按钮 → 后端 `POST /api/interview-questions/{id}/ai-analysis`（需登录）调 Kimi（复用 `MockInterviewAiService.explainQuestion`）生成题目讲解（考点/答案/易错点/追问视角，约 600 字 Markdown）。
- **计费**：每道题 5 积分，**每天 3 次免费额度**（当日 0 点重置，按 `credit_record` type=`ai-analysis` 且 change_amount=0 的当日记录数计）。先调 LLM 成功后计费（失败不扣）；免费用尽后走 `UPDATE user_credit SET active_credits=active_credits-5`（条件 `active_credits>=5` 防并发超扣，**不减 total_credits**，total 为累计获得排行）。积分不足返回 `code 4002`，前端弹确认框引导去 `/pay` 购买。
- **流水**：免费记 `change_amount=0`、扣费记 `-5`，source 分别为「AI解析免费额度」/「AI题目解析」，type 统一 `ai-analysis`。
- **落地文件**：`InterviewQuestionServiceImpl.aiAnalysis`、`MockInterviewAiServiceImpl.explainQuestion`、前端 `aiAnalysisQuestion()` + 详情页解析卡片（结果 `white-space:pre-wrap` 展示，无缓存、不渲染 Markdown）。
- **已实测**：未登录 401；免费 3 次（free=true 积分不变）→ 第 4 次扣费（free=false、active 100→95）、credit_record 落账、Kimi 真实 1000 字解析。前端 build 通过。浏览器 UI 点击流程待用户确认。

### AI 解析/改卷计费扩展（2026-09-10 第二轮，已开发并全链路验证）
- **试卷详情「AI 解析本题」**：`PaperDetail.vue` 每题加「AI 解析本题」按钮 → 后端 `POST /api/papers/{id}/ai-analysis`（body `{questionId}`，需登录）→ `PaperService.aiAnalysis`（DRAFT 归属校验 + 校验题属于该卷）→ 复用新增 `MockInterviewAiService.explainPaperQuestion`（普通题目讲解 prompt，非面试视角）→ 计费。
- **计费与真题共用免费池**：与企业真题**共用** `type='ai-analysis'` 每日 3 次免费（当日 change=0 计数），超出扣 5 积分（条件 `active_credits>=5`，不减 total），source 免费「AI解析免费额度」/付费「AI试题解析」，积分不足 `4002` 前端引导 `/pay`。返回 `{analysis, free, remainingFree, activeCredits}` 复用于 `PaperDetail` token。
- **改卷 AI 计费（异步 worker 场景）**：`ExamServiceImpl.gradeExam` 在 `hasTextQuestion=true` 且判卷成功返回前调用 `billingAiGrading(examRecord.getUserId())`（userId 来自 `exam_record.user_id`，startExam 已写）：当日 `type='exam-ai-grade'` 且 change=0 计数 <3 → 免费；否则条件扣 5；**积分不足 → 免费兜底**（change=0，source「AI判卷免费(积分不足)」），**AI 判卷照常完成、交卷永不失败**。计费提示追加到评语末尾（如「本次 AI 判卷已消耗 5 积分」），`ExamResult.vue` 零改动自动展示。纯客观题卷与 worker 强制结算（无 LLM）路径不计费；`gradeExam` 幂等（已批阅提前返回）保证重投不重扣。
- **落地文件**：`PaperService(Impl).aiAnalysis`、`PaperController /{id}/ai-analysis`、`MockInterviewAiService.explainPaperQuestion`、`ExamServiceImpl.billingAiGrading`（常量 AI_GRADING_COST=5/AI_GRADING_DAILY_FREE=3）、前端 `api/paper.js aiAnalyzePaperQuestion`、`PaperDetail.vue`。
- **已实测（admin，DB/接口级别）**：paper7 判卷 60s → 评语「本次 AI 判卷使用免费额度」；第 2/3 份免费 → 第 4 份扣 -5（balance 90→85）；设 active=3 后第 5 份 → 「本次 AI 判卷因积分不足由平台赠送」+ credit_record change=0/balance=3；试卷 AI 解析（今日真题 3 次免费已用完）→ free=false 扣 5（95→90）、619 字 kimi 解析、remainingFree=0、credit_record 「AI试题解析」-5。测试后 admin 积分已恢复 85。浏览器 UI 未测（按钮点击/4002 弹窗）。

### 支付宝沙箱支付（购买邀请码，已开发）
- **能力**：前端「购买邀请码」页（`/pay`，Home 导航「购买邀请码」按钮 + 激活页购买按钮）选三档商品（ordinary 9.9 / vip 29.9 / enterprise 99.9）→ 后端 `POST /api/pay/create` 用 SDK `pageExecute` 生成支付宝电脑网站支付 form → 新窗口提交 → 沙箱买家付款 → 回跳 `/pay/result`（`PayResult.vue` 轮询 `/api/pay/order/status`）。**购买即自动激活**：`PayOrderServiceImpl.settle` 幂等结算（status='CREATED' 条件下 update），生成 `EDU+uuid8` 邀请码（invite_code 置 used、activedBy=买家）并发放积分（enterprise=300 其余=100，credit_record type='invite'）。
- **落库**：`pay_order` 表（orderNo P+时间戳、productType、amount、status CREATED/PAID/FAILED/CLOSED、tradeNo、payTime、createTime、updateTime、userId）+ user_credit/credit_record/invite_code。已追加 scripts/init.sql 末尾。
- **接口**：`/api/pay/create`、`/api/pay/order/status`（对 CREATED 订单调 `alipay.trade.query` 确认 TRADE_SUCCESS 自动结算，解决本地无公网 notify 收不到的兜底）、`/api/pay/orders`（我的订单）、`/api/pay/alipay/notify`（异步回调，SecurityConfig 放行，接口级 RSA2 验签；未验证因为本地无公网）。`/api/pay/**` 其余需认证。
- **配置**（application.yml `alipay` 节点）：沙箱 `app-id=9021000167699600`、`alipay-public-key`（**平台固定公钥，已写死**）、**商户应用私钥用环境变量 `ALIPAY_PRIVATE_KEY` 注入（PKCS8 DER base64 单行，不落 git）**。私钥对应「应用公钥」须在开放平台沙箱应用→密钥 页粘贴，两者必须成对，否则支付宝报 `invalid-signature`（此事已踩坑：给错密钥对会验签失败）。
- **SDK**：pom 依赖 `com.alipay.sdk:alipay-sdk-java:4.40.989.ALL`（alimaven 无 4.38.x，别用旧版本号）。`AlipayConfig` 提供 AlipayClient bean。
- **前端**：`api/pay.js`（createPayOrder/payOrderStatus/myPayOrders）、`views/Pay.vue`（商品卡 + 我的订单 dialog；同步先 `window.open('', '_blank')` 拿句柄防弹窗拦截，await 后向句柄 `document.write(form)`；3s 后原窗口跳 `/pay/result`）、`views/PayResult.vue`（2s×15 轮询）、router `/pay`、`/pay/result`（requiresAuth）。
- **小坑**：3001 曾被旧 vite 进程（9 天前启动的陈旧代码）占用且仅绑 `::1`，需杀陈旧 node 进程再起 dev；前端 build 有 chunk>500k 警告（非错误）。
- **状态**：create 接口已 python 验证（返回完整 form + sign）；沙箱收银台打开（=平台应用公钥与本地私钥成对验证通过）与付款闭环待浏览器确认。

### 数据库
- `scripts/init.sql` 已执行（MySQL 8.0，库 `exam_system_0625`，账号 root/root）：**16 张核心表 + 11 张商业化闭环表 = 27 张表** + 种子数据。
  - 默认用户：admin/teacher/student，密码均 123456（BCrypt）。
  - 商业化种子：4 家企业（阿里/字节/腾讯/美团）、12 道真题、3 个邀请码（`EDU2024DEMO` 已被 user3 激活、`EDU2024VIP0`、`EDU2024ENTR`），user3 初始积分 100/100。
  - 注意：`mysql` 命令在 PowerShell 中无法用 `<` 重定向，需用 `mysql -e "source 路径"`，且 PowerShell 控制台中文会乱码（数据本身 UTF-8 正常）。重跑 init.sql 加 `--default-character-set=utf8mb4`。
- **MongoDB 8.0.6**（2026-09-12 已迁至 D 盘，原 Temp 副本与仓库内数据目录已删除）：
  - 程序：`D:\mongodb\bin\mongod.exe`
  - 数据目录：`D:\mongodb-data`
  - 启动：`"D:\mongodb\bin\mongod.exe" --dbpath "D:\mongodb-data" --port 27017 --bind_ip 127.0.0.1`

### 前端（frontend/）
- `npm install` + `npm run build`（vite build）通过。
- 新增/改造：
  - `stores/user.js`（pinia：token + userInfo 持久化）
  - `api/user.js`、`api/chat.js`
  - `utils/request.js`：baseURL=`http://localhost:8081`（绝对地址），自动注入 `Authorization: Bearer`，`/chat` 接口 60s 超时，401 自动 logout 并跳 `/login`。
  - `views/Login.vue`、`Register.vue`、`Chat.vue`（Chat 从旧前端整页拷贝）
  - `router/index.js`：新增 `/login`、`/register`、`/chat`（requiresAuth 守卫，带 redirect 回跳）
  - `views/Home.vue`：导航栏加 AI 客服入口、用户下拉退出、管理员登录改走真实 `/api/user/login` 并校验 role=ADMIN 后存 token。
  - `views/InterviewCodes.vue`：原为空文件导致 build 失败，改为占位页。
  - `api/interviewQuestion.js`：补 `getRelatedQuestions` / `toggleFavorite` / `submitEvaluation`。
  - `views/InterviewResult.vue`：图标 `Lightbulb` 不存在，改 `Aim`。
  - `views/Chat.vue`：消息气泡改 `v-html` + `renderContent`（escapeHtml 防 XSS + `/exam/start/(\d+)` 转可点击链接 + `\n` 转 `<br/>`）。
  - `views/MyPapers.vue` 新建：我的AI试卷列表（卡片 + 开始考试/看详情/空态/loading），路由 `/my-papers`（requiresAuth），Home 导航栏加「我的AI试卷」按钮，`api/paper.js` 补 `getMyAiPapers()`。
  - `views/Analysis.vue` 新建：AI学习分析页（概览卡 + echarts 折线/柱状/雷达 + AI建议按钮），路由 `/analysis`（requiresAuth），Home/Analysis 入口已打通。
  - `views/Exam.vue` 修复：头顶进度条原绑**时间进度**（`progressPercentage`，与答题无关，答完也到不了 100%），改为绑 `answerProgress`（已作答/总数，多选按数组长度），并显示「已作答 X/Y 题」提示（`answer-count-tip`）。
  - `views/ExamResult.vue` 修复：「下载成绩单」按钮前缀乱码（破损 emoji `��`）改为 `📊`；`gradeExam` 简答题改 `CompletableFuture` 并行（`AI_GRADING_EXECUTOR` 4 线程）。
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

## 5.1 UI 修复（2026-09-10）

- **「我的 AI 试卷」详情死链已修复**：`MyPapers.viewDetail` 原本和 `startExam` 都跳 `/exam/start/{id}`。新增 `frontend/src/views/PaperDetail.vue`（路由 `/paper/detail/:id`，requiresAuth）展示卷面题目+选项（正确答案绿色高亮）+参考答案+解析，复用 `GET /api/papers/{id}`（`customPaperDetailById` 已带 questions.choices/answer，无需后端改动）。
- **雷达图退化为直线已修复**：原因—答题只落在 1~2 个知识点分类，echarts 雷达 <3 轴就是一条线段。`LearningAnalysisServiceImpl` 雷达改按**题型**聚合（选择题/判断题/简答题 + 整体作答），保证 ≥4 轴成形状（`buildTypeRadar`），知识点掌握度仍由柱状图承载。已验证 admin：choice94/judge100/text0/overall70。
- **分析页分类/卷名带 `*` 已修复**：根因—`AiGeneratedPaperServiceImpl.getOrCreateCategory` 把未清洗的 topic（含 `**` 与「套」前缀）直接存为分类名，存量 `C15=套 **JVM**`。修复：抽 `cleanTopic()`（去 markdown 符+压缩空白）供 `getOrCreateCategory`/`buildPaperName` 共用（后端防御）；`LearningAnalysisServiceImpl.normalizeName` 对展示名兜底清洗；DB 一次性 UPDATE 存量：C12`套Java集合框架测试`→`Java集合框架`、C13→`Java并发`、C14→`Java高并发`、C15→`JVM`，paper2/3 名 REPLACE 同步。
- **注意**：PowerShell 控制台把中文显示成 `?` 是 GBK 渲染假象，DB/接口数据是正常 UTF-8，别被误导（用 HEX/python 验证）。
- **后端 v18 起密钥获取方式（2026-10-12 更新）**：`ALIPAY_PRIVATE_KEY` 已持久化到**用户环境变量**（`[Environment]::SetEnvironmentVariable('ALIPAY_PRIVATE_KEY', <值>, 'User')`；注意 `setx` 有 1024 字符截断限制，1624 字符的密钥会把 `setx` 截断，必须用 .NET API）。生效密钥对来源：`C:\Users\34147\Documents\支付宝开放平台密钥工具\密钥20260910100331\应用私钥RSA2048-敏感数据，请妥善保管.txt`（沙箱网关联调验签通过）。旧 `launch_v18.ps1` 已被系统清理，不再是密钥载体。`DASHSCOPE_API_KEY` 同样在用户环境变量中；`PINECONE_API_KEY` 仍未设置（AI 客服 RAG 检索会失效）。

### 5.2 业务完整性修复 + 单元测试（2026-09-11，27 测试全绿）

**修复清单**（背景：课设演示标准，不考虑安全/并发，聚焦业务闭环）：
- **VIP 档位权益**：`InviteCodeServiceImpl.bonusForType` / `PayOrderServiceImpl.bonusForType`（enterprise=300 / vip=200 / normal=100），`Pay.vue` VIP 文案改"奖励 200 积分"。
- **管理员编辑 DRAFT 卷被拒（演示截图 bug）**：`UserContextUtil.isAdmin()`（读 JWT 过滤器加载的 ROLE_ADMIN authorities）；`PaperServiceImpl.customPaperDetailByIdWithAuth` 管理员放行。
- **分类下拉 No data（演示截图 bug）**：`PaperCreate.vue`/`QuestionManage.vue` 原按"选择题/判断题/简答题"父分类名查子分类，而 DB 是 Java/Spring 知识分类树 → 改为分类树扁平化（`flattenCategories`，子级缩进），分类与题型解耦。
- **手动组卷写 user_paper**：`customCreatePaper(paperVo, userId)` 新增归属关联（`UserPaperServiceImpl.RELATION_MANUAL`），手动 DRAFT 卷创建者可考/可看；`PaperController` 传 `getUserId()`。
- **AI 面试会话 Redis 持久化**：`AiInterviewSession`(vo) + `AiInterviewServiceImpl` 存 `aiinterview:session:{id}`（TTL 2h），重启不再丢会话/白扣积分。
- **新计费（CreditBillingService 统一）**：模拟面试 start 扣 10 分（不足 4002）；聊天生成试卷扣 20 分（`AiGeneratedPaperServiceImpl.generateAndSave` 事务内扣费，LLM 失败回滚自动返还；`CreditNotEnoughException` → 客服友好话术）。
- **考试时长生效**：`ExamTimePolicy.isOvertime`（时长+5 分钟宽限）；`customSubmitAnswer` 超时拒交卷；`startExam` 对超时遗留"进行中"记录按 0 分自动归档（防卡死）。
- **模拟面试同题去重**：submit 前查 (interviewId,questionId) 已答即拒（省 LLM 调用）；`distinctByQuestionKeepMax` 汇总/详情去重保最高分。
- **成绩单题序**：`ExamServiceImpl.sortAnswerRecordsByPaper` 按卷面题序排答题记录（原比较器算了 x/y 却按 questionId 排且 Long/Integer indexOf 恒 -1）。
- **考试记录状态映射**：`ExamRecordController` 1→"判卷中"（原"已完成"永不出现）；`ScoreManage.vue` 筛选/标签/批阅按钮同步（按钮改"重新批阅"仅判卷中显示）。
- **贡献采纳奖励**：`UserContributionServiceImpl.grantContributionReward`（仅 0→1 首次采纳 +20 分 + 流水 type=contribution）。
- **聊天出题意图收紧**：`isGeneratePaperRequest` 需含"一套/生成试卷/出一套"强意图词且含"试卷/题"；"帮我生成这道题的解析"不再误触发整卷生成。
- **空卷拒绝**：`customCreatePaper`/`customUpdatePaper` 空题目抛异常（update 原会 NPE）；`customRemoveId` 删卷清理 user_paper 悬空关联。

**单元测试**（`backend/src/test/java/com/atguigu/exam/`，`mvn test` 27/27 绿）：ExamTimePolicyTest(5)、ChatIntentTest(3)、ExamRecordSortTest(2)、CreditTierTest(2)、AiInterviewSessionCodecTest(2)、MockInterviewDedupTest(3)、CreditBillingServiceImplTest(5)、MockInterviewStartBillingTest(2)、UserContributionRewardTest(3)。UI 级浏览器验证未做。

### 5.3 前端导航与视觉改造（2026-09-12，均已浏览器实测）

**导航闭环（commit fb58b34）**：
- 企业真题详情"相关推荐"跳转修复：`InterviewQuestionDetail.vue` 加 `watch(route.params.id)` 重载（组件复用不重新 mounted，原点击只变 URL 不变内容）。
- 全局悬浮"返回首页"组件 `components/BackHome.vue`（main.js 全局注册，模板直接 `<BackHome />`）：挂载于企业真题列表/练习、模拟面试系列、AI 面试官、我的AI试卷、AI学习分析、AI客服、购买页、企业详情、考试排行榜、考试结果页（共 13 页）；真题/试卷详情页用页头"首页"按钮、答题页用考生信息条内"首页"链接（避免考试中误触悬浮球）。
- `router` 加 `scrollBehavior`：跳转回顶部，前进后退保留位置。

**视觉改造（蓝白浅色主题，去 AI 化）**：
- 全部换浅色对标全局（#f5f7fa + Element 蓝 #409eff 系），衬线标题（Noto Serif SC 系统栈，无外部字体依赖）。
- **考试流程三页统一"蓝白考卷"语言**：`ExamStart.vue`（开始考试，原紫渐变）/`Exam.vue`（答题，原绿色）——浅蓝格纹桌面、白色考卷纸、蓝色双线卷头、题号方块章、左侧竖排"密封线"装饰、考钟胶囊（剩 5 分钟红底脉动）、选项加大至 15px 并左对齐；ExamStart/Exam.vue 考生信息条带"首页"链接。
- **ExamRanking.vue**（原深藏青金）：改浅色白卡，保留金银铜 CSS 奖牌徽章、榜首浅金横幅（NO.1 徽章 + 流光动画）、衬线大分数 + 得分率细条、蓝色统计卡；清除全部 emoji（含 script 内 statsTitle computed 里的）。
- **ExamResult.vue**（原紫渐变 #667eea→#764ba2 六处）：全部替换 Element 蓝渐变，页面底浅蓝灰，loading 文字改深色。
- 验证：vite build 通过；浏览器实测四页截图（开始考试→答题→排行榜→结果页）确认无深色/emoji 残留。

### 5.4 企业财务报表模块（2026-10-11，已浏览器实测）

- **能力**：后台管理新增「财务管理 → 企业财务报表」（`/admin/finance`）：年度概览卡（收入/支出/净利润/利润率）、月度收支趋势图（柱+线）、分类构成饼图（收入/支出可切换）、收支明细（类型/分类/日期范围/关键词筛选 + 分页）、月度报表（12 月 + 全年合计）、年度报表（按年聚合 + 合计）、Excel 导出（按当前筛选，含合计行）。
- **数据源**：新建台账表 `finance_record`（`scripts/init.sql` 末尾已追加 DDL + 215 条演示种子，2025-11~2026-10，含 6 个亏损月；流水号 IN/EX+日期+序号）。**未联动支付订单**（如需真实入账，可在 `PayOrderServiceImpl.settle` 成功后写入一条台账收入）。
- **后端**：`FinanceRecord` / `FinanceRecordMapper` / `FinanceReportVo`（概览/月度/年度/分类嵌套 VO，仿 LearningReportVo 模式）/ `FinanceQueryVo` / `FinanceService(Impl)` / `FinanceController`（`/api/admin/finance/overview|monthly|yearly|category-stats|categories|records|export`）。聚合走「条件查询 + 代码层聚合」，导出用 POI XSSFWorkbook（`#,##0.00` 金额格式 + 底部合计区）。
- **前端**：`views/FinanceReport.vue`（Element 浅色风格 + echarts）、`api/finance.js`、路由 `/admin/finance`、`AdminLayout.vue` 新增「财务管理」菜单分组；同时把该文件内 `el-submenu` 统一改为官方组件名 `el-sub-menu`（消除控制台 Vue warn）。
- **验证**：21 项接口/DB 对账/导出断言全过（2026 年度：收入 ¥334,213.11 / 支出 ¥206,031.45 / 净利润 ¥128,181.66 / 利润率 38%）；浏览器实测统计卡、双图表、三 Tab、筛选（收入筛选 215→145 条）、菜单跳转均正常，控制台无报错。
- **环境备注（2026-10-11）**：启动方式——MySQL 常驻；Redis `D:\Redis\Redis-x64-5.0.14.1\redis-server.exe`；MongoDB `D:\mongodb\bin\mongod.exe --dbpath D:\mongodb-data --port 27017 --bind_ip 127.0.0.1`；后端 `mvn spring-boot:run`（`DASHSCOPE_API_KEY` 已存于用户环境变量）；前端 `npm run dev`。`ALIPAY_PRIVATE_KEY` 环境变量已丢失（旧 launch 脚本被清理），支付类接口需要时需重新注入。注意 8080 可能被其他项目占用；**后端端口自 2026-10-11 起为 8081**（8080 让给其他项目，8081 原被 smart-elderly-companion 占用需先停）。改端口需同步三处：`backend/src/main/resources/application.yml`、`frontend/src/utils/request.js`、`frontend/vite.config.js`（页面内若直写后端地址请改用 `request.defaults.baseURL`）。

### 5.5 管理端三项 UI 修复（2026-10-11，已浏览器实测）

- **视频分类禁用后从列表消失（已修复）**：根因——`VideoCategoryServiceImpl.getCategoryTree()` 无条件过滤 `status=1`，而管理端「视频分类管理」表格用的正是该树接口；禁用后该分类连同其子分类（子级变孤儿被 `buildTree` 丢弃）整棵子树消失，无法再启用。修复：树接口新增 `includeDisabled` 参数（默认 false，用户侧行为不变），管理端列表与两个分类级联选择器（视频管理上传、用户投稿表单）传 `true`；级联中禁用分类以「radio 灰显、不可选」样式展示（`markDisabledOptions` 递归标记），不再隐藏。验证：禁用中的 Java + 其启用子级 Java基础 均正常展示，Java 不可选、选择链路完好（键盘 Enter 可选中）；用户侧树仍只返回启用分类。
- **操作列多按钮被挤到第二行（已修复）**：实测 4 个小按钮需 ~316px（含「添加子分类」4 字按钮需 ~352px），而列宽仅 300px。修复：按实测重排操作列宽（公告 300→340，视频分类/企业题集分类/题目分类 300→360，成绩 240→320，待审核 240→300，轮播图/企业/面试真题 240→260，视频管理 360→400）并加 `:cell-style="{ whiteSpace: 'nowrap' }"` 兜底（窄屏改为表格横向滚动，而非按钮换行）。浏览器验证：公告/视频分类/视频管理/成绩/题目分类 各行按钮 offsetTop 一致（同一行）。
- **管理端无法返回前台首页（已修复）**：`AdminLayout.vue` 侧边栏底部新增「返回前台首页」按钮（HomeFilled 图标），管理端全部页面（含欢迎页）一键回 `/home`；侧边栏改 flex 布局使入口吸底。浏览器验证：点击后正确跳转 `/home`。
- **管理员会话复用（已修复）**：此前从管理端返回首页后，再点「管理员后台」会无条件弹出登录框（`showAdminLogin` 不识别已有会话），造成"每次都要重新登录"的错觉——token 实际一直保存在 localStorage 中，并未丢失。修复：`Home.vue` 增加 `isAdmin` computed（token + role=ADMIN），命中时按钮变为「返回管理后台」并直接 `router.push('/admin')`；未登录/非管理员仍弹登录框。浏览器已验证完整流程（登录→管理端→返回前台→直接再进后台无登录框→退出登录后恢复为需登录）。
- **留待人工确认（非本次改动引入）**：级联选择器中用鼠标点击 radio 圆点在自动化测试里未生效（键盘 Enter 可正常选中）。代码比对：启用分类在改动前后传给 Element Plus 的 `disabled` 均为 falsy（改前 undefined、改后 false），EP 内部状态完全等价，判定与本次修改无关；建议人工浏览器点选确认，如确有问题再单独排查。

### 5.6 登录/注册页改版（极简单列，2026-10-11，已浏览器实测）

- **选型**：从 `PLAN-登录页风格选型.md` 的 6 套方案中选定**方案 3「极简无干扰 · 单列聚焦」**（可视化预览：`frontend/public/login-styles-preview.html`）。按项目设计系统适配：单一强调色用 Element 蓝 `#409eff`（与全站一致），输入框保留可见边框（`#d1d5db`），零装饰。
- **改动**：`Login.vue` / `Register.vue` 重写为「白底 + 单列 360px + 顶部 logo/站名 + 可见 label」；删除深色玻璃拟态（`#1a1a2e` 渐变 / 毛玻璃 / `#667eea→#764ba2` 紫渐变按钮）与 🎓 emoji；按钮改原生 `<button>`（44px/圆角 8）；底部保留注册/返回首页入口并新增「按 Enter 快速提交」提示。
- **可访问性（一并修复）**：原生 `<label>` 包裹 `el-input`（点击标签即聚焦，已实测）；`autocomplete`=username / current-password / new-password；行内错误 `role="alert" aria-live="polite"`（空提交「请输入用户名」、错误密码「用户名或密码错误」、注册「两次输入的密码不一致」均已实测）；改用原生 `<form>` + `type=submit`，**注册页此前不支持回车提交的问题一并修复**；`focus-visible` 2px 焦点环 + 输入框聚焦外发光。
- **验证**：`npm run build` 通过；两页浏览器实测全部断言通过，控制台 0 error。
- **背景动画（2026-10-11）**：`frontend/src/components/AuthBackground.vue` —— **只保留「漂浮几何」一种方案**（＋ ○ □ 共 14 个小元素；符号取自考试页的题号方块 / 答题卡）。节奏为「呼吸」：`opacity 0 → 0.72 → 0`（淡出消失→淡入出现）+ 上下浮动 `translateY -16px→+16px` 与轻微旋转/缩放，半周期 5.5–9s（一次完整呼吸约 11–18s），各元素负 `animation-delay` 错峰；位置贴左右两侧、避开中央内容列；≤768px 仅保留最外侧 6 个并把整层不透明度降到 0.75（`is-mobile-hidden`）。预览见 `frontend/public/login-styles-preview.html` 的「背景动画」区块。（早期实现过的「流动光晕 aurora」与「点阵视差 grid」已按用户要求删除，代码内无残留。）
  性能与降级：只动画 `transform`/`opacity`（合成层）+ `contain: strict` + `pointer-events: none` + `aria-hidden`；`prefers-reduced-motion` 或 CPU ≤ 2 核 / 内存 ≤ 2GB → 全部静止并兜底显示为 0.45 不透明度（避免 `opacity:0` 导致图形彻底不可见）；标签页切后台 → `animation-play-state: paused`。实测（Chromium）：**60 FPS、最长帧 17.6ms、掉帧 0、控制台 0 error**，全周期采样确认透明度在 0↔0.72 之间呼吸；`prefers-reduced-motion` 降级为代码级验证（自动化工具无法模拟系统偏好，可用系统「关闭动画效果」人工复核）。

### 5.7 登录鉴权加固（三层，2026-10-11，已接口级 + 浏览器实测）

- **原漏洞**：后端 `SecurityConfig` 为 `.anyRequest().permitAll()`，考试/刷题/管理端接口匿名可调用；且 `UserContextUtil.getUserId()` 无 token 时**回退默认用户 1**，导致匿名请求能以 user 1 身份开考、扣积分、读写学习路径。
- **三层边界**：
  1. **路由层**（`frontend/src/router/index.js`）：`/exam/list|start/:paperId|:id`、`/exam-result*`、`/exam-ranking`、`/practice`、`/interview-practice/:id`、`/mock-interview/:id`、`/interview-result/:id` 加 `requiresAuth`；`/admin` 加 `requiresAuth + requiresAdmin`（非 ADMIN 跳回 `/home`）；未登录统一跳 `/login?redirect=<原地址>`。
  2. **API 层**（`frontend/src/utils/request.js`）：新增需鉴权前缀清单（exams / exam-records / learning-path / analysis / chat / pay / mock-interview / ai-interview），无 token 时**不发请求**直接跳登录页；401 处理统一带 `redirect` 回跳。
  3. **后端业务层**：`SecurityConfig` 显式收紧（`/api/exams/**`、`/api/exam-records/**`、`/api/learning-path/**`、`/api/mock-interview/**`、`/api/ai-interview/**` 需登录；`/api/papers|questions` 的 POST/PUT/DELETE 需登录）；新增 `UserContextUtil.requireUserId()`（未登录抛 401）并在 `ExamServiceImpl.startExam` 使用；`ExamServiceImpl.assertRecordAccessible()`（考试记录仅本人或管理员可读/删）；`ExamRecordController./list` 非管理员只能查自己的记录。
- **刻意保留的公开边界**：题目浏览（`/api/questions/list|popular|category|difficulty|{id}`）、企业真题浏览（`/api/interview-questions/*` 读接口）、轮播/公告/视频/统计读接口；对应前端 `/home`、`/interview-questions`、`/videos` 等浏览页不限登录。
- **实测**：匿名 `POST /api/exams/start`、`GET /api/exam-records/list|ranking`、`GET /api/learning-path/active` → 全 401；公开接口全 200；登录后 `exam-records/list`、`exams/records` → 200。浏览器路由实测 7 步全过（含 student 登录访问 `/admin/welcome` 被弹回 `/home`；非管理员不再显示「管理员后台」按钮）。
- **已知残留（未处理，非本次引入）**：`Welcome.vue` 企业 logo 引用外链 `picsum.photos`（网络不通时控制台 ERR_ABORTED）；`Welcome.vue` 一条 ElIcon reactive 警告。

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
cd backend; mvn compile -q; mvn spring-boot:run        # 8081
# 前端启动/构建
cd frontend; npm run dev                                # 3001
npm run build
# MySQL 初始化（PowerShell 不能用 < 重定向）
mysql -uroot -proot -e "source C:/Users/.../scripts/init.sql"
# MongoDB 启动（若未运行；程序与数据均在 D 盘）
"D:\mongodb\bin\mongod.exe" --dbpath "D:\mongodb-data" --port 27017 --bind_ip 127.0.0.1
```

## 8. 技术栈速览

- 后端：Spring Boot 3.2.6、MyBatis-Plus 3.5.11、Spring Security + JWT、LangChain4j 1.0.0-beta3（community-dashscope / open-ai / pinecone / easy-rag / pdfbox）、MongoDB、MySQL 8、minio、Swagger。
- 前端：Vue3（组合式 API + `<script setup>`）、Vite 4、vue-router 4、pinia、Element Plus、axios、echarts、sass。
- 环境：JDK 17.0.9、Maven 3.6.3（仓库 `D:\repository`）、Node v24.14.0、MySQL 8.4.5（`D:\mysql-8.4.5-winx64`）。