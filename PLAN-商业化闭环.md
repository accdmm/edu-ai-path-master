# 商业化闭环后端补齐计划

> 企业真题 / AI 模拟面试 / 邀请码 / 积分 / 管理端全链路
> 目标：1 周内，答辩演示标准。优先 AI 模拟面试官，激活码闭环替代真支付。

## 现状

- 前端契约完整（`frontend/src/api/interviewQuestion.js` + 各视图），后端接口全部 404，数据库无对应 4 组表。
- 前端阻断级 bug：`MockInterview.vue` 未保存 `interviewResult.id`（start 响应需返回 `{id, questions}`）。
- AI 通道复用 `KimiAiServiceImpl.callKimiAi()`（Moonshot key 已配置，3 重试 + 100s 超时 + fastjson2 解析）。

## 交付清单

### P0（核心）
- 11 张新表 + 种子数据
- 真题用户端：list / detail / related / favorite / evaluation / view / hot / latest / stats
- AI 模拟面试：start / submit-answer（逐题 AI 评分）/ complete（AI 汇总+雷达五维）/ detail / result
- 邀请码：generate / activate / list / invitees / delete
- 真题练习单题 AI 评分
- MockInterview.vue 前端 bug 修复

### P1（管理端全链路）
- 企业/分类下拉、真题管理 CRUD（难度数字 1/2/3）
- 用户上传真题审核（含本地图片上传 /api/upload）
- 积分 user / active / history

### P2（裁剪）
- 面试历史 / 统计 / 分享 / 语音占位字段（不做上传）

## 新表（11 张）

interview_company / interview_question / interview_question_category / interview_favorite / interview_evaluation / mock_interview / mock_interview_answer / invite_code / user_credit / credit_record / user_contribution

## 后端结构

controller：InterviewQuestionController / MockInterviewController / InterviewPortalController / UserCreditController / CompanyController / QuestionCategoryController / PendingQuestionController
+ service / mapper / entity / vo

AI：新增 `MockInterviewAiService`（复用 callKimiAi，JSON clamp + 兜底）
鉴权：permitAll + 控制器解析 JWT（无 token 回退默认用户）
响应：统一 Result<T>；分页 MP Page<T>

## 前端改动

1. MockInterview.vue 补 id 存储
2. InterviewQuestionManage.vue 接真实 CRUD
3. PendingQuestionManage.vue 接真实接口
4. router 补 /company-detail/:id

## 明确不做

真实支付、MinIO 视频、语音上传、AI 客服多轮迁移、管理端前端鉴权收紧

## 验证

mvn compile → Python 全链路断言（登录→激活→start→submit×N→complete→result）→ 前端 build + 浏览器模拟面试 → 更新 CLAUDE.md