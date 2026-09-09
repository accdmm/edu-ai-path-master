package com.atguigu.exam.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 面试详情响应VO（MockInterviewDetail / InterviewResult 共用基础）
 */
@Data
@Schema(description = "面试详情")
public class MockInterviewDetailVo implements Serializable {

    @Schema(description = "面试记录ID")
    private Long id;

    @Schema(description = "技术方向")
    private String direction;

    @Schema(description = "开始时间")
    private Date startTime;

    @Schema(description = "结束时间")
    private Date endTime;

    @Schema(description = "总题数")
    private Integer totalQuestions;

    @Schema(description = "完成题数")
    private Integer completedQuestions;

    @Schema(description = "用时(分钟)")
    private Integer duration;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "总分")
    private Integer totalScore;

    @Schema(description = "满分")
    private Integer maxScore;

    @Schema(description = "平均分")
    private double averageScore;

    @Schema(description = "答题详情")
    private List<MockInterviewAnswerDetailVo> answers;

    @Schema(description = "面试官总结")
    private String interviewerSummary;

    @Schema(description = "改进建议")
    private List<String> improvementSuggestions;
}