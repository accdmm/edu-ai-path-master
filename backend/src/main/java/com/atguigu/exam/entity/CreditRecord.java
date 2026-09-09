package com.atguigu.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 积分流水实体类
 */
@Data
@TableName("credit_record")
@Schema(description = "积分流水")
public class CreditRecord implements Serializable {

    @Schema(description = "主键")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "积分变动（正负）")
    private Integer changeAmount;

    @Schema(description = "类型：invite/practice/knowledge/other")
    private String type;

    @Schema(description = "来源描述")
    private String source;

    @Schema(description = "变动后余额")
    private Integer balance;

    @Schema(description = "创建时间")
    private Date createTime;
}