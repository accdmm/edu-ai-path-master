package com.atguigu.exam.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 真题评价请求VO
 */
@Data
@Schema(description = "真题评价请求")
public class InterviewEvaluationVo implements Serializable {

    @Schema(description = "题目ID")
    private Long questionId;

    @Schema(description = "评分 1-5")
    private Integer rating;

    @Schema(description = "评价内容")
    private String comment;
}