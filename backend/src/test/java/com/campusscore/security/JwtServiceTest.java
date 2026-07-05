package com.campusscore.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campusscore.common.exception.AuthException;
import com.campusscore.domain.Role;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private JwtProperties props;
    private JwtService svc;

    @BeforeEach
    void setUp() {
        props = new JwtProperties();
        props.setSecret("test-secret-test-secret-test-secret-32bytes-min");
        props.setAccessTtlMinutes(30);
        props.setRefreshTtlDays(7);
        props.setIssuer("campusscore-test");
        svc = new JwtService(props);
    }

    @Test
    void generateAccessCarriesSubLoginNameAndRole() {
        String token = svc.generateAccess(42L, "alice", Role.TEACHER);
        JwtService.ParsedToken p = svc.parse(token);
        assertThat(p.getUserId()).isEqualTo(42L);
        assertThat(p.getLoginName()).isEqualTo("alice");
        assertThat(p.getRole()).isEqualTo(Role.TEACHER);
        assertThat(p.isAccess()).isTrue();
        assertThat(p.isRefresh()).isFalse();
        assertThat(p.getExpiresAt())
                .isAfter(Instant.now().plusSeconds(60 * 25))
                .isBefore(Instant.now().plusSeconds(60 * 35));
    }

    @Test
    void generateRefreshHasRefreshType() {
        String token = svc.generateRefresh(99L, "bob", Role.STUDENT);
        JwtService.ParsedToken p = svc.parse(token);
        assertThat(p.getUserId()).isEqualTo(99L);
        assertThat(p.getRole()).isEqualTo(Role.STUDENT);
        assertThat(p.isRefresh()).isTrue();
        assertThat(p.isAccess()).isFalse();
    }

    @Test
    void tamperedTokenThrowsAuthException() {
        String token = svc.generateAccess(1L, "a", Role.STUDENT);
        // flip a char in the payload segment
        String[] parts = token.split("\\.");
        String tampered = parts[0] + "." + parts[1].substring(0, parts[1].length() - 1)
                + (parts[1].endsWith("a") ? "b" : "a") + "." + parts[2];
        assertThatThrownBy(() -> svc.parse(tampered))
                .isInstanceOf(AuthException.class);
    }

    @Test
    void expiredTokenThrowsAuthException() {
        // hand-craft an already-expired token signed with the same key
        byte[] keyBytes = props.getSecret().getBytes(StandardCharsets.UTF_8);
        Instant past = Instant.now().minus(Duration.ofHours(1));
        Map<String, Object> claims = new HashMap<>();
        claims.put("loginName", "x");
        claims.put("role", Role.STUDENT.name());
        claims.put("type", JwtService.TYPE_ACCESS);
        String expired =
                Jwts.builder()
                        .setIssuer(props.getIssuer())
                        .setSubject("7")
                        .setIssuedAt(Date.from(past.minusSeconds(60)))
                        .setExpiration(Date.from(past))
                        .addClaims(claims)
                        .signWith(Keys.hmacShaKeyFor(keyBytes))
                        .compact();
        assertThatThrownBy(() -> svc.parse(expired))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("过期");
    }

    @Test
    void emptyTokenThrowsAuthException() {
        assertThatThrownBy(() -> svc.parse("")).isInstanceOf(AuthException.class);
        assertThatThrownBy(() -> svc.parse(null)).isInstanceOf(AuthException.class);
    }

    @Test
    void tokenSignedWithDifferentKeyIsRejected() {
        // sign a well-formed token with a different key
        byte[] otherKey =
                "another-secret-32-bytes-minimum!!!other".getBytes(StandardCharsets.UTF_8);
        String rogue =
                Jwts.builder()
                        .setSubject("1")
                        .setIssuedAt(Date.from(Instant.now()))
                        .setExpiration(Date.from(Instant.now().plusSeconds(60)))
                        .addClaims(java.util.Collections.singletonMap("type", "access"))
                        .signWith(Keys.hmacShaKeyFor(otherKey))
                        .compact();
        assertThatThrownBy(() -> svc.parse(rogue)).isInstanceOf(AuthException.class);
    }
}
