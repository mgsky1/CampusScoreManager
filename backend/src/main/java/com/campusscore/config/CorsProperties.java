package com.campusscore.config;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** 绑定 {@code campusscore.cors.*}。 */
@Getter
@Setter
@ConfigurationProperties(prefix = "campusscore.cors")
public class CorsProperties {
    /** 允许的 Origin 白名单。prod profile 不接受通配符 {@code *}。 */
    private List<String> allowedOrigins = new ArrayList<>();
}
