package com.atguigu.exam.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 面试结果页VO（含面试官点评与雷达图）
 */
@Data
@Schema(description = "面试结果")
public class InterviewResultVo implements Serializable {

    @Schema(description = "面试记录ID")
    private Long id;

    @Schema(description = "总分")
    private Integer totalScore;

    @Schema(description = "满分")
    private Integer maxScore;

    @Schema(description = "平均分")
    private double averageScore;

    @Schema(description = "完成题数")
    private Integer completedQuestions;

    @Schema(description = "用时(分钟)")
    private Integer duration;

    @Schema(description = "答题详情")
    private Object answers;

    @Schema(description = "面试官点评")
    private Map<String, Object> interviewerFeedback;

    @Schema(description = "学习建议")
    private java.util.List<Map<String, Object>> learningSuggestions;

    @Schema(description = "能力雷达图")
    private Map<String, Integer> abilityScores;
}