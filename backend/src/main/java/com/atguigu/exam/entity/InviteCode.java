package com.atguigu.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 邀请码实体类
 */
@Data
@TableName("invite_code")
@Schema(description = "邀请码")
public class InviteCode implements Serializable {

    @Schema(description = "主键")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "邀请码")
    private String code;

    @Schema(description = "类型：normal/vip/enterprise")
    private String type;

    @Schema(description = "状态：unused/used/expired")
    private String status;

    @Schema(description = "激活用户ID")
    private Long activedBy;

    @Schema(description = "激活时间")
    private Date activedAt;

    @Schema(description = "过期时间")
    private Date expireTime;

    @Schema(description = "创建时间")
    private Date createTime;
}