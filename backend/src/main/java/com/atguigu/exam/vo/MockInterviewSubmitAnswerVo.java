package com.atguigu.exam.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 提交面试答案请求VO
 */
@Data
@Schema(description = "提交面试答案请求")
public class MockInterviewSubmitAnswerVo implements Serializable {

    @Schema(description = "面试记录ID（无则视为单题练习）")
    private Long interviewRecordId;

    @Schema(description = "题目ID")
    private Long questionId;

    @Schema(description = "用户答案")
    private String userAnswer;

    @Schema(description = "语音回答URL")
    private String voiceFileUrl;

    @Schema(description = "答题用时(秒)")
    private Integer answerTime;
}