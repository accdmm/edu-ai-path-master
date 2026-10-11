package com.atguigu.exam.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 企业财务报表 VO（概览 + 月度 + 年度 + 分类构成）
 */
@Data
@Schema(description = "企业财务报表")
public class FinanceReportVo {

    @Schema(description = "年度概览")
    @Data
    public static class OverviewVo {

        @Schema(description = "统计年度")
        private Integer year;

        @Schema(description = "年度总收入")
        private BigDecimal totalIncome = BigDecimal.ZERO;

        @Schema(description = "年度总支出")
        private BigDecimal totalExpense = BigDecimal.ZERO;

        @Schema(description = "年度净利润（收入-支出）")
        private BigDecimal profit = BigDecimal.ZERO;

        @Schema(description = "利润率（净利润/收入，0-100 整数）")
        private Integer profitRate = 0;

        @Schema(description = "年度记录数")
        private Integer recordCount = 0;
    }

    @Schema(description = "月度报表单项")
    @Data
    public static class MonthlyItemVo {

        @Schema(description = "月份（yyyy-MM）")
        private String month;

        @Schema(description = "当月收入")
        private BigDecimal income = BigDecimal.ZERO;

        @Schema(description = "当月支出")
        private BigDecimal expense = BigDecimal.ZERO;

        @Schema(description = "当月净利润")
        private BigDecimal profit = BigDecimal.ZERO;
    }

    @Schema(description = "年度报表单项")
    @Data
    public static class YearlyItemVo {

        @Schema(description = "年份")
        private Integer year;

        @Schema(description = "当年收入")
        private BigDecimal income = BigDecimal.ZERO;

        @Schema(description = "当年支出")
        private BigDecimal expense = BigDecimal.ZERO;

        @Schema(description = "当年净利润")
        private BigDecimal profit = BigDecimal.ZERO;
    }

    @Schema(description = "分类构成单项（饼图/明细）")
    @Data
    public static class CategoryStatVo {

        @Schema(description = "分类名")
        private String category;

        @Schema(description = "金额合计")
        private BigDecimal amount = BigDecimal.ZERO;

        @Schema(description = "记录数")
        private Integer count = 0;
    }
}