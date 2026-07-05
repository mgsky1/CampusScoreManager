package com.campusscore.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.campusscore.persistence.dto.SubjectEntryOption;
import com.campusscore.support.SharedMysqlExtension;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.ContextConfiguration;

/** SubjectMapper.findEntryOptions 集成测试 (T084)。 */
@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = SharedMysqlExtension.class)
class SubjectMapperEntryOptionsTest {

    @Autowired SubjectMapper subjectMapper;

    /**
     * seed：hhh(id=3, grade=3) 已录 subject 5 + 6（均属教师 www=2，grade=3）；
     * 教师 www 的 grade=3 课程只有这 2 门 → options = 空 & allEntered=true
     */
    @Test
    void hhhVsTeacherWwwAllEntered() {
        List<SubjectEntryOption> options = subjectMapper.findEntryOptions(2L, 3, 3L);
        assertThat(options).isEmpty();
        assertThat(subjectMapper.hasAnyByTeacherAndGrade(2L, 3)).isTrue();
    }

    /**
     * 教师 ttt(id=1) 在 grade=3 无课程 → hasAny=false，findEntryOptions 也返回空。
     */
    @Test
    void teacherWithoutGradeCoverageHasNoOptions() {
        assertThat(subjectMapper.hasAnyByTeacherAndGrade(1L, 3)).isFalse();
        assertThat(subjectMapper.findEntryOptions(1L, 3, 3L)).isEmpty();
    }

    /**
     * 教师 ttt(1) 在 grade=2 有 subject 2 + 3；
     * 学生 zs(id=4, grade=2) 未录任一门 → 两门都是候选。
     */
    @Test
    void zsHasTwoCandidateSubjectsFromTeacherTtt() {
        List<SubjectEntryOption> options = subjectMapper.findEntryOptions(1L, 2, 4L);
        assertThat(options).extracting(SubjectEntryOption::getSubjectId)
                .containsExactlyInAnyOrder(2L, 3L);
    }

    /**
     * 学生 ls(id=5, grade=3) 已录 subject 2 + 3（均属教师 ttt=1, grade=2）；
     * 从教师 ttt 视角看 grade=3 的选项 → 教师 ttt 在 grade=3 无课程，返回空。
     */
    @Test
    void lsFromTttInGrade3IsEmpty() {
        List<SubjectEntryOption> options = subjectMapper.findEntryOptions(1L, 3, 5L);
        assertThat(options).isEmpty();
    }

    @Test
    void findByIdOwnedByTeacherReturnsWhenOwnedElseEmpty() {
        assertThat(subjectMapper.findByIdOwnedByTeacher(1L, 1L)).isPresent();
        assertThat(subjectMapper.findByIdOwnedByTeacher(1L, 2L)).isEmpty();
    }
}
