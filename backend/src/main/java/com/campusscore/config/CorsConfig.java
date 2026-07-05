package com.campusscore.config;

import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * CORS 全局配置。允许 {@code Authorization} 头；prod profile 严禁通配符
 * ({@code *}) — 如配置了 {@code *} 则应用启动直接失败。
 */
@Configuration
@EnableConfigurationProperties(CorsProperties.class)
@RequiredArgsConstructor
public class CorsConfig {

    private final CorsProperties properties;
    private final Environment environment;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        List<String> origins = properties.getAllowedOrigins();

        boolean isProd = Arrays.asList(environment.getActiveProfiles()).contains("prod");
        if (isProd && origins != null && origins.contains("*")) {
            throw new IllegalStateException(
                    "campusscore.cors.allowed-origins 在 prod profile 下禁止使用 '*'。"
                            + " 请在 application-prod.yml 中显式列出允许的 Origin。");
        }

        CorsConfiguration cfg = new CorsConfiguration();
        if (origins != null && !origins.isEmpty()) {
            cfg.setAllowedOrigins(origins);
        }
        cfg.setAllowedMethods(Arrays.asList("GET", "POST", "OPTIONS"));
        cfg.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept"));
        cfg.setExposedHeaders(Arrays.asList("Authorization"));
        cfg.setAllowCredentials(false);
        cfg.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cfg);
        return source;
    }
}
