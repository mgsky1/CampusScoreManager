package com.campusscore.security;

import com.campusscore.common.ApiResponse;
import com.campusscore.common.ErrorCode;
import com.campusscore.common.exception.AuthException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Collections;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 从 {@code Authorization: Bearer <token>} 头解析 JWT，若合法则往
 * {@link SecurityContextHolder} 注入认证对象。
 *
 * <p>无 token 直接放行，交给后续鉴权链决定是否 401（未认证但访问受保护路径时）。
 * token 非空但无效 / 过期 → 立即写回 {@code 401 / 1401}，不再进入后续 filter。
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader(HEADER);
        if (header == null || !header.startsWith(PREFIX)) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(PREFIX.length()).trim();
        try {
            JwtService.ParsedToken parsed = jwtService.parse(token);
            if (!parsed.isAccess()) {
                throw new AuthException("需要 access token");
            }
            Authentication authentication =
                    new UsernamePasswordAuthenticationToken(
                            new AppUserDetails(
                                    parsed.getUserId(),
                                    parsed.getLoginName(),
                                    null,
                                    parsed.getRole()),
                            null,
                            Collections.singletonList(
                                    new SimpleGrantedAuthority(
                                            "ROLE_" + parsed.getRole().name())));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            chain.doFilter(request, response);
        } catch (AuthException ex) {
            writeUnauthorized(response, ex.getMessage());
        }
    }

    private void writeUnauthorized(HttpServletResponse response, String msg) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        objectMapper.writeValue(
                response.getWriter(),
                ApiResponse.error(ErrorCode.UNAUTHORIZED, msg == null ? "未认证" : msg));
    }
}
