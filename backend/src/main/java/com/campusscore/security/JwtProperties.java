package com.campusscore.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Binds {@code campusscore.jwt.*} properties. */
@Getter
@Setter
@ConfigurationProperties(prefix = "campusscore.jwt")
public class JwtProperties {
    /** HS256 signing key. Must be ≥ 32 chars. */
    private String secret;

    /** Access token TTL (minutes). */
    private int accessTtlMinutes = 30;

    /** Refresh token TTL (days). */
    private int refreshTtlDays = 7;

    /** JWT `iss` claim. */
    private String issuer = "campusscore";
}
