package com.atguigu.exam.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 模拟面试题目VO（前端 start 接口返回）
 */
@Data
@Schema(description = "模拟面试题目")
public class MockInterviewQuestionVo implements Serializable {

    @Schema(description = "题目ID")
    private Long id;

    @Schema(description = "题目标题")
    private String title;

    @Schema(description = "题目内容")
    private String content;

    @Schema(description = "技术方向")
    private String direction;

    @Schema(description = "难度")
    private String difficulty;
}