package com.atguigu.exam.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "用户注册参数")
public class UserRegisterDto {

    @Schema(description = "用户姓名")
    private String username;

    @Schema(description = "用户密码")
    private String password;

    @Schema(description = "确认密码")
    private String confirmPassword;
}