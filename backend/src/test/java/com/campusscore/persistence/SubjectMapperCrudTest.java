package com.campusscore.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.campusscore.domain.Subject;
import com.campusscore.support.SharedMysqlExtension;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.ContextConfiguration;

/** SubjectMapper US4 CRUD 集成测试 (T131)。 */
@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = SharedMysqlExtension.class)
class SubjectMapperCrudTest {

    @Autowired SubjectMapper subjectMapper;

    @Test
    void insertAssignsIdAndPersistsFields() {
        Subject s = Subject.builder()
                .name("测试新增课程_" + System.nanoTime())
                .teacherId(1L).grade(2).build();
        subjectMapper.insert(s);
        assertThat(s.getId()).isNotNull();

        Optional<Subject> back = subjectMapper.findById(s.getId());
        assertThat(back).isPresent();
        assertThat(back.get().getTeacherId()).isEqualTo(1L);
        assertThat(back.get().getGrade()).isEqualTo(2);
        assertThat(back.get().getName()).isEqualTo(s.getName());
    }

    @Test
    void selectMineByTeacherReturnsOnlyOwnedAndPagesCorrectly() {
        // seed：teacher_id=1 (ttt) 有 3 门课 (id=1/2/3)；teacher_id=2 (www) 有 3 门
        List<Subject> page1 = subjectMapper.selectMineByTeacher(1L, null, 0, 2, null);
        assertThat(page1).hasSize(2).allMatch(x -> x.getTeacherId() == 1L);

        List<Subject> page2 = subjectMapper.selectMineByTeacher(1L, null, 2, 2, null);
        assertThat(page2).hasSizeLessThanOrEqualTo(2).allMatch(x -> x.getTeacherId() == 1L);

        long total = subjectMapper.countMineByTeacher(1L, null);
        assertThat(total).isGreaterThanOrEqualTo(3L);
    }

    @Test
    void selectMineByTeacherFiltersByKeyword() {
        List<Subject> hits = subjectMapper.selectMineByTeacher(1L, "数据库", 0, 20, null);
        assertThat(hits).isNotEmpty();
        assertThat(hits).allMatch(x -> x.getName().contains("数据库"));

        long total = subjectMapper.countMineByTeacher(1L, "数据库");
        assertThat(total).isEqualTo(hits.size());
    }

    @Test
    void updateChangesNameAndGrade() {
        Subject s = Subject.builder()
                .name("待更新课程_" + System.nanoTime())
                .teacherId(1L).grade(1).build();
        subjectMapper.insert(s);

        s.setName(s.getName() + "_已改");
        s.setGrade(5);
        int rows = subjectMapper.update(s);
        assertThat(rows).isEqualTo(1);

        Optional<Subject> back = subjectMapper.findById(s.getId());
        assertThat(back).isPresent();
        assertThat(back.get().getName()).endsWith("_已改");
        assertThat(back.get().getGrade()).isEqualTo(5);
        // 教师归属不允许通过 update 变更
        assertThat(back.get().getTeacherId()).isEqualTo(1L);
    }

    @Test
    void existsByTeacherAndNameIsCaseInsensitive() {
        String name = "CaseCheck_" + System.nanoTime();
        Subject s = Subject.builder().name(name).teacherId(1L).grade(2).build();
        subjectMapper.insert(s);

        assertThat(subjectMapper.existsByTeacherAndName(1L, name)).isEqualTo(1L);
        assertThat(subjectMapper.existsByTeacherAndName(1L, name.toUpperCase())).isEqualTo(1L);
        assertThat(subjectMapper.existsByTeacherAndName(1L, name.toLowerCase())).isEqualTo(1L);
        // 不同教师视角不冲突
        assertThat(subjectMapper.existsByTeacherAndName(2L, name)).isEqualTo(0L);
    }

    @Test
    void existsExcludingIdSkipsSelfRow() {
        // DB 表在 (teacher_id, name) 上有唯一约束，所以无法插入同教师同名行；
        // 断言：excludingId=自身时返回 0；使用另一个未占用的名字断言 0；
        // 用不同 excludingId 时若名字确实被占用，则返回 1。
        String name = "Self_" + System.nanoTime();
        Subject s = Subject.builder().name(name).teacherId(1L).grade(2).build();
        subjectMapper.insert(s);
        // 排除自身 → 0
        assertThat(subjectMapper.existsByTeacherAndNameExcludingId(1L, name, s.getId()))
                .isEqualTo(0L);
        // 传一个不存在的 excludingId，该名字仍被占用 → 1
        assertThat(subjectMapper.existsByTeacherAndNameExcludingId(1L, name, -999L))
                .isEqualTo(1L);
        // 不同教师视角不冲突
        assertThat(subjectMapper.existsByTeacherAndNameExcludingId(2L, name, -999L))
                .isEqualTo(0L);
    }

    @Test
    void countScoresBySubjectIdReflectsFkReferences() {
        // seed：subject 5 有一条 score（student 3 → score=99）
        long count5 = subjectMapper.countScoresBySubjectId(5L);
        assertThat(count5).isGreaterThanOrEqualTo(1L);
    }

    @Test
    void deleteByIdRemovesRow() {
        Subject s = Subject.builder()
                .name("待删除_" + System.nanoTime()).teacherId(1L).grade(1).build();
        subjectMapper.insert(s);
        long id = s.getId();
        int rows = subjectMapper.deleteById(id);
        assertThat(rows).isEqualTo(1);
        assertThat(subjectMapper.findById(id)).isEmpty();
    }
}
