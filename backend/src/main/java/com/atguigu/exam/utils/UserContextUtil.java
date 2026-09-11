package com.atguigu.exam.utils;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.Principal;

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

    /**
     * 当前请求是否携带有效 token（用于需要登录的私有资源访问鉴权）
     * 注意：避免依赖 getUserId() 的回退默认值 1（该回退是答辩演示友好的）
     */
    public boolean isAuthenticated() {
        String bearerToken = request.getHeader("Authorization");
        if (!StringUtils.hasText(bearerToken) || !bearerToken.startsWith("Bearer ")) {
            return false;
        }
        try {
            Long userId = jwtUtil.getUserIdFromToken(bearerToken.substring(7));
            return userId != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 当前请求是否为管理员角色（JwtAuthenticationFilter 已按 token 加载 DB 角色到 authorities）
     * 用于私有资源的管理员放行（如后台编辑任意 DRAFT 试卷）
     */
    public boolean isAdmin() {
        Principal principal = request.getUserPrincipal();
        if (principal instanceof Authentication authentication) {
            for (GrantedAuthority authority : authentication.getAuthorities()) {
                if ("ROLE_ADMIN".equals(authority.getAuthority())) {
                    return true;
                }
            }
        }
        return false;
    }
}