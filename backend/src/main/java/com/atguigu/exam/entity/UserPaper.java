package com.atguigu.exam.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户-试卷关联表 - 记录用户私有试卷（AI 生成卷默认仅本人可见）
 */
@Data
@TableName("user_paper")
@Schema(description = "用户试卷关联信息")
public class UserPaper extends BaseEntity {

    @Schema(description = "归属用户ID", example = "1")
    private Long userId;

    @Schema(description = "试卷ID", example = "10")
    private Long paperId;

    @Schema(description = "关联类型：AI_GENERATED / ASSIGNED", example = "AI_GENERATED")
    private String relationType;

    @Schema(description = "试卷信息（非本表字段）")
    @TableField(exist = false)
    private Paper paper;
}