package com.atguigu.exam.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 学习分析报告 Vo - AI 分析模块聚合数据
 */
@Schema(description = "学习分析报告")
@Data
public class LearningReportVo {

    @Schema(description = "学习概览统计")
    private OverviewVo overview = new OverviewVo();

    @Schema(description = "成绩趋势（最近考试升序）")
    private List<TrendItemVo> scoreTrend = new ArrayList<>();

    @Schema(description = "知识点掌握度（按题目分类聚合）")
    private List<CategoryMasteryVo> categoryMastery = new ArrayList<>();

    @Schema(description = "能力雷达图数据（知识点掌握度 Top6，0-100）")
    private List<RadarItemVo> radar = new ArrayList<>();

    @Schema(description = "学习概览")
    @Data
    public static class OverviewVo {
        @Schema(description = "考试次数")
        private Integer examCount = 0;

        @Schema(description = "平均得分率（0-100）")
        private Integer avgScoreRate = 0;

        @Schema(description = "答题总数")
        private Integer answerCount = 0;

        @Schema(description = "答对题数（isCorrect=1）")
        private Integer correctCount = 0;

        @Schema(description = "整体正确率（0-100）")
        private Integer correctRate = 0;

        @Schema(description = "AI 生成试卷数量")
        private Integer aiPaperCount = 0;

        @Schema(description = "模拟面试次数")
        private Integer interviewCount = 0;
    }

    @Schema(description = "成绩趋势单项")
    @Data
    public static class TrendItemVo {
        @Schema(description = "考试时间")
        private LocalDateTime examTime;

        @Schema(description = "试卷名称")
        private String paperName;

        @Schema(description = "得分")
        private Integer score;

        @Schema(description = "满分")
        private Integer totalScore;

        @Schema(description = "得分率（0-100）")
        private Integer scoreRate;
    }

    @Schema(description = "知识点掌握度单项")
    @Data
    public static class CategoryMasteryVo {
        @Schema(description = "知识点分类名称")
        private String categoryName;

        @Schema(description = "答题数量")
        private Integer answerCount = 0;

        @Schema(description = "获得分值")
        private Integer earnedScore = 0;

        @Schema(description = "满分值")
        private Integer maxScore = 0;

        @Schema(description = "正确率（0-100，按分值）")
        private Integer correctRate = 0;
    }

    @Schema(description = "雷达图单项")
    @Data
    public static class RadarItemVo {
        @Schema(description = "维度名称")
        private String name;

        @Schema(description = "得分（0-100）")
        private Integer value;
    }
}