package com.atguigu.exam.utils;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 当前登录用户工具类
 * 从 Authorization: Bearer 头解析 JWT 获取用户 ID；无 token 时回退默认用户 1（答辩演示友好）
 */
@Component
public class UserContextUtil {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private HttpServletRequest request;

    public Long getUserId() {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(7);
            try {
                Long userId = jwtUtil.getUserIdFromToken(token);
                if (userId != null) {
                    return userId;
                }
            } catch (Exception ignored) {
            }
        }
        return 1L;
    }
}