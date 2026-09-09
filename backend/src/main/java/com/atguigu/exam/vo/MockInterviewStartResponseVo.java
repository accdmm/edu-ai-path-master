package com.atguigu.exam.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 开始模拟面试响应VO
 */
@Data
@Schema(description = "开始模拟面试响应")
public class MockInterviewStartResponseVo implements Serializable {

    @Schema(description = "面试记录ID")
    private Long id;

    @Schema(description = "面试题目列表")
    private List<MockInterviewQuestionVo> questions;
}