package com.atguigu.exam.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

/**
 * 财务收支台账实体类
 * 后台管理「企业财务报表」数据源：收入/支出流水，支撑统计、筛选与导出
 */
@Data
@TableName("finance_record")
@Schema(description = "财务收支台账")
public class FinanceRecord implements Serializable {

    @Schema(description = "主键")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "流水号")
    private String recordNo;

    @Schema(description = "类型：INCOME（收入）/EXPENSE（支出）")
    private String recordType;

    @Schema(description = "分类（如：邀请码销售/服务器与云服务）")
    private String category;

    @Schema(description = "金额（正数，方向由 recordType 表达）")
    private BigDecimal amount;

    @Schema(description = "业务发生日期")
    private LocalDate recordDate;

    @Schema(description = "结算方式：ALIPAY（支付宝）/BANK（银行转账）/CASH（现金）")
    private String paymentMethod;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @Schema(description = "修改时间")
    private Date updateTime;
}