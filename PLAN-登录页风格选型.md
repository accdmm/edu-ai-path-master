# 登录页风格参考方案（5 套通用 + 1 套本项目专属）

> 用途：登录页改版选型与落地参考。每套方案包含 —— **核心风格定位 / 配色方案 / 布局结构 / 组件设计细节 / 主题标签 / 适用场景 / 可落地实现要点**。
> 覆盖场景：B端企业管理系统、C端消费类产品、工具类应用、内容社区类平台、金融类产品 + 教育科技（本项目）。
> 共同约束：符合 WCAG 2.1 AA 可访问性基线、键盘可达、多端响应式、无重型素材依赖（纯 CSS 装饰优先）。

---

## 0. TL;DR —— 先给结论

**本项目（智能学习平台）推荐：方案 6「教育科技 · 轻学术（蓝白卷面）」**

理由（调研全站样式后的现状）：
- 项目正在「去 AI 化 / 蓝白化」迁移中：`Analysis.vue` / `LearningPath.vue` / 考试三页（「蓝白考卷」语言）等已完成，落地 `#f5f7fa` + `#409eff` + 衬线标题 + 去 emoji；
- **仍待迁移**：`Login.vue` / `Register.vue`（深色玻璃拟态 + 🎓 emoji：`#1a1a2e→#16213e` + 毛玻璃卡 + `#667eea→#764ba2` 紫渐变按钮）、`Chat.vue`（全站最深色 `#1a1a2e→#16162a` + 紫）、`Home.vue`（`#a8edea→#fed6e3` 粉青渐变底 + 紫/橙渐变）、`VideoList.vue`（紫）；
- 登录页是**用户第一入口**中断层最明显的一环 —— 改造成本低、收益最高（Chat / Home / VideoList 可按同一语言后续跟进）；
- 教育/考试主题天然适配"秩序、清爽、可信"的浅色学术调性，且可直接**复用考试页既有视觉语言**（浅蓝格纹、蓝色双线卷头、衬线标题），改造成本低、答辩演示一致性强。

| # | 方案 | 适配项目类型 | 关键词 | 一句话定位 |
|---|---|---|---|---|
| 1 | 栅格秩序 · 蓝灰专业 | B端企业管理系统 | 秩序 / 效率 / 可信 | 左品牌右白卡，克制专业 |
| 2 | 氛围渐变 · 情感化居中卡 | C端消费类产品 | 情绪 / 增长 / 一键登录 | 柔和渐变背景 + 居中大圆角卡 |
| 3 | 极简无干扰 · 单列聚焦 | 工具类应用 | 极简 / 速度 / 开发者 | 零装饰单列，效率优先 |
| 4 | 内容为先 · 双栏叙事 | 内容社区类平台 | UGC / 氛围 / 入驻 | 左侧内容橱窗 + 右侧登录卡 |
| 5 | 稳重可信 · 深蓝高对比 | 金融类产品 | 安全 / 合规 / 高信任 | 深蓝主色 + 安全背书条 |
| 6 | **蓝白卷面 · 轻学术（推荐）** | **教育 / 考试类（本项目）** | 教育 / 学术 / 蓝白 / 去 emoji | 蓝白考卷语言 + 品牌分栏 |

---

## 1. 选型判断维度（先量后选，9 个维度）

| 维度 | 关键自问 | 倾向性判断 |
|---|---|---|
| 目标用户群体 | 内部员工 / 大众消费者 / 开发者 / 学生？ | B端偏秩序，C端偏情绪化 |
| 品牌视觉资产 | 有无 VI、主色、logo 形态约束？ | 有 VI → 严格沿用主色，装饰让位 |
| 产品整体调性 | 严肃 ↔ 活泼；效率 ↔ 情感 | 与登录页的装饰度成正比 |
| 任务属性 | 高频重复登录 / 低频首次进入？ | 高频 → 效率极简；首次 → 引导与情感 |
| 合规与信任要求 | 金融 / 医疗 / 政务？ | 需安全提示与稳重配色 |
| 多端与响应式 | 移动优先？桌面为主？ | 决定布局断点与触控尺寸 |
| 性能预算 | 是否允许大图 / 视频背景？ | 建议：纯 CSS 装饰优先 |
| 可访问性等级 | AA / AAA？ | 本项目按 AA（见 §8） |
| 维护成本 | 是否与现有组件库一致？ | 与现有设计系统一致优先 |

