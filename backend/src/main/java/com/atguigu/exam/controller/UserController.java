package com.atguigu.exam.controller;

import com.atguigu.exam.common.Result;
import com.atguigu.exam.dto.UpdatePwdDto;
import com.atguigu.exam.dto.UserRegisterDto;
import com.atguigu.exam.entity.User;
import com.atguigu.exam.service.UserService;
import com.atguigu.exam.vo.LoginRequestVo;
import com.atguigu.exam.vo.LoginResponseVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器 - 处理用户认证和权限管理相关的HTTP请求
 * 包括用户登录、注册、信息查询、密码修改等功能（移植自 edu-ai-path）
 */
@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = "*")
@Slf4j
@Tag(name = "用户管理", description = "用户相关操作，包括登录认证、注册、信息查询等")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "用户通过用户名和密码进行登录验证，返回用户信息和token")
    public Result<LoginResponseVo> login(@RequestBody LoginRequestVo loginRequestVo) {
        LoginResponseVo loginResponseVo = userService.login(loginRequestVo);
        return Result.success(loginResponseVo, "登录成功");
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "新用户注册账号")
    public Result register(@RequestBody UserRegisterDto userRegisterDto) {
        userService.register(userRegisterDto);
        return Result.success("注册成功");
    }

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/info")
    @Operation(summary = "获取用户信息", description = "根据 token 获取当前登录用户信息")
    public Result<User> getUserInfo(@RequestHeader(value = "Authorization", required = false) String token) {
        User user = userService.getUserInfoByToken(token);
        return Result.success(user);
    }

    /**
     * 修改密码
     */
    @PutMapping("/updatePwd")
    @Operation(summary = "修改密码", description = "修改当前用户密码")
    public Result updatePwd(@RequestBody UpdatePwdDto updatePwdDto) {
        userService.updatePassword(updatePwdDto);
        return Result.success("修改密码成功");
    }

    /**
     * 检查用户权限
     */
    @GetMapping("/check-admin/{userId}")
    @Operation(summary = "检查管理员权限", description = "验证指定用户是否具有管理员权限")
    public Result<Boolean> checkAdmin(@PathVariable Long userId) {
        User user = userService.getById(userId);
        boolean isAdmin = user != null && "ADMIN".equalsIgnoreCase(user.getRole());
        return Result.success(isAdmin);
    }
}