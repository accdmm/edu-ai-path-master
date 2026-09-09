package com.atguigu.exam.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.atguigu.exam.service.PaperGenerateAiService;
import com.atguigu.exam.vo.AiGenerateRequestVo;
import com.atguigu.exam.vo.QuestionImportVo;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * AI 试卷生成服务实现类
 * <p>
 * 使用 DashScope 的 paperChatModel（OpenAI 兼容接口，默认模型 qwen-plus，出题速度快且稳定）
 * 生成整套试卷题目，避免 MockInterviewAiService 依赖 KIMI_API_KEY 的问题。
 */
@Slf4j
@Service
public class PaperGenerateAiServiceImpl implements PaperGenerateAiService {

    private final ChatLanguageModel chatLanguageModel;

    public PaperGenerateAiServiceImpl(@Qualifier("paperChatModel") ChatLanguageModel chatLanguageModel) {
        this.chatLanguageModel = chatLanguageModel;
    }

    @Override
    public List<QuestionImportVo> generatePaperQuestions(AiGenerateRequestVo request) {
        // 1. 构建整卷出题提示词
        String prompt = buildPaperPrompt(request);
        // 2. 调用 DashScope 大模型
        String content = chatLanguageModel.chat(prompt);
        log.debug("AI 生成试卷的原始返回：{}", content);
        if (content == null || content.isEmpty()) {
            throw new RuntimeException("AI 生成试卷失败：模型没有返回任何内容！");
        }
        // 3. 解析 JSON（去掉 ```json 代码块包裹）
        String realResult = extractJson(content);
        JSONObject jsonObject = JSON.parseObject(realResult);
        if (jsonObject == null) {
            throw new RuntimeException("AI 生成试卷失败：返回内容不是合法 JSON！原始内容：" + content);
        }
        JSONArray questions = jsonObject.getJSONArray("questions");
        // 4. 解析每道题
        List<QuestionImportVo> questionImportVoList = new ArrayList<>();
        if (questions != null) {
            for (int i = 0; i < questions.size(); i++) {
                JSONObject questionJson = questions.getJSONObject(i);
                QuestionImportVo questionImportVo = new QuestionImportVo();
                questionImportVo.setTitle(questionJson.getString("title"));
                questionImportVo.setType(questionJson.getString("type"));
                questionImportVo.setMulti(questionJson.getBoolean("multi"));
                questionImportVo.setDifficulty(questionJson.getString("difficulty"));
                questionImportVo.setScore(questionJson.getInteger("score"));
                questionImportVo.setAnalysis(questionJson.getString("analysis"));
                questionImportVo.setKeywords(questionJson.getString("keywords"));

                // 选择题选项处理
                if ("CHOICE".equals(questionImportVo.getType())) {
                    JSONArray choices = questionJson.getJSONArray("choices");
                    List<QuestionImportVo.ChoiceImportDto> choiceImportDtoList = new ArrayList<>();
                    if (choices != null) {
                        for (int j = 0; j < choices.size(); j++) {
                            JSONObject choiceJson = choices.getJSONObject(j);
                            QuestionImportVo.ChoiceImportDto choiceImportDto = new QuestionImportVo.ChoiceImportDto();
                            choiceImportDto.setContent(choiceJson.getString("content"));
                            choiceImportDto.setIsCorrect(choiceJson.getBoolean("isCorrect"));
                            choiceImportDto.setSort(choiceJson.getInteger("sort") != null ? choiceJson.getInteger("sort") : j);
                            choiceImportDtoList.add(choiceImportDto);
                        }
                    }
                    questionImportVo.setChoices(choiceImportDtoList);
                }
                questionImportVo.setAnswer(questionJson.getString("answer"));
                questionImportVoList.add(questionImportVo);
            }
        }
        if (questionImportVoList.isEmpty()) {
            throw new RuntimeException("AI 生成试卷失败：未能解析出任何题目！");
        }
        return questionImportVoList;
    }

    /**
     * 构建生成整套试卷的提示词
     */
    private String buildPaperPrompt(AiGenerateRequestVo request) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是出题专家，请为我生成一套关于【").append(request.getTopic()).append("】的完整试卷，共 ")
                .append(request.getCount()).append(" 道题目。\n\n");

        prompt.append("要求：\n");
        if (request.getTypes() != null && !request.getTypes().isEmpty()) {
            StringBuilder typeText = new StringBuilder();
            for (String type : Arrays.asList(request.getTypes().split(","))) {
                switch (type.trim()) {
                    case "CHOICE" -> typeText.append("选择题 ");
                    case "JUDGE" -> typeText.append("判断题 ");
                    case "TEXT" -> typeText.append("简答题 ");
                }
            }
            prompt.append("- 题目类型：").append(typeText.toString().trim()).append("\n");
        }
        if (request.getDifficulty() != null) {
            String difficultyText = switch (request.getDifficulty()) {
                case "EASY" -> "简单";
                case "MEDIUM" -> "中等";
                case "HARD" -> "困难";
                default -> "中等";
            };
            prompt.append("- 难度等级：").append(difficultyText).append("\n");
        }
        if (request.getRequirements() != null && !request.getRequirements().isEmpty()) {
            prompt.append("- 特殊要求：").append(request.getRequirements()).append("\n");
        }

        prompt.append("\n请严格按照以下 JSON 格式返回，不要包含任何其他文字：\n");
        prompt.append("```json\n");
        prompt.append("{\n");
        prompt.append("  \"questions\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"title\": \"题目内容\",\n");
        prompt.append("      \"type\": \"CHOICE|JUDGE|TEXT\",\n");
        prompt.append("      \"multi\": false,\n");
        prompt.append("      \"difficulty\": \"EASY|MEDIUM|HARD\",\n");
        prompt.append("      \"score\": 因难度而定(建议单选5分、多选10分、判断5分、简答10分),\n");
        prompt.append("      \"choices\": [\n");
        prompt.append("        {\"content\": \"选项内容\", \"isCorrect\": true, \"sort\": 0}\n");
        prompt.append("      ],\n");
        prompt.append("      \"answer\": \"TRUE|FALSE(判断题专用)|文本答案(简答题专用)\",\n");
        prompt.append("      \"keywords\": \"简答题的评分关键词，逗号分隔(仅简答题需要)\",\n");
        prompt.append("      \"analysis\": \"题目解析\"\n");
        prompt.append("    }\n");
        prompt.append("  ]\n");
        prompt.append("}\n");
        prompt.append("```\n\n");

        prompt.append("注意：\n");
        prompt.append("1. 选择题必须有 4 个 choices 选项，且每个选项的 sort 从 0 开始递增；多选题 multi 设为 true\n");
        prompt.append("2. 判断题必须同时提供 answer 字段（TRUE 或 FALSE），且正确与错误的答案数量尽量均衡\n");
        prompt.append("3. 简答题提供 answer（参考答案）和 keywords（评分关键词）\n");
        prompt.append("4. 每道题都要有详细的 analysis 解析\n");
        prompt.append("5. 题目要有实际价值，贴近真实应用场景，不要重复\n");
        prompt.append("6. 严格按照 JSON 格式返回，确保可以正确解析\n");

        return prompt.toString();
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
        int braceStart = content.indexOf("{");
        int braceEnd = content.lastIndexOf("}");
        if (braceStart != -1 && braceEnd > braceStart) {
            return content.substring(braceStart, braceEnd + 1);
        }
        return content.trim();
    }
}