---

## 2. 方案 1：栅格秩序 · 蓝灰专业（B端企业管理系统）

**① 核心风格定位**：克制、秩序、可信。以"信息清晰 + 操作明确"为第一目标，装饰让位于效率；为"每天都要用"的工作台而生。

**② 配色方案**

| 角色 | 色值 | 说明 |
|---|---|---|
| 主色 | `#2F6BFF`（或沿用 `#409EFF`） | 按钮 / 链接 / 焦点环 |
| 页面底 | `#F2F4F7` | 浅灰蓝，低干扰 |
| 卡片 | `#FFFFFF` | 圆角 8，1px `#E5E6EB` |
| 主文本 | `#1D2129` | 对比度 ≥ 4.5:1 |
| 次文本 | `#4E5969` | |
| 占位符 | `#86909C` | 仅作提示，不承载信息 |
| 成功 / 警告 / 危险 | `#00B42A` / `#FF7D00` / `#F53F3F` | |

**③ 布局结构**：桌面左右 5:7 —— 左侧深灰蓝品牌区（logo、产品名、3 条价值点、版本号/客服/备案号），右侧纯白卡 400px 垂直居中；移动端单列，品牌区压缩为顶部一句话。

**④ 组件设计细节**
- 输入框：高 40、圆角 6、边框 `#DCDFE6`；聚焦 2px 主色环 + 外发光 `rgba(47,107,255,.18)`
- 按钮：主色实心、高 40、全宽；次要操作（忘记密码 / SSO）用文字链
- 错误：行内红字 + 图标（不单靠颜色），容器 `role="alert"`
- 附加：记住我、忘记密码、企业 SSO（钉钉 / 企微）入口行

**⑤ 主题标签**：`#B端` `#后台管理` `#SaaS` `#数据密集` `#可信` `#克制`

**⑥ 适用场景**：管理后台、CRM/ERP、内部工作台、B端 SaaS；**不适用**：面向大众的消费类产品（会显得冷）。

**⑦ 实现要点**
```html
<div class="login-page login--b2b">
  <section class="brand"><!-- logo / 平台名 / 价值点 / 版权 --></section>
  <section class="panel"><form class="card"><!-- 表单 --></form></section>
</div>
```
```css
.login--b2b { --brand:#2F6BFF; --bg:#F2F4F7;
  display:grid; grid-template-columns:5fr 7fr; min-height:100dvh; background:var(--bg); }
.login--b2b .card { width:400px; padding:40px; background:#fff;
  border:1px solid #E5E6EB; border-radius:8px; }
```
Element Plus 覆盖见 §9。

---

## 3. 方案 2：氛围渐变 · 情感化居中卡（C端消费类产品）

**① 核心风格定位**：轻松、有情绪、强 CTA。降低首次进入的心理门槛，让"登录/注册"成为自然动作而非负担。

**② 配色方案**

| 角色 | 色值 | 说明 |
|---|---|---|
| 品牌渐变 | `#FF7A59 → #FF3D71`（或 `#7C3AED → #06B6D4`） | 背景氛围 / 主按钮 |
| 卡片 | `#FFFFFF`（85% 不透明 + 背景模糊） | 圆角 20 |
| 主文本 | `#111827` | |
| 次文本 | `#6B7280` | |
| 强调色 | 与品牌渐变一致 | 链接 / 高亮 |
| 装饰 | 2~3 个大尺度柔光圆斑（`radial-gradient` + blur） | 纯 CSS，无图片 |

**③ 布局结构**：全屏氛围背景（柔和 mesh 渐变 + 装饰形状）；居中卡片 380–420px；移动端表单贴底、圆角只保留顶部、按钮全宽。

**④ 组件设计细节**
- 输入框：高 48、圆角 12、浅灰底 `#F3F4F6`（无边框，聚焦时描边主色）
- 按钮：高 48、圆角 24（胶囊）、渐变实心；下方第三方登录圆形图标行（微信 / Apple / Google）
- 手机号 + 验证码双栏输入；协议勾选（必须，未勾选按钮禁用）
- 注册引导强："3 秒创建账号"

**⑤ 主题标签**：`#C端` `#消费` `#增长` `#一键登录` `#情感化` `#移动优先`

**⑥ 适用场景**：电商、出行、生活服务、内容消费 App / H5；**不适用**：强合规与内部系统。

