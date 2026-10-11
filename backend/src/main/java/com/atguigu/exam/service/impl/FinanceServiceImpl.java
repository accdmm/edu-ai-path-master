package com.atguigu.exam.service.impl;

import com.atguigu.exam.entity.FinanceRecord;
import com.atguigu.exam.mapper.FinanceRecordMapper;
import com.atguigu.exam.service.FinanceService;
import com.atguigu.exam.vo.FinanceQueryVo;
import com.atguigu.exam.vo.FinanceReportVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * 企业财务报表服务实现
 * <p>
 * 数据源为 finance_record 财务台账（收入/支出流水）；数据量为千级以内，
 * 采用「条件查询 + 代码层聚合」口径，与学习分析模块一致，避免数据库方言差异。
 */
@Slf4j
@Service
public class FinanceServiceImpl implements FinanceService {

    private static final String TYPE_INCOME = "INCOME";
    private static final String TYPE_EXPENSE = "EXPENSE";
    /** 导出上限（防御异常大数据量） */
    private static final int EXPORT_LIMIT = 10000;

    @Autowired
    private FinanceRecordMapper financeRecordMapper;

    @Override
    public FinanceReportVo.OverviewVo overview(Integer year) {
        int targetYear = year == null ? Year.now().getValue() : year;
        List<FinanceRecord> records = listByYear(targetYear);

        FinanceReportVo.OverviewVo vo = new FinanceReportVo.OverviewVo();
        vo.setYear(targetYear);
        BigDecimal income = sumByType(records, TYPE_INCOME);
        BigDecimal expense = sumByType(records, TYPE_EXPENSE);
        vo.setTotalIncome(income);
        vo.setTotalExpense(expense);
        BigDecimal profit = income.subtract(expense);
        vo.setProfit(profit);
        vo.setProfitRate(income.compareTo(BigDecimal.ZERO) > 0
                ? profit.multiply(BigDecimal.valueOf(100)).divide(income, 0, RoundingMode.HALF_UP).intValue()
                : 0);
        vo.setRecordCount(records.size());
        return vo;
    }

    @Override
    public List<FinanceReportVo.MonthlyItemVo> monthly(Integer year) {
        int targetYear = year == null ? Year.now().getValue() : year;
        List<FinanceRecord> records = listByYear(targetYear);
        Map<Integer, List<FinanceRecord>> byMonth = records.stream()
                .filter(r -> r.getRecordDate() != null)
                .collect(Collectors.groupingBy(r -> r.getRecordDate().getMonthValue()));

        List<FinanceReportVo.MonthlyItemVo> result = new ArrayList<>(12);
        for (int month = 1; month <= 12; month++) {
            List<FinanceRecord> monthRecords = byMonth.getOrDefault(month, new ArrayList<>());
            BigDecimal income = sumByType(monthRecords, TYPE_INCOME);
            BigDecimal expense = sumByType(monthRecords, TYPE_EXPENSE);
            FinanceReportVo.MonthlyItemVo item = new FinanceReportVo.MonthlyItemVo();
            item.setMonth(String.format("%d-%02d", targetYear, month));
            item.setIncome(income);
            item.setExpense(expense);
            item.setProfit(income.subtract(expense));
            result.add(item);
        }
        return result;
    }

    @Override
    public List<FinanceReportVo.YearlyItemVo> yearly() {
        List<FinanceRecord> all = financeRecordMapper.selectList(
                new LambdaQueryWrapper<FinanceRecord>().isNotNull(FinanceRecord::getRecordDate));
        Map<Integer, List<FinanceRecord>> byYear = new TreeMap<>(all.stream()
                .collect(Collectors.groupingBy(r -> r.getRecordDate().getYear())));

        List<FinanceReportVo.YearlyItemVo> result = new ArrayList<>(byYear.size());
        for (Map.Entry<Integer, List<FinanceRecord>> entry : byYear.entrySet()) {
            BigDecimal income = sumByType(entry.getValue(), TYPE_INCOME);
            BigDecimal expense = sumByType(entry.getValue(), TYPE_EXPENSE);
            FinanceReportVo.YearlyItemVo item = new FinanceReportVo.YearlyItemVo();
            item.setYear(entry.getKey());
            item.setIncome(income);
            item.setExpense(expense);
            item.setProfit(income.subtract(expense));
            result.add(item);
        }
        return result;
    }

