package com.atguigu.exam.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 题集分类实体类（管理端企业-分类下拉）
 */
@Data
@TableName("interview_question_category")
@Schema(description = "题集分类")
public class InterviewQuestionCategory implements Serializable {

    @Schema(description = "主键")
    @TableField(value = "id", fill = FieldFill.INSERT)
    private Long id;

    @Schema(description = "企业ID")
    private Long companyId;

    @Schema(description = "题集名称")
    private String name;

    @Schema(description = "技术方向")
    private String direction;

    @Schema(description = "难度")
    private String difficulty;

    @Schema(description = "面试年份")
    private Integer year;

    @Schema(description = "轮次")
    private String round;

    @Schema(description = "题目数量")
    private Integer questionCount;

    @Schema(description = "排序")
    private Integer sort;

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