**⑦ 实现要点**
```css
.login--c { background:
  radial-gradient(1200px 600px at 15% 10%, rgba(255,122,89,.35), transparent 60%),
  radial-gradient(900px 500px at 85% 85%, rgba(124,58,237,.30), transparent 60%),
  #FFF7F5; }
.login--c .card { width:400px; border-radius:20px; padding:36px;
  background:rgba(255,255,255,.86); backdrop-filter:blur(12px); }
.login--c .btn { height:48px; border-radius:24px;
  background:linear-gradient(135deg,#FF7A59,#FF3D71); }
```

---

## 4. 方案 3：极简无干扰 · 单列聚焦（工具类应用）

**① 核心风格定位**：零装饰、速度优先、开发者友好。"打开→输入→进入"三步之间不出现任何干扰元素。

**② 配色方案**

| 角色 | 色值 | 说明 |
|---|---|---|
| 主色 | `#111827`（近黑）或单色 `#2563EB` | 按钮 / 链接 |
| 页面底 | `#FFFFFF`（可选暗色 `#0B0F19` 变体） | 纯白 / 纯暗 |
| 文本 | `#111827` / 次 `#6B7280` | |
| 边框 | `#D1D5DB` | 可见边框优先于阴影 |
| 焦点 | `#2563EB` 2px 环 | 强可见 |

**③ 布局结构**：单列 360px 居中，无背景装饰；顶部小 logo（24–28px）；表单下方一行快捷键提示（如 `Enter 提交 · Esc 清空`）。

**④ 组件设计细节**
- 输入框：高 36–40、圆角 6–8、**可见边框**（不用浅底填充）
- 按钮：全宽实心；次要操作以纯文字链接同行排布（忘记密码 · 注册）
- OAuth 行：GitHub / Google 图标按钮，图标 + 文案
- 错误提示紧凑（单行，位于字段下方 4px）

**⑤ 主题标签**：`#工具` `#效率` `#极简` `#开发者` `#低装饰` `#暗色可选`

**⑥ 适用场景**：SaaS 工具、代码/效率工具、API 控制台、开发者平台；**不适用**：需要情感化引导的消费场景。

**⑦ 实现要点**
```css
.login--tool { display:grid; place-items:center; min-height:100dvh; background:#fff; }
.login--tool .card { width:360px; padding:24px; } /* 无卡片边框/阴影，仅留白 */
.login--tool :deep(.el-input__wrapper) { box-shadow:0 0 0 1px #D1D5DB inset; height:40px; }
```

---

## 5. 方案 4：内容为先 · 双栏叙事（内容社区类平台）

**① 核心风格定位**：让内容说话。登录前先传达"社区正在发生什么"，用氛围与数据降低注册犹豫。

**② 配色方案**

| 角色 | 色值 | 说明 |
|---|---|---|
| 页面底 | `#FFFDF8`（暖白） | 比纯白更亲和 |
| 主色 | `#FF6154` 或品牌色 | CTA / 标签 |
| 卡片 | `#FFFFFF` | 圆角 12 |
| 文本 | `#1F2937` / 次 `#6B7280` | |
| 辅助标签 | 多色但低饱和（`#FEF3C7` / `#DBEAFE` / `#DCFCE7`） | 内容分类氛围 |

**③ 布局结构**：桌面左侧 = 内容橱窗（今日热帖 2–3 张卡 / 精选创作者 / 活跃数据），右侧 = 登录卡 400px；移动端仅保留登录卡 + 顶部一句社区介绍。

**④ 组件设计细节**
- 登录卡内嵌一行社区数据（如「今日 1.2k 新帖 · 8.6k 在线」），增强入驻动机
- 输入框高 44、圆角 10；按钮圆角 10 实心
- 注册引导突出："3 秒加入，开始创作"
- 支持邮箱 / 手机双方式（Tab 或切换链接）

**⑤ 主题标签**：`#社区` `#UGC` `#内容` `#氛围` `#增长` `#暖色`

**⑥ 适用场景**：论坛、知识社区、博客/短视频平台、创作社区；**不适用**：B端后台与金融类。

**⑦ 实现要点**
```css
.login--community { display:grid; grid-template-columns:1.2fr 1fr; min-height:100dvh; background:#FFFDF8; }
.login--community .feed { padding:48px; display:flex; flex-direction:column; gap:16px; }
.login--community .card { width:400px; border-radius:12px; padding:32px; background:#fff; }
```

