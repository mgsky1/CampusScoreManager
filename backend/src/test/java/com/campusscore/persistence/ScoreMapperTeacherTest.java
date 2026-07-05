package com.campusscore.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.campusscore.domain.Score;
import com.campusscore.persistence.dto.TeacherScoreItem;
import com.campusscore.support.SharedMysqlExtension;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.ContextConfiguration;

/** ScoreMapper 教师视角测试 (T084)。 */
@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = SharedMysqlExtension.class)
class ScoreMapperTeacherTest {

    @Autowired ScoreMapper scoreMapper;

    /**
     * seed：学生 hhh(id=3) 有 2 条成绩，均属教师 www(id=2)。
     * 从教师 ttt(id=1) 视角看这 2 条都不可编辑。
     */
    @Test
    void editableFalseWhenScoreBelongsToAnotherTeacher() {
        List<TeacherScoreItem> rows =
                scoreMapper.selectByStudentIdForTeacher(3L, 1L, 0, 50, null);
        assertThat(rows).hasSize(2);
        assertThat(rows).allMatch(r -> !r.isEditable());
    }

    /** 教师 www(id=2) 视角下同样的 2 条都是 editable=true。 */
    @Test
    void editableTrueWhenScoreBelongsToCurrentTeacher() {
        List<TeacherScoreItem> rows =
                scoreMapper.selectByStudentIdForTeacher(3L, 2L, 0, 50, null);
        assertThat(rows).hasSize(2);
        assertThat(rows).allMatch(TeacherScoreItem::isEditable);
    }

    @Test
    void existsByStudentAndSubjectMatchesSeed() {
        // hhh(3) 在 subject 5 有分数
        assertThat(scoreMapper.existsByStudentAndSubject(3L, 5L)).isTrue();
        // hhh(3) 在 subject 1 无分数
        assertThat(scoreMapper.existsByStudentAndSubject(3L, 1L)).isFalse();
    }

    @Test
    void insertAndFindByIdRoundTrip() {
        // 使用未被 seed 占用的组合：ww(id=6) × 汇编语言(subject 2, teacher 1)
        Score s = Score.builder()
                .studentId(6L)
                .subjectId(2L)
                .teacherId(1L)
                .score(88)
                .grade(1)
                .build();
        int rows = scoreMapper.insert(s);
        assertThat(rows).isEqualTo(1);
        assertThat(s.getId()).isNotNull();

        Optional<Score> back = scoreMapper.findById(s.getId());
        assertThat(back).isPresent();
        assertThat(back.get().getScore()).isEqualTo(88);
    }

    @Test
    void updateScoreChangesValue() {
        // seed score id=4：ls (studentId=5) 在 subject 2 (汇编)，score=88
        int rows = scoreMapper.updateScore(4L, 91);
        assertThat(rows).isEqualTo(1);
        assertThat(scoreMapper.findById(4L).get().getScore()).isEqualTo(91);
    }

    @Test
    void deleteByIdRemovesRow() {
        // 用 seed score id=6：ls subject 3
        int deleted = scoreMapper.deleteById(6L);
        assertThat(deleted).isEqualTo(1);
        assertThat(scoreMapper.findById(6L)).isEmpty();
    }
}
