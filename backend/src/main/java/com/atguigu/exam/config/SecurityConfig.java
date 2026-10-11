package com.atguigu.exam.config;

import com.atguigu.exam.common.Result;
import com.atguigu.exam.filter.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;

/**
 * Spring Security 配置
 *
 * 渐进式安全策略：
 * - 登录/注册/Swagger/学生端接口默认放行（保持现有前端可用）
 * - 仅 AI 客服、用户信息、修改密码等需要登录认证
 * - 管理端写接口暂未强制鉴权，待前端 token 机制就绪后可收紧
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(auth -> auth
                // ✅ 登录注册 & Swagger：公开
                .requestMatchers("/api/user/login", "/api/user/register").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/doc.html",
                        "/webjars/**", "/swagger-resources/**", "/v2/api-docs/**").permitAll()

                // 🔒 需认证：AI 客服、用户信息、修改密码、AI学习分析
                .requestMatchers("/api/chat/**").authenticated()
                .requestMatchers("/api/user/info", "/api/user/updatePwd").authenticated()
                .requestMatchers("/api/analysis/**").authenticated()

                // 🔒 考试与刷题：必须登录（此前匿名即可开始/提交考试，且 userId 会回退为默认用户 1）
                .requestMatchers("/api/exams/**", "/api/exam-records/**").authenticated()
                // 🔒 试卷写操作（创建/修改/删除/AI 组卷）需登录；读接口保持公开以便浏览试卷与排行
                .requestMatchers(HttpMethod.POST, "/api/papers/**").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/papers/**").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/papers/**").authenticated()
                // 🔒 题目写操作（新增/修改/删除/批量导入）需登录；题目浏览保持公开
                .requestMatchers(HttpMethod.POST, "/api/questions/**").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/questions/**").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/questions/**").authenticated()
                // 🔒 按用户维度生成的功能：学习路径 / 模拟面试 / AI 面试官
                .requestMatchers("/api/learning-path/**").authenticated()
                .requestMatchers("/api/mock-interview/**", "/api/ai-interview/**").authenticated()

                // 💳 支付：支付宝服务器回调（无 token）需放行，其余支付接口需登录
                .requestMatchers("/api/pay/alipay/notify").permitAll()
                .requestMatchers("/api/pay/**").authenticated()

                // 🔒 其余接口渐进式放行（含学生端与管理端读接口），后续可收紧
                .anyRequest().permitAll()
            )
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(authenticationEntryPoint())
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 未登录访问受保护接口时，统一返回 Result(code=401) 的 JSON，方便前端拦截处理
     */
    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        ObjectMapper objectMapper = new ObjectMapper();
        return (request, response, authException) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(objectMapper.writeValueAsString(Result.error(401, "未登录或登录已过期")));
        };
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}