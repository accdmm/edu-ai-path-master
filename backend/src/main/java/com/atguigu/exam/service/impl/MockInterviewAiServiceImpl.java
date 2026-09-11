package com.atguigu.exam.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.atguigu.exam.service.KimiAiService;
import com.atguigu.exam.service.MockInterviewAiService;
import com.atguigu.exam.vo.LearningPathDetailVo;
import com.atguigu.exam.vo.MockInterviewAnswerDetailVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 模拟面试官服务实现
 * 复用 KimiAiService.callKimiAi()，走 OpenAI 兼容接口
 */
@Slf4j
@Service
public class MockInterviewAiServiceImpl implements MockInterviewAiService {

    @Autowired
    private KimiAiService kimiAiService;

    @Override
    public Map<String, Object> gradeAnswer(String direction, String questionContent, String difficultyLevel, String userAnswer) {
        String prompt = buildGradePrompt(direction, questionContent, difficultyLevel, userAnswer);
        String content = safeCall(prompt);

        String realResult = extractJson(content);
        Map<String, Object> result = new HashMap<>();
        // 默认兜底：答了给 60，空答给 0
        boolean emptyAnswer = userAnswer == null || userAnswer.trim().isEmpty();
        int defaultScore = emptyAnswer ? 0 : 60;
        result.put("score", defaultScore);
        result.put("comment", emptyAnswer ? "未作答，建议补充完整回答。" : "回答已记录。");
        result.put("technicalAccuracy", 3);
        result.put("clarity", 3);
        result.put("logic", 3);

        try {
            JSONObject json = JSON.parseObject(realResult);
            if (json != null) {
                int score = json.getIntValue("score", defaultScore);
                score = Math.max(0, Math.min(100, score));
                result.put("score", score);
                result.put("comment", json.getString("comment") != null ? json.getString("comment") : result.get("comment"));
                result.put("technicalAccuracy", clamp1to5(json.getIntValue("technicalAccuracy", 3)));
                result.put("clarity", clamp1to5(json.getIntValue("clarity", 3)));
                result.put("logic", clamp1to5(json.getIntValue("logic", 3)));
            }
        } catch (Exception e) {
            log.warn("AI 评分结果解析失败，使用兜底分数。原始内容: {}", realResult, e);
        }
        return result;
    }

    @Override
    public Map<String, Object> summarizeInterview(List<MockInterviewAnswerDetailVo> answers) {
        String prompt = buildSummaryPrompt(answers);
        String content = safeCall(prompt);
        String realResult = extractJson(content);

        Map<String, Object> result = new HashMap<>();
        result.put("summary", "本次模拟面试已完成，整体表现中规中矩，建议针对薄弱知识点加强练习。");
        result.put("strengths", new ArrayList<String>());
        result.put("improvements", List.of("加强基础概念理解", "增加代码实践"));
        Map<String, Integer> ability = new HashMap<>();
        ability.put("technicalAccuracy", 60);
        ability.put("clarity", 60);
        ability.put("logic", 60);
        ability.put("knowledge", 60);
        ability.put("experience", 50);
        result.put("abilityScores", ability);

        try {
            JSONObject json = JSON.parseObject(realResult);
            if (json != null) {
                if (json.getString("summary") != null) {
                    result.put("summary", json.getString("summary"));
                }
                JSONArray strengths = json.getJSONArray("strengths");
                if (strengths != null && !strengths.isEmpty()) {
                    result.put("strengths", strengths.toList(String.class));
                }
                JSONArray improvements = json.getJSONArray("improvements");
                if (improvements != null && !improvements.isEmpty()) {
                    result.put("improvements", improvements.toList(String.class));
                }
                JSONObject abilityObj = json.getJSONObject("abilityScores");
                if (abilityObj != null) {
                    Map<String, Integer> parsed = new HashMap<>();
                    parsed.put("technicalAccuracy", clamp0to100(abilityObj.getIntValue("technicalAccuracy", 60)));
                    parsed.put("clarity", clamp0to100(abilityObj.getIntValue("clarity", 60)));
                    parsed.put("logic", clamp0to100(abilityObj.getIntValue("logic", 60)));
                    parsed.put("knowledge", clamp0to100(abilityObj.getIntValue("knowledge", 60)));
                    parsed.put("experience", clamp0to100(abilityObj.getIntValue("experience", 50)));
                    result.put("abilityScores", parsed);
                }
            }
        } catch (Exception e) {
            log.warn("AI 总结解析失败，使用兜底总结。原始内容: {}", realResult, e);
        }
        return result;
    }

