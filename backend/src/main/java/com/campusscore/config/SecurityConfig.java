package com.campusscore.config;

import com.campusscore.common.ApiResponse;
import com.campusscore.common.ErrorCode;
import com.campusscore.domain.Role;
import com.campusscore.security.JwtAuthenticationFilter;
import com.campusscore.security.JwtProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Spring Security 主配置：
 *
 * <ul>
 *   <li>无状态（不用 session）；
 *   <li>路径 → 角色映射按 {@code contracts/api.md §0.6}；
 *   <li>{@link JwtAuthenticationFilter} 放在 {@link UsernamePasswordAuthenticationFilter} 之前；
 *   <li>401 / 403 均返回统一 {@link ApiResponse} 信封（{@code code=1401 / 1403}）。
 * </ul>
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    private final ObjectMapper objectMapper;
    private final CorsConfigurationSource corsConfigurationSource;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg)
            throws Exception {
        return cfg.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        reg ->
                                reg
                                        // 公开
                                        .antMatchers(
                                                "/api/v1/auth/login",
                                                "/api/v1/auth/refresh")
                                        .permitAll()
                                        .antMatchers(
                                                "/v3/api-docs/**",
                                                "/swagger-ui/**",
                                                "/swagger-ui.html")
                                        .permitAll()
                                        // 角色矩阵
                                        .antMatchers("/api/v1/student/**")
                                        .hasRole(Role.STUDENT.name())
                                        .antMatchers("/api/v1/teacher/**")
                                        .hasRole(Role.TEACHER.name())
                                        .antMatchers("/api/v1/account/**")
                                        .authenticated()
                                        .antMatchers("/api/v1/auth/logout", "/api/v1/auth/me")
                                        .authenticated()
                                        // 兜底
                                        .anyRequest()
                                        .authenticated())
                .exceptionHandling(
                        eh ->
                                eh.authenticationEntryPoint(
                                                (req, res, ex) -> {
                                                    res.setStatus(401);
                                                    res.setContentType(
                                                            "application/json;charset=UTF-8");
                                                    objectMapper.writeValue(
                                                            res.getWriter(),
                                                            ApiResponse.error(
                                                                    ErrorCode.UNAUTHORIZED,
                                                                    "未认证"));
                                                })
                                        .accessDeniedHandler(
                                                (req, res, ex) -> {
                                                    res.setStatus(403);
                                                    res.setContentType(
                                                            "application/json;charset=UTF-8");
                                                    objectMapper.writeValue(
                                                            res.getWriter(),
                                                            ApiResponse.error(
                                                                    ErrorCode.FORBIDDEN,
                                                                    "无权限"));
                                                }))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
