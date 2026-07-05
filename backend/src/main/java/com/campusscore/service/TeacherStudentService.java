package com.campusscore.service;

import com.campusscore.common.ErrorCode;
import com.campusscore.common.PageQuery;
import com.campusscore.common.PageResult;
import com.campusscore.common.exception.BusinessException;
import com.campusscore.common.exception.NotFoundException;
import com.campusscore.domain.Role;
import com.campusscore.domain.Student;
import com.campusscore.domain.User;
import com.campusscore.persistence.StudentMapper;
import com.campusscore.persistence.UserMapper;
import com.campusscore.persistence.dto.StudentListItem;
import com.campusscore.web.dto.CreateStudentRequest;
import com.campusscore.web.dto.UpdateStudentRequest;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 教师视角的学生管理服务（{@code search / findById} + {@code create / update / delete}）。
 */
@Service
@RequiredArgsConstructor
public class TeacherStudentService {

    private static final Map<String, String> SORT_COLUMNS;

    static {
        Map<String, String> m = new HashMap<>();
        m.put("real_name", "u.real_name");
        m.put("grade", "st.grade");
        m.put("id", "u.id");
        SORT_COLUMNS = Collections.unmodifiableMap(m);
    }

    private static final String DEFAULT_ORDER_BY = "u.id ASC";

    private final StudentMapper studentMapper;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    // ---------- 查询 ----------

    public PageResult<StudentListItem> search(PageQuery query) {
        int page = query.safePage();
        int size = query.safeSize();
        int offset = (page - 1) * size;
        String keyword = query.safeKeyword();
        String orderBy = resolveOrderBy(query.getSort());

        List<StudentListItem> rows = studentMapper.search(keyword, offset, size, orderBy);
        long total = studentMapper.count(keyword);
        return PageResult.of(rows, total, page, size);
    }

    public StudentListItem findById(long id) {
        return studentMapper.findById(id)
                .orElseThrow(() -> new NotFoundException("学生不存在"));
    }

    // ---------- 新增 ----------

    @Transactional
    public StudentListItem create(CreateStudentRequest req) {
        assertLoginNameAvailable(req.getLoginName(), null);

        User user = User.builder()
                .loginName(req.getLoginName())
                .realName(req.getRealName())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .role(Role.STUDENT)
                .build();
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException dup) {
            throw new BusinessException(
                    ErrorCode.LOGIN_NAME_TAKEN, "登录名已被使用", dup);
        }

        studentMapper.insertStudentRow(Student.builder()
                .id(user.getId())
                .tel(req.getTel())
                .address(req.getAddress())
                .grade(req.getGrade())
                .build());
        return findById(user.getId());
    }

    // ---------- 修改 ----------

    @Transactional
    public StudentListItem update(long id, UpdateStudentRequest req) {
        // 必须先存在
        User existing = userMapper.findById(id)
                .orElseThrow(() -> new NotFoundException("学生不存在"));
        if (existing.getRole() != Role.STUDENT) {
            throw new NotFoundException("学生不存在");
        }
        assertLoginNameAvailable(req.getLoginName(), id);

        String hash = null;
        if (req.getPassword() != null && !req.getPassword().isEmpty()) {
            hash = passwordEncoder.encode(req.getPassword());
        }
        try {
            userMapper.updateBasic(User.builder()
                    .id(id)
                    .loginName(req.getLoginName())
                    .realName(req.getRealName())
                    .passwordHash(hash)
                    .build());
        } catch (DuplicateKeyException dup) {
            throw new BusinessException(
                    ErrorCode.LOGIN_NAME_TAKEN, "登录名已被使用", dup);
        }
        studentMapper.updateStudentRow(Student.builder()
                .id(id).tel(req.getTel()).address(req.getAddress()).grade(req.getGrade())
                .build());
        return findById(id);
    }

    // ---------- 删除 ----------

    @Transactional
    public void delete(long id) {
        User existing = userMapper.findById(id)
                .orElseThrow(() -> new NotFoundException("学生不存在"));
        if (existing.getRole() != Role.STUDENT) {
            throw new NotFoundException("学生不存在");
        }
        // DB 外键 CASCADE 会自动清理 student / score
        userMapper.deleteById(id);
    }

    // ---------- helpers ----------

    private void assertLoginNameAvailable(String loginName, Long excludingId) {
        long dup;
        if (excludingId == null) {
            dup = userMapper.countByLoginNameIgnoreCase(loginName);
        } else {
            dup = userMapper.countByLoginNameIgnoreCaseExcludingId(loginName, excludingId);
        }
        if (dup > 0) {
            throw new BusinessException(ErrorCode.LOGIN_NAME_TAKEN, "登录名已被使用");
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
