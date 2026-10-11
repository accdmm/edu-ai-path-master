package com.atguigu.exam.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 财务台账查询条件（明细列表 + 导出共用）
 */
@Data
@Schema(description = "财务台账查询条件")
public class FinanceQueryVo {

    @Schema(description = "类型：INCOME/EXPENSE，空=全部")
    private String recordType;

    @Schema(description = "分类名，空=全部")
    private String category;

    @Schema(description = "开始日期（yyyy-MM-dd）")
    private String startDate;

    @Schema(description = "结束日期（yyyy-MM-dd）")
    private String endDate;

    @Schema(description = "关键词（流水号/分类/备注模糊匹配）")
    private String keyword;

    @Schema(description = "页码，从 1 开始")
    private Integer page = 1;

    @Schema(description = "每页大小")
    private Integer size = 20;
}