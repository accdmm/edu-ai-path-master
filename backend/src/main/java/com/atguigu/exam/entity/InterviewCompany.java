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
 * 企业实体类
 */
@Data
@TableName("interview_company")
@Schema(description = "企业")
public class InterviewCompany implements Serializable {

    @Schema(description = "主键")
    @TableField(value = "id", fill = FieldFill.INSERT)
    private Long id;

    @Schema(description = "企业名称")
    private String name;

    @Schema(description = "企业logo URL")
    private String logo;

    @Schema(description = "企业描述")
    private String description;

    @Schema(description = "是否付费可见：0-否，1-是")
    private Boolean isPremium;

    @Schema(description = "题目总数")
    private Integer totalQuestions;

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