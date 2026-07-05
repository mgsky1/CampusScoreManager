package com.campusscore.service;

import com.campusscore.common.ErrorCode;
import com.campusscore.common.PageQuery;
import com.campusscore.common.PageResult;
import com.campusscore.common.exception.BusinessException;
import com.campusscore.common.exception.NotFoundException;
import com.campusscore.domain.Score;
import com.campusscore.domain.Subject;
import com.campusscore.persistence.ScoreMapper;
import com.campusscore.persistence.StudentMapper;
import com.campusscore.persistence.SubjectMapper;
import com.campusscore.persistence.dto.StudentListItem;
import com.campusscore.persistence.dto.TeacherScoreItem;
import com.campusscore.web.dto.EntryOptionResponse;
import com.campusscore.web.dto.EntryOptionsResponse;
import com.campusscore.web.dto.StudentBriefResponse;
import com.campusscore.web.dto.TeacherScoreItemResponse;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 教师视角的成绩服务：查看学生成绩、查候选、录入、修改、删除。
 * 归属校验通过 {@link SubjectMapper#findByIdOwnedByTeacher} 完成。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TeacherScoreService {

    private static final int MIN_SCORE = 0;
    private static final int MAX_SCORE = 100;

    private static final Map<String, String> SORT_COLUMNS;

    static {
        Map<String, String> m = new HashMap<>();
        m.put("grade", "s.grade");
        m.put("score", "s.score");
        m.put("subject_name", "sub.name");
        m.put("updated_at", "s.updated_at");
        SORT_COLUMNS = Collections.unmodifiableMap(m);
    }

    private static final String DEFAULT_ORDER_BY = "s.grade DESC, s.updated_at DESC";

    private final ScoreMapper scoreMapper;
    private final SubjectMapper subjectMapper;
    private final StudentMapper studentMapper;

    // ---------- 查看 ----------

    public PageResult<TeacherScoreItemResponse> listScoresForStudent(
            long currentTeacherId, long studentId, PageQuery query) {
        studentMapper.findById(studentId)
                .orElseThrow(() -> new NotFoundException("学生不存在"));

        int page = query.safePage();
        int size = query.safeSize();
        int offset = (page - 1) * size;
        String orderBy = resolveOrderBy(query.getSort());

        List<TeacherScoreItem> rows = scoreMapper.selectByStudentIdForTeacher(
                studentId, currentTeacherId, offset, size, orderBy);
        long total = scoreMapper.countByStudentIdForTeacher(studentId);
        List<TeacherScoreItemResponse> items = rows.stream()
                .map(TeacherScoreItemResponse::from).collect(Collectors.toList());
        return PageResult.of(items, total, page, size);
    }

    public EntryOptionsResponse getEntryOptions(long currentTeacherId, long studentId) {
        StudentListItem stu = studentMapper.findById(studentId)
                .orElseThrow(() -> new NotFoundException("学生不存在"));
        List<EntryOptionResponse> options = subjectMapper.findEntryOptions(
                        currentTeacherId, stu.getGrade(), studentId)
                .stream().map(EntryOptionResponse::from).collect(Collectors.toList());
        boolean hasCoverage = subjectMapper.hasAnyByTeacherAndGrade(currentTeacherId, stu.getGrade());
        boolean allEntered = options.isEmpty() && hasCoverage;
        return EntryOptionsResponse.builder()
                .student(StudentBriefResponse.builder()
                        .id(stu.getId()).realName(stu.getRealName()).grade(stu.getGrade())
                        .build())
                .options(options)
                .allEntered(allEntered)
                .build();
    }

    // ---------- 录入 ----------

    @Transactional
    public TeacherScoreItemResponse createScore(
            long currentTeacherId, long studentId, long subjectId, int score) {
        assertScoreInRange(score);

        StudentListItem stu = studentMapper.findById(studentId)
                .orElseThrow(() -> new NotFoundException("学生不存在"));

        Subject sub = subjectMapper.findByIdOwnedByTeacher(subjectId, currentTeacherId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.SCORE_SUBJECT_NOT_OWNED, "目标课程不在您的授课表中"));

        if (!sub.getGrade().equals(stu.getGrade())) {
            throw new BusinessException(
                    ErrorCode.SCORE_SUBJECT_NOT_OWNED, "课程年级与学生年级不匹配");
        }

        if (scoreMapper.existsByStudentAndSubject(studentId, subjectId)) {
            // 若前端跳过 6.2 直接提交而学生实际已录完：区分 2302/2303
            if (isAllEnteredForTeacher(currentTeacherId, stu)) {
                throw new BusinessException(
                        ErrorCode.SCORE_ALL_ENROLLED, "该学生本教师课程已全部录入完毕");
            }
            throw new BusinessException(
                    ErrorCode.SCORE_ALREADY_EXISTS, "该学生的该门课程成绩已存在");
        }

        Score entity = Score.builder()
                .studentId(studentId)
                .subjectId(subjectId)
                .teacherId(currentTeacherId)
                .score(score)
                .grade(stu.getGrade())
                .build();
        try {
            scoreMapper.insert(entity);
        } catch (DuplicateKeyException dup) {
            throw new BusinessException(
                    ErrorCode.SCORE_ALREADY_EXISTS, "该学生的该门课程成绩已存在", dup);
        }
        return findAsResponse(currentTeacherId, studentId, entity.getId());
    }

    // ---------- 修改 / 删除 ----------

    @Transactional
    public TeacherScoreItemResponse updateScore(
            long currentTeacherId, long scoreId, int score) {
        assertScoreInRange(score);
        Score existing = scoreMapper.findById(scoreId)
                .orElseThrow(() -> new NotFoundException("成绩不存在"));

        subjectMapper.findByIdOwnedByTeacher(existing.getSubjectId(), currentTeacherId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.SCORE_SUBJECT_NOT_OWNED, "该成绩对应的课程不在您的授课表中"));

        scoreMapper.updateScore(scoreId, score);
        return findAsResponse(currentTeacherId, existing.getStudentId(), scoreId);
    }

    @Transactional
    public void deleteScore(long currentTeacherId, long scoreId) {
        Score existing = scoreMapper.findById(scoreId)
                .orElseThrow(() -> new NotFoundException("成绩不存在"));
        subjectMapper.findByIdOwnedByTeacher(existing.getSubjectId(), currentTeacherId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.SCORE_SUBJECT_NOT_OWNED, "该成绩对应的课程不在您的授课表中"));
        scoreMapper.deleteById(scoreId);
    }

    // ---------- helpers ----------

    private void assertScoreInRange(int score) {
        if (score < MIN_SCORE || score > MAX_SCORE) {
            throw new BusinessException(ErrorCode.SCORE_OUT_OF_RANGE, "成绩必须在 0-100 之间");
        }
    }

    private boolean isAllEnteredForTeacher(long teacherId, StudentListItem stu) {
        boolean coverage = subjectMapper.hasAnyByTeacherAndGrade(teacherId, stu.getGrade());
        if (!coverage) return false;
        return subjectMapper.findEntryOptions(teacherId, stu.getGrade(), stu.getId()).isEmpty();
    }

    /** 复用列表查询以拼装单条响应（包含 editable / joined 字段）。 */
    private TeacherScoreItemResponse findAsResponse(
            long currentTeacherId, long studentId, long scoreId) {
        List<TeacherScoreItem> rows = scoreMapper.selectByStudentIdForTeacher(
                studentId, currentTeacherId, 0, 100, null);
        return rows.stream()
                .filter(r -> r.getId() != null && r.getId() == scoreId)
                .findFirst()
                .map(TeacherScoreItemResponse::from)
                .orElseThrow(() -> new NotFoundException("成绩不存在"));
    }

    private String resolveOrderBy(String sort) {
        if (sort == null || sort.isEmpty()) return DEFAULT_ORDER_BY;
        String normalized = sort.replace(',', ':');
        Set<String> whitelist = SORT_COLUMNS.keySet();
        PageQuery pq = new PageQuery();
        pq.setSort(normalized);
        String parsed = pq.safeSort(whitelist);
        if (parsed == null) return DEFAULT_ORDER_BY;
        String[] parts = parsed.split(" ");
        String col = SORT_COLUMNS.get(parts[0]);
        return col + " " + parts[1];
    }
}
