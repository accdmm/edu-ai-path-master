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
 * AI 学习路径节点实体类
 * PAPER 节点通过 exam_records 自动判定完成；KNOWLEDGE/QUESTION 手动打勾
 */
@Data
@TableName("learning_path_node")
@Schema(description = "AI 学习路径节点")
public class LearningPathNode implements Serializable {

    @Schema(description = "主键")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "路径ID")
    private Long pathId;

    @Schema(description = "阶段序号（从1开始）")
    private Integer phase;

    @Schema(description = "阶段标题")
    private String phaseTitle;

    @Schema(description = "节点在路径内的全局顺序")
    private Integer sortOrder;

    @Schema(description = "节点类型：KNOWLEDGE/PAPER/QUESTION")
    private String nodeType;

    @Schema(description = "关联实体ID：PAPER=paper.id / QUESTION=interview_question.id / KNOWLEDGE=NULL")
    private Long refId;

    @Schema(description = "关联知识点分类名（KNOWLEDGE 必填）")
    private String categoryName;

    @Schema(description = "节点标题")
    private String title;

    @Schema(description = "节点描述/讲解文本/完成指引")
    private String description;

    @Schema(description = "完成状态：PENDING/COMPLETED")
    private String status;

    @Schema(description = "创建时间")
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @Schema(description = "修改时间")
    private Date updateTime;
}
