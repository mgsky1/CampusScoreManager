package com.campusscore.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.campusscore.persistence.dto.ScoreListItem;
import com.campusscore.support.SharedMysqlExtension;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.ContextConfiguration;

/** ScoreMapper 学生视角查询 (T058) — Testcontainers MySQL. */
@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = SharedMysqlExtension.class)
class ScoreMapperStudentQueryTest {

    @Autowired ScoreMapper scoreMapper;

    /** dev seed 中 hhh (id=3) 有 2 条成绩：subject 5 (99) + subject 6 (100)。 */
    @Test
    void selectByStudentIdDefaultOrderByGradeDesc() {
        List<ScoreListItem> rows = scoreMapper.selectByStudentId(3L, 0, 50, null);
        assertThat(rows).hasSize(2);
        assertThat(rows)
                .extracting(ScoreListItem::getSubjectName)
                .containsExactlyInAnyOrder("Java EE", "云计算");
        assertThat(rows.get(0).getGrade()).isEqualTo(3);
        assertThat(rows.get(0).getTeacherName()).isEqualTo("伍老师");
    }

    @Test
    void selectByStudentIdOrderByScoreAsc() {
        List<ScoreListItem> rows = scoreMapper.selectByStudentId(3L, 0, 50, "s.score ASC");
        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).getScore()).isEqualTo(99);
        assertThat(rows.get(1).getScore()).isEqualTo(100);
    }

    @Test
    void selectByStudentIdPaginationLimitsRows() {
        List<ScoreListItem> rows = scoreMapper.selectByStudentId(3L, 0, 1, null);
        assertThat(rows).hasSize(1);
        List<ScoreListItem> rows2 = scoreMapper.selectByStudentId(3L, 1, 1, null);
        assertThat(rows2).hasSize(1);
        assertThat(rows.get(0).getId()).isNotEqualTo(rows2.get(0).getId());
    }

    @Test
    void countByStudentIdMatchesSeed() {
        assertThat(scoreMapper.countByStudentId(3L)).isEqualTo(2L);
        assertThat(scoreMapper.countByStudentId(6L)).isEqualTo(1L);
        assertThat(scoreMapper.countByStudentId(999L)).isEqualTo(0L);
    }

    @Test
    void selectByStudentIdReturnsEmptyWhenNoScore() {
        assertThat(scoreMapper.selectByStudentId(999L, 0, 50, null)).isEmpty();
    }
}
