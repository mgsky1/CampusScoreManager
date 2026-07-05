package com.campusscore.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import com.campusscore.common.PageQuery;
import com.campusscore.common.PageResult;
import com.campusscore.persistence.ScoreMapper;
import com.campusscore.persistence.dto.ScoreListItem;
import com.campusscore.web.dto.ScoreItemResponse;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** StudentScoreService 单元测试 (T059)。 */
@ExtendWith(MockitoExtension.class)
class StudentScoreServiceTest {

    @Mock ScoreMapper scoreMapper;

    @InjectMocks StudentScoreService service;

    private ScoreListItem row(long id, int score) {
        return ScoreListItem.builder()
                .id(id)
                .subjectId(5L)
                .subjectName("Java EE")
                .teacherId(2L)
                .teacherName("伍老师")
                .grade(3)
                .score(score)
                .updatedAt(LocalDateTime.of(2026, 7, 4, 12, 0))
                .build();
    }

    @Test
    void listMyScoresReturnsMappedPageResult() {
        when(scoreMapper.selectByStudentId(eq(3L), eq(0), eq(50), anyString()))
                .thenReturn(Arrays.asList(row(11L, 99), row(12L, 55)));
        when(scoreMapper.countByStudentId(3L)).thenReturn(2L);

        PageQuery q = PageQuery.builder().page(1).size(50).sort("grade:desc").build();
        PageResult<ScoreItemResponse> page = service.listMyScores(3L, q);

        assertThat(page.getTotal()).isEqualTo(2L);
        assertThat(page.getPage()).isEqualTo(1);
        assertThat(page.getSize()).isEqualTo(50);
        assertThat(page.isHasNext()).isFalse();
        assertThat(page.getItems()).hasSize(2);
        ScoreItemResponse a = page.getItems().get(0);
        assertThat(a.getScore()).isEqualTo(99);
        assertThat(a.isFailing()).isFalse();
        assertThat(a.getSubjectName()).isEqualTo("Java EE");
        assertThat(a.getTeacherName()).isEqualTo("伍老师");
        ScoreItemResponse b = page.getItems().get(1);
        assertThat(b.getScore()).isEqualTo(55);
        assertThat(b.isFailing()).isTrue();
    }

    @Test
    void listMyScoresClampsOversizedPageSize() {
        when(scoreMapper.selectByStudentId(anyLong(), anyInt(), anyInt(), anyString()))
                .thenReturn(Collections.emptyList());
        when(scoreMapper.countByStudentId(anyLong())).thenReturn(0L);

        PageQuery q = PageQuery.builder().page(1).size(200).build();
        PageResult<ScoreItemResponse> page = service.listMyScores(3L, q);
        assertThat(page.getSize()).isEqualTo(100);

        ArgumentCaptor<Integer> sizeCap = ArgumentCaptor.forClass(Integer.class);
        org.mockito.Mockito.verify(scoreMapper)
                .selectByStudentId(eq(3L), eq(0), sizeCap.capture(), anyString());
        assertThat(sizeCap.getValue()).isEqualTo(100);
    }

    @Test
    void listMyScoresFallsBackToDefaultOrderWhenInvalidSort() {
        when(scoreMapper.selectByStudentId(anyLong(), anyInt(), anyInt(), anyString()))
                .thenReturn(Collections.emptyList());
        when(scoreMapper.countByStudentId(anyLong())).thenReturn(0L);

        PageQuery q = PageQuery.builder().page(1).size(20).sort("password:asc").build();
        service.listMyScores(3L, q);

        ArgumentCaptor<String> orderCap = ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(scoreMapper)
                .selectByStudentId(eq(3L), eq(0), eq(20), orderCap.capture());
        // 非法 sort 时回退到 service 的默认排序表达式
        assertThat(orderCap.getValue()).contains("grade");
    }
}
