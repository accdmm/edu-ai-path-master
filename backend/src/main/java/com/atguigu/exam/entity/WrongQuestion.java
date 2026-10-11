package com.atguigu.exam.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 错题本实体类
 *
 * 数据来源：判卷结算后由判卷埋点自动采集（答错 is_correct=0 / 部分正确 is_correct=2）
 * 去重规则：唯一键 (user_id, question_id)，同一道题对同一用户仅保留一条
 * 掌握度：UNMASTERED 未掌握 / LEARNING 半掌握 / MASTERED 已掌握
 */
@Data
@TableName("wrong_question")
@Schema(description = "错题本记录")
public class WrongQuestion extends BaseEntity {

    @Schema(description = "归属用户ID", example = "7")
    private Long userId;

    @Schema(description = "题目ID", example = "1024")
    private Long questionId;

    @Schema(description = "知识点分类ID（冗余自 questions.category_id）", example = "3")
    private Long categoryId;

    @Schema(description = "累计答错次数", example = "2")
    private Integer wrongCount;

    @Schema(description = "连续答对次数（达到阈值判定已掌握）", example = "1")
    private Integer correctStreak;

    @Schema(description = "掌握状态：UNMASTERED/LEARNING/MASTERED", example = "UNMASTERED")
    private String status;

    @Schema(description = "最近一次答错时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lastWrongTime;

    @Schema(description = "最近一次采集来源的考试记录ID（用于幂等去重）", example = "88")
    private Long lastExamRecordId;
}