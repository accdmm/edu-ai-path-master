package com.atguigu.exam.utils;

import com.atguigu.exam.entity.Paper;
import com.atguigu.exam.service.AiGeneratedPaperService;
import com.atguigu.exam.vo.AiGenerateRequestVo;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 试卷生成工具 - 供 AI 客服小胡调用，按用户要求智能生成整套试卷并入库
 */
@Slf4j
@Component
public class GeneratePaperUtil {

    @Autowired
    private AiGeneratedPaperService aiGeneratedPaperService;

    @Autowired
    private UserContextUtil userContextUtil;

    @Tool(name = "生成一套完整试卷", value = "根据用户提供的知识点主题、难度和题目数量，调用 AI 智能生成一套完整试卷并保存，返回试卷信息。")
    public String generatePaper(
            @P(value = "知识点主题，必填，如：JVM、集合框架、多线程、Spring Boot", required = true) String topic,
            @P(value = "难度等级，选填，如：简单、中等、困难", required = false) String difficulty,
            @P(value = "题目数量，选填，默认10道，范围1-20", required = false) Integer count) {

        log.info("AI 生成整套试卷 - 主题：{}, 难度：{}, 数量：{}", topic, difficulty, count);

        if (topic == null || topic.trim().isEmpty()) {
            return "请提供需要生成试卷的知识点主题，例如：JVM、集合框架、多线程";
        }

        try {
            Long userId = userContextUtil.getUserId();

            AiGenerateRequestVo request = new AiGenerateRequestVo();
            request.setTopic(topic.trim());
            request.setCount(count == null || count < 1 ? 10 : Math.min(count, 20));
            request.setDifficulty(convertDifficulty(difficulty));
            request.setTypes("CHOICE,JUDGE,TEXT");

            Paper paper = aiGeneratedPaperService.generateAndSave(userId, request);

            return "试卷生成成功！\n"
                    + "试卷名称：" + paper.getName() + "\n"
                    + "题目数量：" + paper.getQuestionCount() + " 道\n"
                    + "总分：" + paper.getTotalScore() + " 分\n"
                    + "考试时长：60 分钟\n"
                    + "试卷ID：" + paper.getId() + "\n"
                    + "您可以回复：【开始考试 试卷" + paper.getId() + "】来打开这张试卷，或者访问页面：/exam/start/" + paper.getId();
        } catch (Exception e) {
            log.error("AI 生成整套试卷失败", e);
            return "生成试卷失败：" + e.getMessage();
        }
    }

    private String convertDifficulty(String difficulty) {
        if (difficulty == null || difficulty.isEmpty()) {
            return "MEDIUM";
        }
        return switch (difficulty.trim()) {
            case "简单", "容易", "EASY" -> "EASY";
            case "困难", "难", "HARD" -> "HARD";
            default -> "MEDIUM";
        };
    }
}