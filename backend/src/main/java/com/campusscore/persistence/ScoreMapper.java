package com.campusscore.persistence;

import com.campusscore.domain.Score;
import com.campusscore.persistence.dto.ScoreListItem;
import com.campusscore.persistence.dto.TeacherScoreItem;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 成绩持久层。SQL 定义在 {@code resources/mapper/ScoreMapper.xml}。
 */
@Mapper
public interface ScoreMapper {

    // ---------- 学生视角（US1）----------

    List<ScoreListItem> selectByStudentId(
            @Param("studentId") long studentId,
            @Param("offset") int offset,
            @Param("size") int size,
            @Param("orderBy") String orderBy);

    long countByStudentId(@Param("studentId") long studentId);

    // ---------- 教师视角（US2）----------

    /**
     * 该学生的全部成绩，附带 {@code editable} 计算列
     * （= {@code subject.teacher_id = #{teacherId}}）。
     */
    List<TeacherScoreItem> selectByStudentIdForTeacher(
            @Param("studentId") long studentId,
            @Param("teacherId") long teacherId,
            @Param("offset") int offset,
            @Param("size") int size,
            @Param("orderBy") String orderBy);

    long countByStudentIdForTeacher(@Param("studentId") long studentId);

    /** 学生 × 课程唯一约束前置探测（配合 DuplicateKeyException 兜底）。 */
    boolean existsByStudentAndSubject(
            @Param("studentId") long studentId, @Param("subjectId") long subjectId);

    /** 插入。返回 id 通过 {@code useGeneratedKeys} 回填到实体上。 */
    int insert(Score score);

    /** 仅更新 score 字段。返回受影响行数。 */
    int updateScore(@Param("id") long id, @Param("score") int score);

    Optional<Score> findById(@Param("id") long id);

    int deleteById(@Param("id") long id);
}
