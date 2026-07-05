package com.campusscore.persistence;

import com.campusscore.domain.Student;
import com.campusscore.persistence.dto.StudentListItem;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 学生持久层。SQL 在 {@code resources/mapper/StudentMapper.xml}。
 *
 * <p>{@code user} 与 {@code student} 通过共享主键关联，实际存在两张表。
 * 新增学生需先 {@link UserMapper#insert} 拿到 id，再 {@link #insertStudentRow(Student)}；
 * 删除学生只需 {@link UserMapper#deleteById(long)}，DB 外键 CASCADE 会级联清理。
 */
@Mapper
public interface StudentMapper {

    /** 分页 + 关键字搜索。 */
    List<StudentListItem> search(
            @Param("keyword") String keyword,
            @Param("offset") int offset,
            @Param("size") int size,
            @Param("orderBy") String orderBy);

    /** 满足 keyword 的总数。 */
    long count(@Param("keyword") String keyword);

    /** 按 id 查完整档案。 */
    Optional<StudentListItem> findById(@Param("id") long id);

    /** 插入 student 行（id 由外层 User#insert 生成后回填）。 */
    int insertStudentRow(Student student);

    /** 更新 student 行的可变字段：tel / address / grade。 */
    int updateStudentRow(Student student);
}