    @Override
    public List<FinanceReportVo.CategoryStatVo> categoryStats(String recordType, Integer year) {
        LambdaQueryWrapper<FinanceRecord> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(recordType)) {
            wrapper.eq(FinanceRecord::getRecordType, recordType);
        }
        if (year != null) {
            wrapper.ge(FinanceRecord::getRecordDate, LocalDate.of(year, 1, 1))
                    .le(FinanceRecord::getRecordDate, LocalDate.of(year, 12, 31));
        }
        List<FinanceRecord> records = financeRecordMapper.selectList(wrapper);

        Map<String, FinanceReportVo.CategoryStatVo> statMap = new LinkedHashMap<>();
        for (FinanceRecord record : records) {
            String category = StringUtils.hasText(record.getCategory()) ? record.getCategory() : "未分类";
            FinanceReportVo.CategoryStatVo stat = statMap.computeIfAbsent(category, name -> {
                FinanceReportVo.CategoryStatVo vo = new FinanceReportVo.CategoryStatVo();
                vo.setCategory(name);
                vo.setAmount(BigDecimal.ZERO);
                vo.setCount(0);
                return vo;
            });
            BigDecimal amount = record.getAmount() == null ? BigDecimal.ZERO : record.getAmount();
            stat.setAmount(stat.getAmount().add(amount));
            stat.setCount(stat.getCount() + 1);
        }
        return statMap.values().stream()
                .sorted(Comparator.comparing(FinanceReportVo.CategoryStatVo::getAmount).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public List<String> categories(String recordType) {
        LambdaQueryWrapper<FinanceRecord> wrapper = new LambdaQueryWrapper<FinanceRecord>()
                .select(FinanceRecord::getCategory)
                .groupBy(FinanceRecord::getCategory)
                .orderByAsc(FinanceRecord::getCategory);
        if (StringUtils.hasText(recordType)) {
            wrapper.eq(FinanceRecord::getRecordType, recordType);
        }
        return financeRecordMapper.selectList(wrapper).stream()
                .map(FinanceRecord::getCategory)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
    }

    @Override
    public IPage<FinanceRecord> page(FinanceQueryVo query) {
        int pageNo = query == null || query.getPage() == null || query.getPage() < 1 ? 1 : query.getPage();
        int pageSize = query == null || query.getSize() == null || query.getSize() < 1 ? 20 : query.getSize();
        Page<FinanceRecord> page = new Page<>(pageNo, pageSize);
        return financeRecordMapper.selectPage(page, buildWrapper(query));
    }

    @Override
    public byte[] exportExcel(FinanceQueryVo query) {
        List<FinanceRecord> records = financeRecordMapper.selectList(
                buildWrapper(query).last("LIMIT " + EXPORT_LIMIT));
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("收支明细");

            // 表头样式：浅蓝底 + 加粗 + 居中
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            setThinBorder(headerStyle);

            // 金额样式：千分位两位小数
            CellStyle moneyStyle = workbook.createCellStyle();
            moneyStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));
            setThinBorder(moneyStyle);

            String[] headers = {"序号", "流水号", "日期", "类型", "分类", "金额(元)", "结算方式", "备注"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIndex = 1;
            int seq = 1;
            for (FinanceRecord record : records) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(seq++);
                row.createCell(1).setCellValue(nvl(record.getRecordNo()));
                row.createCell(2).setCellValue(record.getRecordDate() == null ? "" : record.getRecordDate().toString());
                row.createCell(3).setCellValue(typeText(record.getRecordType()));
                row.createCell(4).setCellValue(nvl(record.getCategory()));
                Cell amountCell = row.createCell(5);
                amountCell.setCellValue(record.getAmount() == null ? 0D : record.getAmount().doubleValue());
                amountCell.setCellStyle(moneyStyle);
                row.createCell(6).setCellValue(methodText(record.getPaymentMethod()));
                row.createCell(7).setCellValue(nvl(record.getRemark()));
            }

