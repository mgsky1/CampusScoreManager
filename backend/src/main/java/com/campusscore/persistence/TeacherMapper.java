package com.campusscore.persistence;

import com.campusscore.domain.Teacher;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 教师扩展信息持久层。SQL 在 {@code resources/mapper/TeacherMapper.xml}。
 * <p>与 {@code user} 表共享主键；tel 字段可空。
 */
@Mapper
public interface TeacherMapper {

    /** 按 id 查教师扩展行。 */
    Optional<Teacher> findById(@Param("id") long id);

    /** 插入 teacher 行（id 由外层 User#insert 生成）。 */
    int insertTeacherRow(Teacher teacher);

    /** 更新 teacher 行的 tel（唯一可自助修改字段）。 */
    int updateTeacherRow(Teacher teacher);
}
