package com.atguigu.exam.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 真题管理端查询请求VO
 */
@Data
@Schema(description = "真题管理查询请求")
public class InterviewQuestionManageQueryVo implements Serializable {

    @Schema(description = "企业ID")
    private Long companyId;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "难度：1=简单/2=中等/3=困难")
    private Integer difficulty;

    @Schema(description = "关键词")
    private String keyword;

    @Schema(description = "页码")
    private Integer page = 1;

    @Schema(description = "每页大小")
    private Integer size = 10;
}