            // 合计区（收入/支出/净利润）
            BigDecimal income = sumByType(records, TYPE_INCOME);
            BigDecimal expense = sumByType(records, TYPE_EXPENSE);
            rowIndex++;
            rowIndex = appendSummaryRow(sheet, rowIndex, "合计收入", income, moneyStyle);
            rowIndex = appendSummaryRow(sheet, rowIndex, "合计支出", expense, moneyStyle);
            appendSummaryRow(sheet, rowIndex, "净利润", income.subtract(expense), moneyStyle);

            int[] widths = {6 * 256, 18 * 256, 12 * 256, 8 * 256, 18 * 256, 14 * 256, 12 * 256, 30 * 256};
            for (int i = 0; i < widths.length; i++) {
                sheet.setColumnWidth(i, widths[i]);
            }

            workbook.write(bos);
            return bos.toByteArray();
        } catch (IOException e) {
            log.error("导出财务报表失败：{}", e.getMessage());
            throw new RuntimeException("导出财务报表失败：" + e.getMessage());
        }
    }

    /**
     * 构建筛选条件（明细分页与导出共用），默认按日期倒序
     */
    private LambdaQueryWrapper<FinanceRecord> buildWrapper(FinanceQueryVo query) {
        LambdaQueryWrapper<FinanceRecord> wrapper = new LambdaQueryWrapper<>();
        if (query != null) {
            if (StringUtils.hasText(query.getRecordType())) {
                wrapper.eq(FinanceRecord::getRecordType, query.getRecordType());
            }
            if (StringUtils.hasText(query.getCategory())) {
                wrapper.eq(FinanceRecord::getCategory, query.getCategory());
            }
            LocalDate start = parseDate(query.getStartDate());
            if (start != null) {
                wrapper.ge(FinanceRecord::getRecordDate, start);
            }
            LocalDate end = parseDate(query.getEndDate());
            if (end != null) {
                wrapper.le(FinanceRecord::getRecordDate, end);
            }
            if (StringUtils.hasText(query.getKeyword())) {
                String keyword = query.getKeyword().trim();
                wrapper.and(w -> w.like(FinanceRecord::getRemark, keyword)
                        .or().like(FinanceRecord::getRecordNo, keyword)
                        .or().like(FinanceRecord::getCategory, keyword));
            }
        }
        wrapper.orderByDesc(FinanceRecord::getRecordDate).orderByDesc(FinanceRecord::getId);
        return wrapper;
    }

    private List<FinanceRecord> listByYear(int year) {
        return financeRecordMapper.selectList(new LambdaQueryWrapper<FinanceRecord>()
                .ge(FinanceRecord::getRecordDate, LocalDate.of(year, 1, 1))
                .le(FinanceRecord::getRecordDate, LocalDate.of(year, 12, 31)));
    }

    private BigDecimal sumByType(List<FinanceRecord> records, String type) {
        return records.stream()
                .filter(r -> type.equals(r.getRecordType()))
                .map(r -> r.getAmount() == null ? BigDecimal.ZERO : r.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private int appendSummaryRow(Sheet sheet, int rowIndex, String label, BigDecimal value, CellStyle moneyStyle) {
        Row row = sheet.createRow(rowIndex);
        row.createCell(4).setCellValue(label);
        Cell cell = row.createCell(5);
        cell.setCellValue(value.doubleValue());
        cell.setCellStyle(moneyStyle);
        return rowIndex + 1;
    }

    private void setThinBorder(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

    private LocalDate parseDate(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        try {
            return LocalDate.parse(text.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private String typeText(String type) {
        if (TYPE_INCOME.equals(type)) {
            return "收入";
        }
        if (TYPE_EXPENSE.equals(type)) {
            return "支出";
        }
        return nvl(type);
    }

    private String methodText(String method) {
        if (!StringUtils.hasText(method)) {
            return "-";
        }
        return switch (method) {
            case "ALIPAY" -> "支付宝";
            case "BANK" -> "银行转账";
            case "CASH" -> "现金";
            default -> method;
        };
    }

    private String nvl(String text) {
        return text == null ? "" : text;
    }
}