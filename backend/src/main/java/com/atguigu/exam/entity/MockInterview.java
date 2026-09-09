package com.atguigu.exam.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 模拟面试记录实体类
 */
@Data
@TableName("mock_interview")
@Schema(description = "模拟面试记录")
public class MockInterview implements Serializable {

    @Schema(description = "主键")
    @TableField(value = "id", fill = FieldFill.INSERT)
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "技术方向")
    private String direction;

    @Schema(description = "题目数量")
    private Integer questionCount;

    @Schema(description = "难度：easy/medium/hard")
    private String difficulty;

    @Schema(description = "公司类型：large/medium/startup")
    private String companyType;

    @Schema(description = "面试时长(分钟)")
    private Integer duration;

    @Schema(description = "状态：in_progress/completed")
    private String status;

    @Schema(description = "总分")
    private Integer totalScore;

    @Schema(description = "满分")
    private Integer maxScore;

    @Schema(description = "平均分")
    private BigDecimal averageScore;

    @Schema(description = "完成题数")
    private Integer completedQuestions;

    @Schema(description = "开始时间")
    private Date startTime;

    @Schema(description = "结束时间")
    private Date endTime;

    @Schema(description = "面试官总结")
    private String interviewerSummary;

    @Schema(description = "改进建议(JSON数组)")
    private String improvementSuggestions;

    @Schema(description = "创建时间")
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @Schema(description = "修改时间")
    @TableField(fill = FieldFill.UPDATE)
    private Date updateTime;

    @Schema(description = "逻辑删除")
    @TableField("is_deleted")
    @TableLogic
    @JsonIgnore
    private Byte isDeleted;
}