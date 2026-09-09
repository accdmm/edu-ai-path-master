package com.atguigu.exam.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "修改密码参数")
public class UpdatePwdDto {

    @Schema(description = "用户id")
    private Long id;

    @Schema(description = "原来的密码")
    private String oldPassword;

    @Schema(description = "新的密码")
    private String newPassword;
}