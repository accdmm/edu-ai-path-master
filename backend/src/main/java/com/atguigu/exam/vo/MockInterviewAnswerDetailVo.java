package com.atguigu.exam.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 面试答题详情VO（内嵌 question）
 */
@Data
@Schema(description = "面试答题详情")
public class MockInterviewAnswerDetailVo implements Serializable {

    @Schema(description = "答题记录ID")
    private Long id;

    @Schema(description = "得分")
    private Integer score;

    @Schema(description = "满分")
    private Integer maxScore;

    @Schema(description = "用户答案")
    private String userAnswer;

    @Schema(description = "语音URL")
    private String voiceFileUrl;

    @Schema(description = "AI评价")
    private String aiEvaluation;

    @Schema(description = "技术准确性 1-5")
    private Integer technicalAccuracy;

    @Schema(description = "表达清晰度 1-5")
    private Integer clarity;

    @Schema(description = "逻辑性 1-5")
    private Integer logic;

    @Schema(description = "题目信息")
    private QuestionBriefVo question;

    @Data
    @Schema(description = "题目简要")
    public static class QuestionBriefVo implements Serializable {
        private String direction;
        private String difficultyLevel;
        private String questionContent;
    }
}