---

## 6. 方案 5：稳重可信 · 深蓝高对比（金融类产品）

**① 核心风格定位**：安全、稳妥、精确。所有视觉决策服务于"信任"——高对比、强背书、零花哨。

**② 配色方案**

| 角色 | 色值 | 说明 |
|---|---|---|
| 主色 | `#1E3A8A`（深蓝） | 按钮 / 链接 |
| 页面底 | `#FFFFFF` 或深色 `#0B1F3A` | 二选一，整站统一 |
| 文本 | `#111827`（对比 ≥ 7:1 优先） | 正文加粗关键信息 |
| 数字 | 等宽字体（`ui-monospace`） | 金额 / 卡号 / 验证码 |
| 成功 / 危险 | `#047857` / `#B91C1C` | 均为高对比深色 |
| 安全提示条 | `#F8FAFC` + `#334155` 文本 | 低饱和但可读 |

**③ 布局结构**：居中卡 420px（或左品牌右表单）；卡片下方固定安全提示条：「SSL 加密传输 · 资金由 XX 存管 · 已通过 XX 认证」。

**④ 组件设计细节**
- 输入框：高 44、圆角 8、边框 `#CBD5E1`；聚焦深蓝 2px 环
- 双因素：密码 + 图形/短信验证码（倒计时按钮）
- 按钮：深蓝实心、高 44
- 风险提示文字 ≥ 12px 且对比 ≥ 4.5:1；登录设备 / 上次登录时间提示
- 禁用色盲唯一线索：错误状态用"图标 + 文案 + 颜色"三重表达

**⑤ 主题标签**：`#金融` `#安全` `#合规` `#高信任` `#稳重` `#高对比`

**⑥ 适用场景**：银行 / 证券 / 保险 / 支付 / 政务；**不适用**：轻娱乐与年轻化产品。

**⑦ 实现要点**
```css
.login--fin { background:#fff; display:grid; place-items:center; min-height:100dvh; }
.login--fin .card { width:420px; padding:40px; border-radius:8px; border:1px solid #E2E8F0; }
.login--fin .secure-bar { margin-top:16px; padding:10px 12px; border-radius:6px;
  background:#F8FAFC; color:#334155; font-size:12px; line-height:1.6; }
```

---

## 7. 方案 6：蓝白卷面 · 轻学术（教育 / 考试类 —— **本项目推荐**）

**① 核心风格定位**：与全站「蓝白考卷」语言同源 —— 学习/考试的**秩序感** + 轻盈现代感。关键词：清爽、可信、学术、去装饰噪音。**去掉 emoji 与深色玻璃拟态**。

**② 配色方案（直接沿用全站既有 token）**

| 角色 | 色值 | 来源 |
|---|---|---|
| 主色 | `#409EFF` | Element 蓝（全站主色） |
| 页面底 | `#F5F7FA` | 全站背景 |
| 卡片 | `#FFFFFF` | 考卷纸白 |
| 主文本 | `#303133` | |
| 次文本 | `#606266` | |
| 占位符 | `#909399`（全站亦用 `#c0c4cc`） | 仅作提示，不承载信息 |
| 边框 | `#EBEEF5`（考试页亦用 `#e4eaf2`） | |
| 成功 / 警告 / 危险 | `#67C23A` / `#E6A23C` / `#F56C6C` | |
| 衬线标题 | `"Noto Serif SC","Source Han Serif SC","Songti SC",STSong,SimSun,serif` | 考试三页（Exam/ExamStart/ExamRanking）既有字体栈，登录页照抄 |
| 考卷蓝（装饰可选） | `#2f6bdf` | 考试三页的「考卷蓝」，用于格纹/卷头更贴合考试语言（通用控件仍用 `#409eff`） |

**③ 布局结构**：桌面左右 5:7 分栏 ——
- **左侧品牌区**：logo + 「智能学习平台」+ 一句话价值（"从刷题到上岸，AI 陪你走完整个学习路径"）+ 3 条能力点（AI 学习路径 / 企业真题 / AI 面试官）；背景用 **浅蓝格纹**（`rgba(64,158,255,.06)` 24px 网格）+ 左侧竖排「密封线」装饰，**直接呼应考试页视觉语言**；
- **右侧登录卡**：白色「考卷纸」卡片 420px，顶部**蓝色双线卷头**（2px + 1px 双线），下方表单。

