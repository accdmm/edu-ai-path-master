package com.atguigu.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 真题评价实体类
 */
@Data
@TableName("interview_evaluation")
@Schema(description = "真题评价")
public class InterviewEvaluation implements Serializable {

    @Schema(description = "主键")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "题目ID")
    private Long questionId;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "评分 1-5")
    private Integer rating;

    @Schema(description = "评价内容")
    private String comment;

    @Schema(description = "创建时间")
    private Date createTime;
}