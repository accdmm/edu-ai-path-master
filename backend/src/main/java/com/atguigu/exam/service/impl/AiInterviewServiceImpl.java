package com.atguigu.exam.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.atguigu.exam.common.Result;
import com.atguigu.exam.entity.CreditRecord;
import com.atguigu.exam.entity.InterviewQuestion;
import com.atguigu.exam.entity.UserCredit;
import com.atguigu.exam.mapper.CreditRecordMapper;
import com.atguigu.exam.mapper.InterviewQuestionMapper;
import com.atguigu.exam.mapper.UserCreditMapper;
import com.atguigu.exam.service.AiInterviewService;
import com.atguigu.exam.service.KimiAiService;
import com.atguigu.exam.service.UserDiagnosisService;
import com.atguigu.exam.vo.AiInterviewSession;
import com.atguigu.exam.vo.AiInterviewStartVo;
import com.atguigu.exam.vo.LearningPathDetailVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * AI 面试官实现（BOSS 直聘式多轮对话面试）
 * 会话保存在内存（低频操作），面试结束后保留 30 分钟供结果页拉取，超时清理
 */
@Slf4j
@Service
public class AiInterviewServiceImpl implements AiInterviewService {

    /** 会话 Redis Key 前缀 */
    private static final String SESSION_KEY_PREFIX = "aiinterview:session:";

    /** 会话保留时长（Redis TTL 自动过期，含面试报告查看窗口） */
    private static final Duration SESSION_TTL = Duration.ofHours(2);

    @Autowired
    private KimiAiService kimiAiService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private UserDiagnosisService userDiagnosisService;

    @Autowired
    private InterviewQuestionMapper interviewQuestionMapper;

    @Autowired
    private UserCreditMapper userCreditMapper;

    @Autowired
    private CreditRecordMapper creditRecordMapper;

    /** AI 面试计费：每场面试扣积分，无免费额度 */
    private static final int AI_INTERVIEW_COST = 10;

