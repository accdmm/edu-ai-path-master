package com.atguigu.exam.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 邀请码申请请求VO
 */
@Data
@Schema(description = "邀请码申请请求")
public class InviteCodeRequestVo implements Serializable {

    @Schema(description = "申请类型：normal/vip/enterprise")
    private String type;

    @Schema(description = "申请理由")
    private String reason;

    @Schema(description = "联系方式")
    private String contact;
}