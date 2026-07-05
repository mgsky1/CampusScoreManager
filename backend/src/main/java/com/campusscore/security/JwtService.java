package com.campusscore.security;

import com.campusscore.common.exception.AuthException;
import com.campusscore.domain.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.SecretKey;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * HS256 JWT 签发 / 解析。载入 {@code sub}（用户 id）、{@code loginName}、
 * {@code role}、{@code type}（access/refresh）、{@code iat}、{@code exp}、{@code iss}。
 *
 * <p>验证失败一律抛 {@link AuthException}（{@code 401 / 1401}）。
 */
@Service
@RequiredArgsConstructor
public class JwtService {

    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    private static final String CLAIM_LOGIN_NAME = "loginName";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TYPE = "type";

    private final JwtProperties properties;

    private SecretKey key() {
        String secret = properties.getSecret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "campusscore.jwt.secret must be at least 32 characters");
        }
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccess(long userId, String loginName, Role role) {
        return build(
                userId,
                loginName,
                role,
                TYPE_ACCESS,
                Duration.ofMinutes(properties.getAccessTtlMinutes()));
    }

    public String generateRefresh(long userId, String loginName, Role role) {
        return build(
                userId,
                loginName,
                role,
                TYPE_REFRESH,
                Duration.ofDays(properties.getRefreshTtlDays()));
    }

    private String build(long userId, String loginName, Role role, String type, Duration ttl) {
        Instant now = Instant.now();
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_LOGIN_NAME, loginName);
        claims.put(CLAIM_ROLE, role == null ? null : role.name());
        claims.put(CLAIM_TYPE, type);
        return Jwts.builder()
                .setIssuer(properties.getIssuer())
                .setSubject(Long.toString(userId))
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(ttl)))
                .addClaims(claims)
                .signWith(key())
                .compact();
    }

    /**
     * 解析 token；对篡改 / 无效 / 过期一律抛 {@link AuthException}。
     */
    public ParsedToken parse(String token) {
        if (token == null || token.isEmpty()) {
            throw new AuthException("token 缺失");
        }
        try {
            Claims c = Jwts.parserBuilder().setSigningKey(key()).build().parseClaimsJws(token).getBody();
            long userId = Long.parseLong(c.getSubject());
            String loginName = c.get(CLAIM_LOGIN_NAME, String.class);
            String roleName = c.get(CLAIM_ROLE, String.class);
            String type = c.get(CLAIM_TYPE, String.class);
            Role role = roleName == null ? null : Role.valueOf(roleName);
            return new ParsedToken(userId, loginName, role, type, c.getExpiration().toInstant());
        } catch (ExpiredJwtException ex) {
            throw new AuthException("token 已过期", ex);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new AuthException("token 无效", ex);
        }
    }

    /** 解析结果值对象。 */
    @Getter
    public static class ParsedToken {
        private final long userId;
        private final String loginName;
        private final Role role;
        private final String type;
        private final Instant expiresAt;

        public ParsedToken(
                long userId, String loginName, Role role, String type, Instant expiresAt) {
            this.userId = userId;
            this.loginName = loginName;
            this.role = role;
            this.type = type;
            this.expiresAt = expiresAt;
        }

        public boolean isAccess() {
            return TYPE_ACCESS.equals(type);
        }

        public boolean isRefresh() {
            return TYPE_REFRESH.equals(type);
        }
    }
}
