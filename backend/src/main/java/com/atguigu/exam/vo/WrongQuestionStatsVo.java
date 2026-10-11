package com.atguigu.exam.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 错题本统计 VO - 总览 + 按知识点聚合（支撑筛选与巩固练习入口）
 */
@Data
@Schema(description = "错题本统计")
public class WrongQuestionStatsVo {

    @Schema(description = "错题总数", example = "12")
    private Integer total;

    @Schema(description = "未掌握数量", example = "6")
    private Integer unmastered;

    @Schema(description = "半掌握数量（部分正确）", example = "4")
    private Integer learning;

    @Schema(description = "已掌握数量", example = "2")
    private Integer mastered;

    @Schema(description = "按知识点聚合统计（按错题数倒序）")
    private List<CategoryStat> categories = new ArrayList<>();

    /**
     * 单个知识点的错题统计
     */
    @Data
    @Schema(description = "知识点维度错题统计")
    public static class CategoryStat {

        @Schema(description = "分类ID", example = "3")
        private Long categoryId;

        @Schema(description = "分类名称", example = "Java基础")
        private String categoryName;

        @Schema(description = "该知识点错题总数", example = "5")
        private Integer total;

        @Schema(description = "未掌握数量", example = "3")
        private Integer unmastered;

        @Schema(description = "半掌握数量", example = "1")
        private Integer learning;

        @Schema(description = "已掌握数量", example = "1")
        private Integer mastered;
    }
}