**④ 组件设计细节**
- 输入框：高 44、圆角 8、`#EBEEF5` 边框；聚焦 2px `#409EFF` 环
- 按钮：高 44、主色实心、圆角 8（**移除紫渐变**）；hover 提亮 8%
- 错误：行内红字 + 图标 + `aria-live="polite"`
- 底部辅助行：`没有账号？立即注册` · `返回首页` · （可选）`管理员入口`
- 标题使用衬线体，正文使用系统无衬线；**全页无 emoji**

**⑤ 主题标签**：`#教育` `#学习平台` `#考试` `#轻学术` `#蓝白` `#去emoji` `#与全站一致`

**⑥ 适用场景**：本项目登录 / 注册页；也适配其他教育、考试、知识付费类产品。**不适用**：金融（需更强信任背书）与纯娱乐 C 端。

**⑦ 实现要点（本项目落地，可直接执行）**

改造 `frontend/src/views/Login.vue`（Register.vue 同款处理）：

```html
<div class="login-page">
  <!-- 左：品牌区（呼应考试页「蓝白考卷」） -->
  <section class="brand">
    <div class="brand-inner">
      <img src="@/assets/logo.svg" alt="" class="brand-logo" />
      <h1 class="brand-title">智能学习平台</h1>
      <p class="brand-slogan">从刷题到上岸，AI 陪你走完整个学习路径</p>
      <ul class="brand-points">
        <li>AI 学习路径 · 按薄弱点规划</li>
        <li>企业真题 · 一线大厂实战</li>
        <li>AI 面试官 · 模拟真实面试</li>
      </ul>
    </div>
  </section>
  <!-- 右：考卷纸登录卡 -->
  <section class="panel">
    <form class="paper">
      <div class="paper-head"><span>用户登录</span><span class="paper-stamp">LOGIN</span></div>
      <label class="field">
        <span class="field-label">用户名</span>
        <el-input v-model="loginForm.username" size="large" autocomplete="username" placeholder="请输入用户名" />
      </label>
      <label class="field">
        <span class="field-label">密码</span>
        <el-input v-model="loginForm.password" type="password" size="large"
                  autocomplete="current-password" show-password placeholder="请输入密码"
                  @keyup.enter="handleLogin" />
      </label>
      <p v-if="errorMsg" class="form-error" role="alert" aria-live="polite">{{ errorMsg }}</p>
      <el-button type="primary" size="large" class="submit" :loading="loading" @click="handleLogin">登 录</el-button>
      <div class="links"><router-link to="/register">没有账号？立即注册</router-link><router-link to="/home">返回首页</router-link></div>
    </form>
  </section>
</div>
```

```scss
.login-page {
  display: grid;
  grid-template-columns: 5fr 7fr;
  min-height: 100dvh;
  background: #F5F7FA;
}
/* 左侧：浅蓝格纹 + 竖排「密封线」 */
.brand {
  position: relative;
  display: grid;
  place-items: center;
  background-color: #F0F6FF;
  background-image:
    linear-gradient(rgba(64, 158, 255, .06) 1px, transparent 1px),
    linear-gradient(90deg, rgba(64, 158, 255, .06) 1px, transparent 1px);
  background-size: 24px 24px;
  border-right: 1px solid #EBEEF5;
}
.brand::after {           /* 密封线装饰（与考试页同语言） */
  content: '密封线内不要答题';
  position: absolute; right: 18px; top: 50%; transform: translateY(-50%);
  writing-mode: vertical-rl; letter-spacing: .4em;
  color: rgba(64, 158, 255, .35); font-size: 12px;
}
.brand-logo { width: 48px; height: 48px; }
.brand-title { font-family: "Noto Serif SC", "Source Han Serif SC", "Songti SC", STSong, SimSun, serif; font-size: 30px; color: #303133; margin: 12px 0 8px; }
.brand-slogan { color: #606266; }
.brand-points li { color: #606266; line-height: 2; }

/* 右侧：考卷纸卡片 + 蓝色双线卷头 */
.panel { display: grid; place-items: center; padding: 24px; }
.paper {
  width: 420px; padding: 36px 32px; background: #fff;
  border: 1px solid #EBEEF5; border-radius: 10px;
  box-shadow: 0 6px 24px rgba(64, 158, 255, .08);
}
.paper-head {
  display: flex; justify-content: space-between; align-items: center;
  padding-bottom: 12px; margin-bottom: 24px;
  border-bottom: 2px solid #409EFF;               /* 双线卷头 */
  box-shadow: 0 3px 0 -1px #409EFF;               /* 第二道细线 */
  font-family: "Noto Serif SC", "Source Han Serif SC", "Songti SC", STSong, SimSun, serif;
  font-size: 20px; color: #303133;
}
.paper-stamp { font-size: 12px; letter-spacing: .2em; color: #409EFF; border: 1px solid #409EFF; padding: 2px 8px; border-radius: 4px; }
.field { display: block; margin-bottom: 18px; }
.field-label { display: block; margin-bottom: 6px; color: #606266; font-size: 13px; }
.form-error { color: #F56C6C; font-size: 13px; margin: 0 0 12px; }
.submit { width: 100%; height: 44px; border-radius: 8px; font-size: 16px; }
.links { display: flex; justify-content: space-between; margin-top: 18px; }
.links a { color: #409EFF; text-decoration: none; font-size: 14px; }

/* 移动端：单列，品牌区压为顶部一行 */
@media (max-width: 768px) {
  .login-page { grid-template-columns: 1fr; }
  .brand { padding: 28px 20px; }
  .brand::after { display: none; }
  .brand-points { display: none; }
  .paper { width: 100%; border: none; box-shadow: none; padding: 24px 20px; }
}
```

