package com.atguigu.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 模拟面试答题记录实体类
 */
@Data
@TableName("mock_interview_answer")
@Schema(description = "模拟面试答题记录")
public class MockInterviewAnswer implements Serializable {

    @Schema(description = "主键")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "面试记录ID")
    private Long interviewId;

    @Schema(description = "题目ID")
    private Long questionId;

    @Schema(description = "题目内容快照")
    private String questionContent;

    @Schema(description = "技术方向快照")
    private String direction;

    @Schema(description = "难度快照")
    private String difficultyLevel;

    @Schema(description = "用户答案")
    private String userAnswer;

    @Schema(description = "语音回答URL")
    private String voiceFileUrl;

    @Schema(description = "答题用时(秒)")
    private Integer answerTime;

    @Schema(description = "得分")
    private Integer score;

    @Schema(description = "满分")
    private Integer maxScore;

    @Schema(description = "AI评价")
    private String aiEvaluation;

    @Schema(description = "技术准确性 1-5")
    private Integer technicalAccuracy;

    @Schema(description = "表达清晰度 1-5")
    private Integer clarity;

    @Schema(description = "逻辑性 1-5")
    private Integer logic;

    @Schema(description = "创建时间")
    private Date createTime;
}