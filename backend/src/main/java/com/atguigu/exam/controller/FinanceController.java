package com.atguigu.exam.controller;

import com.atguigu.exam.common.Result;
import com.atguigu.exam.entity.FinanceRecord;
import com.atguigu.exam.service.FinanceService;
import com.atguigu.exam.vo.FinanceQueryVo;
import com.atguigu.exam.vo.FinanceReportVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * 后台管理 - 企业财务报表控制器
 * 收支统计 / 明细筛选 / 月度报表 / 年度报表 / Excel 导出
 * （遵循现有管理端约定：SecurityConfig 渐进式放行，接口层不额外鉴权）
 */
@Slf4j
@RestController
@RequestMapping
@CrossOrigin
@Tag(name = "企业财务报表")
public class FinanceController {

    @Autowired
    private FinanceService financeService;

    @Operation(summary = "年度概览", description = "总收入/总支出/净利润/利润率/记录数；year 为空取当前年")
    @GetMapping("/api/admin/finance/overview")
    public Result<FinanceReportVo.OverviewVo> overview(@RequestParam(required = false) Integer year) {
        return Result.success(financeService.overview(year));
    }

    @Operation(summary = "月度报表", description = "指定年度 12 个月的收入/支出/净利润；year 为空取当前年")
    @GetMapping("/api/admin/finance/monthly")
    public Result<List<FinanceReportVo.MonthlyItemVo>> monthly(@RequestParam(required = false) Integer year) {
        return Result.success(financeService.monthly(year));
    }

    @Operation(summary = "年度报表", description = "全部数据按年聚合（升序）")
    @GetMapping("/api/admin/finance/yearly")
    public Result<List<FinanceReportVo.YearlyItemVo>> yearly() {
        return Result.success(financeService.yearly());
    }

    @Operation(summary = "分类构成", description = "按类型（收入/支出）聚合分类金额，用于饼图；year 为空取全部年份")
    @GetMapping("/api/admin/finance/category-stats")
    public Result<List<FinanceReportVo.CategoryStatVo>> categoryStats(@RequestParam(required = false) String recordType,
                                                                      @RequestParam(required = false) Integer year) {
        return Result.success(financeService.categoryStats(recordType, year));
    }

    @Operation(summary = "分类列表", description = "筛选下拉用；recordType 为空取全部")
    @GetMapping("/api/admin/finance/categories")
    public Result<List<String>> categories(@RequestParam(required = false) String recordType) {
        return Result.success(financeService.categories(recordType));
    }

    @Operation(summary = "收支明细分页", description = "支持类型/分类/日期范围/关键词筛选")
    @GetMapping("/api/admin/finance/records")
    public Result<IPage<FinanceRecord>> records(FinanceQueryVo query) {
        return Result.success(financeService.page(query));
    }

    @Operation(summary = "导出收支明细 Excel", description = "与明细列表同筛选条件，含合计行")
    @GetMapping("/api/admin/finance/export")
    public void export(FinanceQueryVo query, HttpServletResponse response) throws IOException {
        byte[] data = financeService.exportExcel(query);
        String fileName = "财务报表_" + new SimpleDateFormat("yyyyMMddHHmm").format(new Date()) + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''"
                + URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20"));
        response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
        response.getOutputStream().write(data);
        response.getOutputStream().flush();
        log.info("导出财务报表：{} 条记录，{} 字节", data.length, data.length);
    }
}