配套修改点（4 处）：
1. 删除 `🎓` emoji 与深色/玻璃拟态样式（`backdrop-filter` / `rgba(255,255,255,.05)` / 紫渐变按钮）；
2. 表单加真实 `<label>`（视觉可隐藏）+ `autocomplete` + 错误 `aria-live`（见 §8）；
3. `Register.vue` 用同一套 `.paper` 样式（字段多一行「确认密码」）；
4. `Register.vue` 补回车提交（现状 `Login.vue` 仅密码框支持 `@keyup.enter`，注册页完全不支持）。

---

## 8. 可访问性基线（WCAG 2.1 AA，所有方案共同遵守）

| 项 | 要求 | 落地做法 |
|---|---|---|
| 文本对比度 | 正文 ≥ 4.5:1；≥ 24px 或 ≥ 18.66px 粗体 ≥ 3:1 | 占位符不承载信息；浅色文字仅作装饰 |
| 非文本对比度 | UI 组件边框 / 焦点指示 ≥ 3:1 | 输入框边框 `#DCDFE6` 起，聚焦环主色 |
| 表单标签 | 每个输入有真实 `<label>`（可视觉隐藏） | `placeholder` 只作示例，不作唯一标签 |
| 自动填充 | `autocomplete="username"` / `"current-password"` / `"new-password"` | 提升可用性与密码管理器兼容 |
| 键盘可达 | 全部控件可 Tab 到达；Enter 提交；焦点顺序 = 视觉顺序 | 禁止 div 冒充按钮；`tabindex` 保持自然序 |
| 焦点可见 | `:focus-visible` 2px 主色环 + 2px 偏移 | 不写 `outline:none` 除非有替代样式 |
| 错误提示 | 文字 + 图标（不靠颜色单独表达）；容器 `role="alert"` / `aria-live="polite"` | 错误消息与字段用 `aria-describedby` 关联 |
| 触控目标 | 移动端 ≥ 44×44px，间距 ≥ 8px | 按钮高 44 起；链接加内边距 |
| 动效 | 尊重 `prefers-reduced-motion`；避免 > 3Hz 闪烁 | `@media (prefers-reduced-motion: reduce) { *{animation:none!important;transition:none!important} }` |
| 语义 | `<html lang="zh-CN">`；唯一 `<h1>`；装饰图 `alt=""` | 品牌 logo 装饰用空 alt |
| 不使用 emoji 作功能图标 | 用 SVG 图标（Element Plus Icons） | 本项目全站已去 emoji，登录页同步 |

> **本项目现状实测（待一并修复）**：`Login.vue` / `Register.vue` 占位符 `#666` 对深底 `#1a1a2e` 对比度约 **2:1**（远低于 4.5:1），副标题 `#999` 约 4:1 也偏低；两个页面均无真实 label 与 `autocomplete`，错误提示无 `aria-live`；`Register.vue` 不支持回车提交。

---

## 9. Element Plus 通用改造模板（所有方案复用）

