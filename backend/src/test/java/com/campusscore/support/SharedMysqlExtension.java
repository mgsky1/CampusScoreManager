package com.campusscore.support;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.testcontainers.containers.MySQLContainer;

/**
 * 单例 MySQL 8 容器 + Flyway V1..V4 迁移，跨测试类复用。
 *
 * <p>用作 {@code @ContextConfiguration(initializers = SharedMysqlExtension.class)}，
 * 会把 {@code spring.datasource.*} 注入到 Spring 环境里；Flyway 自动在应用启动时
 * 跑 V1..V4（{@code spring.flyway.locations=classpath:db/migration}）。
 *
 * <p>容器只启动一次（static 字段 + JVM 停止钩子），后续测试直接复用连接。
 */
public class SharedMysqlExtension
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @SuppressWarnings("resource")
    public static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.0.33")
                    .withDatabaseName("smxy_class_test")
                    .withUsername("test")
                    .withPassword("test")
                    .withUrlParam("characterEncoding", "UTF-8")
                    .withUrlParam("serverTimezone", "UTC")
                    .withReuse(false);

    static {
        MYSQL.start();
        Runtime.getRuntime().addShutdownHook(new Thread(MYSQL::stop));
    }

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
                applicationContext,
                "spring.datasource.url=" + MYSQL.getJdbcUrl(),
                "spring.datasource.username=" + MYSQL.getUsername(),
                "spring.datasource.password=" + MYSQL.getPassword(),
                "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
                "spring.flyway.enabled=true",
                "spring.flyway.locations=classpath:db/migration,classpath:db/migration_dev",
                "spring.flyway.clean-disabled=false");
    }
}
