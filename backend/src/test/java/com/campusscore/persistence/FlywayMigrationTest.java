package com.campusscore.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashSet;
import java.util.Set;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;

/**
 * Verifies that V1..V4 apply cleanly and yield the schema promised in
 * data-model.md (§ 实体与字段, § 迁移策略).
 *
 * <p>Only V1..V4 are applied here (production baseline). V5 dev seed is
 * intentionally excluded so structural assertions are not coupled to sample
 * data. dev seed is covered separately by {@code FlywayDevSeedTest} later.
 */
class FlywayMigrationTest {

    @SuppressWarnings("resource")
    private static final MySQLContainer<?> mysql =
            new MySQLContainer<>("mysql:8.0.33")
                    .withDatabaseName("smxy_class")
                    .withUsername("test")
                    .withPassword("test")
                    .withUrlParam("characterEncoding", "UTF-8")
                    .withUrlParam("serverTimezone", "UTC");

    private static Flyway flyway;

    @BeforeAll
    static void migrate() {
        mysql.start();
        flyway =
                Flyway.configure()
                        .dataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword())
                        .locations("classpath:db/migration")
                        .load();
        flyway.migrate();
    }

    @AfterAll
    static void tearDown() {
        if (mysql.isRunning()) {
            mysql.stop();
        }
    }

    @Test
    void allBusinessTablesExist() throws Exception {
        Set<String> tables = fetchTableNames();
        assertThat(tables)
                .contains("user", "student", "teacher", "subject", "score")
                .doesNotContain("subjectall");
    }

    @Test
    void userTableHasRenamedColumnsAndPasswordHash() throws Exception {
        Set<String> columns = fetchColumnNames("user");
        assertThat(columns)
                .contains(
                        "id",
                        "real_name",
                        "login_name",
                        "password_hash",
                        "role",
                        "created_at",
                        "updated_at")
                .doesNotContain("realName", "loginName", "priviledge", "password");
    }

    @Test
    void scoreTableHasSnakeCaseForeignKeys() throws Exception {
        Set<String> columns = fetchColumnNames("score");
        assertThat(columns)
                .contains("student_id", "subject_id", "teacher_id", "score", "grade")
                .doesNotContain("stu", "subject", "teacher");
    }

    @Test
    void uniqueIndexOnLowerLoginNameExists() throws Exception {
        assertThat(indexExists("user", "uk_user_login_name_lower")).isTrue();
    }

    @Test
    void uniqueIndexOnSubjectTeacherNameExists() throws Exception {
        assertThat(indexExists("subject", "uk_subject_teacher_name")).isTrue();
    }

    @Test
    void uniqueIndexOnScoreStudentSubjectExists() throws Exception {
        assertThat(indexExists("score", "uk_score_student_subject")).isTrue();
    }

    @Test
    void scoreToSubjectForeignKeyIsRestrict() throws Exception {
        String rule = fetchDeleteRule("score", "fk_score_subject");
        assertThat(rule).isEqualToIgnoringCase("RESTRICT");
    }

    // ---------- helpers ----------

    private Connection open() throws Exception {
        return DriverManager.getConnection(
                mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword());
    }

    private Set<String> fetchTableNames() throws Exception {
        Set<String> out = new LinkedHashSet<>();
        try (Connection c = open();
                Statement s = c.createStatement();
                ResultSet rs = s.executeQuery("SHOW TABLES")) {
            while (rs.next()) {
                out.add(rs.getString(1));
            }
        }
        return out;
    }

    private Set<String> fetchColumnNames(String table) throws Exception {
        Set<String> out = new LinkedHashSet<>();
        try (Connection c = open()) {
            DatabaseMetaData md = c.getMetaData();
            try (ResultSet rs = md.getColumns(mysql.getDatabaseName(), null, table, null)) {
                while (rs.next()) {
                    out.add(rs.getString("COLUMN_NAME"));
                }
            }
        }
        return out;
    }

    private boolean indexExists(String table, String indexName) throws Exception {
        try (Connection c = open()) {
            DatabaseMetaData md = c.getMetaData();
            try (ResultSet rs = md.getIndexInfo(mysql.getDatabaseName(), null, table, false, false)) {
                while (rs.next()) {
                    if (indexName.equalsIgnoreCase(rs.getString("INDEX_NAME"))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private String fetchDeleteRule(String table, String fkName) throws Exception {
        try (Connection c = open()) {
            DatabaseMetaData md = c.getMetaData();
            try (ResultSet rs =
                    md.getImportedKeys(mysql.getDatabaseName(), null, table)) {
                while (rs.next()) {
                    if (fkName.equalsIgnoreCase(rs.getString("FK_NAME"))) {
                        short rule = rs.getShort("DELETE_RULE");
                        switch (rule) {
                            case DatabaseMetaData.importedKeyRestrict:
                            case DatabaseMetaData.importedKeyNoAction:
                                return "RESTRICT";
                            case DatabaseMetaData.importedKeyCascade:
                                return "CASCADE";
                            case DatabaseMetaData.importedKeySetNull:
                                return "SET_NULL";
                            case DatabaseMetaData.importedKeySetDefault:
                                return "SET_DEFAULT";
                            default:
                                return "UNKNOWN";
                        }
                    }
                }
            }
        }
        return "NOT_FOUND";
    }
}