```css
/* 全局 token（按方案替换 --brand / --bg） */
:root {
  --brand: #409EFF;
  --bg: #F5F7FA;
  --el-color-primary: var(--brand);
  --el-border-radius-base: 8px;
}
/* 输入框：统一高度 / 圆角 / 聚焦环 */
.login-page :deep(.el-input__wrapper) {
  height: 44px; border-radius: 8px;
  box-shadow: 0 0 0 1px #EBEEF5 inset;
}
.login-page :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px var(--brand) inset, 0 0 0 3px rgba(64,158,255,.15);
}
/* 按钮：去掉渐变，实心主色 */
.login-page :deep(.el-button--primary) {
  background: var(--brand); border: none; border-radius: 8px;
}
```
> 注意：Element Plus 2.x 的输入框边框由 `box-shadow` 实现（不是 border），覆盖时用 `box-shadow: … inset`，否则会出现双边框。

---

## 10. 响应式与性能（多端适配）

| 断点 | 布局 | 要点 |
|---|---|---|
| ≥ 1200px | 双栏（品牌 + 登录卡） | 品牌区可加装饰，登录卡 400–420px |
| 768–1199px | 双栏收窄 / 品牌区减装饰 | 隐藏次要装饰，保留 logo 与 slogan |
| < 768px | 单列：顶部一句介绍 + 表单贴底 | 按钮全宽、输入框高 ≥ 44、字号 ≥ 16px（防 iOS 聚焦缩放） |

性能约定：
- **零外部素材依赖**：背景装饰全部用 CSS 渐变 / 几何图形，不加载大图与视频；
- 字体：优先系统栈（`Noto Serif SC` 为系统栈候选，不引入 web font 阻塞首屏）；若必须引入，`font-display: swap`；
- 避免 CLS：装饰层固定尺寸、绝对定位；`prefers-reduced-motion` 下关闭动画；
- 首屏增量预算：< 10KB CSS（登录页样式为 scoped，随路由按需加载）。

---

## 11. 本项目落地路径（3 步 + 自测清单）

1. **改 Login.vue**：套用 §7 第 ⑦ 节代码（布局 + 配色 + 去 emoji）；补 `<label>` / `autocomplete` / `aria-live`；
2. **改 Register.vue**：复用同一套 `.paper` 样式，字段增加「确认密码」；注册成功后的提示与跳转保持现状；
3. **验证**：
   - `npm run build` 通过；浏览器实测 1440 / 1024 / 375 三档宽度；
   - 键盘走查：Tab 顺序正确、聚焦环可见、Enter 可提交；
   - 对比度检查（浏览器 DevTools 或 axe）；
   - 与首页 / 考试页并排对比，确认视觉语言一致（无深色、无 emoji）；
   - 登录成功跳转、错误提示（错误密码）表现正常。

自测清单（勾选）：
- [ ] 无 emoji、无深色底、无紫渐变
- [ ] 每个输入框有 label（或视觉隐藏 label）+ autocomplete
- [ ] 聚焦环可见且对比 ≥ 3:1
- [ ] 错误提示带 `aria-live` 且不只靠颜色
- [ ] 375px 宽下单列、按钮全宽、无横向滚动
- [ ] `prefers-reduced-motion` 下无动画
- [ ] build 通过、登录/注册链路功能不回退

---

## 附：与本项目现状的差异对照（改造前 → 改造后）

| 维度 | 现状（Login.vue） | 目标（方案 6） |
|---|---|---|
| 背景 | `#1a1a2e → #16213e` 深色渐变 | `#F5F7FA` + 浅蓝格纹 |
| 卡片 | 半透明 + `backdrop-filter: blur(10px)` | 白色「考卷纸」+ 蓝色双线卷头 |
| 按钮 | `#667eea → #764ba2` 紫渐变 | `#409EFF` 实心 |
| 标题 | `🎓 智能学习平台`（含 emoji） | 衬线标题「智能学习平台」，无 emoji |
| 字体 | 默认无衬线 | 标题衬线（`Noto Serif SC` 栈） | 
| 可访问性 | 仅 placeholder、无 autocomplete、无 aria-live、深色底对比不足 | label + autocomplete + aria-live + AA 对比度 |
| 与全站一致性 | 第一入口中唯一深色页（`Chat.vue` 亦为深色，可后续一并迁移） | 与考试页 / 学习分析页同语言 |