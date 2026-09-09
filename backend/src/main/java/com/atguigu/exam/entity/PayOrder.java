package com.atguigu.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 支付宝沙箱支付订单实体类
 */
@Data
@TableName("pay_order")
@Schema(description = "支付订单")
public class PayOrder implements Serializable {

    @Schema(description = "主键")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "商户订单号")
    private String orderNo;

    @Schema(description = "下单用户ID")
    private Long userId;

    @Schema(description = "商品类型：normal/vip/enterprise")
    private String productType;

    @Schema(description = "支付金额（元）")
    private BigDecimal amount;

    @Schema(description = "状态：CREATED/PAID/CLOSED")
    private String status;

    @Schema(description = "支付宝交易号")
    private String tradeNo;

    @Schema(description = "支付时间")
    private Date payTime;

    @Schema(description = "异步回调时间")
    private Date notifyTime;

    @Schema(description = "创建时间")
    private Date createTime;
}