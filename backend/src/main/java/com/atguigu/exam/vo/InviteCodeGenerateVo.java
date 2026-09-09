package com.atguigu.exam.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 邀请码生成请求VO
 */
@Data
@Schema(description = "邀请码生成请求")
public class InviteCodeGenerateVo implements Serializable {

    @Schema(description = "生成数量")
    private Integer count = 1;

    @Schema(description = "类型：normal/vip/enterprise")
    private String type = "normal";

    @Schema(description = "有效天数")
    private Integer validDays = 365;
}