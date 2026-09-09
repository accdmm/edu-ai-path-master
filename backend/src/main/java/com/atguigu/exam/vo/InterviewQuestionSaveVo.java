package com.atguigu.exam.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 真题管理端保存请求VO
 */
@Data
@Schema(description = "真题保存请求")
public class InterviewQuestionSaveVo implements Serializable {

    @Schema(description = "题目ID（编辑时必填）")
    private Long id;

    @Schema(description = "企业ID")
    private Long companyId;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "题目标题")
    private String title;

    @Schema(description = "题目内容")
    private String content;

    @Schema(description = "难度：1=简单/2=中等/3=困难")
    private Integer difficulty;

    @Schema(description = "参考答案")
    private String answer;

    @Schema(description = "题目解析")
    private String analysis;

    @Schema(description = "标签")
    private String tags;

    @Schema(description = "状态：1=上架/0=下架")
    private Integer status;
}