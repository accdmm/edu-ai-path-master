package com.atguigu.exam.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 单题练习 AI 评分响应VO
 */
@Data
@Schema(description = "单题练习评分结果")
public class PracticeScoreVo implements Serializable {

    @Schema(description = "得分 0-100")
    private Integer score;

    @Schema(description = "AI评价")
    private String comment;
}