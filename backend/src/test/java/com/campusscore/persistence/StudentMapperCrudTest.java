package com.campusscore.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.campusscore.domain.Role;
import com.campusscore.domain.Student;
import com.campusscore.domain.User;
import com.campusscore.persistence.dto.StudentListItem;
import com.campusscore.support.SharedMysqlExtension;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.ContextConfiguration;

/** Student / User CRUD 持久层集成测试 (T111)。 */
@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = SharedMysqlExtension.class)
class StudentMapperCrudTest {

    @Autowired UserMapper userMapper;
    @Autowired StudentMapper studentMapper;
    @Autowired ScoreMapper scoreMapper;

    private long insertStudent(String loginName, String realName, int grade) {
        User u = User.builder()
                .loginName(loginName)
                .realName(realName)
                .passwordHash("$2a$10$dummy")
                .role(Role.STUDENT)
                .build();
        userMapper.insert(u);
        assertThat(u.getId()).isNotNull();
        studentMapper.insertStudentRow(Student.builder()
                .id(u.getId())
                .tel("13800138000")
                .address("SMU")
                .grade(grade)
                .build());
        return u.getId();
    }

    @Test
    void insertThenFindById() {
        long id = insertStudent("stu_" + System.nanoTime(), "测试学生", 2);
        Optional<StudentListItem> row = studentMapper.findById(id);
        assertThat(row).isPresent();
        assertThat(row.get().getRealName()).isEqualTo("测试学生");
        assertThat(row.get().getGrade()).isEqualTo(2);
        assertThat(row.get().getTel()).isEqualTo("13800138000");
    }

    @Test
    void updateStudentRowChangesFields() {
        long id = insertStudent("stu_" + System.nanoTime(), "老名字", 1);
        studentMapper.updateStudentRow(Student.builder()
                .id(id).tel("13900139000").address("New Addr").grade(4).build());
        User u = userMapper.findById(id).orElseThrow(() -> new AssertionError("not found"));
        userMapper.updateBasic(User.builder()
                .id(id).loginName(u.getLoginName()).realName("新名字").passwordHash(null).build());

        StudentListItem row = studentMapper.findById(id).orElseThrow(() -> new AssertionError("not found"));
        assertThat(row.getRealName()).isEqualTo("新名字");
        assertThat(row.getTel()).isEqualTo("13900139000");
        assertThat(row.getAddress()).isEqualTo("New Addr");
        assertThat(row.getGrade()).isEqualTo(4);
    }

    @Test
    void deleteByIdCascadesToScoreAndStudentTables() {
        // dev seed: 学生 hhh(id=3) 有 2 条成绩
        long before = scoreMapper.countByStudentId(3L);
        assertThat(before).isEqualTo(2L);

        int deleted = userMapper.deleteById(3L);
        assertThat(deleted).isEqualTo(1);

        assertThat(studentMapper.findById(3L)).isEmpty();
        assertThat(scoreMapper.countByStudentId(3L)).isZero();
    }

    @Test
    void countByLoginNameIgnoreCaseMatchesSeed() {
        // dev seed 存在 zs
        assertThat(userMapper.countByLoginNameIgnoreCase("zs")).isEqualTo(1L);
        assertThat(userMapper.countByLoginNameIgnoreCase("ZS")).isEqualTo(1L);
        assertThat(userMapper.countByLoginNameIgnoreCase("Zs")).isEqualTo(1L);
        assertThat(userMapper.countByLoginNameIgnoreCase("no_such_user_here")).isZero();
    }

    @Test
    void countByLoginNameIgnoreCaseExcludingIdSkipsSelf() {
        // 用 zs 本人 id 排除后应为 0
        Optional<User> zs = userMapper.findByLoginName("zs");
        assertThat(zs).isPresent();
        long zsId = zs.get().getId();
        assertThat(userMapper.countByLoginNameIgnoreCaseExcludingId("ZS", zsId)).isZero();
        // 用其他 id 排除时仍应命中
        assertThat(userMapper.countByLoginNameIgnoreCaseExcludingId("ZS", 999L)).isEqualTo(1L);
    }
}
