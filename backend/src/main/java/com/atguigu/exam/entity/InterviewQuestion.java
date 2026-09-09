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
 * 企业真题实体类
 */
@Data
@TableName("interview_question")
@Schema(description = "企业真题")
public class InterviewQuestion implements Serializable {

    @Schema(description = "主键")
    @TableField(value = "id", fill = FieldFill.INSERT)
    private Long id;

    @Schema(description = "企业ID")
    private Long companyId;

    @Schema(description = "题集分类ID")
    private Long categoryId;

    @Schema(description = "技术方向：java/frontend/bigdata/algorithm/devops/testing")
    private String direction;

    @Schema(description = "难度：easy/medium/hard")
    private String difficultyLevel;

    @Schema(description = "面试年份")
    private Integer interviewYear;

    @Schema(description = "题目内容")
    private String questionContent;

    @Schema(description = "参考答案")
    private String referenceAnswer;

    @Schema(description = "浏览次数")
    private Integer viewCount;

    @Schema(description = "收藏次数")
    private Integer favoriteCount;

    @Schema(description = "状态：approved/disabled")
    private String status;

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

    @Schema(description = "当前用户是否已收藏")
    @TableField(exist = false)
    private Boolean isFavorited;

    @Schema(description = "企业名称")
    @TableField(exist = false)
    private String companyName;

    @Schema(description = "题集名称")
    @TableField(exist = false)
    private String categoryName;
}