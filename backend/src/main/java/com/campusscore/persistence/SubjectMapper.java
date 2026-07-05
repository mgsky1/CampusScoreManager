package com.campusscore.persistence;

import com.campusscore.domain.Subject;
import com.campusscore.persistence.dto.SubjectEntryOption;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 课程（授课记录）持久层。SQL 在 {@code resources/mapper/SubjectMapper.xml}。
 */
@Mapper
public interface SubjectMapper {

    // ---------- 已有（US2 / US3 依赖）----------

    /** 查该教师、该学生年级下、该学生尚未录入过分数的课程列表。 */
    List<SubjectEntryOption> findEntryOptions(
            @Param("teacherId") long teacherId,
            @Param("studentGrade") int studentGrade,
            @Param("studentId") long studentId);

    /** 该教师在指定年级是否存在任一课程（用于 allEntered 判定）。 */
    boolean hasAnyByTeacherAndGrade(
            @Param("teacherId") long teacherId, @Param("grade") int grade);

    /** 按 id 查课程 —— 仅当归属该教师才返回。 */
    Optional<Subject> findByIdOwnedByTeacher(
            @Param("id") long id, @Param("teacherId") long teacherId);

    /** 简单按 id 查，不做归属校验。 */
    Optional<Subject> findById(@Param("id") long id);

    // ---------- US4 · CRUD (T130) ----------

    /** 分页查询本教师授课记录（按 keyword LIKE，name 或 id 排序）。 */
    List<Subject> selectMineByTeacher(
            @Param("teacherId") long teacherId,
            @Param("keyword") String keyword,
            @Param("offset") int offset,
            @Param("size") int size,
            @Param("orderBy") String orderBy);

    /** 计数（与 {@link #selectMineByTeacher} 的过滤条件一致）。 */
    long countMineByTeacher(
            @Param("teacherId") long teacherId,
            @Param("keyword") String keyword);

    /** 新增授课记录；useGeneratedKeys 回填 {@code id}。 */
    int insert(Subject subject);

    /** 修改 name/grade（仅这两列可改）。 */
    int update(Subject subject);

    /**
     * 该教师是否已有同名（大小写不敏感）课程 —— 用于新增前置探测。
     * <p>返回统计数量（0 表示可用）。
     */
    long existsByTeacherAndName(
            @Param("teacherId") long teacherId,
            @Param("name") String name);

    /** 该教师是否已有同名课程（大小写不敏感），排除给定 id —— 用于更新前置探测。 */
    long existsByTeacherAndNameExcludingId(
            @Param("teacherId") long teacherId,
            @Param("name") String name,
            @Param("excludingId") long excludingId);

    /** 按 id 删除；<b>调用方必须先做归属 / 有无成绩校验</b>。 */
    int deleteById(@Param("id") long id);

    /** 该课程当前被引用的成绩条数（用于删除前置探测）。 */
    long countScoresBySubjectId(@Param("subjectId") long subjectId);
}
