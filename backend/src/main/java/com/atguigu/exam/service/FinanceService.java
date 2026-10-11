package com.atguigu.exam.service;

import com.atguigu.exam.entity.FinanceRecord;
import com.atguigu.exam.vo.FinanceQueryVo;
import com.atguigu.exam.vo.FinanceReportVo;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

/**
 * 企业财务报表服务（后台管理）
 */
public interface FinanceService {

    /**
     * 年度概览：总收入/总支出/净利润/利润率/记录数
     *
     * @param year 统计年度，空=当前年
     */
    FinanceReportVo.OverviewVo overview(Integer year);

    /**
     * 月度报表：指定年度 12 个月的收入/支出/净利润
     *
     * @param year 统计年度，空=当前年
     */
    List<FinanceReportVo.MonthlyItemVo> monthly(Integer year);

    /**
     * 年度报表：全部数据按年聚合（升序）
     */
    List<FinanceReportVo.YearlyItemVo> yearly();

    /**
     * 分类构成：指定类型（收入/支出）在指定年度的分类金额汇总，按金额降序
     *
     * @param recordType INCOME/EXPENSE，空=全部
     * @param year       统计年度，空=全部年份
     */
    List<FinanceReportVo.CategoryStatVo> categoryStats(String recordType, Integer year);

    /**
     * 可用分类列表（筛选下拉）
     *
     * @param recordType INCOME/EXPENSE，空=全部
     */
    List<String> categories(String recordType);

    /**
     * 收支明细分页（类型/分类/日期范围/关键词筛选）
     */
    IPage<FinanceRecord> page(FinanceQueryVo query);

    /**
     * 导出 Excel（与明细列表同筛选条件，含合计行）
     */
    byte[] exportExcel(FinanceQueryVo query);
}