    @Override
    public Result<Map<String, Object>> start(Long userId, AiInterviewStartVo vo) {
        try {
            String direction = vo.getDirection() == null ? "java" : vo.getDirection();
            String difficulty = vo.getDifficulty() == null ? "medium" : vo.getDifficulty();
            int maxRounds = vo.getMaxRounds() == null ? 6 : Math.max(3, Math.min(12, vo.getMaxRounds()));

            AiInterviewSession s = new AiInterviewSession();
            s.userId = userId;
            s.direction = direction;
            s.difficulty = difficulty;
            s.maxRounds = maxRounds;
            s.round = 1;
            s.startTime = System.currentTimeMillis();
            s.lastActive = s.startTime;

            // 个性化：注入薄弱知识点诊断
            if (Boolean.TRUE.equals(vo.getPersonalized())) {
                List<LearningPathDetailVo.DiagnosisItemVo> diagnosis = userDiagnosisService.buildDiagnosis(userId);
                if (!diagnosis.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < diagnosis.size(); i++) {
                        LearningPathDetailVo.DiagnosisItemVo d = diagnosis.get(i);
                        sb.append(i + 1).append(". ").append(d.getCategoryName())
                                .append(" 得分率 ").append(d.getCorrectRate()).append("%（答题").append(d.getAnswerCount()).append("道）");
                        if (i < diagnosis.size() - 1) sb.append("；");
                    }
                    s.diagnosisText = sb.toString();
                }
            }

            // 生成会话ID：时间戳 + 随机
            Long id = System.currentTimeMillis() * 1000 + (long) (Math.random() * 1000);

            // 开场白 + 第一题：不调 LLM，秒开（真题库随机抽题，查不到用兜底模板）
            StringBuilder opening = new StringBuilder("你好，我是你的 AI 面试官。");
            if (!s.diagnosisText.isBlank()) {
                opening.append("我看过你的答题记录，会重点考察你的薄弱环节。");
            }
            opening.append("放轻松，我们像真实面试一样开始吧。");
            String question = pickRealQuestion(s.direction, s.difficulty, s);
            if (question == null) question = buildFallbackQuestion(s);

            s.messages.add(msg("ai", opening + "\n\n" + question));
            s.askedQuestions.add(question);

            // 计费：每场面试扣 AI_INTERVIEW_COST 积分，无免费额度；条件扣费防并发超扣
            int updated = userCreditMapper.update(null, new UpdateWrapper<UserCredit>()
                    .eq("user_id", userId)
                    .ge("active_credits", AI_INTERVIEW_COST)
                    .setSql("active_credits = active_credits - " + AI_INTERVIEW_COST)
                    .setSql("update_time = NOW()"));
            if (updated == 0) {
                return Result.error(4002, "积分不足，开始一场 AI 面试需 " + AI_INTERVIEW_COST
                        + " 积分，可通过购买邀请码获取积分");
            }

            UserCredit latest = userCreditMapper.selectOne(
                    new LambdaQueryWrapper<UserCredit>().eq(UserCredit::getUserId, userId));
            int balance = latest == null || latest.getActiveCredits() == null ? 0 : latest.getActiveCredits();
            CreditRecord record = new CreditRecord();
            record.setUserId(userId);
            record.setChangeAmount(-AI_INTERVIEW_COST);
            record.setType("ai-interview");
            record.setSource("AI面试官");
            record.setBalance(balance);
            record.setCreateTime(new Date());
            creditRecordMapper.insert(record);

            saveSession(id, s);

            Map<String, Object> data = new HashMap<>();
            data.put("interviewId", id);
            data.put("direction", direction);
            data.put("difficulty", difficulty);
            data.put("round", s.round);
            data.put("maxRounds", maxRounds);
            data.put("ended", false);
            data.put("messages", s.messages);
            return Result.success(data);
        } catch (Exception e) {
            log.error("开始 AI 面试失败", e);
            return Result.error("开始 AI 面试失败，请重试");
        }
    }

    @Override
    public Result<Map<String, Object>> answer(Long userId, Long interviewId, String userAnswer) {
        try {
            AiInterviewSession s = loadSession(interviewId, userId);
            if (s == null || !s.userId.equals(userId)) {
                return Result.error(404, "面试会话不存在或已过期");
            }
            if ("GENERATING".equals(s.reportStatus) || "DONE".equals(s.reportStatus)) {
                return Result.error(400, "面试已结束，报告生成中或已完成");
            }
            if (userAnswer == null || userAnswer.isBlank()) {
                return Result.error("请先输入你的回答");
            }

            s.messages.add(msg("user", userAnswer));
            s.round++;
            s.lastActive = System.currentTimeMillis();

            // 达到轮数上限：AI 对最后一轮回答做收尾点评（不出题），然后异步生成报告
            if (s.round > s.maxRounds) {
                String finalComment = "";
                try {
                    String raw = callLlm(buildFinalCommentPrompt(s));
                    JSONObject json = parseJson(raw);
                    finalComment = json == null ? "" : json.getString("comment");
                } catch (Exception llmEx) {
                    log.warn("最后一轮点评降级（LLM 不可用）：{}", llmEx.getMessage());
                }
                if (finalComment == null || finalComment.isBlank()) {
                    finalComment = "好的，本场面试的提问环节到这里就结束了。";
                }
                s.messages.add(msg("ai", finalComment));
                return startReportAndReturnPending(s, interviewId, finalComment);
            }

            // LLM 调用失败时降级：固定点评 + 真题库换题，保证流程不中断
            String comment;
            String question;
            boolean ended = false;
            try {
                String raw = callLlm(buildTurnPrompt(s));
                JSONObject json = parseJson(raw);
                comment = json == null ? "" : json.getString("comment");
                question = json == null ? null : json.getString("question");
                ended = json != null && Boolean.TRUE.equals(json.getBoolean("ended"));
            } catch (Exception llmEx) {
                log.warn("AI 面试追问降级（LLM 不可用）：{}", llmEx.getMessage());
                comment = "收到你的回答。我们换个问题：";
                question = pickRealQuestion(s.direction, s.difficulty, s);
                if (question == null) question = buildFallbackQuestion(s);
            }
            if (comment == null) comment = "";
            if (question == null || question.isBlank()) question = buildFallbackQuestion(s);

            String reply = comment.isBlank() ? question : comment + "\n\n" + question;
            s.messages.add(msg("ai", reply));

            // AI 自主判断面试结束：转异步报告
            if (ended) {
                return startReportAndReturnPending(s, interviewId, comment);
            }

            saveSession(interviewId, s);

            Map<String, Object> data = new HashMap<>();
            data.put("comment", comment);
            data.put("question", question);
            data.put("ended", false);
            data.put("round", s.round);
            data.put("maxRounds", s.maxRounds);
            data.put("messages", s.messages);
            return Result.success(data);
        } catch (Exception e) {
            log.error("AI 面试答题失败", e);
            return Result.error("AI 面试答题失败，请重试");
        }
    }

    @Override
    public Result<Map<String, Object>> finish(Long userId, Long interviewId) {
        try {
            AiInterviewSession s = loadSession(interviewId, userId);
            if (s == null || !s.userId.equals(userId)) {
                return Result.error(404, "面试会话不存在或已过期");
            }
            // 幂等：已在生成中/已生成，直接返回当前状态
            if ("GENERATING".equals(s.reportStatus)) {
                return Result.success(pendingData(s));
            }
            if ("DONE".equals(s.reportStatus)) {
                return Result.success(doneData(s));
            }
            // 手动提前结束：直接进入报告生成
            return startReportAndReturnPending(s, interviewId, null);
        } catch (Exception e) {
            log.error("结束 AI 面试失败", e);
            return Result.error("结束 AI 面试失败，请重试");
        }
    }

    @Override
    public Result<Map<String, Object>> getReport(Long userId, Long interviewId) {
        AiInterviewSession s = loadSession(interviewId, userId);
        if (s == null || !s.userId.equals(userId)) {
            return Result.error(404, "面试报告不存在或已过期");
        }
        s.lastActive = System.currentTimeMillis();
        if ("GENERATING".equals(s.reportStatus)) {
            return Result.success(pendingData(s));
        }
        if ("FAILED".equals(s.reportStatus)) {
            Map<String, Object> data = new HashMap<>();
            data.put("reportStatus", "FAILED");
            return Result.success(data);
        }
        return Result.success(doneData(s));
    }

    // ---------- 报告生成 ----------

    /** 触发异步报告生成，立即返回 reportPending 状态 */
    private Result<Map<String, Object>> startReportAndReturnPending(AiInterviewSession s, Long interviewId, String lastComment) {
        s.reportStatus = "GENERATING";
        s.lastActive = System.currentTimeMillis();
        saveSession(interviewId, s);
        CompletableFuture.runAsync(() -> {
            try {
                JSONObject json = null;
                try {
                    String raw = callLlm(buildFinishPrompt(s));
                    json = parseJson(raw);
                } catch (Exception llmEx) {
                    log.warn("AI 面试报告生成降级（LLM 不可用）：{}", llmEx.getMessage());
                }
                s.report = buildReportData(s, json);
                s.reportStatus = "DONE";
                saveSession(interviewId, s);
                log.info("AI 面试报告已生成，interviewId={}", interviewId);
            } catch (Exception e) {
                log.error("生成 AI 面试报告失败", e);
                s.reportStatus = "FAILED";
                saveSession(interviewId, s);
            }
        });

        Map<String, Object> data = pendingData(s);
        if (lastComment != null) {
            data.put("comment", lastComment);
        }
        return Result.success(data);
    }

    private Map<String, Object> pendingData(AiInterviewSession s) {
        Map<String, Object> data = new HashMap<>();
        data.put("ended", true);
        data.put("reportStatus", "GENERATING");
        data.put("reportPending", true);
        data.put("round", s.round);
        data.put("maxRounds", s.maxRounds);
        data.put("messages", s.messages);
        return data;
    }

    private Map<String, Object> doneData(AiInterviewSession s) {
        Map<String, Object> data = s.report == null ? new HashMap<>() : new HashMap<>(s.report);
        data.put("reportStatus", "DONE");
        return data;
    }

    /** 组装面试报告（含每题参考答案 qaReview） */
    private Map<String, Object> buildReportData(AiInterviewSession s, JSONObject json) {
        Map<String, Object> data = new HashMap<>();
        data.put("ended", true);
        data.put("round", s.round);
        data.put("maxRounds", s.maxRounds);
        data.put("messages", s.messages);
        data.put("score", json == null ? localScore(s) : clamp(json.getIntValue("score", localScore(s))));
        data.put("summary", json == null || json.getString("summary") == null
                ? "本次面试共完成 " + s.round + " 轮问答。AI 总结服务暂时不可用，建议对照题目回顾回答内容，并针对薄弱知识点加强练习。"
                : json.getString("summary"));
        data.put("strengths", json == null || json.getJSONArray("strengths") == null
                ? new ArrayList<String>() : json.getJSONArray("strengths").toList(String.class));
        data.put("improvements", json == null || json.getJSONArray("improvements") == null
                ? List.of("加强基础概念理解", "增加实战练习") : json.getJSONArray("improvements").toList(String.class));
        Map<String, Integer> ability = new HashMap<>();
        if (json != null && json.getJSONObject("abilityScores") != null) {
            JSONObject ab = json.getJSONObject("abilityScores");
            ability.put("technicalAccuracy", clamp(ab.getIntValue("technicalAccuracy", 60)));
            ability.put("clarity", clamp(ab.getIntValue("clarity", 60)));
            ability.put("logic", clamp(ab.getIntValue("logic", 60)));
            ability.put("knowledge", clamp(ab.getIntValue("knowledge", 60)));
            ability.put("experience", clamp(ab.getIntValue("experience", 50)));
        } else {
            int base = localScore(s);
            ability.put("technicalAccuracy", base);
            ability.put("clarity", base);
            ability.put("logic", base);
            ability.put("knowledge", base);
            ability.put("experience", Math.max(0, base - 10));
        }
        data.put("abilityScores", ability);

        // 每题参考答案回顾（AI 不可用时降级为真题库答案/空提示）
        List<Map<String, Object>> qaReview = new ArrayList<>();
        if (json != null && json.getJSONArray("qaReview") != null) {
            for (Object o : json.getJSONArray("qaReview")) {
                JSONObject qa = (JSONObject) o;
                Map<String, Object> item = new HashMap<>();
                item.put("question", qa.getString("question"));
                item.put("userAnswer", qa.getString("userAnswer"));
                item.put("idealAnswer", qa.getString("idealAnswer"));
                item.put("score", clamp(qa.getIntValue("score", 60)));
                qaReview.add(item);
            }
        }
        data.put("qaReview", qaReview);

        // 个性化：返回诊断供前端展示
        if (!s.diagnosisText.isBlank()) {
            data.put("diagnosis", userDiagnosisService.buildDiagnosis(s.userId));
        }
        return data;
    }

    // ---------- 内部工具 ----------

    private String buildTurnPrompt(AiInterviewSession s) {
        StringBuilder p = new StringBuilder();
        p.append("你是一名资深技术面试官，正在多轮面试候选人。请像真实面试一样：先简短点评上一回答（肯定优点/指出不足，80字内），然后根据回答决定是【追问本题】还是【换下一题】，继续提问。\n\n");
        p.append("【面试方向】").append(directionLabel(s.direction)).append("　【难度】").append(s.difficulty).append("\n");
        if (!s.diagnosisText.isBlank()) {
            p.append("【候选人薄弱知识点诊断】").append(s.diagnosisText).append("\n");
            p.append("可针对薄弱知识点追问或换题。\n");
        }
        p.append("【面试历史】\n");
        for (Map<String, String> m : s.messages) {
            p.append(m.get("role").equals("ai") ? "面试官：" : "候选人：").append(m.get("content")).append("\n");
        }
        p.append("\n这是第 ").append(s.round).append(" 轮。输出 JSON：\n");
        p.append("{\"comment\":\"对候选人上一回答的点评(80字内)\",\"question\":\"下一道问题：可以是追问上一题或新题(70字内)\",\"ended\":false}\n");
        p.append("若你认为面试已足够（至少 ").append(s.maxRounds).append(" 轮）或候选人明显答不上来，可将 ended 设为 true。\n");
        p.append("只输出 JSON，禁止其他文字。");
        return p.toString();
    }

    private String buildFinishPrompt(AiInterviewSession s) {
        StringBuilder p = new StringBuilder();
        p.append("你是一名资深技术面试官，请对候选人的整场面试给出总结评分与学习建议。\n\n");
        p.append("【面试方向】").append(directionLabel(s.direction)).append("　【难度】").append(s.difficulty).append("\n");
        if (!s.diagnosisText.isBlank()) {
            p.append("【候选人薄弱知识点诊断】").append(s.diagnosisText).append("\n");
        }
        p.append("【面试完整对话】\n");
        for (Map<String, String> m : s.messages) {
            p.append(m.get("role").equals("ai") ? "面试官：" : "候选人：").append(m.get("content")).append("\n");
        }
        p.append("\n输出 JSON：\n");
        p.append("{\n");
        p.append("  \"score\":0到100整数,\n");
        p.append("  \"summary\":\"200字内整体总结,指出表现与薄弱点关联\",\n");
        p.append("  \"strengths\":[\"优点1\"],\n");
        p.append("  \"improvements\":[\"改进1(可执行)\"],\n");
        p.append("  \"abilityScores\":{\"technicalAccuracy\":70,\"clarity\":65,\"logic\":60,\"knowledge\":72,\"experience\":55},\n");
        p.append("  \"qaReview\":[{\"question\":\"本轮问题原文\",\"userAnswer\":\"候选人回答要点\",\"idealAnswer\":\"该问题的理想参考答案要点(120字内,结构化便于学习)\",\"score\":0到100}]\n");
        p.append("}\n");
        p.append("qaReview 必须覆盖候选人回答过的每一轮问题（含追问），question 用问题原文，idealAnswer 给出该题的标准参考答案要点。\n");
        p.append("只输出 JSON，禁止其他文字。");
        return p.toString();
    }

    /** 最后一轮收尾点评：只点评不出题 */
    private String buildFinalCommentPrompt(AiInterviewSession s) {
        StringBuilder p = new StringBuilder();
        p.append("你是一名资深技术面试官，这是面试的最后一个问题，候选人刚刚作答完毕。请像真实面试收尾一样：\n");
        p.append("1. 简短点评候选人最后的这个回答（肯定优点/指出不足，100字内）；\n");
        p.append("2. 用一句话自然收尾告别（如\"今天的面试就到这里\"）。\n");
        p.append("不要再提出任何新问题。\n\n");
        p.append("【面试方向】").append(directionLabel(s.direction)).append("\n");
        p.append("【面试最后片段】\n");
        int from = Math.max(0, s.messages.size() - 4);
        for (int i = from; i < s.messages.size(); i++) {
            Map<String, String> m = s.messages.get(i);
            p.append(m.get("role").equals("ai") ? "面试官：" : "候选人：").append(m.get("content")).append("\n");
        }
        p.append("\n输出 JSON：{\"comment\":\"点评+收尾告别(120字内)\"}\n");
        p.append("只输出 JSON，禁止其他文字。");
        return p.toString();
    }

    private String buildFallbackQuestion(AiInterviewSession s) {
        return "请谈谈你对 " + directionLabel(s.direction) + " 方向核心知识点的理解，以及你平时是如何学习和实践的？";
    }

    /**
     * 从企业真题库随机抽一道匹配方向/难度的真题（start 秒开 & LLM 降级时使用），自动排除已问过的题
     */
    private String pickRealQuestion(String direction, String difficulty, AiInterviewSession s) {
        try {
            List<InterviewQuestion> list = interviewQuestionMapper.selectList(
                    new LambdaQueryWrapper<InterviewQuestion>()
                            .eq(InterviewQuestion::getDirection, direction)
                            .eq(InterviewQuestion::getDifficultyLevel, difficulty)
                            .eq(InterviewQuestion::getStatus, "approved"));
            if (list == null || list.isEmpty()) {
                // 放宽难度限制再试一次
                list = interviewQuestionMapper.selectList(
                        new LambdaQueryWrapper<InterviewQuestion>()
                                .eq(InterviewQuestion::getDirection, direction)
                                .eq(InterviewQuestion::getStatus, "approved"));
            }
            if (list == null || list.isEmpty()) {
                return null;
            }
            // 排除已问过的题，全部问过则允许重复
            List<InterviewQuestion> fresh = list;
            if (s != null && !s.askedQuestions.isEmpty()) {
                fresh = list.stream()
                        .filter(q -> !s.askedQuestions.contains(q.getQuestionContent()))
                        .collect(java.util.stream.Collectors.toList());
                if (fresh.isEmpty()) fresh = list;
            }
            String picked = fresh.get((int) (Math.random() * fresh.size())).getQuestionContent();
            if (s != null) s.askedQuestions.add(picked);
            return picked;
        } catch (Exception e) {
            log.warn("真题库抽题失败，使用兜底题目：{}", e.getMessage());
            return null;
        }
    }

    /**
     * 本地保底评分：按完成轮数占比估算（LLM 不可用时使用）
     */
    private int localScore(AiInterviewSession s) {
        int answered = Math.max(1, s.round - 1);
        double ratio = (double) answered / s.maxRounds;
        return clamp((int) Math.round(50 + ratio * 30));
    }

    private String callLlm(String prompt) throws InterruptedException {
        return kimiAiService.callKimiAi(prompt);
    }

    private JSONObject parseJson(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }
        int start = content.indexOf("{");
        int end = content.lastIndexOf("}");
        if (start == -1 || end <= start) {
            return null;
        }
        try {
            return JSON.parseObject(content.substring(start, end + 1));
        } catch (Exception e) {
            log.warn("AI 面试 JSON 解析失败: {}", e.getMessage());
            return null;
        }
    }

    private Map<String, String> msg(String role, String content) {
        Map<String, String> m = new HashMap<>();
        m.put("role", role);
        m.put("content", content);
        return m;
    }

    private int clamp(int v) {
        return Math.max(0, Math.min(100, v));
    }

    private String directionLabel(String direction) {
        Map<String, String> map = new HashMap<>();
        map.put("java", "Java后端");
        map.put("frontend", "前端");
        map.put("bigdata", "大数据");
        map.put("algorithm", "算法");
        map.put("devops", "运维");
        map.put("testing", "测试");
        return map.getOrDefault(direction, direction);
    }

    /**
     * 会话持久化到 Redis（TTL 自动过期）：后端重启不再丢失进行中的面试会话，积分不白扣
     */
    private void saveSession(Long interviewId, AiInterviewSession s) {
        try {
            stringRedisTemplate.opsForValue().set(SESSION_KEY_PREFIX + interviewId, JSON.toJSONString(s), SESSION_TTL);
        } catch (Exception e) {
            log.warn("保存 AI 面试会话失败 interviewId={}: {}", interviewId, e.getMessage());
        }
    }

    /**
     * 从 Redis 加载会话并校验归属；不存在/过期/非本人返回 null
     */
    private AiInterviewSession loadSession(Long interviewId, Long userId) {
        try {
            String json = stringRedisTemplate.opsForValue().get(SESSION_KEY_PREFIX + interviewId);
            if (json == null) {
                return null;
            }
            AiInterviewSession s = JSON.parseObject(json, AiInterviewSession.class);
            if (s == null || s.userId == null || !s.userId.equals(userId)) {
                return null;
            }
            return s;
        } catch (Exception e) {
            log.warn("读取 AI 面试会话失败 interviewId={}: {}", interviewId, e.getMessage());
            return null;
        }
    }
}
