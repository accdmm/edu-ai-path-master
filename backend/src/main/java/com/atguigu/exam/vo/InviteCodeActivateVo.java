package com.atguigu.exam.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 邀请码激活请求VO
 */
@Data
@Schema(description = "邀请码激活请求")
public class InviteCodeActivateVo implements Serializable {

    @Schema(description = "邀请码")
    private String code;
}