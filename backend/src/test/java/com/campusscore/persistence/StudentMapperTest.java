package com.campusscore.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.campusscore.persistence.dto.StudentListItem;
import com.campusscore.support.SharedMysqlExtension;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.ContextConfiguration;

/** StudentMapper 集成测试 (T081)。dev seed 有 4 名学生。 */
@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = SharedMysqlExtension.class)
class StudentMapperTest {

    @Autowired StudentMapper studentMapper;

    @Test
    void searchWithoutKeywordReturnsAllStudents() {
        List<StudentListItem> rows = studentMapper.search(null, 0, 50, null);
        assertThat(rows).hasSize(4);
        assertThat(rows).extracting(StudentListItem::getLoginName)
                .containsExactlyInAnyOrder("hhh", "zs", "ls", "ww");
        assertThat(studentMapper.count(null)).isEqualTo(4L);
    }

    @Test
    void searchWithKeywordUsesLikeOnRealName() {
        List<StudentListItem> hits = studentMapper.search("张", 0, 50, null);
        assertThat(hits).hasSize(1);
        assertThat(hits.get(0).getRealName()).isEqualTo("张三");
        assertThat(studentMapper.count("张")).isEqualTo(1L);
    }

    @Test
    void searchWithBlankKeywordTreatedAsAll() {
        // 空白应由 Service 层归一为 null；这里传空串验证 Mapper 层不会误加 WHERE
        assertThat(studentMapper.count("")).isEqualTo(4L);
    }

    @Test
    void searchPaginationSplitsRows() {
        List<StudentListItem> page1 = studentMapper.search(null, 0, 2, "u.id ASC");
        List<StudentListItem> page2 = studentMapper.search(null, 2, 2, "u.id ASC");
        assertThat(page1).hasSize(2);
        assertThat(page2).hasSize(2);
        assertThat(page1.get(0).getId()).isLessThan(page2.get(0).getId());
    }

    @Test
    void searchOrderByGradeDescPutsHighestFirst() {
        List<StudentListItem> rows = studentMapper.search(null, 0, 10, "st.grade DESC, u.id ASC");
        assertThat(rows.get(0).getGrade()).isEqualTo(3);
        // seed 有 grade=3 的 hhh(3) / ls(5)
    }

    @Test
    void findByIdReturnsFullProfile() {
        Optional<StudentListItem> zs = studentMapper.findById(4L);
        assertThat(zs).isPresent();
        assertThat(zs.get().getLoginName()).isEqualTo("zs");
        assertThat(zs.get().getRealName()).isEqualTo("张三");
        assertThat(zs.get().getGrade()).isEqualTo(2);
    }

    @Test
    void findByIdReturnsEmptyForUnknown() {
        assertThat(studentMapper.findById(9999L)).isEmpty();
    }

    @Test
    void findByIdReturnsEmptyForTeacherId() {
        // id=1 是 TEACHER，不应被当学生查出
        assertThat(studentMapper.findById(1L)).isEmpty();
    }
}
