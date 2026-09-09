package com.atguigu.exam.service;

import com.atguigu.exam.dto.UpdatePwdDto;
import com.atguigu.exam.dto.UserRegisterDto;
import com.atguigu.exam.entity.User;
import com.atguigu.exam.vo.LoginResponseVo;
import com.atguigu.exam.vo.LoginRequestVo;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 用户Service接口
 * 定义用户相关的业务方法
 */
public interface UserService extends IService<User> {

    /**
     * 用户登录，认证成功返回包含 token 的登录信息
     *
     * @param loginRequestVo 登录信息（username/password）
     * @return 登录结果（含 token）
     */
    LoginResponseVo login(LoginRequestVo loginRequestVo);

    /**
     * 用户注册
     *
     * @param userRegisterDto 注册信息
     */
    void register(UserRegisterDto userRegisterDto);

    /**
     * 根据 token 获取用户信息
     *
     * @param token JWT token（可能带 Bearer 前缀）
     * @return 用户信息
     */
    User getUserInfoByToken(String token);

    /**
     * 修改密码
     *
     * @param updatePwdDto 修改密码信息
     */
    void updatePassword(UpdatePwdDto updatePwdDto);
}