    @Override
    public String explainQuestion(String direction, String questionContent, String referenceAnswer) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一名经验丰富的").append(directionLabel(direction))
                .append("技术讲师，正在给求职者深入讲解一道面试真题。\n\n");
        prompt.append("【面试题】\n").append(questionContent).append("\n\n");
        if (referenceAnswer != null && !referenceAnswer.isBlank()) {
            prompt.append("【参考答案】\n").append(referenceAnswer).append("\n\n");
        }
        prompt.append("【讲解要求】\n");
        prompt.append("1. 从考察点开始，说明这道题在面试中考察什么能力\n");
        prompt.append("2. 结合底层原理给出详细的正确答案与推导过程\n");
        prompt.append("3. 给出常见错误回答与易错点\n");
        prompt.append("4. 补充一个面试追问角度，帮助应对面试官延伸提问\n");
        prompt.append("5. 使用中文 Markdown，控制在 600 字以内，层次清晰\n");
        String content = safeCall(prompt.toString());
        return (content == null || content.trim().isEmpty()) ? null : content.trim();
    }

    @Override
    public String explainPaperQuestion(String questionContent, String referenceAnswer) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一名经验丰富的技术讲师，正在给学生深入讲解一道练习题。\n\n");
        prompt.append("【题目】\n").append(questionContent == null ? "" : questionContent).append("\n\n");
        if (referenceAnswer != null && !referenceAnswer.isBlank()) {
            prompt.append("【参考答案】\n").append(referenceAnswer).append("\n\n");
        }
        prompt.append("【讲解要求】\n");
        prompt.append("1. 开门见山点出本题考察的核心知识点\n");
        prompt.append("2. 结合底层原理给出清晰的解题思路与结论\n");
        prompt.append("3. 指出常见错误或易踩的坑\n");
        prompt.append("4. 补充一个举一反三的延伸思考\n");
        prompt.append("5. 使用中文，控制在 500 字以内，层次清晰\n");
        String content = safeCall(prompt.toString());
        return (content == null || content.trim().isEmpty()) ? null : content.trim();
    }

    private String safeCall(String prompt) {
        try {
            return kimiAiService.callKimiAi(prompt);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("AI 调用被中断", e);
            return "";
        } catch (Exception e) {
            log.warn("AI 调用失败", e);
            return "";
        }
    }

    /**
     * 从 LLM 返回内容中提取 JSON（去掉 ```json 代码块包裹）
     */
    private String extractJson(String content) {
        if (content == null || content.isEmpty()) {
            return "{}";
        }
        int startIndex = content.indexOf("```json");
        int endIndex = content.lastIndexOf("```");
        if (startIndex != -1 && endIndex != -1 && startIndex < endIndex) {
            return content.substring(startIndex + 7, endIndex).trim();
        }
        // 尝试直接提取最外层花括号 JSON
        int braceStart = content.indexOf("{");
        int braceEnd = content.lastIndexOf("}");
        if (braceStart != -1 && braceEnd > braceStart) {
            return content.substring(braceStart, braceEnd + 1);
        }
        return content.trim();
    }

    private String buildGradePrompt(String direction, String questionContent, String difficultyLevel, String userAnswer) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一名经验丰富的").append(directionLabel(direction)).append("技术面试官，正在对候选人进行面试评分。\n\n");
        prompt.append("【面试题信息】\n");
        prompt.append("题目：" ).append(questionContent).append("\n");
        prompt.append("难度：" ).append(difficultyLabel(difficultyLevel)).append("\n\n");
        prompt.append("【候选人回答】\n");
        prompt.append(userAnswer == null || userAnswer.trim().isEmpty() ? "（未作答）" : userAnswer).append("\n\n");
        prompt.append("【评分要求】\n");
        prompt.append("1. score：0-100 整数，根据回答的准确性、完整性、逻辑性评分\n");
        prompt.append("2. technicalAccuracy：技术准确性 1-5 整数\n");
        prompt.append("3. clarity：表达清晰度 1-5 整数\n");
        prompt.append("4. logic：逻辑严谨性 1-5 整数\n");
        prompt.append("5. comment：50 字以内的针对性评价\n\n");
        prompt.append("请严格按照以下 JSON 格式返回，禁止包含任何其他文字：\n");
        prompt.append("```json\n");
        prompt.append("{\n");
        prompt.append("  \"score\": 80,\n");
        prompt.append("  \"comment\": \"回答要点清晰，但深度不足，建议补充底层原理\",\n");
        prompt.append("  \"technicalAccuracy\": 4,\n");
        prompt.append("  \"clarity\": 4,\n");
        prompt.append("  \"logic\": 3\n");
        prompt.append("}\n");
        prompt.append("```\n");
        return prompt.toString();
    }

    @Override
    public Map<String, Object> summarizePersonalizedInterview(List<MockInterviewAnswerDetailVo> answers,
                                                              List<LearningPathDetailVo.DiagnosisItemVo> diagnosis) {
        String prompt = buildPersonalizedSummaryPrompt(answers, diagnosis);
        String content = safeCall(prompt);
        String realResult = extractJson(content);

        Map<String, Object> result = new HashMap<>();
        result.put("summary", "本次模拟面试已完成，整体表现中规中矩，建议针对薄弱知识点加强练习。");
        result.put("strengths", new ArrayList<String>());
        result.put("improvements", List.of("加强基础概念理解", "增加代码实践"));
        Map<String, Integer> ability = new HashMap<>();
        ability.put("technicalAccuracy", 60);
        ability.put("clarity", 60);
        ability.put("logic", 60);
        ability.put("knowledge", 60);
        ability.put("experience", 50);
        result.put("abilityScores", ability);

        try {
            JSONObject json = JSON.parseObject(realResult);
            if (json != null) {
                if (json.getString("summary") != null) {
                    result.put("summary", json.getString("summary"));
                }
                JSONArray strengths = json.getJSONArray("strengths");
                if (strengths != null && !strengths.isEmpty()) {
                    result.put("strengths", strengths.toList(String.class));
                }
                JSONArray improvements = json.getJSONArray("improvements");
                if (improvements != null && !improvements.isEmpty()) {
                    result.put("improvements", improvements.toList(String.class));
                }
                JSONObject abilityObj = json.getJSONObject("abilityScores");
                if (abilityObj != null) {
                    Map<String, Integer> parsed = new HashMap<>();
                    parsed.put("technicalAccuracy", clamp0to100(abilityObj.getIntValue("technicalAccuracy", 60)));
                    parsed.put("clarity", clamp0to100(abilityObj.getIntValue("clarity", 60)));
                    parsed.put("logic", clamp0to100(abilityObj.getIntValue("logic", 60)));
                    parsed.put("knowledge", clamp0to100(abilityObj.getIntValue("knowledge", 60)));
                    parsed.put("experience", clamp0to100(abilityObj.getIntValue("experience", 50)));
                    result.put("abilityScores", parsed);
                }
            }
        } catch (Exception e) {
            log.warn("AI 个性化总结解析失败，使用兜底总结。原始内容: {}", realResult, e);
        }
        return result;
    }

    private String buildPersonalizedSummaryPrompt(List<MockInterviewAnswerDetailVo> answers,
                                                  List<LearningPathDetailVo.DiagnosisItemVo> diagnosis) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一名资深面试官，请基于以下一次模拟面试的答题情况和用户的答题诊断数据，输出整体评价、能力评估与学习规划建议（用于联动 AI 学习路径）。\n\n");
        if (diagnosis != null && !diagnosis.isEmpty()) {
            prompt.append("【用户知识点诊断（得分率升序=最薄弱在前）】\n");
            for (int i = 0; i < diagnosis.size(); i++) {
                LearningPathDetailVo.DiagnosisItemVo d = diagnosis.get(i);
                prompt.append(i + 1).append(". ").append(d.getCategoryName())
                        .append("：得分率 ").append(d.getCorrectRate())
                        .append("%，答题 ").append(d.getAnswerCount()).append(" 道\n");
            }
            prompt.append("\n");
        }
        prompt.append("【答题明细】\n");
        int totalScore = 0;
        for (int i = 0; i < answers.size(); i++) {
            MockInterviewAnswerDetailVo a = answers.get(i);
            totalScore += a.getScore();
            String content = a.getQuestion() != null ? a.getQuestion().getQuestionContent() : "";
            prompt.append(i + 1).append(". 题目：").append(content)
                    .append("\n   得分：").append(a.getScore()).append("/").append(a.getMaxScore())
                    .append("   AI评价：").append(a.getAiEvaluation() == null ? "" : a.getAiEvaluation()).append("\n");
        }
        if (!answers.isEmpty()) {
            prompt.append("平均得分：").append(totalScore / (double) answers.size()).append("/100\n\n");
        }
        prompt.append("【输出要求】\n");
        prompt.append("1. summary：200 字以内的个性化总结，必须结合【用户知识点诊断】，指出本次面试表现与薄弱知识点的关联\n");
        prompt.append("2. strengths：候选人优点数组\n");
        prompt.append("3. improvements：需要改进的点数组，优先针对诊断中最薄弱的知识点给出可执行建议\n");
        prompt.append("4. abilityScores：能力雷达图得分(0-100整数)，字段为 technicalAccuracy(技术准确性)、clarity(表达清晰度)、logic(逻辑性)、knowledge(知识储备)、experience(实践经验)\n\n");
        prompt.append("请严格按照以下 JSON 格式返回，禁止包含任何其他文字：\n");
        prompt.append("```json\n");
        prompt.append("{\n");
        prompt.append("  \"summary\": \"个性化总结内容\",\n");
        prompt.append("  \"strengths\": [\"优点1\", \"优点2\"],\n");
        prompt.append("  \"improvements\": [\"改进1\", \"改进2\"],\n");
        prompt.append("  \"abilityScores\": {\"technicalAccuracy\": 70, \"clarity\": 65, \"logic\": 60, \"knowledge\": 72, \"experience\": 55}\n");
        prompt.append("}\n");
        prompt.append("```\n");
        return prompt.toString();
    }

    private String buildSummaryPrompt(List<MockInterviewAnswerDetailVo> answers) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一名资深面试官，请基于以下一次模拟面试的答题情况，输出整体评价与能力评估。\n\n");
        prompt.append("【答题明细】\n");
        int totalScore = 0;
        for (int i = 0; i < answers.size(); i++) {
            MockInterviewAnswerDetailVo a = answers.get(i);
            totalScore += a.getScore();
            String content = a.getQuestion() != null ? a.getQuestion().getQuestionContent() : "";
            prompt.append(i + 1).append(". 题目：").append(content)
                    .append("\n   得分：").append(a.getScore()).append("/").append(a.getMaxScore())
                    .append("   AI评价：").append(a.getAiEvaluation() == null ? "" : a.getAiEvaluation()).append("\n");
        }
        if (!answers.isEmpty()) {
            prompt.append("平均得分：").append(totalScore / (double) answers.size()).append("/100\n\n");
        }
        prompt.append("【输出要求】\n");
        prompt.append("1. summary：150 字以内的整体面试总结，指出优势与不足\n");
        prompt.append("2. strengths：候选人优点数组\n");
        prompt.append("3. improvements：需要改进的点数组\n");
        prompt.append("4. abilityScores：能力雷达图得分(0-100整数)，字段为 technicalAccuracy(技术准确性)、clarity(表达清晰度)、logic(逻辑性)、knowledge(知识储备)、experience(实践经验)\n\n");
        prompt.append("请严格按照以下 JSON 格式返回，禁止包含任何其他文字：\n");
        prompt.append("```json\n");
        prompt.append("{\n");
        prompt.append("  \"summary\": \"整体总结内容\",\n");
        prompt.append("  \"strengths\": [\"优点1\", \"优点2\"],\n");
        prompt.append("  \"improvements\": [\"改进1\", \"改进2\"],\n");
        prompt.append("  \"abilityScores\": {\"technicalAccuracy\": 70, \"clarity\": 65, \"logic\": 60, \"knowledge\": 72, \"experience\": 55}\n");
        prompt.append("}\n");
        prompt.append("```\n");
        return prompt.toString();
    }

    private String directionLabel(String direction) {
        Map<String, String> map = new HashMap<>();
        map.put("java", "Java后端");
        map.put("frontend", "前端");
        map.put("bigdata", "大数据");
        map.put("algorithm", "算法");
        map.put("devops", "运维");
        map.put("testing", "测试");
        return map.getOrDefault(direction, "IT");
    }

    private String difficultyLabel(String difficulty) {
        Map<String, String> map = new HashMap<>();
        map.put("easy", "简单");
        map.put("medium", "中等");
        map.put("hard", "困难");
        return map.getOrDefault(difficulty, "中等");
    }

    private int clamp1to5(int value) {
        return Math.max(1, Math.min(5, value));
    }

    private int clamp0to100(int value) {
        return Math.max(0, Math.min(100, value));
    }
}