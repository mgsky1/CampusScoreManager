package com.campusscore.service;

import com.campusscore.common.PageQuery;
import com.campusscore.common.PageResult;
import com.campusscore.persistence.ScoreMapper;
import com.campusscore.persistence.dto.ScoreListItem;
import com.campusscore.web.dto.ScoreItemResponse;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 学生视角的成绩服务。 */
@Service
@RequiredArgsConstructor
public class StudentScoreService {

    /** 排序字段白名单 → SQL 列（带表前缀）。 */
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

    /** 我的成绩分页查询。 */
    public PageResult<ScoreItemResponse> listMyScores(long studentId, PageQuery query) {
        int page = query.safePage();
        int size = query.safeSize();
        int offset = (page - 1) * size;
        String orderBy = resolveOrderBy(query.getSort());

        List<ScoreListItem> rows = scoreMapper.selectByStudentId(studentId, offset, size, orderBy);
        long total = scoreMapper.countByStudentId(studentId);

        List<ScoreItemResponse> items =
                rows.stream().map(ScoreItemResponse::from).collect(Collectors.toList());
        return PageResult.of(items, total, page, size);
    }

    /** 支持 {@code grade,desc} 与 {@code grade:desc} 两种前端写法。 */
    private String resolveOrderBy(String sort) {
        if (sort == null || sort.isEmpty()) {
            return DEFAULT_ORDER_BY;
        }
        String normalized = sort.replace(',', ':');
        Set<String> whitelist = SORT_COLUMNS.keySet();
        PageQuery pq = new PageQuery();
        pq.setSort(normalized);
        String parsed = pq.safeSort(whitelist);
        if (parsed == null) {
            return DEFAULT_ORDER_BY;
        }
        // parsed 形如 "grade DESC"; 换成带表前缀的实际列
        String[] parts = parsed.split(" ");
        String col = SORT_COLUMNS.get(parts[0]);
        return col + " " + parts[1];
    }
}
