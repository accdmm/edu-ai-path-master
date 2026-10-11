package com.atguigu.exam.vo;

import com.atguigu.exam.entity.QuestionChoice;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 错题本列表项 VO - 错题记录 + 题目详情（题干/选项/答案/解析/最近作答）
 */
@Data
@Schema(description = "错题本列表项")
public class WrongQuestionVo {

    @Schema(description = "错题记录ID", example = "1")
    private Long id;

    @Schema(description = "题目ID", example = "1024")
    private Long questionId;

    @Schema(description = "题干内容")
    private String title;

    @Schema(description = "题型：CHOICE/JUDGE/TEXT", example = "CHOICE")
    private String type;

    @Schema(description = "是否多选题", example = "false")
    private Boolean multi;

    @Schema(description = "难度：EASY/MEDIUM/HARD", example = "MEDIUM")
    private String difficulty;

    @Schema(description = "知识点分类ID", example = "3")
    private Long categoryId;

    @Schema(description = "知识点分类名称", example = "Java基础")
    private String categoryName;

    @Schema(description = "选择题选项列表（按 sort 排序）")
    private List<QuestionChoice> choices;

    @Schema(description = "标准答案")
    private String correctAnswer;

    @Schema(description = "题目解析")
    private String analysis;

    @Schema(description = "最近一次作答答案")
    private String userAnswer;

    @Schema(description = "最近一次 AI 批改意见（简答题）")
    private String aiCorrection;

    @Schema(description = "累计答错次数", example = "2")
    private Integer wrongCount;

    @Schema(description = "连续答对次数", example = "0")
    private Integer correctStreak;

    @Schema(description = "掌握状态：UNMASTERED/LEARNING/MASTERED", example = "UNMASTERED")
    private String status;

    @Schema(description = "最近一次答错时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lastWrongTime;
}