package com.campusscore.service;

import com.campusscore.common.ErrorCode;
import com.campusscore.common.PageQuery;
import com.campusscore.common.PageResult;
import com.campusscore.common.exception.BusinessException;
import com.campusscore.common.exception.ForbiddenException;
import com.campusscore.common.exception.NotFoundException;
import com.campusscore.domain.Subject;
import com.campusscore.persistence.SubjectMapper;
import com.campusscore.web.dto.CreateSubjectRequest;
import com.campusscore.web.dto.UpdateSubjectRequest;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 教师视角的课程（授课记录）CRUD 服务。见 {@code contracts/api.md §5}。
 */
@Service
@RequiredArgsConstructor
public class TeacherSubjectService {

    private static final Map<String, String> SORT_COLUMNS;

    static {
        Map<String, String> m = new HashMap<>();
        m.put("id", "s.id");
        m.put("name", "s.name");
        m.put("grade", "s.grade");
        SORT_COLUMNS = Collections.unmodifiableMap(m);
    }

    private static final String DEFAULT_ORDER_BY = "s.id ASC";

    private final SubjectMapper subjectMapper;

    // ---------- 查询 ----------

    public PageResult<Subject> listMine(long teacherId, PageQuery query) {
        int page = query.safePage();
        int size = query.safeSize();
        int offset = (page - 1) * size;
        String keyword = query.getKeyword();
        String orderBy = resolveOrderBy(query.getSort());

        List<Subject> rows = subjectMapper.selectMineByTeacher(teacherId, keyword, offset, size, orderBy);
        long total = subjectMapper.countMineByTeacher(teacherId, keyword);
        return PageResult.of(rows, total, page, size);
    }

    // ---------- 新增 ----------

    @Transactional
    public Subject create(long teacherId, CreateSubjectRequest req) {
        assertNameAvailable(teacherId, req.getName(), null);

        Subject s = Subject.builder()
                .teacherId(teacherId)
                .name(req.getName())
                .grade(req.getGrade())
                .build();
        try {
            subjectMapper.insert(s);
        } catch (DuplicateKeyException dup) {
            throw new BusinessException(
                    ErrorCode.SUBJECT_DUPLICATE_FOR_TEACHER,
                    "该教师已存在同名课程", dup);
        }
        return subjectMapper.findById(s.getId())
                .orElseThrow(() -> new NotFoundException("课程刚创建但读不到，数据不一致"));
    }

    // ---------- 修改 ----------

    @Transactional
    public Subject update(long teacherId, long subjectId, UpdateSubjectRequest req) {
        Subject existing = subjectMapper.findById(subjectId)
                .orElseThrow(() -> new NotFoundException("课程不存在"));
        if (!existing.getTeacherId().equals(teacherId)) {
            throw new ForbiddenException("该课程不属于当前教师");
        }
        assertNameAvailable(teacherId, req.getName(), subjectId);

        existing.setName(req.getName());
        existing.setGrade(req.getGrade());
        try {
            subjectMapper.update(existing);
        } catch (DuplicateKeyException dup) {
            throw new BusinessException(
                    ErrorCode.SUBJECT_DUPLICATE_FOR_TEACHER,
                    "该教师已存在同名课程", dup);
        }
        return subjectMapper.findById(subjectId)
                .orElseThrow(() -> new NotFoundException("课程不存在"));
    }

    // ---------- 删除 ----------

    @Transactional
    public void delete(long teacherId, long subjectId) {
        Subject existing = subjectMapper.findById(subjectId)
                .orElseThrow(() -> new NotFoundException("课程不存在"));
        if (!existing.getTeacherId().equals(teacherId)) {
            throw new ForbiddenException("该课程不属于当前教师");
        }
        long scoreCount = subjectMapper.countScoresBySubjectId(subjectId);
        if (scoreCount > 0) {
            throw new BusinessException(
                    ErrorCode.SUBJECT_HAS_SCORES,
                    "该课程存在成绩记录，请先删除相关成绩或联系管理员归档");
        }
        subjectMapper.deleteById(subjectId);
    }

    // ---------- helpers ----------

    private void assertNameAvailable(long teacherId, String name, Long excludingId) {
        long dup;
        if (excludingId == null) {
            dup = subjectMapper.existsByTeacherAndName(teacherId, name);
        } else {
            dup = subjectMapper.existsByTeacherAndNameExcludingId(teacherId, name, excludingId);
        }
        if (dup > 0) {
            throw new BusinessException(
                    ErrorCode.SUBJECT_DUPLICATE_FOR_TEACHER,
                    "该教师已存在同名课程");
        }
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
