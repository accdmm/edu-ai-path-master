package com.atguigu.exam.service.impl;

import com.atguigu.exam.dto.UpdatePwdDto;
import com.atguigu.exam.dto.UserRegisterDto;
import com.atguigu.exam.entity.User;
import com.atguigu.exam.mapper.UserMapper;
import com.atguigu.exam.service.UserService;
import com.atguigu.exam.utils.JwtUtil;
import com.atguigu.exam.vo.LoginRequestVo;
import com.atguigu.exam.vo.LoginResponseVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * 用户Service实现类
 * 实现用户认证、注册、信息查询、密码修改等业务逻辑（移植自 edu-ai-path）
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private AuthenticationManager authenticationManager;

    @Override
    public LoginResponseVo login(LoginRequestVo loginRequestVo) {
        try {
            if (authenticationManager == null) {
                throw new RuntimeException("认证管理器未初始化");
            }

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequestVo.getUsername(), loginRequestVo.getPassword())
            );

            if (!authentication.isAuthenticated()) {
                throw new RuntimeException("用户登录失败");
            }

            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(User::getUsername, loginRequestVo.getUsername());
            User user = this.getOne(queryWrapper);

            if (user == null) {
                throw new RuntimeException("用户不存在");
            }

            String token = jwtUtil.generateToken(user.getId(), user.getUsername());

            log.info("用户登录成功：{}", user.getUsername());

            LoginResponseVo loginResponseVo = new LoginResponseVo();
            loginResponseVo.setUserId(user.getId());
            loginResponseVo.setUsername(user.getUsername());
            loginResponseVo.setRealName(user.getRealName());
            loginResponseVo.setRole(user.getRole());
            loginResponseVo.setToken(token);
            return loginResponseVo;

        } catch (BadCredentialsException e) {
            log.error("用户名或密码错误");
            throw new RuntimeException("用户名或密码错误", e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("用户登录失败", e);
            throw new RuntimeException("用户登录失败：" + e.getMessage(), e);
        }
    }

    @Override
    public void register(UserRegisterDto userRegisterDto) {
        log.info("用户名：{}", userRegisterDto.getUsername());

        if (userRegisterDto.getUsername() == null || userRegisterDto.getUsername().trim().isEmpty()) {
            throw new RuntimeException("用户名不能为空");
        }
        if (userRegisterDto.getPassword() == null || userRegisterDto.getPassword().trim().isEmpty()) {
            throw new RuntimeException("密码不能为空");
        }
        if (!userRegisterDto.getPassword().equals(userRegisterDto.getConfirmPassword())) {
            throw new RuntimeException("两次输入的密码不一致");
        }

        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, userRegisterDto.getUsername());
        User existingUser = this.getOne(queryWrapper);
        if (existingUser != null) {
            log.error("用户名已存在：{}", userRegisterDto.getUsername());
            throw new RuntimeException("用户名已存在");
        }

        User newUser = new User();
        newUser.setUsername(userRegisterDto.getUsername());
        newUser.setPassword(passwordEncoder.encode(userRegisterDto.getPassword()));
        newUser.setRealName(userRegisterDto.getUsername());
        newUser.setRole("USER");
        newUser.setStatus("ACTIVE");
        newUser.setIsDeleted((byte) 0);
        newUser.setCreateTime(new Date());
        newUser.setUpdateTime(new Date());

        boolean saved = this.save(newUser);
        if (saved) {
            log.info("用户注册成功：{}", userRegisterDto.getUsername());
        } else {
            log.error("用户注册失败：{}", userRegisterDto.getUsername());
            throw new RuntimeException("注册失败");
        }
    }

    @Override
    public User getUserInfoByToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new RuntimeException("token 不能为空");
        }
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        Long userId = jwtUtil.getUserIdFromToken(token);
        if (userId == null) {
            log.error("无效的 token");
            throw new RuntimeException("无效的 token");
        }

        User user = this.getById(userId);
        if (user == null) {
            log.error("用户不存在，userId: {}", userId);
            throw new RuntimeException("用户不存在");
        }
        if (user.getIsDeleted() != null && user.getIsDeleted() == 1) {
            log.error("用户已被删除，userId: {}", userId);
            throw new RuntimeException("用户已被删除");
        }

        // 不向前端暴露密码
        user.setPassword(null);
        log.info("获取用户信息成功：{}", user.getUsername());
        return user;
    }

    @Override
    public void updatePassword(UpdatePwdDto updatePwdDto) {
        log.info("=== 开始修改密码 ===");

        if (updatePwdDto.getId() == null) {
            throw new RuntimeException("用户 ID 不能为空");
        }
        if (updatePwdDto.getOldPassword() == null || updatePwdDto.getOldPassword().trim().isEmpty()) {
            throw new RuntimeException("原密码不能为空");
        }
        if (updatePwdDto.getNewPassword() == null || updatePwdDto.getNewPassword().trim().isEmpty()) {
            throw new RuntimeException("新密码不能为空");
        }

        User user = this.getById(updatePwdDto.getId());
        if (user == null) {
            log.error("用户不存在，userId: {}", updatePwdDto.getId());
            throw new RuntimeException("用户不存在");
        }

        if (!passwordEncoder.matches(updatePwdDto.getOldPassword(), user.getPassword())) {
            log.error("原密码错误，userId: {}", updatePwdDto.getId());
            throw new RuntimeException("原密码错误");
        }

        user.setPassword(passwordEncoder.encode(updatePwdDto.getNewPassword()));
        user.setUpdateTime(new Date());

        boolean updated = this.updateById(user);
        if (updated) {
            log.info("修改密码成功，userId: {}", updatePwdDto.getId());
        } else {
            log.error("修改密码失败，userId: {}", updatePwdDto.getId());
            throw new RuntimeException("修改密码失败");
        }
    }

    @Autowired
    public void setAuthenticationManager(AuthenticationConfiguration config) throws Exception {
        this.authenticationManager = config.getAuthenticationManager();
    }
}