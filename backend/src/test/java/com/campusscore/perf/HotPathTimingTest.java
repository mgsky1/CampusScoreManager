package com.campusscore.perf;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import com.campusscore.domain.Role;
import com.campusscore.security.AppUserDetails;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * 热路径耗时基线（tasks.md T172、宪章原则四、spec SC-003/004）。
 *
 * <p>对两个高频端点各调 100 次，断言：
 * <ul>
 *   <li>p95 &lt; 200 ms（{@link #P95_LIMIT_MS}）；
 *   <li>p99 &lt; 500 ms（{@link #P99_LIMIT_MS}）。
 * </ul>
 *
 * <p>把结果写入 {@code backend/target/perf-report.txt}，供 CI 归档。
 *
 * <p>默认在本地开发环境跳过；设置 {@code CAMPUSSCORE_PERF=1} 才启用（避免 CI 抖动）。
 * 需要 dev seed（学生 4=张三、教师 1=ttt 授课 subjectId=1）已就绪。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "CAMPUSSCORE_PERF", matches = "1")
class HotPathTimingTest {

    private static final int ITERATIONS = 100;
    private static final int WARMUP = 20;
    private static final long P95_LIMIT_MS = 200;
    private static final long P99_LIMIT_MS = 500;

    @Autowired private MockMvc mockMvc;

    private AppUserDetails student() {
        return new AppUserDetails(3L, "hhh", null, Role.STUDENT);
    }

    private AppUserDetails teacher() {
        return new AppUserDetails(1L, "ttt", null, Role.TEACHER);
    }

    @Test
    void studentScoresListP95Under200ms() throws Exception {
        // JIT / filter chain 冷启动摆脱：热身 10 次不计时
        for (int i = 0; i < WARMUP; i++) {
            mockMvc.perform(
                    MockMvcRequestBuilders.get("/api/v1/student/scores").with(user(student())));
        }
        long[] times = new long[ITERATIONS];
        for (int i = 0; i < ITERATIONS; i++) {
            long t0 = System.nanoTime();
            mockMvc.perform(
                    MockMvcRequestBuilders.get("/api/v1/student/scores").with(user(student())));
            times[i] = (System.nanoTime() - t0) / 1_000_000L;
        }
        report("GET /api/v1/student/scores", times);
    }

    @Test
    void teacherEntryOptionsP95Under200ms() throws Exception {
        for (int i = 0; i < WARMUP; i++) {
            mockMvc.perform(
                    MockMvcRequestBuilders.get("/api/v1/teacher/students/4/entry-options")
                            .with(user(teacher())));
        }
        long[] times = new long[ITERATIONS];
        for (int i = 0; i < ITERATIONS; i++) {
            long t0 = System.nanoTime();
            mockMvc.perform(
                    MockMvcRequestBuilders.get("/api/v1/teacher/students/4/entry-options")
                            .with(user(teacher())));
            times[i] = (System.nanoTime() - t0) / 1_000_000L;
        }
        report("GET /api/v1/teacher/students/{sid}/entry-options", times);
    }

    private void report(String label, long[] times) throws IOException {
        Arrays.sort(times);
        long p50 = times[(int) (0.50 * ITERATIONS)];
        long p95 = times[(int) (0.95 * ITERATIONS)];
        long p99 = times[(int) (0.99 * ITERATIONS)];
        long max = times[ITERATIONS - 1];

        Path outPath = Paths.get("target/perf-report.txt");
        Files.createDirectories(outPath.getParent());
        String line =
                String.format(
                        "%-60s  n=%d  p50=%dms  p95=%dms  p99=%dms  max=%dms%n",
                        label, ITERATIONS, p50, p95, p99, max);
        Files.write(
                outPath,
                line.getBytes(StandardCharsets.UTF_8),
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.APPEND);
        System.out.print(line);

        org.assertj.core.api.Assertions.assertThat(p95)
                .as("p95 of %s should be < %d ms (actual %d)", label, P95_LIMIT_MS, p95)
                .isLessThan(P95_LIMIT_MS);
        org.assertj.core.api.Assertions.assertThat(p99)
                .as("p99 of %s should be < %d ms (actual %d)", label, P99_LIMIT_MS, p99)
                .isLessThan(P99_LIMIT_MS);
    }
}
