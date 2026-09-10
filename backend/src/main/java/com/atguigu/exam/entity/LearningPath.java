package com.atguigu.exam.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * AI 学习路径实体类（结构化可执行任务清单）
 * 同一用户同时仅一条 ACTIVE，重新生成时旧路径置 OUTDATED 归档
 */
@Data
@TableName("learning_path")
@Schema(description = "AI 学习路径")
public class LearningPath implements Serializable {

    @Schema(description = "主键")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "归属用户ID")
    private Long userId;

    @Schema(description = "状态：ACTIVE/OUTDATED/COMPLETED")
    private String status;

    @Schema(description = "诊断快照JSON：[{categoryName,answerCount,earnedScore,maxScore,correctRate}]")
    private String diagnosisJson;

    @Schema(description = "AI 规划总结")
    private String summary;

    @Schema(description = "节点总数")
    private Integer nodeCount;

    @Schema(description = "创建时间")
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @Schema(description = "修改时间")
    private Date updateTime;
}
