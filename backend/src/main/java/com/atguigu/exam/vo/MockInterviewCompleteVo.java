package com.atguigu.exam.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 完成面试响应VO（前端 complete 接口 Object.assign 展开）
 */
@Data
@Schema(description = "完成面试响应")
public class MockInterviewCompleteVo implements Serializable {

    @Schema(description = "面试记录ID")
    private Long id;

    @Schema(description = "总分")
    private Integer totalScore;

    @Schema(description = "满分")
    private Integer maxScore;

    @Schema(description = "平均分")
    private double averageScore;

    @Schema(description = "总题数")
    private Integer totalQuestions;

    @Schema(description = "完成题数")
    private Integer completedQuestions;

    @Schema(description = "用时(分钟)")
    private Integer duration;

    @Schema(description = "详细评分")
    private List<Map<String, Object>> details;

    @Schema(description = "个性化报告（联动答题诊断）：{diagnosis:[{categoryName,correctRate,answerCount}], summary, suggestions:[...]}；无诊断数据时为 null")
    private Map<String, Object> personalizedReport;
}