package com.atguigu.exam.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 智能组卷结果 VO - 返回试卷ID与实际组成（题型/难度分布、错题与题库来源拆分）
 */
@Data
@Schema(description = "智能组卷结果")
public class SmartPaperVo {

    @Schema(description = "生成的试卷ID（前端跳 /exam/start/{paperId}）", example = "12")
    private Long paperId;

    @Schema(description = "试卷名称", example = "AI智能组卷-10121530")
    private String name;

    @Schema(description = "实际题量", example = "10")
    private Integer questionCount;

    @Schema(description = "试卷总分", example = "50")
    private BigDecimal totalScore;

    @Schema(description = "考试时长（分钟）", example = "30")
    private Integer duration;

    @Schema(description = "其中来自历史错题的题数", example = "3")
    private Integer wrongQuestionCount;

    @Schema(description = "其中来自题库的题数", example = "7")
    private Integer bankQuestionCount;

    @Schema(description = "本次消耗积分", example = "10")
    private Integer cost;

    @Schema(description = "扣费后剩余积分", example = "90")
    private Integer activeCredits;

    @Schema(description = "实际题型分布", example = "{\"CHOICE\":6,\"JUDGE\":2,\"TEXT\":2}")
    private Map<String, Integer> typeBreakdown = new LinkedHashMap<>();

    @Schema(description = "实际难度分布", example = "{\"EASY\":3,\"MEDIUM\":5,\"HARD\":2}")
    private Map<String, Integer> difficultyBreakdown = new LinkedHashMap<>();

    /**
     * 薄弱知识点（错题本按知识点聚合，支撑页面展示"AI 将重点考察什么"）
     */
    @Data
    @Schema(description = "薄弱知识点")
    public static class WeakPoint {

        @Schema(description = "分类ID", example = "2")
        private Long categoryId;

        @Schema(description = "分类名称", example = "Java基础")
        private String categoryName;

        @Schema(description = "未掌握/半掌握错题数", example = "2")
        private Integer wrongCount;
    }
}